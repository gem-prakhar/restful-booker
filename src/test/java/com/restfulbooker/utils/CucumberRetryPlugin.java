package com.restfulbooker.utils;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cucumber plugin to track failed scenarios for retry mechanism.
 * Generates a rerun file with failed scenarios.
 */
public class CucumberRetryPlugin implements ConcurrentEventListener {

    private final String outputPath;
    private final Set<String> failedScenarios = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Map<String, ScenarioRetryInfo> scenarioRetryInfo = new ConcurrentHashMap<>();

    public CucumberRetryPlugin(String outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
        publisher.registerHandlerFor(TestRunFinished.class, this::handleTestRunFinished);
    }

    private void handleTestCaseFinished(TestCaseFinished event) {
        TestCase testCase = event.getTestCase();
        Result result = event.getResult();

        String scenarioId = generateScenarioId(testCase);
        String scenarioLocation = testCase.getUri().toString() + ":" + testCase.getLocation().getLine();

        ScenarioRetryInfo info = scenarioRetryInfo.computeIfAbsent(
                scenarioId,
                k -> new ScenarioRetryInfo(testCase.getName(), scenarioLocation)
        );

        info.totalAttempts++;

        if (result.getStatus() == Status.FAILED) {
            info.failedAttempts++;
            info.lastError = result.getError() != null ? result.getError().getMessage() : "Unknown error";
            failedScenarios.add(scenarioLocation);
        } else if (result.getStatus() == Status.PASSED) {
            info.passedOnAttempt = info.totalAttempts;
            info.finalStatus = "PASSED";
            // Remove from failed set if it eventually passed
            failedScenarios.remove(scenarioLocation);
        }
    }

    private void handleTestRunFinished(TestRunFinished event) {
        // Write rerun file for failed scenarios
        if (!failedScenarios.isEmpty()) {
            writeRerunFile();
        }

        // Write retry summary
        writeRetrySummary();
    }

    private void writeRerunFile() {
        try {
            Files.createDirectories(Paths.get(outputPath).getParent());
            try (FileWriter writer = new FileWriter(outputPath)) {
                writer.write(String.join(" ", failedScenarios));
            }
            System.out.println("Rerun file generated at: " + outputPath);
            System.out.println("Failed scenarios to retry: " + failedScenarios.size());
        } catch (IOException e) {
            System.err.println("Failed to write rerun file: " + e.getMessage());
        }
    }

    private void writeRetrySummary() {
        String summaryPath = outputPath.replace(".txt", "-summary.json");

        try {
            Files.createDirectories(Paths.get(summaryPath).getParent());

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"totalScenarios\": ").append(scenarioRetryInfo.size()).append(",\n");
            json.append("  \"scenariosPassedFirstAttempt\": ").append(countPassedFirstAttempt()).append(",\n");
            json.append("  \"scenariosPassedAfterRetry\": ").append(countPassedAfterRetry()).append(",\n");
            json.append("  \"scenariosStillFailing\": ").append(failedScenarios.size()).append(",\n");
            json.append("  \"scenarios\": [\n");

            List<ScenarioRetryInfo> infoList = new ArrayList<>(scenarioRetryInfo.values());
            for (int i = 0; i < infoList.size(); i++) {
                ScenarioRetryInfo info = infoList.get(i);
                json.append("    {\n");
                json.append("      \"name\": \"").append(escape(info.scenarioName)).append("\",\n");
                json.append("      \"location\": \"").append(escape(info.scenarioLocation)).append("\",\n");
                json.append("      \"totalAttempts\": ").append(info.totalAttempts).append(",\n");
                json.append("      \"failedAttempts\": ").append(info.failedAttempts).append(",\n");
                json.append("      \"passedOnAttempt\": ").append(info.passedOnAttempt != null ? info.passedOnAttempt : "null").append(",\n");
                json.append("      \"finalStatus\": \"").append(info.finalStatus).append("\",\n");
                json.append("      \"lastError\": \"").append(escape(info.lastError)).append("\"\n");
                json.append("    }").append(i < infoList.size() - 1 ? "," : "").append("\n");
            }

            json.append("  ]\n");
            json.append("}\n");

            try (FileWriter writer = new FileWriter(summaryPath)) {
                writer.write(json.toString());
            }

            System.out.println("Retry summary generated at: " + summaryPath);
        } catch (IOException e) {
            System.err.println("Failed to write retry summary: " + e.getMessage());
        }
    }

    private int countPassedFirstAttempt() {
        return (int) scenarioRetryInfo.values().stream()
                .filter(info -> "PASSED".equals(info.finalStatus) && info.passedOnAttempt != null && info.passedOnAttempt == 1)
                .count();
    }

    private int countPassedAfterRetry() {
        return (int) scenarioRetryInfo.values().stream()
                .filter(info -> "PASSED".equals(info.finalStatus) && info.passedOnAttempt != null && info.passedOnAttempt > 1)
                .count();
    }

    private String generateScenarioId(TestCase testCase) {
        return testCase.getUri().toString() + ":" + testCase.getLocation().getLine();
    }

    private String escape(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    static class ScenarioRetryInfo {
        String scenarioName;
        String scenarioLocation;
        int totalAttempts = 0;
        int failedAttempts = 0;
        Integer passedOnAttempt = null;
        String finalStatus = "FAILED";
        String lastError = null;

        ScenarioRetryInfo(String name, String location) {
            this.scenarioName = name;
            this.scenarioLocation = location;
        }
    }
}