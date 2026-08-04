import os
import pytest
import sqlite3
import io
import unittest
import employee as employee
from employee import Employee
from contextlib import redirect_stdout
import expenseManager as expenseManager

# Run tests as follows:
# pytest python/tests/employeeApp_happyPath_sadPath.py -v

# Happy and sad paths of the Python employee application
# for the login, submit expense, view status of an expense,
# edit an expense, delete an expense, and view history of 
# approved/denied expenses.

class TestEmployeeAppLogin(unittest.TestCase):

    # Tests for the Python employee application, to ensure that 
    # the happy and sad paths are working correctly. First two methods 
    # are setting up and tearing down the test environment. The remaining 
    # methods ensure that the login functionality works.

    @classmethod
    def setUpClass(cls):
        cls.dbConnect = sqlite3.connect(os.environ["EXPENSE_DB_PATH"])

    def setUp(self):
        self.emp = Employee(1, "testuser", "testpassword")

    def test_login_path_happy(self):
        '''Tests an attempt to ensure login is successful with valid credentials.'''
        cases = [("Bob", "bob_22", 2)]
        for username, password, expected_id in cases:
            with self.subTest(username=username):
                emp = Employee(1, "testuser", "testpassword")
                emp.login(username, password)
                self.assertTrue(emp.signed_in)
                self.assertEqual(emp.id, expected_id)

    def test_login_path_sad(self):
        '''Tests an attempt to ensure login fails with invalid credentials.'''
        cases = [("B0b", "bob_22"), ("Bob", "wrong"), ("", "bob_22")]
        for username, password in cases:
            with self.subTest(username=username):
                emp = Employee(1, "testuser", "testpassword")
                with self.assertRaises(employee.InvalidLoginError):
                    emp.login(username, password)

    def tearDown(self):
        self.emp.logout()
        self.emp = None

    @classmethod
    def tearDownClass(cls):
        cls.dbConnect.close()

