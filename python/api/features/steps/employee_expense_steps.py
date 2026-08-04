from behave import given, then, when
from selenium.webdriver.common.alert import Alert
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import Select


@given("the employee application is open")
def open_employee_application(context):
    context.driver.get(context.base_url)

    context.wait.until(
        EC.visibility_of_element_located(
            (By.ID, "login-form")
        )
    )


@when("the employee logs in with valid credentials")
def log_in_with_valid_credentials(context):
    username_input = context.driver.find_element(
        By.ID,
        "username"
    )

    password_input = context.driver.find_element(
        By.ID,
        "password"
    )

    username_input.clear()
    username_input.send_keys(
        context.employee_username
    )

    password_input.clear()
    password_input.send_keys(
        context.employee_password
    )

    context.driver.find_element(
        By.ID,
        "login-button"
    ).click()


@then("the employee dashboard should be displayed")
def verify_dashboard(context):
    dashboard = context.wait.until(
        EC.visibility_of_element_located(
            (By.ID, "dashboard-view")
        )
    )

    assert dashboard.is_displayed()

    welcome_message = context.driver.find_element(
        By.ID,
        "welcome-message"
    ).text

    assert context.employee_username in welcome_message


@when("the employee submits a new expense")
def submit_new_expense(context):
    amount_input = context.driver.find_element(
        By.ID,
        "expense-amount"
    )

    description_input = context.driver.find_element(
        By.ID,
        "expense-description"
    )

    category_select = Select(
        context.driver.find_element(
            By.ID,
            "expense-category"
        )
    )

    amount_input.clear()
    amount_input.send_keys("25.50")

    description_input.clear()
    description_input.send_keys(
        context.original_description
    )

    category_select.select_by_value("Meals")

    context.driver.find_element(
        By.ID,
        "submit-expense-button"
    ).click()


@then("the new expense should appear in the expense table")
def verify_new_expense(context):
    context.wait.until(
        EC.text_to_be_present_in_element(
            (By.ID, "expenses-table-body"),
            context.original_description
        )
    )

    table_text = context.driver.find_element(
        By.ID,
        "expenses-table-body"
    ).text

    assert context.original_description in table_text
    assert "Meals" in table_text
    assert "25.50" in table_text
    assert "pending" in table_text.lower()


@when("the employee edits the new pending expense")
def edit_new_expense(context):
    row = find_expense_row(
        context,
        context.original_description
    )

    row.find_element(
        By.CLASS_NAME,
        "edit-button"
    ).click()

    context.wait.until(
        EC.visibility_of_element_located(
            (By.ID, "edit-modal")
        )
    )

    amount_input = context.driver.find_element(
        By.ID,
        "edit-expense-amount"
    )

    description_input = context.driver.find_element(
        By.ID,
        "edit-expense-description"
    )

    category_select = Select(
        context.driver.find_element(
            By.ID,
            "edit-expense-category"
        )
    )

    amount_input.clear()
    amount_input.send_keys("35.75")

    description_input.clear()
    description_input.send_keys(
        context.updated_description
    )

    category_select.select_by_value("Travel")

    context.driver.find_element(
        By.ID,
        "save-edit-button"
    ).click()


@then("the updated expense should appear in the expense table")
def verify_updated_expense(context):
    context.wait.until(
        EC.text_to_be_present_in_element(
            (By.ID, "expenses-table-body"),
            context.updated_description
        )
    )

    updated_row = find_expense_row(
        context,
        context.updated_description
    )

    row_text = updated_row.text

    assert context.updated_description in row_text, (
        f"Updated description was not found in row: {row_text}"
    )

    assert "Travel" in row_text, (
        f"Updated category was not found in row: {row_text}"
    )

    assert "35.75" in row_text, (
        f"Updated amount was not found in row: {row_text}"
    )


@when("the employee deletes the updated expense")
def delete_updated_expense(context):
    row = find_expense_row(
        context,
        context.updated_description
    )

    row.find_element(
        By.CLASS_NAME,
        "delete-button"
    ).click()

    context.wait.until(
        EC.alert_is_present()
    )

    Alert(context.driver).accept()


