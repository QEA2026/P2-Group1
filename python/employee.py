import os
import sqlite3
from pathlib import Path

#from tabulate import tabulate
from expenseManager import ExpenseManager


def get_database_path() -> str:
    override = os.environ.get("EXPENSE_DB_PATH")
    if override:
        return override
    return str(Path(__file__).resolve().parent.parent / "revExpenseData.db")

class InvalidLoginError(Exception):
    ''' Incorrect username and password combination.'''
    pass

class NotSignedInError(Exception):
    ''' Employee's not signed in. '''
    pass

class ValueOutOfRangeError(Exception):
    ''' Amount value is not within acceptable range (valid range is [1, 10,000]). '''
    pass

class IdNotFoundError(Exception):
    ''' Expense id could not be found in expenses table for this employee. '''
    pass

class Employee:
    '''Represents one of the Employees in the user table, with an id, username, password, a logged in status, and a connection to the database.

    The employee has function calls to all the actions an employee can do, including login, logout, submit_expense, view_expense_status, edit_expense, and view_expense_history.
    Any of the function calls that edit the database (inserts, updates, deletes) have corresponding function calls in expenseManager.py to edit the database.

    Remember to close the passed in database connection object when it is no longer needed, the employee object will never close it.
    '''
    
    def __init__(self, id : int, username : str, password : str, connection : sqlite3.Connection = None, attempt_login = False):
        ''' Constructor for the Employee class,
        set signed_in to false by default.
        A connection object should be passed in, but if no connection object is passed in, defaults to connecting to revExpenseData.db, and this connection will never be closed.
        
        Raises an InvalidLoginError if attempt_login is set to true, and the login credentials are invalid'''
        self.id = id
        self.username = username
        self.password = password
        self.signed_in = False
        if(connection is not None):
            self.connection = connection
        else:
            self.connection = sqlite3.connect(get_database_path())
        if attempt_login:
            self.login(username, password)

    def login(self, username : str, password : str) -> None:
        ''' Signs the employee into the expense manager portal.
        Sets the instance variable signed_in to True if successful.
        
        Raises an InvalidLoginError if an incorrect Username and Password combination are entered'''
        dbCursor = self.connection.cursor()
        dbCursor.execute("SELECT * FROM users WHERE username == ?", (username,))
        user_entry = dbCursor.fetchone()
        if (user_entry is not None) and user_entry[2] == password and user_entry[3] == "Employee":
            self.signed_in = True
            self.id = user_entry[0]
            dbCursor.close()
        else:
            dbCursor.close()
            raise InvalidLoginError("Incorrect username and password combination")

    def logout(self) -> None:
        ''' Signs the employee out of the portal if signed in.
        Sets the instance variable signed_in to False if successful.
        Does nothing if the employee is not singed in.'''
        if self.signed_in == True:
            self.signed_in = False

    def submit_expense(self, amount : float, description : str, category : str = "Other") -> int:
        ''' Adds the expense with the specified amount and description entry into the expenses table,
        as long as the employee is signed in. Amount restrictions are between $1 and $10000 inclusive.
        Calls the add_expense method in expenseManager.py. Returns the expense id assigned to the expense.
        
        Raises a ValueOutOfRangeError if the amount is outside the value range [1,10000].
        Raises a NotSingedInError if the employee is not signed in.
        '''
        if self.signed_in == True:
            if amount < 1.00:
                raise ValueOutOfRangeError("Sorry, the reimbursement amount for the expense is invalid.")
            elif amount > 10000.00:
                raise ValueOutOfRangeError("Sorry, the maximum amount of reimbursement you can request for is $10,000.")
            elif len(description.strip()) == 0:
                raise ValueError("Sorry, the description cannot be empty.")
            else:
                return ExpenseManager.add_expense(self, amount, description, category)
        else:
            raise NotSignedInError("Sorry, you are not signed in. Please sign in first.")
    
    #TODO Make two SQL statements into one
    def view_expense_status(self, expense_id : int) -> str:
        ''' Returns the status of an expense given the specified expense id if the expense id is present
        in the list of submitted expenses. Additionally, the 
        expense id belongs to the employee that submitted the expense.
        
        Returns the status of the expense (pending, approved, or denied). 

        Raises an IdNotFoundError if no expense with the corresponding id is found in the database for this employee.
        Raises a NotSignedInError if the employee is not signed in.
        '''
        if self.signed_in == True:

            dbCursor = self.connection.cursor()
            dbCursor.execute("SELECT * FROM expenses WHERE user_id == ? AND id == ?", (self.id, expense_id))
            results = dbCursor.fetchall()
            matched = False
            for entry in results:
                if entry[0] == expense_id:
                    matched = True
            dbCursor.execute("SELECT * FROM approvals WHERE expense_id == ?", (expense_id,))
            approval_entry = dbCursor.fetchone()
            dbCursor.close()
            if matched and (approval_entry is not None):
                return approval_entry[2]
            else:
                raise IdNotFoundError("Sorry, we could not find the expense you were looking for.")
        else:
            raise NotSignedInError("Sorry, you are not signed in. Please sign in first.")

    def edit_expense(self, expense_id : int, amount : float, description : str, category : str = "Other") -> None:
        ''' Updates the expense with the specified id, amount (if specified), and description 
        (if specified) in the expenses table, as long as the employee is signed in. The 
        time should be updated as well. Amount restrictions are between $1 and $10000 
        inclusive. Calls the edit_expense method in expenseManager.py. More details in the 
        edit_expense function in expenseManager.py.

        Raises a ValueOutOfRangeError on values outside the range [1, 10000]
        Raises a NotSingedInError if the employee is not singed in.
        Raises an IdNotFoundError if no expense with the given id is found in the database for this employee.
        '''
        if self.signed_in == True:
            if amount < 1.00:
                raise ValueOutOfRangeError("Sorry, the reimbursement amount for the expense is invalid.")
            elif amount > 10000.00:
                raise ValueOutOfRangeError("Sorry, the maximum amount of reimbursement you can request for is $10,000.")
            elif len(description.strip()) == 0:
                raise ValueError("Sorry, the description cannot be empty.")
            else:
                ExpenseManager.edit_expense(self, expense_id, amount, description, category)
        else:
            raise NotSignedInError("Sorry, you are not signed in. Please sign in first.")

    def delete_expense(self, expense_id : int) -> None:
        ''' Removes the expense with the specified id from the expense list, as long as the employee
        is signed in. Calls the remove_expense method in expenseManager.py, with more details.

        Raises a NotSignedInError if the employee is not singed in.
        Raises an IdNotFoundError if no expense is found in the database with the given expense_id for this employee.
        Raises a ManagerDecisionError if the expense is found in the database but has already been approved or denied by a manager.
        '''
        if self.signed_in == True:
            ExpenseManager.remove_expense(self, expense_id)
        else:
            raise NotSignedInError("Sorry, you are not signed in. Please sign in first.")
        
    def view_expense_history(self) -> list:
        ''' Returns the list of all approved and denied expenses, in chronological order based on when the manager
        reviewed the expense, starting from newest to oldest, as long as the employee is signed in. Also 
        includes the status, manager comments, and review date from the approvals column, via a foreign key 
        named expense_id.

        Raises a NotSignedInError if the employee is not signed in.
        '''

        if self.signed_in == True:
            select_state = "SELECT " \
            "e.id, e.user_id, e.amount, e.description, a.status, a.comment, a.review_date " \
            "FROM expenses AS e " \
            "INNER JOIN approvals AS a ON a.expense_id = e.id " \
            "WHERE e.user_id == ? AND a.status != 'pending'" \
            "ORDER BY a.review_date DESC"
            dbCursor = self.connection.cursor()
            dbCursor.execute(select_state, (self.id, ))
            entries = dbCursor.fetchall()
            dbCursor.close()
            if not entries:
                print("Sorry, you don't have any expenses that are approved or denied yet.")
                return []
            else:
                if hasattr(dbCursor.description, "__iter__"):
                    list(dbCursor.description)
                formatted_entries = []
                for entry in entries:
                    entry_list = list(entry)
                    entry_list[2] = float(entry_list[2])
                    formatted_entries.append(tuple(entry_list))
                return formatted_entries
        else:
            raise NotSignedInError("Sorry, you are not signed in. Please sign in first.")

    def list_expenses(self, only_pending : bool = False) -> list:
        '''Returns a list of all pending expenses, including the expense id, amount, description, date, category and status (in that order).
        if is pending is true, will only list out pending expenses. Otherwise, all expenses will be included.
        
        Raises a NotSingedInError if the employee is not signed in.'''

        if not self.signed_in:
            raise NotSignedInError("Sorry, you are not signed in. Please sign in first.")
        dbCursor = self.connection.cursor()
        statement = "SELECT e.id, e.amount, e.description, e.date, e.category, a.status " \
                        "FROM expenses e JOIN approvals a " \
                        "ON e.id = a.expense_id " \
                        "WHERE e.user_id == ?"
        if only_pending:
            statement += " AND a.status = 'pending'"
        statement += " ORDER BY a.status"
        dbCursor.execute(statement, (self.id,))
        entries = dbCursor.fetchall()
        dbCursor.close()
        return list(entries)
    
#connect = sqlite3.connect("revExpenseData.db")
#bob = Employee(-1, "Bob", "bob_22", connect, False)