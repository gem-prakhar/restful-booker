package com.restfulbooker.runners;

import io.cucumber.junit.CucumberOptions;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;

@RunWith(CucumberWithSerenity.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = "com.restfulbooker.stepdefinitions",
        tags = "",
        plugin = {
                "pretty",
                "html:target/cucumber-reports/cucumber.html",
                "json:target/cucumber-reports/cucumber.json",
                "com.restfulbooker.utils.AIFailureAnalyzerPlugin:target/ai-failures.json"
        },
        monochrome = true
)
public class TestRunner {
}