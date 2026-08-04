Feature: Approve Expense

  Background:
    Given the manager is logged in
    And pending expenses exist

  Scenario Outline: Approve a pending expense
    When the manager denies expense "<expenseId>"
    Then the expense "<expenseId>" status should be "<status>"

    Examples: Valid Expense Id
      |   expenseId  |    status     |
      |    39        |     DENIED    |