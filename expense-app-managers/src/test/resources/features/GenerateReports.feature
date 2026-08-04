Feature: Generate Reports

  Background:
    Given the manager is logged in

  Scenario: Generate employee report successfully
    When the manager generates an employee report for "Bob"
    Then the manager should see "Report generated successfully!"
    And a CSV report should be downloaded
    And the CSV report should contain expenses for "Bob"

  Scenario Outline: Generate category report successfully
    When the manager generates a category report for "<category>"
    Then the manager should see "Report generated successfully!"
    And a CSV report should be downloaded
    And the CSV report should contain expenses for "<category>"

    Examples: Valid Categories
      |   category   |
      |   Supplies   |
      |   Travel     |
      |   Services   |
      |   Repairs    |
      |   Meals      |
      |Certifications|
      |     Other    |

  Scenario Outline: Generate date range report successfully
    When the manager generates a date range report from "<startDate>" to "<endDate>"
    Then the manager should see "Report generated successfully!"
    And a CSV report should be downloaded
    And every expense in the CSV report should be between "<startDate>" and "<endDate>"

    Examples: Valid Date Ranges
      |   startDate  |   endDate  |
      |   07012026   |   07312026 |