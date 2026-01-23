package com.restfulbooker.steps;

import com.restfulbooker.utils.ApiConfig;
import com.restfulbooker.utils.TestContext;
import io.restassured.response.Response;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.rest.SerenityRest;
import static org.junit.Assert.*;

public class HealthCheckSteps {

    private TestContext context = TestContext.getInstance();

    @Step("Perform health check on the API")
    public void performHealthCheck() {
        Response response = SerenityRest.given()
                .when()
                .get(ApiConfig.getPingEndpoint());

        context.setResponse(response);
    }

    @Step("Verify health check returns status code {0}")
    public void verifyHealthCheckStatusCode(int expectedStatusCode) {
        Response response = context.getResponse();
        assertEquals("Health check status code mismatch",
                expectedStatusCode,
                response.getStatusCode());

        boolean passed = response.getStatusCode() == expectedStatusCode;
        if (passed) {
            System.out.println("response" + response.getStatusCode());
        }
        context.setHealthCheckPassed(passed);

        assertTrue("API health check failed - cannot proceed with tests",
                context.isHealthCheckPassed());
    }
}