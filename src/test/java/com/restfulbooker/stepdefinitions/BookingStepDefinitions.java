package com.restfulbooker.stepdefinitions;

import com.restfulbooker.steps.BookingSteps;
import com.restfulbooker.utils.TestContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.annotations.Steps;

public class BookingStepDefinitions {

    @Steps
    BookingSteps bookingSteps;

    private TestContext context = TestContext.getInstance();

    @When("I create a booking with the following details:")
    public void createBookingWithDetails(DataTable dataTable) {
        var data = dataTable.asMaps().get(0);
        bookingSteps.createBooking(
                data.get("firstname"),
                data.get("lastname"),
                Integer.parseInt(data.get("totalprice")),
                Boolean.parseBoolean(data.get("depositpaid")),
                data.get("checkin"),
                data.get("checkout"),
                data.get("additionalneeds")
        );
    }

    @When("I get the booking with ID {int}")
    @Given("a booking exists with ID {int}")
    public void getBookingById(int bookingId) {
        bookingSteps.getBookingById(bookingId);
        context.setBookingId(bookingId);
    }

    @When("I get all bookings")
    public void getAllBookings() {
        bookingSteps.getAllBookings();
    }

    @When("I update the created booking with firstname {string} and lastname {string}")
    public void updateCreatedBooking(String firstname, String lastname) {
        Integer bookingId = context.getBookingId();
        bookingSteps.updateBooking(bookingId, firstname, lastname, 150, true,
                "2024-01-01", "2024-01-05", "Updated booking");
    }

    @When("I partially update the booking with firstname {string}")
    public void partiallyUpdateBooking(String firstname) {
        Integer bookingId = context.getBookingId();
        String updateJson = String.format("{\"firstname\":\"%s\"}", firstname);
        bookingSteps.partialUpdateBooking(bookingId, updateJson);
    }

    @When("I delete the created booking")
    public void deleteCreatedBooking() {
        Integer bookingId = context.getBookingId();
        bookingSteps.deleteBooking(bookingId);
    }

    @Then("the response status code should be {int}")
    public void verifyResponseStatusCode(int statusCode) {
        bookingSteps.verifyStatusCode(statusCode);
    }

    @Then("the booking should be created successfully")
    public void verifyBookingCreated() {
        bookingSteps.verifyBookingIdReturned();
        bookingSteps.verifyStatusCode(200);
    }

    @Then("the booking details should match:")
    public void verifyBookingDetailsMatch(io.cucumber.datatable.DataTable dataTable) {
        var data = dataTable.asMaps().get(0);
        bookingSteps.verifyBookingDetails(data.get("firstname"), data.get("lastname"));
    }

    @Then("a list of bookings should be returned")
    public void verifyBookingListReturned() {
        bookingSteps.verifyBookingListReturned();
    }
}