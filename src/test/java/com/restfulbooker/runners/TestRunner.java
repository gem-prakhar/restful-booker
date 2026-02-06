package com.restfulbooker.runners;

import com.restfulbooker.utils.RetryRule;
import io.cucumber.junit.CucumberOptions;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.Rule;
import org.junit.runner.RunWith;

@RunWith(CucumberWithSerenity.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {
                "com.restfulbooker.stepdefinitions",
                "com.restfulbooker.utils"
        },
        tags = "",
        plugin = {
                "pretty",
                "html:target/cucumber-reports/cucumber.html",
                "json:target/cucumber-reports/cucumber.json",
                "com.restfulbooker.utils.CucumberRetryPlugin:target/rerun.txt",
                "com.restfulbooker.utils.AIFailureAnalyzerPluginWithRetry:target/ai-failures.json",
        },
        monochrome = true
)
public class TestRunner {
        @Rule
        public RetryRule retryRule = new RetryRule(getMaxRetries());

        private static int getMaxRetries() {
                String maxRetriesStr = System.getProperty("maxRetries", "3");
                try {
                        return Integer.parseInt(maxRetriesStr);
                } catch (NumberFormatException e) {
                        System.err.println("Invalid maxRetries value: " + maxRetriesStr + ", using default: 3");
                        return 3;
                }
        }
}