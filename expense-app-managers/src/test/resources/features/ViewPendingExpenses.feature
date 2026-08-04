Feature: View Pending Expenses

    Scenario: Pending expenses are displayed
        Given the manager is logged in
        When the manager refreshes pending expenses
        Then the pending expense table should contain pending expenses only

    Scenario: No pending expenses
        Given there are no pending expenses
        And the manager is logged in
        When the manager refreshes pending expenses
        Then the manager should see no pending expenses
