@smoke
Feature: Manager End-to-End Workflow

    Scenario: Manager reviews and approves an expense
        Given the manager is on the login page

        When the manager enters username "Andrew"
        And enters password "onetwothree"
        And clicks login button
        Then the login message should be "Login successful! Redirecting to manager dashboard..."

        When the manager refreshes pending expenses
        Then the pending expense table should contain pending expenses only

        When the manager approves expense "39"
        Then the expense "39" status should be "APPROVED"

        When the manager generates a category report for "Meals"
        Then the manager should see "Report generated successfully!"
        And a CSV report should be downloaded
        And the CSV report should contain expenses for "Meals"

        When the manager logs out
        Then the manager should be redirected to the login page
        And the login form should be displayed