package com.restfulbooker.steps;

import com.restfulbooker.utils.ApiConfig;
import com.restfulbooker.utils.TestContext;
import io.restassured.response.Response;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.annotations.Step;
import static org.junit.Assert.*;

public class BookingSteps {

    private TestContext context = TestContext.getInstance();

    @Step("Create a new booking")
    public void createBooking(String firstname, String lastname, int totalPrice,
                              boolean depositPaid, String checkin, String checkout,
                              String additionalNeeds) {
        String requestBody = String.format(
                "{\"firstname\":\"%s\",\"lastname\":\"%s\",\"totalprice\":%d," +
                        "\"depositpaid\":%b,\"bookingdates\":{\"checkin\":\"%s\",\"checkout\":\"%s\"}," +
                        "\"additionalneeds\":\"%s\"}",
                firstname, lastname, totalPrice, depositPaid, checkin, checkout, additionalNeeds
        );

        Response response = SerenityRest.given()
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post(ApiConfig.getBookingEndpoint());

        context.setResponse(response);

        if (response.getStatusCode() == 200) {
            Integer bookingId = response.jsonPath().getInt("bookingid");
            context.setBookingId(bookingId);
        }
    }

    @Step("Get booking by ID {0}")
    public void getBookingById(int bookingId) {
        Response response = SerenityRest.given()
                .when()
                .get(ApiConfig.getBookingEndpoint() + "/" + bookingId);

        context.setResponse(response);
    }

    @Step("Get all bookings")
    public void getAllBookings() {
        Response response = SerenityRest.given()
                .when()
                .get(ApiConfig.getBookingEndpoint());

        context.setResponse(response);
    }

    @Step("Update booking with ID {0}")
    public void updateBooking(int bookingId, String firstname, String lastname,
                              int totalPrice, boolean depositPaid,
                              String checkin, String checkout, String additionalNeeds) {
        String requestBody = String.format(
                "{\"firstname\":\"%s\",\"lastname\":\"%s\",\"totalprice\":%d," +
                        "\"depositpaid\":%b,\"bookingdates\":{\"checkin\":\"%s\",\"checkout\":\"%s\"}," +
                        "\"additionalneeds\":\"%s\"}",
                firstname, lastname, totalPrice, depositPaid, checkin, checkout, additionalNeeds
        );

        Response response = SerenityRest.given()
                .contentType("application/json")
                .header("Cookie", "token=" + context.getAuthToken())
                .body(requestBody)
                .when()
                .put(ApiConfig.getBookingEndpoint() + "/" + bookingId);

        context.setResponse(response);
    }

    @Step("Partially update booking with ID {0}")
    public void partialUpdateBooking(int bookingId, String updateJson) {
        Response response = SerenityRest.given()
                .contentType("application/json")
                .header("Cookie", "token=" + context.getAuthToken())
                .body(updateJson)
                .when()
                .patch(ApiConfig.getBookingEndpoint() + "/" + bookingId);

        context.setResponse(response);
    }

    @Step("Delete booking with ID {0}")
    public void deleteBooking(int bookingId) {
        Response response = SerenityRest.given()
                .contentType("application/json")
                .header("Cookie", "token=" + context.getAuthToken())
                .when()
                .delete(ApiConfig.getBookingEndpoint() + "/" + bookingId);

        context.setResponse(response);
    }

    @Step("Verify response status code is {0}")
    public void verifyStatusCode(int expectedStatusCode) {
        Response response = context.getResponse();
        assertEquals("Status code mismatch",
                expectedStatusCode,
                response.getStatusCode());
    }

    @Step("Verify booking details match expected values")
    public void verifyBookingDetails(String firstname, String lastname) {
        Response response = context.getResponse();
        assertEquals(firstname, response.jsonPath().getString("firstname"));
        assertEquals(lastname, response.jsonPath().getString("lastname"));
    }

    @Step("Verify booking ID is returned")
    public void verifyBookingIdReturned() {
        Integer bookingId = context.getBookingId();
        assertNotNull("Booking ID should not be null", bookingId);
        assertTrue("Booking ID should be greater than 0", bookingId > 0);
    }

    @Step("Verify response contains list of bookings")
    public void verifyBookingListReturned() {
        Response response = context.getResponse();
        assertNotNull("Response should contain bookings list", response.jsonPath().getList("$"));
    }
}