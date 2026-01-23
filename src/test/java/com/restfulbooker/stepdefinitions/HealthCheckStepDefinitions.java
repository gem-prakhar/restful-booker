package com.restfulbooker.stepdefinitions;

import com.restfulbooker.steps.HealthCheckSteps;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.annotations.Steps;

public class HealthCheckStepDefinitions {

    @Steps
    HealthCheckSteps healthCheckSteps;

    @When("I perform a health check")
    public void performHealthCheck() {
        healthCheckSteps.performHealthCheck();
    }

    @Then("the health check should return status code {int}")
    public void verifyHealthCheckStatus(int statusCode) {
        healthCheckSteps.verifyHealthCheckStatusCode(statusCode);
    }

}