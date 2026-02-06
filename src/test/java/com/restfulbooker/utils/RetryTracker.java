package com.restfulbooker.utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton class to track retry attempts across all tests.
 * Provides data to the AI Failure Analyzer plugin about retry history.
 */
public class RetryTracker {

    private static RetryTracker instance;

    private final Map<String, RetryInfo> retryHistory = new ConcurrentHashMap<>();

    private RetryTracker() {}

    public static synchronized RetryTracker getInstance() {
        if (instance == null) {
            instance = new RetryTracker();
        }
        return instance;
    }

    public void recordAttempt(String testName, int attemptNumber) {
        RetryInfo info = retryHistory.computeIfAbsent(testName, k -> new RetryInfo(testName));
        info.totalAttempts = attemptNumber;
        info.lastAttemptTime = System.currentTimeMillis();
    }

    public void recordFailure(String testName, int attemptNumber, Throwable error) {
        RetryInfo info = retryHistory.computeIfAbsent(testName, k -> new RetryInfo(testName));

        AttemptResult result = new AttemptResult();
        result.attemptNumber = attemptNumber;
        result.status = "FAILED";
        result.errorMessage = error.getMessage();
        result.errorType = error.getClass().getName();
        result.timestamp = System.currentTimeMillis();

        info.attempts.add(result);
    }

    public void recordSuccess(String testName, int attemptNumber) {
        RetryInfo info = retryHistory.computeIfAbsent(testName, k -> new RetryInfo(testName));
        info.passedOnAttempt = attemptNumber;
        info.finalStatus = "PASSED";

        AttemptResult result = new AttemptResult();
        result.attemptNumber = attemptNumber;
        result.status = "PASSED";
        result.timestamp = System.currentTimeMillis();

        info.attempts.add(result);
    }

    public RetryInfo getRetryInfo(String testName) {
        return retryHistory.get(testName);
    }

    public Map<String, RetryInfo> getAllRetryInfo() {
        return new HashMap<>(retryHistory);
    }

    public void clear() {
        retryHistory.clear();
    }

    /**
     * Check if a test eventually passed (even if it failed on earlier attempts)
     */
    public boolean eventuallyPassed(String testName) {
        RetryInfo info = retryHistory.get(testName);
        return info != null && "PASSED".equals(info.finalStatus);
    }

    /**
     * Check if a test required retries to pass
     */
    public boolean passedAfterRetry(String testName) {
        RetryInfo info = retryHistory.get(testName);
        return info != null && info.passedOnAttempt != null && info.passedOnAttempt > 1;
    }

    // Data classes
    public static class RetryInfo {
        public String testName;
        public int totalAttempts;
        public Integer passedOnAttempt; // null if never passed
        public String finalStatus; // PASSED or FAILED
        public long lastAttemptTime;
        public List<AttemptResult> attempts = new ArrayList<>();

        public RetryInfo(String testName) {
            this.testName = testName;
            this.finalStatus = "FAILED"; // Default to failed
        }
    }

    public static class AttemptResult {
        public int attemptNumber;
        public String status;
        public String errorMessage;
        public String errorType;
        public long timestamp;
    }
}