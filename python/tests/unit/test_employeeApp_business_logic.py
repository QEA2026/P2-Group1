import os
import sqlite3
import pytest
import unittest
import employee as employee
from employee import Employee
import expenseManager as expenseManager
import io
from contextlib import redirect_stdout

# Run tests as follows:
# pytest python/tests/employeeApp_business_logic.py -v

# Business logic of the Python employee application, such as valid login,
# incorrect username and password combo, valid amount range, expense and approval 
# entries created and added to respective database tables, success of expense submission
# database table saved after expense submission
# expense exist, expense belongs to user, status: (Pending, Approved, Denied), 
# expense in pending status, if manager reviewed expense, respective fields should be updated
# database table saved after updating expense 
# expense in pending status, if manager reviewed expense, respective entries should be removed from 
# expense and approval tables
# database table saved after expense deletion
# view expense history: shows entries that are approved or denied, don't include pending expenses

# Lastly, all inputs should be validated, invalid inputs should raise exception

@pytest.mark.parametrize("username,password",[("Bob", "bob_22"),],)
def test_login_valid_credentials_succeeds(username, password):
    emp = Employee(1, "testuser", "testpassword")
    emp.login(username, password)
    assert emp.signed_in is True


@pytest.mark.parametrize("username,password",[("B0b", "bob_22"), ("Bob", "bb_22"), ("", "bob_22"), ("Bob", ""), ("Andrew", "onetwothree"),],)
def test_login_invalid_credentials_raise_error(username, password):
    emp = Employee(1, "testuser", "testpassword")
    with pytest.raises(employee.InvalidLoginError):
        emp.login(username, password)

@pytest.mark.parametrize("amount,description,expected_exception",[
        (0.99, "Eraser set from Walmart", employee.ValueOutOfRangeError),
        (10000.01, "Car insurance", employee.ValueOutOfRangeError),
        (64.01, "", ValueError),
        (64.01, "         ", ValueError),],)
def test_submit_expense_invalid_inputs_raise_error(amount, description, expected_exception):
    emp = Employee(1, "testuser", "testpassword")
    emp.login("Bob", "bob_22")
    with pytest.raises(expected_exception):
        emp.submit_expense(amount, description)


@pytest.mark.parametrize("expense_id,expected_status",[(40, "pending"),(5, "denied"),(75, "approved"),],)
def test_view_expense_status_returns_expected_status(expense_id, expected_status):
    emp = Employee(1, "testuser", "testpassword")
    emp.login("Bob", "bob_22")
    assert emp.view_expense_status(expense_id) == expected_status

