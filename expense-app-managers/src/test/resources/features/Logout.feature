Feature: Logout

    Background:
        Given the manager is logged in
    
    Scenario: Successful logout
        When the manager logs out
        Then the manager should be redirected to the login page
        And the login form should be displayed