package com.restfulbooker.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced AI Failure Analyzer Plugin with retry awareness.
 * Only reports final results - hides failures that eventually passed after retry.
 */
public class AIFailureAnalyzerPluginWithRetry implements ConcurrentEventListener {

    private final String outputPath;
    private final Map<String, FeatureResult> featureResults = new ConcurrentHashMap<>();
    private final Map<String, ScenarioResult> scenarioResults = new ConcurrentHashMap<>();
    private final Map<String, TestCase> testCaseMap = new ConcurrentHashMap<>();
    private final TestRunMetadata metadata = new TestRunMetadata();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Retry tracking
    private Map<String, RetryInfo> retryInfo = new HashMap<>();
    private final boolean filterRetryFailures;

    public AIFailureAnalyzerPluginWithRetry(String outputPath) {
        this(outputPath, true); // Default: filter out retry failures
    }

    public AIFailureAnalyzerPluginWithRetry(String outputPath, boolean filterRetryFailures) {
        this.outputPath = outputPath;
        this.filterRetryFailures = filterRetryFailures;
        metadata.startTime = Instant.now().toString();
        metadata.environment = System.getProperty("environment", "default");
        metadata.buildNumber = System.getenv("BUILD_NUMBER");
        metadata.jenkinsBuildUrl = System.getenv("BUILD_URL");
        metadata.branch = System.getenv("GIT_BRANCH");
        metadata.commit = System.getenv("GIT_COMMIT");
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class, this::handleTestRunStarted);
        publisher.registerHandlerFor(TestRunFinished.class, this::handleTestRunFinished);
        publisher.registerHandlerFor(TestCaseStarted.class, this::handleTestCaseStarted);
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
        publisher.registerHandlerFor(TestStepStarted.class, this::handleTestStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::handleTestStepFinished);
    }

    private void handleTestRunStarted(TestRunStarted event) {
        metadata.startTime = event.getInstant().toString();
    }

    private void handleTestRunFinished(TestRunFinished event) {
        metadata.endTime = event.getInstant().toString();
        metadata.duration = event.getResult().getDuration().toMillis();

        // Load retry information if available
        loadRetryInfo();

        generateReport();
    }

    private void handleTestCaseStarted(TestCaseStarted event) {
        TestCase testCase = event.getTestCase();
        String scenarioId = generateScenarioId(testCase);

        testCaseMap.put(scenarioId, testCase);

        ScenarioResult scenario = new ScenarioResult();
        scenario.scenarioName = testCase.getName();
        scenario.featureName = testCase.getUri().toString();
        scenario.line = testCase.getLocation().getLine();
        scenario.tags = new ArrayList<>(testCase.getTags());
        scenario.startTime = event.getInstant().toString();
        scenario.scenarioId = scenarioId;

        scenarioResults.put(scenarioId, scenario);
    }

    private void handleTestCaseFinished(TestCaseFinished event) {
        TestCase testCase = event.getTestCase();
        String scenarioId = generateScenarioId(testCase);
        ScenarioResult scenario = scenarioResults.get(scenarioId);

        if (scenario != null) {
            Result result = event.getResult();
            scenario.status = result.getStatus().toString();
            scenario.endTime = event.getInstant().toString();
            scenario.duration = result.getDuration().toMillis();

            if (result.getError() != null) {
                scenario.errorMessage = result.getError().getMessage();
                scenario.stackTrace = getStackTrace(result.getError());
                scenario.errorType = result.getError().getClass().getName();
            }

            updateFeatureResult(testCase, scenario);
        }

        testCaseMap.remove(scenarioId);
    }

    private void handleTestStepStarted(TestStepStarted event) {
        TestStep testStep = event.getTestStep();

        if (testStep instanceof PickleStepTestStep) {
            PickleStepTestStep step = (PickleStepTestStep) testStep;
            ScenarioResult currentScenario = findCurrentScenario(step);

            if (currentScenario != null) {
                StepResult stepResult = new StepResult();
                stepResult.keyword = step.getStep().getKeyword();
                stepResult.text = step.getStep().getText();
                stepResult.line = step.getStep().getLine();
                stepResult.startTime = event.getInstant().toString();

                currentScenario.steps.add(stepResult);
            }
        }
    }

    private void handleTestStepFinished(TestStepFinished event) {
        TestStep testStep = event.getTestStep();

        if (testStep instanceof PickleStepTestStep) {
            PickleStepTestStep step = (PickleStepTestStep) testStep;
            ScenarioResult currentScenario = findCurrentScenario(step);

            if (currentScenario != null && !currentScenario.steps.isEmpty()) {
                StepResult stepResult = currentScenario.steps.get(currentScenario.steps.size() - 1);
                Result result = event.getResult();

                stepResult.status = result.getStatus().toString();
                stepResult.endTime = event.getInstant().toString();
                stepResult.duration = result.getDuration().toMillis();

                if (result.getError() != null) {
                    stepResult.errorMessage = result.getError().getMessage();
                    stepResult.stackTrace = getStackTrace(result.getError());
                    stepResult.errorType = result.getError().getClass().getName();

                    if (currentScenario.failingStep == null) {
                        currentScenario.failingStep = stepResult;
                    }
                }
            }
        }
    }

    private ScenarioResult findCurrentScenario(PickleStepTestStep step) {
        String stepUri = step.getUri().toString();
        int stepLine = step.getStep().getLine();

        for (Map.Entry<String, ScenarioResult> entry : scenarioResults.entrySet()) {
            String scenarioId = entry.getKey();
            ScenarioResult scenario = entry.getValue();
            TestCase testCase = testCaseMap.get(scenarioId);

            if (testCase != null) {
                if (testCase.getUri().toString().equals(stepUri)) {
                    if (!scenario.steps.isEmpty() || (stepLine >= scenario.line)) {
                        return scenario;
                    }
                }
            }
        }

        ScenarioResult lastScenario = null;
        for (ScenarioResult scenario : scenarioResults.values()) {
            if (scenario.endTime == null) {
                if (lastScenario == null ||
                        scenario.startTime.compareTo(lastScenario.startTime) > 0) {
                    lastScenario = scenario;
                }
            }
        }

        return lastScenario;
    }

    private void updateFeatureResult(TestCase testCase, ScenarioResult scenario) {
        String featureName = extractFeatureName(testCase.getUri().toString());
        FeatureResult feature = featureResults.computeIfAbsent(featureName, k -> new FeatureResult(featureName));

        feature.totalScenarios++;

        switch (Status.valueOf(scenario.status)) {
            case PASSED:
                feature.passedScenarios++;
                break;
            case FAILED:
                feature.failedScenarios++;
                feature.failures.add(scenario);
                break;
            case SKIPPED:
                feature.skippedScenarios++;
                feature.skipped.add(scenario);
                break;
            case PENDING:
            case UNDEFINED:
                feature.undefinedScenarios++;
                feature.undefined.add(scenario);
                break;
            case AMBIGUOUS:
            case UNUSED:
                break;
        }
    }

    private void loadRetryInfo() {
        String retrySummaryPath = outputPath.replace("ai-failures.json", "rerun-summary.json");

        try {
            if (Files.exists(Paths.get(retrySummaryPath))) {
                try (FileReader reader = new FileReader(retrySummaryPath)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> summary = gson.fromJson(reader, Map.class);

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> scenarios = (List<Map<String, Object>>) summary.get("scenarios");

                    if (scenarios != null) {
                        for (Map<String, Object> scenarioData : scenarios) {
                            String location = (String) scenarioData.get("location");
                            RetryInfo info = new RetryInfo();
                            info.totalAttempts = ((Double) scenarioData.get("totalAttempts")).intValue();
                            info.failedAttempts = ((Double) scenarioData.get("failedAttempts")).intValue();
                            Object passedOn = scenarioData.get("passedOnAttempt");
                            info.passedOnAttempt = passedOn != null ? ((Double) passedOn).intValue() : null;
                            info.finalStatus = (String) scenarioData.get("finalStatus");

                            retryInfo.put(location, info);
                        }
                    }
                }
                System.out.println("Loaded retry information from: " + retrySummaryPath);
            }
        } catch (Exception e) {
            System.err.println("Failed to load retry information: " + e.getMessage());
        }
    }

    private void generateReport() {
        AIFailureReport report = new AIFailureReport();
        report.metadata = metadata;
        report.features = new ArrayList<>();

        // Apply retry filtering if enabled
        for (FeatureResult feature : featureResults.values()) {
            FeatureResult filteredFeature = filterFeatureResults(feature);
            report.features.add(filteredFeature);
        }

        // Calculate summary statistics
        report.summary = new TestSummary();
        for (FeatureResult feature : report.features) {
            report.summary.totalScenarios += feature.totalScenarios;
            report.summary.passedScenarios += feature.passedScenarios;
            report.summary.failedScenarios += feature.failedScenarios;
            report.summary.skippedScenarios += feature.skippedScenarios;
            report.summary.undefinedScenarios += feature.undefinedScenarios;
        }

        report.summary.totalFeatures = report.features.size();
        report.summary.passRate = report.summary.totalScenarios > 0
                ? (double) report.summary.passedScenarios / report.summary.totalScenarios * 100
                : 0.0;

        // Add retry metadata
        report.retryMetadata = new RetryMetadata();
        report.retryMetadata.retryEnabled = !retryInfo.isEmpty();
        report.retryMetadata.filterRetryFailures = filterRetryFailures;
        report.retryMetadata.scenariosPassedAfterRetry = countPassedAfterRetry();
        report.retryMetadata.scenariosFailedAllRetries = countFailedAllRetries();

        // Write to file
        try {
            Files.createDirectories(Paths.get(outputPath).getParent());
            try (FileWriter writer = new FileWriter(outputPath)) {
                gson.toJson(report, writer);
            }
            System.out.println("AI Failure Report (with retry filtering) generated at: " + outputPath);
        } catch (IOException e) {
            System.err.println("Failed to write AI Failure Report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private FeatureResult filterFeatureResults(FeatureResult original) {
        if (!filterRetryFailures || retryInfo.isEmpty()) {
            return original;
        }

        FeatureResult filtered = new FeatureResult(original.featureName);
        filtered.totalScenarios = original.totalScenarios;
        filtered.passedScenarios = original.passedScenarios;
        filtered.skippedScenarios = original.skippedScenarios;
        filtered.undefinedScenarios = original.undefinedScenarios;

        // Filter failures: only include scenarios that failed ALL retry attempts
        for (ScenarioResult scenario : original.failures) {
            String scenarioLocation = scenario.featureName + ":" + scenario.line;
            RetryInfo retry = retryInfo.get(scenarioLocation);

            if (retry != null && "PASSED".equals(retry.finalStatus)) {
                // This scenario eventually passed - convert it to passed
                filtered.passedScenarios++;
                filtered.failedScenarios--;

                // Add retry annotation to scenario
                scenario.retryInfo = new ScenarioRetryInfo();
                scenario.retryInfo.passedAfterRetry = true;
                scenario.retryInfo.passedOnAttempt = retry.passedOnAttempt;
                scenario.retryInfo.totalAttempts = retry.totalAttempts;
            } else {
                // Still failed after all retries - include in failures
                filtered.failures.add(scenario);

                if (retry != null) {
                    scenario.retryInfo = new ScenarioRetryInfo();
                    scenario.retryInfo.passedAfterRetry = false;
                    scenario.retryInfo.totalAttempts = retry.totalAttempts;
                    scenario.retryInfo.failedAttempts = retry.failedAttempts;
                }
            }
        }

        filtered.failedScenarios = filtered.failures.size();

        return filtered;
    }

    private int countPassedAfterRetry() {
        return (int) retryInfo.values().stream()
                .filter(info -> "PASSED".equals(info.finalStatus) && info.passedOnAttempt != null && info.passedOnAttempt > 1)
                .count();
    }

    private int countFailedAllRetries() {
        return (int) retryInfo.values().stream()
                .filter(info -> "FAILED".equals(info.finalStatus))
                .count();
    }

    private String generateScenarioId(TestCase testCase) {
        return testCase.getUri().toString() + ":" + testCase.getLocation().getLine();
    }

    private String extractFeatureName(String uri) {
        String[] parts = uri.split("/");
        return parts[parts.length - 1].replace(".feature", "");
    }

    private String getStackTrace(Throwable error) {
        if (error == null) return null;

        StringBuilder sb = new StringBuilder();
        sb.append(error.toString()).append("\n");
        for (StackTraceElement element : error.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }

        if (error.getCause() != null) {
            sb.append("Caused by: ").append(getStackTrace(error.getCause()));
        }

        return sb.toString();
    }

    // Data Classes
    static class AIFailureReport {
        TestRunMetadata metadata;
        TestSummary summary;
        List<FeatureResult> features;
        RetryMetadata retryMetadata;
    }

    static class TestRunMetadata {
        String startTime;
        String endTime;
        Long duration;
        String environment;
        String buildNumber;
        String jenkinsBuildUrl;
        String branch;
        String commit;
        String hostname = getHostname();
        String javaVersion = System.getProperty("java.version");
        String os = System.getProperty("os.name");

        private static String getHostname() {
            try {
                return java.net.InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                return "unknown";
            }
        }
    }

    static class TestSummary {
        int totalFeatures;
        int totalScenarios;
        int passedScenarios;
        int failedScenarios;
        int skippedScenarios;
        int undefinedScenarios;
        double passRate;
    }

    static class RetryMetadata {
        boolean retryEnabled;
        boolean filterRetryFailures;
        int scenariosPassedAfterRetry;
        int scenariosFailedAllRetries;
    }

    static class FeatureResult {
        String featureName;
        int totalScenarios;
        int passedScenarios;
        int failedScenarios;
        int skippedScenarios;
        int undefinedScenarios;
        List<ScenarioResult> failures = new ArrayList<>();
        List<ScenarioResult> skipped = new ArrayList<>();
        List<ScenarioResult> undefined = new ArrayList<>();

        FeatureResult(String name) {
            this.featureName = name;
        }
    }

    static class ScenarioResult {
        String scenarioName;
        String featureName;
        String scenarioId;
        int line;
        List<String> tags = new ArrayList<>();
        String status;
        String startTime;
        String endTime;
        Long duration;
        String errorMessage;
        String errorType;
        String stackTrace;
        List<StepResult> steps = new ArrayList<>();
        StepResult failingStep;
        ScenarioRetryInfo retryInfo;
    }

    static class ScenarioRetryInfo {
        boolean passedAfterRetry;
        Integer passedOnAttempt;
        Integer totalAttempts;
        Integer failedAttempts;
    }

    static class StepResult {
        String keyword;
        String text;
        int line;
        String status;
        String startTime;
        String endTime;
        Long duration;
        String errorMessage;
        String errorType;
        String stackTrace;
    }

    static class RetryInfo {
        int totalAttempts;
        int failedAttempts;
        Integer passedOnAttempt;
        String finalStatus;
    }
}