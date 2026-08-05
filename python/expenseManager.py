import datetime
import random
import sqlite3
#from employee import Employee

class ManagerDecisionError(Exception):
    ''' A manager already made a decision on the expense. '''
    pass

#TODO: Take in a cursor object from the employee.py for all transactions.
class ExpenseManager:
    ''' Class with some helper utility functions for the employee.py, for any function that edits the database directly.

    Includes add_expense, edit_expense, and remove_expense.'''
    @classmethod
    def date_conversion(cls, date : str) -> str:
        ''' Converts the specified date to YYYY/MM/DD format
        Current format is MM/DD/YYYY
        '''
        date_list = date.split("/")
        month = date_list[0]
        day = date_list[1]
        date_list[0] = date_list[2]
        date_list[1] = month
        date_list[2] = day
        dateStr = "/".join(date_list)
        return dateStr
    

    def add_expense(emp, amount : float, description : str, category : str) -> int:
        ''' Adds an entry with the specified amount and description to the expenses 
        table (also adds an entry to the approvals table), the employee id 
        associated with the employee submitting the expense
        is added to the expense entry as well. The expense_id is randomly generated 
        between 1-2999 and is unique from other expense ids. The approval id uses the expense id.
        The employee also puts in the category for the submitted expense.
        '''
        dbCursor = emp.connection.cursor()
        assign_id = random.randint(1, 2999)
        while dbCursor.fetchone != None:
            dbCursor.execute("SELECT * FROM expenses WHERE id = ?", (assign_id,))
            result = dbCursor.fetchall()
            if len(result) != 0:
                assign_id = random.randint(1, 2999)
            else:
                break
        dbCursor.execute("INSERT INTO expenses (id, user_id, amount, description, date, category) VALUES (?, ?, FLOOR(? * 100) / 100.0, ?, ?, ?)", (assign_id, emp.id, amount, description, ExpenseManager.date_conversion(datetime.datetime.now().strftime("%m/%d/%Y")), category))
        pending_null = None
        dbCursor.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (?, ?, ?, ?, ?, ?)", (assign_id, assign_id, "pending", pending_null, pending_null, pending_null))
        emp.connection.commit()
        dbCursor.close()
        return assign_id

    def edit_expense(emp, id : int, amount : float, description : str, category : str) -> None:
        ''' Edits the expense with the specified id, amount, and description.
        Calls the instance method view_expense_status in the employee class
        to check if the expense is pending.
        The category can also be updated by the discretion of the employee.

        Raises a ManagerDecisionError if the expense is not pending (either approved or denied by a manager).
        Raises an IdNotFoundError if no expense with the given id is found in the database for the employee.
        '''

        status = emp.view_expense_status(id)
        if status == "pending":
            dbCursor = emp.connection.cursor()
            dbCursor.execute("UPDATE expenses SET amount = FLOOR(? * 100) / 100.0, description = ?, date = ?, category = ? WHERE id = ?", (amount, description, ExpenseManager.date_conversion(datetime.datetime.now().strftime("%m/%d/%Y")), category, id))
            emp.connection.commit()
            dbCursor.close()

        else:
            raise ManagerDecisionError(f"Sorry, a manager has made the decision for this expense. It has been {status}.")

    def remove_expense(emp, id : int) -> None:
        ''' Removes the expense from the list given the specified id.
        Calls the instance method view_expense_status in the employee class
        to check if the expense is pending. If expense is not pending, a 
        ManagerDecisionError is thrown, otherwise, the expense entry gets
        removed from the expenses table. Additionally, the respective approvals
        entry is removed from the approvals table.

        Raises a ManagerDecisionError if the expense is not in pending status (either approved or denied by a manager).
        Raises an IdNotFoundError if no expense with the given id is found in the database for the employee.
        ''' 
        status = emp.view_expense_status(id)
        if status == "pending":
            dbCursor = emp.connection.cursor()
            dbCursor.execute("DELETE from expenses WHERE id = ?", (id,))
            dbCursor.execute("DELETE from approvals WHERE expense_id = ?", (id,))
            dbCursor.close()
            emp.connection.commit()
        else:
            raise ManagerDecisionError(f"Sorry, a manager has made the decision for this expense. It has been {status}.")