class TestEmployeeAppHappySadPath(unittest.TestCase):

    # Tests for the Python employee application, to ensure that 
    # the happy and sad paths are working correctly. First two methods 
    # are setting up and tearing down the test environment. The remaining 
    # methods ensure that the submit expense, view status of an expense,
    # edit an expense, delete an expense, and view history of 
    # approved/denied expenses functionality works.

    @classmethod
    def setUpClass(cls):
        cls.dbConnect = sqlite3.connect(os.environ["EXPENSE_DB_PATH"])

    def setUp(self):
        # Begin a database transaction
        self.emp = Employee(1, "testuser", "testpassword")
        self.emp.login("Bob", "bob_22")

    def test_submit_expense_path_happy(self):
        '''Submit expense with valid input is successful.'''
        cases = [(22.22, "Snacks catering")]
        for amount, description in cases:
            with self.subTest(amount=amount, description=description):
                dbCursor = self.dbConnect.cursor()
                dbCursor.execute("SELECT COUNT(*) FROM expenses")
                before_rows_expenses = dbCursor.fetchone()[0]
                dbCursor.execute("SELECT COUNT(*) FROM approvals")
                before_rows_approvals = dbCursor.fetchone()[0]
                self.emp.submit_expense(amount, description)
                dbCursor.execute("SELECT COUNT(*) FROM expenses")
                after_rows_expenses = dbCursor.fetchone()[0]
                dbCursor.execute("SELECT COUNT(*) FROM approvals")
                after_rows_approvals = dbCursor.fetchone()[0]
                self.assertEqual(after_rows_expenses, before_rows_expenses + 1)
                self.assertEqual(after_rows_approvals, before_rows_approvals + 1)
                dbCursor.execute("SELECT * FROM expenses")
                entries = dbCursor.fetchall()
                reversed_entries = entries[::-1]
                last_row = reversed_entries[0]
                id_to_delete = last_row[0]
                dbCursor.execute("DELETE FROM expenses WHERE id == ?", (id_to_delete,))
                dbCursor.execute("DELETE FROM approvals WHERE expense_id == ?", (id_to_delete,))
                self.dbConnect.commit()

    def test_submit_expense_path_sad(self):
        '''Submit expense with invalid input should fail properly.'''
        cases = [(0.56, "Toy car", employee.ValueOutOfRangeError), (64.01, "", ValueError)]
        for amount, description, expected_exception in cases:
            with self.subTest(amount=amount, description=description):
                with self.assertRaises(expected_exception):
                    self.emp.submit_expense(amount, description)

    def test_view_status_expense_path_happy(self):
        '''View status of an expense with an expense id the employee has submitted.'''
        cases = [(5,), (40,)]
        for (expense_id,) in cases:
            with self.subTest(expense_id=expense_id):
                status = self.emp.view_expense_status(expense_id)
                self.assertIn(status, {"pending", "approved", "denied"})

    def test_view_status_expense_path_sad(self):
        '''Attempt to view status of an expense that the employee has not submitted should fail properly.'''
        cases = [(99,), (1000,)]
        for (expense_id,) in cases:
            with self.subTest(expense_id=expense_id):
                with self.assertRaises(employee.IdNotFoundError):
                    self.emp.view_expense_status(expense_id)

    def test_update_expense_path_happy(self):
        '''Expense is updated successfully with all valid inputs.'''
        cases = [(64, 24.92, "Forgot to add in the snacks catering tax rate amount", "Meals")]
        for expense_id, new_amount, new_description, new_category in cases:
            with self.subTest(expense_id=expense_id):
                self.emp.edit_expense(expense_id, new_amount, new_description, new_category)
                dbCursor = self.dbConnect.cursor()
                dbCursor.execute("SELECT * FROM expenses WHERE id == ?", (expense_id,))
                result = dbCursor.fetchone()
                self.assertEqual(result[2], new_amount)
                self.assertEqual(result[3], new_description)
                self.assertEqual(result[5], new_category)
                self.emp.edit_expense(expense_id, 22.22, "Snacks catering", "Repairs")
                dbCursor.execute("SELECT * FROM expenses WHERE id == ?", (expense_id,))
                result = dbCursor.fetchone()
                self.assertEqual(result[2], 22.22)
                self.assertEqual(result[3], "Snacks catering")
                self.assertEqual(result[5], "Repairs")

    def test_update_expense_path_sad(self):
        '''Attempting to update an expense with an invalid input raises an exception.'''
        cases = [(75, 255.99, "Software update fee changed due to various factors", "Repairs")]
        for expense_id, new_amount, new_description, new_category in cases:
            with self.subTest(expense_id=expense_id):
                with self.assertRaises(expenseManager.ManagerDecisionError):
                    self.emp.edit_expense(expense_id, new_amount, new_description, new_category)

    def test_delete_expense_path_happy(self):
        '''Expense is deleted successfully with a valid expense id.'''
        cases = [(40,)]
        for (expense_id,) in cases:
            with self.subTest(expense_id=expense_id):
                dbCursor = self.dbConnect.cursor()
                dbCursor.execute("SELECT * FROM expenses WHERE id == ?", (expense_id,))
                deleted_exp = dbCursor.fetchone()
                deleted_app = dbCursor.execute("SELECT * FROM approvals WHERE expense_id == ?", (expense_id,)).fetchone()
                self.emp.delete_expense(expense_id)
                self.assertIsNone(dbCursor.execute("SELECT * FROM expenses WHERE id == ?", (expense_id,)).fetchone())
                self.assertIsNone(dbCursor.execute("SELECT * FROM approvals WHERE expense_id == ?", (expense_id,)).fetchone())
                dbCursor.execute(
                    "INSERT INTO expenses (id, user_id, amount, description, date, category) VALUES (?, ?, ?, ?, ?, ?)",
                    (deleted_exp[0], deleted_exp[1], deleted_exp[2], deleted_exp[3], deleted_exp[4], deleted_exp[5]),
                )
                dbCursor.execute(
                    "INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (?, ?, ?, ?, ?, ?)",
                    (deleted_app[0], deleted_app[1], deleted_app[2], deleted_app[3], deleted_app[4], deleted_app[5]),
                )
                self.assertIsNotNone(dbCursor.execute("SELECT * FROM expenses WHERE id == ?", (expense_id,)).fetchone())
                self.assertIsNotNone(dbCursor.execute("SELECT * FROM approvals WHERE expense_id == ?", (expense_id,)).fetchone())
                self.dbConnect.commit()

    def test_delete_expense_path_sad(self):
        '''Attempting to delete an expense with an invalid input raises an exception.'''
        cases = [(75,)]
        for (expense_id,) in cases:
            with self.subTest(expense_id=expense_id):
                with self.assertRaises(expenseManager.ManagerDecisionError):
                    self.emp.delete_expense(expense_id)

    def test_view_history_expenses_path_happy(self):
        '''Viewing a list of expenses successfully where there is at least one approved or denied expense.'''
        cases = [("Bob", "bob_22")]
        for username, password in cases:
            with self.subTest(username=username):
                self.emp.logout()
                self.emp.login(username, password)
                printed_line = io.StringIO()
                with redirect_stdout(printed_line):
                    self.emp.view_expense_history()
                self.assertNotIn("Sorry, you don't have any expenses that are approved or denied yet.", printed_line.getvalue())

    def test_view_history_expenses_path_sad(self):
        '''Attempts to view a list of expenses where there aren't any approved or denied expenses.'''
        cases = [("Caleb", "jira1")]
        for username, password in cases:
            with self.subTest(username=username):
                self.emp.logout()
                self.emp.login(username, password)
                printed_line = io.StringIO()
                with redirect_stdout(printed_line):
                    self.emp.view_expense_history()
                self.assertIn("Sorry, you don't have any expenses that are approved or denied yet.", printed_line.getvalue())

    def tearDown(self):
        self.emp.logout()
        self.emp = None

    @classmethod
    def tearDownClass(cls):
        cls.dbConnect.close()