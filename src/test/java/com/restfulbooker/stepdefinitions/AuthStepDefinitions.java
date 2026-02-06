package com.restfulbooker.stepdefinitions;

import com.restfulbooker.steps.AuthSteps;
import com.restfulbooker.utils.CucumberData.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.annotations.Steps;

public class AuthStepDefinitions {

    @Steps
    AuthSteps authSteps;

    @When("I authenticate with username \"{data}\" and password \"{data}\"")
    @Given("I am authenticated with username \"{data}\" and password \"{data}\"")
    public void authenticateWithCredentials(Object username, Object password) {
        authSteps.createAuthToken(String.valueOf(username), String.valueOf(password));
    }

    @Then("the authentication should return status code {int}")
    public void verifyAuthenticationStatus(int statusCode) {
        authSteps.verifyAuthStatusCode(statusCode);
    }

    @Then("a valid token should be generated")
    public void verifyTokenGenerated() {
        authSteps.verifyTokenIsGenerated();
    }

    @Then("authentication should fail with bad credentials")
    public void verifyAuthenticationFails() {
        authSteps.verifyAuthenticationFails();
    }
}