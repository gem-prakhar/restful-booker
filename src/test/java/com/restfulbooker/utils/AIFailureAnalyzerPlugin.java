package com.restfulbooker.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom Cucumber plugin that generates AI-consumable JSON reports
 * focused on test failures and execution context.
 */
public class AIFailureAnalyzerPlugin implements ConcurrentEventListener {

    private final String outputPath;
    private final Map<String, FeatureResult> featureResults = new ConcurrentHashMap<>();
    private final Map<String, ScenarioResult> scenarioResults = new ConcurrentHashMap<>();
    private final TestRunMetadata metadata = new TestRunMetadata();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public AIFailureAnalyzerPlugin(String outputPath) {
        this.outputPath = outputPath;
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
        generateReport();
    }

    private void handleTestCaseStarted(TestCaseStarted event) {
        TestCase testCase = event.getTestCase();
        String scenarioId = generateScenarioId(testCase);

        ScenarioResult scenario = new ScenarioResult();
        scenario.scenarioName = testCase.getName();
        scenario.featureName = testCase.getUri().toString();
        scenario.line = testCase.getLocation().getLine();
        scenario.tags = new ArrayList<>(testCase.getTags());
        scenario.startTime = event.getInstant().toString();

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

            // Categorize the scenario result
            updateFeatureResult(testCase, scenario);
        }
    }

    private void handleTestStepStarted(TestStepStarted event) {
        if (event.getTestStep() instanceof PickleStepTestStep) {
            PickleStepTestStep step = (PickleStepTestStep) event.getTestStep();
            String scenarioId = generateScenarioId(step.getTestCase());
            ScenarioResult scenario = scenarioResults.get(scenarioId);

            if (scenario != null) {
                StepResult stepResult = new StepResult();
                stepResult.keyword = step.getStep().getKeyword();
                stepResult.text = step.getStep().getText();
                stepResult.line = step.getStep().getLine();
                stepResult.startTime = event.getInstant().toString();

                scenario.steps.add(stepResult);
            }
        }
    }

    private void handleTestStepFinished(TestStepFinished event) {
        if (event.getTestStep() instanceof PickleStepTestStep) {
            PickleStepTestStep step = (PickleStepTestStep) event.getTestStep();
            String scenarioId = generateScenarioId(step.getTestCase());
            ScenarioResult scenario = scenarioResults.get(scenarioId);

            if (scenario != null && !scenario.steps.isEmpty()) {
                StepResult stepResult = scenario.steps.get(scenario.steps.size() - 1);
                Result result = event.getResult();

                stepResult.status = result.getStatus().toString();
                stepResult.endTime = event.getInstant().toString();
                stepResult.duration = result.getDuration().toMillis();

                if (result.getError() != null) {
                    stepResult.errorMessage = result.getError().getMessage();
                    stepResult.stackTrace = getStackTrace(result.getError());
                    stepResult.errorType = result.getError().getClass().getName();

                    // Mark this as the failing step
                    if (scenario.failingStep == null) {
                        scenario.failingStep = stepResult;
                    }
                }
            }
        }
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
        }
    }

    private void generateReport() {
        AIFailureReport report = new AIFailureReport();
        report.metadata = metadata;
        report.features = new ArrayList<>(featureResults.values());

        // Calculate summary statistics
        report.summary = new TestSummary();
        for (FeatureResult feature : featureResults.values()) {
            report.summary.totalScenarios += feature.totalScenarios;
            report.summary.passedScenarios += feature.passedScenarios;
            report.summary.failedScenarios += feature.failedScenarios;
            report.summary.skippedScenarios += feature.skippedScenarios;
            report.summary.undefinedScenarios += feature.undefinedScenarios;
        }

        report.summary.totalFeatures = featureResults.size();
        report.summary.passRate = report.summary.totalScenarios > 0
                ? (double) report.summary.passedScenarios / report.summary.totalScenarios * 100
                : 0.0;

        // Write to file
        try {
            Files.createDirectories(Paths.get(outputPath).getParent());
            try (FileWriter writer = new FileWriter(outputPath)) {
                gson.toJson(report, writer);
            }
            System.out.println("AI Failure Report generated at: " + outputPath);
        } catch (IOException e) {
            System.err.println("Failed to write AI Failure Report: " + e.getMessage());
            e.printStackTrace();
        }
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
}