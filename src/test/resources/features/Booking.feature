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
      | firstname         | lastname         | totalprice         | depositpaid         | checkin         | checkout         | additionalneeds         |
      | booking:firstname | booking:lastname | booking:totalprice | booking:depositpaid | booking:checkin | booking:checkout | booking:additionalneeds |
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
    Given I am authenticated with username "auth:username" and password "auth:password"
    When I create a booking with the following details:
      | firstname         | lastname         | totalprice         | depositpaid         | checkin         | checkout         | additionalneeds         |
      | booking:firstname | booking:lastname | booking:totalprice | booking:depositpaid | booking:checkin | booking:checkout | booking:additionalneeds |
    And I update the created booking with firstname "update:firstname" and lastname "update:lastname"
    Then the response status code should be 200
    And the booking details should match:
      | firstname        | lastname        |
      | update:firstname | update:lastname |

  @PartialUpdate
  Scenario: Partially update a booking
    Given I am authenticated with username "auth:username" and password "auth:password"
    When I create a booking with the following details:
      | firstname         | lastname         | totalprice         | depositpaid         | checkin         | checkout         | additionalneeds         |
      | booking:firstname | booking:lastname | booking:totalprice | booking:depositpaid | booking:checkin | booking:checkout | booking:additionalneeds |
    And I partially update the booking with firstname "partial:firstname"
    Then the response status code should be 200

  @DeleteBooking
  Scenario: Delete a booking
    Given I am authenticated with username "auth:username" and password "auth:password"
    When I create a booking with the following details:
      | firstname         | lastname         | totalprice         | depositpaid         | checkin         | checkout         | additionalneeds         |
      | booking:firstname | booking:lastname | booking:totalprice | booking:depositpaid | booking:checkin | booking:checkout | booking:additionalneeds |
    And I delete the created booking
    Then the response status code should be 201