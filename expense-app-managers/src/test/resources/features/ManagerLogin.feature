@login
@smoke
@allure.label.story:Manager_Authentication
Feature: Manager Login

  Background:
    Given the manager is on the login page

  @blocker
  Scenario Outline: Valid manager credentials
    When the manager enters username "<username>"
    And enters password "<password>"
    And clicks login button
    Then the login message should be "<message>"

    Examples: Valid Credentials
      | username | password    |                        message                          |
      | Andrew   | onetwothree |  Login successful! Redirecting to manager dashboard...  |

  @critical
  Scenario Outline: Invalid manager credentials
    When the manager enters username "<username>"
    And enters password "<password>"
    And clicks login button
    Then the login message should be "<message>"

    Examples: Invalid Credentials
      | username     | password      |                  message                     |
      | Andrew       | WrongPassword | Invalid credentials or user is not a manager |
      | Bob          | bob_22        | Invalid credentials or user is not a manager |
      | notarealuser | onetwothree   | Invalid credentials or user is not a manager |