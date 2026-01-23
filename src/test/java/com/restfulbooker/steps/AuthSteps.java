package com.restfulbooker.steps;

import com.restfulbooker.utils.ApiConfig;
import com.restfulbooker.utils.TestContext;
import io.restassured.response.Response;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.annotations.Step;
import static org.junit.Assert.*;

public class AuthSteps {

    private TestContext context = TestContext.getInstance();

    @Step("Create authentication token with username '{0}' and password '{1}'")
    public void createAuthToken(String username, String password) {
        String requestBody = String.format("{\"username\":\"%s\",\"password\":\"%s\"}",
                username, password);

        Response response = SerenityRest.given()
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post(ApiConfig.getAuthEndpoint());

        context.setResponse(response);

        if (response.getStatusCode() == 200) {
            String token = response.jsonPath().getString("token");
            context.setAuthToken(token);
        }
    }

    @Step("Verify authentication response status code is {0}")
    public void verifyAuthStatusCode(int expectedStatusCode) {
        Response response = context.getResponse();
        assertEquals("Authentication status code mismatch",
                expectedStatusCode,
                response.getStatusCode());
    }

    @Step("Verify authentication token is generated")
    public void verifyTokenIsGenerated() {
        String token = context.getAuthToken();
        assertNotNull("Authentication token should not be null", token);
        assertFalse("Authentication token should not be empty", token.isEmpty());
    }

    @Step("Verify authentication fails with invalid credentials")
    public void verifyAuthenticationFails() {
        Response response = context.getResponse();
        String reason = response.jsonPath().getString("reason");
        assertEquals("Bad credentials", reason);
    }
}