package com.restfulbooker.stepdefinitions;

import com.restfulbooker.steps.BookingSteps;
import com.restfulbooker.utils.CucumberData;
import com.restfulbooker.utils.TestContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.annotations.Steps;

import java.util.HashMap;
import java.util.Map;

public class BookingStepDefinitions {

    @Steps
    BookingSteps bookingSteps;

    private TestContext context = TestContext.getInstance();

    @When("I create a booking with the following details:")
    public void createBookingWithDetails(DataTable dataTable) {
        Map<String, String> rawData = dataTable.asMaps().get(0);

        Map<String, String> data = new HashMap<>();
        rawData.forEach((key, value) -> {
            data.put(key, String.valueOf(CucumberData.get(value)));
        });

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
    public void updateCreatedBooking(String firstnameKey, String lastnameKey) {
        Integer bookingId = context.getBookingId();

        String firstname = String.valueOf(CucumberData.get(firstnameKey));
        String lastname = String.valueOf(CucumberData.get(lastnameKey));

        bookingSteps.updateBooking(bookingId, firstname, lastname, 150, true,
                "2024-01-01", "2024-01-05", "Updated booking");
    }

    @When("I partially update the booking with firstname {string}")
    public void partiallyUpdateBooking(String firstnameKey) {
        Integer bookingId = context.getBookingId();

        String firstname = String.valueOf(CucumberData.get(firstnameKey));
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
    public void verifyBookingDetailsMatch(DataTable dataTable) {
        Map<String, String> rawData = dataTable.asMaps().get(0);

        String expectedFirstname = String.valueOf(CucumberData.get(rawData.get("firstname")));
        String expectedLastname = String.valueOf(CucumberData.get(rawData.get("lastname")));

        bookingSteps.verifyBookingDetails(expectedFirstname, expectedLastname);
    }

    @Then("a list of bookings should be returned")
    public void verifyBookingListReturned() {
        bookingSteps.verifyBookingListReturned();
    }
}