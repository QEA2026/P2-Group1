import unittest
from unittest.mock import patch, Mock, MagicMock, ANY
import sqlite3
import pytest

import employee as employee
from employee import Employee
import expenseManager as expenseManager
from expenseManager import ExpenseManager

# Run tests as follows:
# pytest python/tests/employeeApp_mocking.py -v

class TestEmployeeAppMocking(unittest.TestCase):

    def setUp(self):
        self.mock_cursor = MagicMock()
        self.mock_conn = Mock(spec=sqlite3.Connection)
        self.mock_conn.cursor.return_value = self.mock_cursor
        self.employee = Employee(1, "testuser", "testpassword", connection=self.mock_conn)
        self.employee.signed_in = True    

    def test_login_invalid_password_does_not_commit(self):        
        mock_cursor = MagicMock()
        mock_cursor.fetchone.return_value = (2, "Bob", "wrong_password", "Employee")
        mock_conn = Mock(spec=sqlite3.Connection)
        mock_conn.cursor.return_value = mock_cursor

        emp = Employee(0, "B0b", "bob_22", connection=mock_conn)
        with self.assertRaises(employee.InvalidLoginError):
            emp.login("B0b", "bob_22")       
        mock_cursor.close.assert_called_once()
        mock_conn.commit.assert_not_called()
    
    def test_login_valid_username_password_calls_database(self):
        mock_cursor = Mock()
        mock_cursor.fetchone.return_value = (2, "Bob", "bob_22", "Employee")
        mock_conn = Mock(spec=sqlite3.Connection)
        mock_conn.cursor.return_value = mock_cursor
        emp = Employee(0, "Bob", "bob_22", connection=mock_conn)
        emp.login("Bob", "bob_22")
        mock_cursor.execute.assert_called_once_with("SELECT * FROM users WHERE username = ?", ("Bob",))
        mock_cursor.fetchone.assert_called_once()
        mock_conn.cursor.assert_called_once()
        mock_cursor.close.assert_called_once()
        assert emp.signed_in == True
        assert emp.id == 2

    def test_submit_expense_while_logged_out_does_not_call_database(self):
        emp = Employee(1, "testuser", "testpassword", connection=self.mock_conn)
        with self.assertRaises(employee.NotSignedInError):
            emp.submit_expense(100.0, "Pens and pencils")
        self.mock_conn.cursor.assert_not_called()

    @patch("employee.ExpenseManager.add_expense")
    def test_submit_expense_uses_expense_manager(self, mock_add_expense):
        mock_add_expense.return_value = 777
        status = self.employee.submit_expense(100.0, "Pens and pencils", "Office")
        mock_add_expense.assert_called_once_with(self.employee, 100.0, "Pens and pencils", "Office", convert_syntax=False)
        assert status == 777

    @patch("expenseManager.random.randint", return_value = 123)
    def test_add_expense_commits_and_inserts_records(self, mock_assign_random_id):
        self.mock_cursor.close = MagicMock()
        self.mock_conn.commit = MagicMock()
        self.mock_cursor.execute = MagicMock()
        self.mock_cursor.fetchall.return_value = []
        assigned_id = ExpenseManager.add_expense(self.employee, 100.0, "Lunch", "Food")        
        self.mock_cursor.execute.assert_any_call(
            "INSERT INTO expenses (id, user_id, amount, description, date, category) VALUES (?, ?, FLOOR(? * 100) / 100.0, ?, ?, ?)",
            (123, 1, 100.0, "Lunch", ANY, "Food"),)
        self.mock_cursor.execute.assert_any_call(
            "INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (?, ?, ?, ?, ?, ?)",
            (123, 123, "pending", None, None, None),)
        self.mock_cursor.close.assert_called_once()
        self.mock_conn.commit.assert_called_once()
        assert assigned_id == 123

    def test_view_expense_status_raises_when_expense_not_found(self):
        self.mock_cursor.fetchone.return_value = None
        self.mock_cursor.fetchall.return_value = []        
        with self.assertRaises(employee.IdNotFoundError):
            self.employee.view_expense_status(999)
        self.mock_cursor.close.assert_called_once()
        self.mock_cursor.execute.assert_any_call("SELECT * FROM expenses WHERE user_id = ? AND id = ?", (1, 999))
        self.mock_cursor.execute.assert_any_call("SELECT * FROM approvals WHERE expense_id = ?", (999,))        
    
    def test_view_expense_status_queries_expenses_and_approvals(self):
        self.mock_cursor.fetchall.return_value = [(42, 1, 100.0, "desc", "2026-01-01", "Other")]
        self.mock_cursor.fetchone.return_value = (42, 42, "pending", None, None, None)
        exp_status = self.employee.view_expense_status(42)
        self.mock_cursor.execute.assert_any_call(
            "SELECT * FROM expenses WHERE user_id = ? AND id = ?", (1, 42))
        self.mock_cursor.execute.assert_any_call(
            "SELECT * FROM approvals WHERE expense_id = ?", (42,))
        self.mock_cursor.close.assert_called_once()
        assert exp_status == "pending"

    @patch("employee.Employee.view_expense_status", return_value="approved")
    def test_edit_expense_raises_without_database_update(self, status_of_mock):
        with self.assertRaises(expenseManager.ManagerDecisionError):
            ExpenseManager.edit_expense(self.employee, 42, 250.0, "Updated description", "Travel")        
        self.mock_conn.commit.assert_not_called()
        self.mock_cursor.execute.assert_not_called()
        status_of_mock.assert_called_once_with(42)
        

    @patch("employee.Employee.view_expense_status", return_value="pending")
    def test_edit_expense_updates_database_and_commits(self, status_of_mock):
        ExpenseManager.edit_expense(self.employee, 42, 250.0, "Updated description", "Travel")
        self.mock_cursor.close.assert_called_once()
        self.mock_conn.commit.assert_called_once()
        self.mock_cursor.execute.assert_called_once_with(
            "UPDATE expenses SET amount = FLOOR(? * 100) / 100.0, description = ?, date = ?, category = ? WHERE id = ?",
            (250.0, "Updated description", ANY, "Travel", 42),)  
        status_of_mock.assert_called_once_with(42)      

    @patch("employee.Employee.view_expense_status", return_value="pending")
    def test_remove_expense_deletes_records_and_commits(self, status_of_mock):
        ExpenseManager.remove_expense(self.employee, 42)
        self.mock_cursor.close.assert_called_once()
        self.mock_conn.commit.assert_called_once()
        self.mock_cursor.execute.assert_any_call("DELETE from expenses WHERE id = ?", (42,))
        self.mock_cursor.execute.assert_any_call("DELETE from approvals WHERE expense_id = ?", (42,))       

    def test_view_expense_history_while_signed_out_raises_and_no_query(self):
        self.employee.logout()
        with self.assertRaises(employee.NotSignedInError):
            self.employee.view_expense_history()
        self.mock_conn.cursor.assert_not_called()

    def test_list_expenses_only_pending_calls_database(self):
        self.mock_cursor.fetchall.return_value = [(10, 123.0, "Lunch", "2026-05-01", "Food", "pending")]
        list_exp = self.employee.list_expenses(True)
        self.mock_cursor.execute.assert_called_once_with(
            "SELECT e.id, e.amount, e.description, e.date, e.category, a.status "
            "FROM expenses e JOIN approvals a "
            "ON e.id = a.expense_id "
            "WHERE e.user_id = ? AND a.status = 'pending'"
            " ORDER BY a.status", (1,),)
        assert list_exp == [(10, 123.0, "Lunch", "2026-05-01", "Food", "pending")]

    def test_view_expense_history_queries_database_and_returns_entries(self):
        self.mock_cursor.description = [("id",), ("user_id",), ("amount",), ("description",), ("status",), ("comment",), ("review_date",)]
        self.mock_cursor.fetchall.return_value = [(1, 1, 45.0, "Taxi", "denied", "No receipt", "2026-05-01")]
        list_exp = self.employee.view_expense_history()
        self.mock_cursor.close.assert_called_once()
        assert list_exp == [(1, 1, 45.0, "Taxi", "denied", "No receipt", "2026-05-01")]
        self.mock_cursor.execute.assert_called_once_with(
            "SELECT e.id, e.user_id, e.amount, e.description, a.status, a.comment, a.review_date "
            "FROM expenses AS e "
            "INNER JOIN approvals AS a ON a.expense_id = e.id "
            "WHERE e.user_id = ? AND a.status != 'pending'"
            "ORDER BY a.review_date DESC", (1,),)
        
    def test_view_expense_history_uses_magicmock_description(self):
        self.mock_cursor.description.__iter__.return_value = iter([("id",), ("user_id",), ("amount",), ("description",), ("status",), ("comment",), ("review_date",)])
        self.mock_cursor.description = MagicMock()
        self.mock_cursor.fetchall.return_value = [(2, 1, 75.0, "Parking", "approved", "Looks good", "2026-05-02")]       
        table_entries = self.employee.view_expense_history()               
        assert table_entries == [(2, 1, 75.0, "Parking", "approved", "Looks good", "2026-05-02")]
        self.mock_cursor.description.__iter__.assert_called()

    def tearDown(self):
        self.employee.signed_in = False
        self.employee = None
        self.mock_cursor = None
        self.mock_conn = None