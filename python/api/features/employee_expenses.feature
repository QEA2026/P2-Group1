Feature: Employee expense management

  As an employee
  I want to manage my reimbursement requests
  So that I can submit and maintain my expenses

  Scenario: Employee completes the pending expense workflow
    Given the employee application is open
    When the employee logs in with valid credentials
    Then the employee dashboard should be displayed
    When the employee submits a new expense
    Then the new expense should appear in the expense table
    When the employee edits the new pending expense
    Then the updated expense should appear in the expense table
    When the employee deletes the updated expense
    Then the expense should no longer appear in the expense table
    When the employee logs out
    Then the login page should be displayed



  Scenario: Employee enters an invalid password
    Given the employee application is open
    When the employee logs in with an invalid password
    Then an invalid login message should be displayed
    And the employee should remain on the login page


  Scenario: Employee submits an expense with an invalid amount
    Given the employee application is open
    When the employee logs in with valid credentials
    Then the employee dashboard should be displayed
    When the employee submits an expense with an amount of zero
    Then an invalid expense amount message should be displayed
    And the invalid expense should not appear in the expense table  


  Scenario: Employee cannot edit an approved expense
    Given the employee application is open
    When the employee logs in with valid credentials
    Then the employee dashboard should be displayed
    And the employee has an approved expense
    Then the Edit button should not be available for the approved expense


  Scenario: Employee submits multiple expenses
    Given the employee application is open
    When the employee logs in with valid credentials
    Then the employee dashboard should be displayed
    When the employee submits multiple expenses
    Then all submitted expenses should appear in the expense table
    And the employee deletes the multiple test expenses



  Scenario: Employee attempts to log in without a password
    Given the employee application is open
    When the employee enters a valid username but leaves the password blank
    Then the password field should display a required validation message
    And the employee should remain on the login page