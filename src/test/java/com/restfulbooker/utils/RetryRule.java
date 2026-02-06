package com.restfulbooker.utils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit rule that retries failed tests up to a maximum number of attempts.
 * Only shows passed result in reports if test passes within retry limit.
 */
public class RetryRule implements TestRule {

    private final int maxRetries;
    private final RetryTracker retryTracker;

    public RetryRule(int maxRetries) {
        this.maxRetries = maxRetries;
        this.retryTracker = RetryTracker.getInstance();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                String testName = description.getDisplayName();
                Throwable caughtThrowable = null;

                for (int i = 0; i <= maxRetries; i++) {
                    try {
                        // Track attempt
                        retryTracker.recordAttempt(testName, i + 1);

                        if (i > 0) {
                            System.out.println("Retry attempt " + i + " for: " + testName);
                        }

                        base.evaluate();

                        // Test passed
                        if (i > 0) {
                            System.out.println("Test passed on retry attempt " + i + ": " + testName);
                            retryTracker.recordSuccess(testName, i + 1);
                        }

                        return; // Test passed, exit retry loop

                    } catch (Throwable t) {
                        caughtThrowable = t;
                        retryTracker.recordFailure(testName, i + 1, t);

                        if (i < maxRetries) {
                            System.out.println("Test failed on attempt " + (i + 1) + ": " + testName);
                            System.out.println("   Error: " + t.getMessage());
                        }
                    }
                }

                // All retries exhausted
                System.out.println("Test failed after " + (maxRetries + 1) + " attempts: " + testName);
                throw caughtThrowable;
            }
        };
    }
}