@then("the expense should no longer appear in the expense table")
def verify_expense_deleted(context):
    context.wait.until(
        lambda driver: (
            context.updated_description
            not in driver.find_element(
                By.ID,
                "expenses-table-body"
            ).text
        )
    )

    table_text = context.driver.find_element(
        By.ID,
        "expenses-table-body"
    ).text

    assert context.updated_description not in table_text


@when("the employee logs out")
def log_out(context):
    context.driver.find_element(
        By.ID,
        "logout-button"
    ).click()


@then("the login page should be displayed")
def verify_login_page(context):
    login_view = context.wait.until(
        EC.visibility_of_element_located(
            (By.ID, "login-view")
        )
    )

    assert login_view.is_displayed()

    username_input = context.driver.find_element(
        By.ID,
        "username"
    )

    assert username_input.is_displayed()


def find_expense_row(context, description):
    """Find the table row containing a specific description."""

    rows = context.driver.find_elements(
        By.CSS_SELECTOR,
        "#expenses-table-body tr"
    )

    for row in rows:
        if description in row.text:
            return row

    raise AssertionError(
        f'Could not find expense row containing "{description}".'
    )

@when("the employee logs in with an invalid password")
def log_in_with_invalid_password(context):
    username_input = context.driver.find_element(
        By.ID,
        "username"
    )

    password_input = context.driver.find_element(
        By.ID,
        "password"
    )

    username_input.clear()
    username_input.send_keys(
        context.employee_username
    )

    password_input.clear()
    password_input.send_keys(
        "incorrect_password"
    )

    context.driver.find_element(
        By.ID,
        "login-button"
    ).click()


@then("an invalid login message should be displayed")
def verify_invalid_login_message(context):
    error_message = context.wait.until(
        EC.visibility_of_element_located(
            (By.ID, "login-message")
        )
    )

    message_text = error_message.text.strip()

    assert message_text, (
        "Expected an invalid-login message, "
        "but the message was empty."
    )

    assert (
        "invalid" in message_text.lower()
        or "incorrect" in message_text.lower()
        or "failed" in message_text.lower()
    ), (
        f"Unexpected login error message: {message_text}"
    )

@then("the employee should remain on the login page")
def verify_employee_remains_on_login_page(context):
    login_form = context.wait.until(
        EC.visibility_of_element_located(
            (By.ID, "login-form")
        )
    )

    assert login_form.is_displayed(), (
        "The login form was not displayed "
        "after the invalid login attempt."
    )

    dashboard = context.driver.find_element(
        By.ID,
        "dashboard-view"
    )

    assert not dashboard.is_displayed(), (
        "The dashboard was displayed even though "
        "the login credentials were invalid."
    )

@when("the employee submits an expense with an amount of zero")
def submit_expense_with_zero_amount(context):
    amount_input = context.driver.find_element(
        By.ID,
        "expense-amount"
    )

    description_input = context.driver.find_element(
        By.ID,
        "expense-description"
    )

    category_select = Select(
        context.driver.find_element(
            By.ID,
            "expense-category"
        )
    )

    context.invalid_description = (
        "E2E Invalid Zero Amount Expense"
    )

    amount_input.clear()
    amount_input.send_keys("0")

    description_input.clear()
    description_input.send_keys(
        context.invalid_description
    )

    category_select.select_by_value("Meals")

    context.driver.find_element(
        By.ID,
        "submit-expense-button"
    ).click()


@then("an invalid expense amount message should be displayed")
def verify_invalid_expense_amount_message(context):
    amount_input = context.driver.find_element(
        By.ID,
        "expense-amount"
    )

    validation_message = amount_input.get_attribute(
        "validationMessage"
    )

    assert validation_message, (
        "Expected an amount validation message, "
        "but no message was displayed."
    )

@then("the invalid expense should not appear in the expense table")
def verify_invalid_expense_not_added(context):
    table_text = context.driver.find_element(
        By.ID,
        "expenses-table-body"
    ).text

    assert context.invalid_description not in table_text, (
        "The expense with an invalid amount was added "
        "to the expense table."
    )

