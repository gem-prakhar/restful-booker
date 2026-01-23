@Booking
Feature: Booking Management
  As a user
  I want to manage bookings
  So that I can create, read, update and delete bookings

  Background:
    When I perform a health check
    Then the health check should return status code 201

  @CreateBooking
  Scenario: Create a new booking
    When I create a booking with the following details:
      | firstname | lastname | totalprice | depositpaid | checkin    | checkout   | additionalneeds |
      | Prakhar      | Singh      | 100        | true        | 2024-01-01 | 2024-01-05 | Breakfast       |
    Then the booking should be created successfully

  @GetBooking
  Scenario: Get an existing booking
    When I get the booking with ID 1
    Then the response status code should be 200

  @GetAllBookings
  Scenario: Get all bookings
    When I get all bookings
    Then the response status code should be 200
    And a list of bookings should be returned

  @UpdateBooking
  Scenario: Update an existing booking
    Given I am authenticated with username "admin" and password "password123"
    When I create a booking with the following details:
      | firstname | lastname | totalprice | depositpaid | checkin    | checkout   | additionalneeds |
      | Prakhar      | Singh    | 200        | false       | 2024-02-01 | 2024-02-10 | Lunch           |
    And I update the created booking with firstname "Ravi" and lastname "Kumar"
    Then the response status code should be 200
    And the booking details should match:
      | firstname | lastname |
      | Ravi     | Kumar  |

  @PartialUpdate
  Scenario: Partially update a booking
    Given I am authenticated with username "admin" and password "password123"
    When I create a booking with the following details:
      | firstname | lastname | totalprice | depositpaid | checkin    | checkout   | additionalneeds |
      | Bob       | Wilson   | 150        | true        | 2024-03-01 | 2024-03-05 | Dinner          |
    And I partially update the booking with firstname "Robert"
    Then the response status code should be 200

  @DeleteBooking
  Scenario: Delete a booking
    Given I am authenticated with username "admin" and password "password123"
    When I create a booking with the following details:
      | firstname | lastname | totalprice | depositpaid | checkin    | checkout   | additionalneeds |
      | Alice     | Brown    | 300        | true        | 2024-04-01 | 2024-04-15 | None            |
    And I delete the created booking
    Then the response status code should be 201