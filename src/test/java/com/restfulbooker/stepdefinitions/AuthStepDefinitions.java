package com.restfulbooker.stepdefinitions;

import com.restfulbooker.steps.AuthSteps;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.annotations.Steps;

public class AuthStepDefinitions {

    @Steps
    AuthSteps authSteps;

    @When("I authenticate with username {string} and password {string}")
    @Given("I am authenticated with username {string} and password {string}")
    public void authenticateWithCredentials(String username, String password) {
        authSteps.createAuthToken(username, password);
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