@then("the employee has an approved expense")
def verify_employee_has_approved_expense(context):
    rows = context.wait.until(
        EC.presence_of_all_elements_located(
            (By.CSS_SELECTOR, "#expenses-table-body tr")
        )
    )

    for row in rows:
        if "approved" in row.text.lower():
            context.approved_expense_row = row
            return

    raise AssertionError(
        "No approved expense was found for the employee."
    )

@then("the Edit button should not be available for the approved expense")
def verify_no_edit_button_for_approved_expense(context):
    approved_row = context.approved_expense_row

    edit_buttons = approved_row.find_elements(
        By.CLASS_NAME,
        "edit-button"
    )

    assert len(edit_buttons) == 0, (
        "An Edit button was available for an approved expense."
    )


def submit_expense(context, amount, description, category):
    """Fill out and submit one expense."""

    amount_input = context.driver.find_element(
        By.ID,
        "expense-amount"
    )

    description_input = context.driver.find_element(
        By.ID,
        "expense-description"
    )

    category_select = Select(
        context.driver.find_element(
            By.ID,
            "expense-category"
        )
    )

    amount_input.clear()
    amount_input.send_keys(amount)

    description_input.clear()
    description_input.send_keys(description)

    category_select.select_by_value(category)

    context.driver.find_element(
        By.ID,
        "submit-expense-button"
    ).click()

    context.wait.until(
        EC.text_to_be_present_in_element(
            (By.ID, "expenses-table-body"),
            description
        )
    )

@when("the employee submits multiple expenses")
def submit_multiple_expenses(context):
    context.multiple_expenses = [
        {
            "amount": "12.50",
            "description": "E2E Multiple Meals Expense",
            "category": "Meals"
        },
        {
            "amount": "45.25",
            "description": "E2E Multiple Travel Expense",
            "category": "Travel"
        },
        {
            "amount": "18.75",
            "description": "E2E Multiple Supplies Expense",
            "category": "Supplies"
        }
    ]

    for expense in context.multiple_expenses:
        submit_expense(
            context,
            expense["amount"],
            expense["description"],
            expense["category"]
        )

@then("all submitted expenses should appear in the expense table")
def verify_multiple_expenses(context):
    table_text = context.driver.find_element(
        By.ID,
        "expenses-table-body"
    ).text

    for expense in context.multiple_expenses:
        assert expense["description"] in table_text, (
            f'Expense was not displayed: {expense["description"]}'
        )

        row = find_expense_row(
            context,
            expense["description"]
        )

        row_text = row.text

        assert expense["amount"] in row_text, (
            f'Expected amount {expense["amount"]} '
            f'in row: {row_text}'
        )

        assert expense["category"] in row_text, (
            f'Expected category {expense["category"]} '
            f'in row: {row_text}'
        )

        assert "pending" in row_text.lower(), (
            f'Expected pending status in row: {row_text}'
        )

@then("the employee deletes the multiple test expenses")
def delete_multiple_test_expenses(context):
    for expense in context.multiple_expenses:
        row = find_expense_row(
            context,
            expense["description"]
        )

        row.find_element(
            By.CLASS_NAME,
            "delete-button"
        ).click()

        context.wait.until(
            EC.alert_is_present()
        )

        Alert(context.driver).accept()

        description = expense["description"]

        context.wait.until(
            lambda driver: (
                description
                not in driver.find_element(
                    By.ID,
                    "expenses-table-body"
                ).text
            )
        )


@when("the employee enters a valid username but leaves the password blank")
def login_without_password(context):
    username_input = context.driver.find_element(
        By.ID,
        "username"
    )

    password_input = context.driver.find_element(
        By.ID,
        "password"
    )

    username_input.clear()
    username_input.send_keys(
        context.employee_username
    )

    password_input.clear()

    context.driver.find_element(
        By.ID,
        "login-button"
    ).click()

@then("the password field should display a required validation message")
def verify_password_required_message(context):
    password_input = context.driver.find_element(
        By.ID,
        "password"
    )

    validation_message = password_input.get_attribute(
        "validationMessage"
    )

    assert validation_message, (
        "Expected the browser to display a required-field "
        "validation message for the password."
    )