class TestEmployeeAppBusinessLogic(unittest.TestCase):

    # Basic tests for the Python employee application, to ensure the business logic
    # is working correctly. First two methods are setting up and tearing down the 
    # test environment.

    @classmethod
    def setUpClass(cls):
        cls.dbConnect = sqlite3.connect(os.environ["EXPENSE_DB_PATH"])

    def setUp(self):
        # Begin a database transaction
        emp = Employee(1, "testuser", "testpassword")
        emp.id = 1  # Assign an employee ID
        emp.signed_in = True  # Set employee as signed in
        self.dbConnect.commit()

    def test_submit_expense_while_logged_out(self):
        ''' Tests an attempt to submit an expense if the employee is not signed in.
        Should raise custom exception NotSignedInError. '''
        emp = Employee(1, "testuser", "testpassword")  
        with self.assertRaises(employee.NotSignedInError):
            emp.submit_expense(100, "Pens and pencils")
    
    def test_submit_expense_succesfully(self):
        ''' Tests an expense has been successfully submitted, with valid amount, 
        description and category. Checks if the total number of rows in the expenses and approvals tables
         have each incremented by 1. '''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        dbCursor = self.dbConnect.cursor()
        dbCursor.execute("SELECT COUNT(*) FROM expenses")
        total_rows_expenses_before = dbCursor.fetchone()[0]
        dbCursor.execute("SELECT COUNT(*) FROM approvals")
        total_rows_approvals_before = dbCursor.fetchone()[0]
        emp.submit_expense(100, "Pens and pencils")
        dbCursor.execute("SELECT COUNT(*) FROM expenses")
        total_rows_expenses_after = dbCursor.fetchone()[0]
        dbCursor.execute("SELECT COUNT(*) FROM approvals")
        total_rows_approvals_after = dbCursor.fetchone()[0]
        assert total_rows_expenses_after == total_rows_expenses_before + 1
        assert total_rows_approvals_after == total_rows_approvals_before + 1
        dbCursor.execute("SELECT * FROM expenses")
        entries = dbCursor.fetchall()
        reversed_entries = entries[::-1]
        last_row = reversed_entries[0]
        id_to_delete = last_row[0]
        dbCursor.execute("DELETE FROM expenses WHERE id == ?", (id_to_delete,))
        dbCursor.execute("DELETE FROM approvals WHERE expense_id == ?", (id_to_delete,))
        self.dbConnect.commit()

    def test_submit_expense_adds_approval_as_pending(self):
        ''' Checks that the entry in the approvals table has been added, as the last row. 
        Retrieves the last row. Assume a valid category input is entered. '''
        emp = Employee(1, "testuser", "testpassword")
        final_row = None
        approval_id = None
        emp.login("Bob", "bob_22")
        dbCursor = self.dbConnect.cursor()
        dbCursor.execute("SELECT * FROM approvals")
        every_row_before = dbCursor.fetchall()
        if every_row_before:
            final_row = every_row_before[-1]
        approval_id = final_row[0]
        emp.submit_expense(94.53, "Travel tolls and gas")      
        dbCursor.execute("SELECT * FROM approvals")
        every_row_after = dbCursor.fetchall()
        if every_row_after:
            final_row = every_row_after[-1]
        approval_id_diff = final_row[0]
        assert approval_id != approval_id_diff
        assert final_row[2] == "pending"
        assert final_row[3] == None
        assert final_row[4] == None
        assert final_row[5] == None
        dbCursor.execute("SELECT * FROM expenses")
        entries = dbCursor.fetchall()
        reversed_entries = entries[::-1]
        last_row = reversed_entries[0]
        id_to_delete = last_row[0]
        dbCursor.execute("DELETE FROM expenses WHERE id == ?", (id_to_delete,))
        dbCursor.execute("DELETE FROM approvals WHERE expense_id == ?", (id_to_delete,))
        self.dbConnect.commit()

    def test_view_status_expense_while_logged_out(self):
        ''' Tests an attempt to view status of an expense if the employee 
        is not signed in. Should raise custom exception NotSignedInError. '''
        emp = Employee(1, "testuser", "testpassword")  
        with self.assertRaises(employee.NotSignedInError):
            emp.view_expense_status(5)

    def test_view_status_expense_doesnt_exist(self):
        ''' Tests an attempt to view status of an expense that the employee 
        has not submitted, regardless of whether or not the expense id
        is submitted by a different employee.  '''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        with self.assertRaises(employee.IdNotFoundError):
            emp.view_expense_status(51)

    def test_update_expense_while_logged_out(self):
        ''' Tests an attempt to update an expense if the employee 
        is not signed in. Should raise custom exception NotSignedInError. '''
        emp = Employee(1, "testuser", "testpassword")  
        with self.assertRaises(employee.NotSignedInError):
            emp.edit_expense(42, 255.96, "Software update price changed due to various factors")

    def test_update_expense_doesnt_exist(self):
        ''' Tests an attempt to edit an expense that the employee 
        has not submitted, regardless of whether or not the expense id
        is submitted by a different employee. '''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        with self.assertRaises(employee.IdNotFoundError):
            emp.edit_expense(51, 255.96, "Software update price changed due to various factors")

    def test_update_expense_manager_reviewed(self):
        ''' Tests an attempt to edit an expense that a manager 
        has already reviewed, regardless if a manager approved 
        or denied it. Expenses that a manager has approved or 
        denied (no need to test all of them, just pick one of the following
        expenses): 5, 75, 20, 57, 26, 11 '''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        with self.assertRaises(expenseManager.ManagerDecisionError):
            emp.edit_expense(5, 26.01, "Hotel accommodations")

    def test_update_expense_description_blank(self):
        ''' Tests an attempt to edit an expense where the description is updated
        to be empty, raises a ValueError as a result. '''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        with self.assertRaises(ValueError):
            emp.edit_expense(42, 26.01, "")
        self.dbConnect.rollback()

    def test_update_expense_description_whitespace(self):
        ''' Tests an attempt to edit an expense where the description is updated
        to just contain whitespaces, raises a ValueError as a result. '''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        with self.assertRaises(ValueError):
            emp.edit_expense(42, 26.01, "      ")
        self.dbConnect.rollback()

    def test_update_expense_just_amount(self):
        ''' Tests an attempt to edit an expense where only the amount is updated. '''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        emp.edit_expense(40, 22.62, "Fix broken computer", "Repairs")
        dbCursor = self.dbConnect.cursor()
        dbCursor.execute("SELECT * FROM expenses WHERE id == 40")
        result = dbCursor.fetchone()
        assert result[2] == 22.62
        assert result[3] == "Fix broken computer"
        assert result[5] == "Repairs"
        emp.edit_expense(40, 19.34, "Fix broken computer", "Repairs")
        dbCursor.execute("SELECT * FROM expenses WHERE id == 40")
        result = dbCursor.fetchone()
        assert result[2] == 19.34
        assert result[3] == "Fix broken computer"
        assert result[5] == "Repairs"

    def test_update_expense_just_description(self):
        ''' Tests an attempt to edit an expense where only the description is updated. '''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        emp.edit_expense(40, 19.34, "Software update fee", "Repairs")
        dbCursor = self.dbConnect.cursor()
        dbCursor.execute("SELECT * FROM expenses WHERE id == 40")
        result = dbCursor.fetchone()
        assert result[2] == 19.34
        assert result[3] == "Software update fee"
        assert result[5] == "Repairs"
        emp.edit_expense(40, 19.34, "Fix broken computer", "Repairs")
        dbCursor.execute("SELECT * FROM expenses WHERE id == 40")
        result = dbCursor.fetchone()
        assert result[2] == 19.34
        assert result[3] == "Fix broken computer"
        assert result[5] == "Repairs"


    def test_update_expense_just_category(self):
        ''' Tests an attempt to edit an expense where only the category is updated. '''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        emp.edit_expense(40, 19.34, "Fix broken computer", "Computer")
        dbCursor = self.dbConnect.cursor()
        dbCursor.execute("SELECT * FROM expenses WHERE id == 40")
        result = dbCursor.fetchone()
        assert result[2] == 19.34
        assert result[3] == "Fix broken computer"
        assert result[5] != "Repairs"
        emp.edit_expense(40, 19.34, "Fix broken computer", "Repairs")
        dbCursor.execute("SELECT * FROM expenses WHERE id == 40")
        result = dbCursor.fetchone()
        assert result[2] == 19.34
        assert result[3] == "Fix broken computer"
        assert result[5] == "Repairs"

    def test_update_expense_description_and_amount(self):
        ''' Tests an attempt to edit an expense where the description 
        and amount are updated.'''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        emp.edit_expense(40, 22.62, "Repair price changed because of inflation", "Repairs")
        dbCursor = self.dbConnect.cursor()
        dbCursor.execute("SELECT * FROM expenses WHERE id == 40")
        result = dbCursor.fetchone()
        assert result[2] == 22.62
        assert result[3] == "Repair price changed because of inflation"
        assert result[5] == "Repairs"
        emp.edit_expense(40, 19.34, "Fix broken computer", "Repairs")
        dbCursor.execute("SELECT * FROM expenses WHERE id == 40")
        result = dbCursor.fetchone()
        assert result[2] == 19.34
        assert result[3] == "Fix broken computer"
        assert result[5] == "Repairs"
    
    def test_update_expense_category_and_amount(self):
        ''' Tests an attempt to edit an expense where the amount
        and category are updated.'''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        emp.edit_expense(40, 22.62, "Software update price changed due to various factors", "Computer")
        dbCursor = self.dbConnect.cursor()
        dbCursor.execute("SELECT * FROM expenses WHERE id == 40")
        result = dbCursor.fetchone()
        assert result[2] == 22.62
        assert result[3] == "Software update price changed due to various factors"
        assert result[5] != "Repairs"
        emp.edit_expense(40, 19.34, "Fix broken computer", "Repairs")
        dbCursor.execute("SELECT * FROM expenses WHERE id == 40")
        result = dbCursor.fetchone()
        assert result[2] == 19.34
        assert result[3] == "Fix broken computer"
        assert result[5] == "Repairs"

    def test_update_expense_category_and_description(self):
        ''' Tests an attempt to edit an expense where the description
        and category are updated. Assume the category option is any number except
        1 and 8. '''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        emp.edit_expense(40, 19.34, "Software price", "Computer")
        dbCursor = self.dbConnect.cursor()
        dbCursor.execute("SELECT * FROM expenses WHERE id == 40")
        result = dbCursor.fetchone()
        assert result[2] == 19.34
        assert result[3] == "Software price"
        assert result[5] != "Repairs"
        emp.edit_expense(40, 19.34, "Fix broken computer", "Repairs")
        dbCursor.execute("SELECT * FROM expenses WHERE id == 40")
        result = dbCursor.fetchone()
        assert result[2] == 19.34
        assert result[3] == "Fix broken computer"
        assert result[5] == "Repairs"

    def test_update_expense_amount_category_and_description(self):
        ''' Tests an attempt to edit an expense where the amount, description,
        and category are all updated.'''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        emp.edit_expense(40, 22.62, "Additional charge for fix bc something else broke", "Software")
        dbCursor = self.dbConnect.cursor()
        dbCursor.execute("SELECT * FROM expenses WHERE id == 40")
        result = dbCursor.fetchone()
        assert result[2] == 22.62
        assert result[3] == "Additional charge for fix bc something else broke"
        assert result[5] != "Repairs"
        emp.edit_expense(40, 19.34, "Fix broken computer", "Repairs")
        dbCursor.execute("SELECT * FROM expenses WHERE id == 40")
        result = dbCursor.fetchone()
        assert result[2] == 19.34
        assert result[3] == "Fix broken computer"
        assert result[5] == "Repairs"

    def test_update_expense_amount_below_one_dollar(self):
        ''' Tests an attempt to update the expense amount below the 
        minimum acceptable amount. Should throw a ValueOutOfRangeError. '''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        with self.assertRaises(employee.ValueOutOfRangeError):
            emp.edit_expense(42, 0.99, "New pencil")

    def test_update_expense_amount_above_10000_dollars(self):
        ''' Tests an attempt to update the expense amount above the 
        maximum acceptable amount. Should throw a ValueOutOfRangeError. '''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        with self.assertRaises(employee.ValueOutOfRangeError):
            emp.edit_expense(42, 10000.01, "New Cognizant truck rental")

    def test_delete_expense_while_logged_out(self):
        ''' Tests an attempt to delete an expense if the employee is not signed in.
        Should raise custom exception NotSignedInError. '''
        emp = Employee(1, "testuser", "testpassword")  
        with self.assertRaises(employee.NotSignedInError):
            emp.delete_expense(39)

    def test_delete_expense_doesnt_exist(self):
        ''' Tests an attempt to delete an expense that the employee has not submitted,
        regardless of whether or not the expense id is submitted by a different employee. '''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        with self.assertRaises(employee.IdNotFoundError):
            emp.delete_expense(51)

    def test_delete_expense_manager_reviewed(self):
        ''' Tests an attempt to delete an expense that a manager 
        has already reviewed, regardless if a manager approved 
        or denied it. Expenses that a manager has approved or 
        denied (no need to test all of them, just pick one of the following
        expenses): 5, 75, 20, 57, 26, 11 '''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        with self.assertRaises(expenseManager.ManagerDecisionError):
            emp.delete_expense(5)

    def test_delete_expense_successfully(self):
        ''' Tests if the attempt to delete an expense is successful. 
        The expenses that can be deleted are 40 and 39. '''        
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        dbCursor = self.dbConnect.cursor()
        deleted_exp = dbCursor.execute("SELECT * FROM expenses WHERE id == 40").fetchone()
        deleted_app = dbCursor.execute("SELECT * FROM approvals WHERE expense_id == 40").fetchone()
        emp.delete_expense(40)
        assert dbCursor.execute("SELECT * FROM expenses WHERE id == 40").fetchone() == None
        assert dbCursor.execute("SELECT * FROM approvals WHERE expense_id == 40").fetchone() == None
        dbCursor.execute("INSERT INTO expenses (id, user_id, amount, description, date, category) VALUES (?, ?, ?, ?, ?, ?)", (deleted_exp[0], deleted_exp[1], deleted_exp[2], deleted_exp[3], deleted_exp[4], deleted_exp[5]))
        dbCursor.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (?, ?, ?, ?, ?, ?)", (deleted_app[0], deleted_app[1], deleted_app[2], deleted_app[3], deleted_app[4], deleted_app[5]))
        assert dbCursor.execute("SELECT * FROM expenses WHERE id == 40").fetchone() != None
        assert dbCursor.execute("SELECT * FROM approvals WHERE expense_id == 40").fetchone() != None
        self.dbConnect.commit()

    def test_view_history_expenses_while_logged_out(self):
        ''' Tests an attempt to view expense history if the employee is not signed in.
        Should raise custom exception NotSignedInError. '''
        emp = Employee(1, "testuser", "testpassword")  
        with self.assertRaises(employee.NotSignedInError):
            emp.view_expense_history()

    def test_view_history_expenses_all_pending(self):
        ''' Tests an attempt to view expense history if there are
        no submitted expenses that are approved or denied. The table 
        should not show any entries at all. '''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Caleb", "jira1")
        printed_line = io.StringIO()
        with redirect_stdout(printed_line):
            emp.view_expense_history()
        self.assertIn("Sorry, you don't have any expenses that are approved or denied yet.", printed_line.getvalue())


    def test_view_history_expenses(self):
        ''' Tests an attempt to view expense history ensuring the approval 
        review date is sorted from newest to oldest. '''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        printed_line = io.StringIO()
        with redirect_stdout(printed_line):
            emp.view_expense_history()
        self.assertNotIn("Sorry, you don't have any expenses that are approved or denied yet.", printed_line.getvalue())

    def test_logout_successful(self):
        ''' Tests a successful attempt to sign the employee out of the portal. '''
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        emp.logout()
        assert emp.signed_in == False

    def test_logout_unsuccessful(self):
        emp = Employee(1, "testuser", "testpassword")
        emp.login("Bob", "bob_22")
        assert emp.signed_in == True
    
    def tearDown(self):
        # Clean up any test data from the database if necessary
        emp = Employee(1, "testuser", "testpassword")
        emp.signed_in = False  # Set employee as logged out
        emp = None
        self.dbConnect.rollback()

    @classmethod
    def tearDownClass(cls):
        cls.dbConnect.close()