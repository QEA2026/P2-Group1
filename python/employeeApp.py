# CLI Interface for the Employee App, takes user input and data entry
import sqlite3
from getpass import getpass
from tabulate import tabulate
import employee
from employee import Employee
import expenseManager

# Beginning of the employee app
print("Welcome to the Revature Expense Manager! Please login first: ")
while True:
    emp = Employee(3, "Alice", "rocks77")
    username = input("Enter your username: ")
    password = getpass(prompt = "Enter your password: ")
    emp.login(username, password)
    if (emp.signed_in == True):
        print(f"Login successful! Welcome {username}!")
        dbConnect = sqlite3.connect("revExpenseData.db")
        dbCursor = dbConnect.cursor()
        dbCursor.execute("SELECT * FROM users WHERE username == ?", (username,))
        user_entry = dbCursor.fetchone()
        emp.id = user_entry[0]
        while True:
            try:
                print("Here are your options to manage expenses. Press the number of the option you want to choose:")
                print("1) Submit an expense")
                print("2) View status of an expense")
                print("3) Edit an expense")
                print("4) Delete an expense")
                print("5) View all approved/denied expenses")
                print("6) Exit")
                option = int(input())
            except ValueError:
                print("Invalid input. Try again.")
                continue
            if option < 1 or option > 6:
                print("Sorry, only options 1-6 are available, try again.")
            else:
                if option == 1:
                    while True:
                        try:
                            amount = input("Enter an amount for the expense you want to be reimbursed for, or press any non-numeric key to return to the main menu: ")
                            number = float(amount)
                            desc = input("Enter a description for the expense: ")
                            emp.submit_expense(number, desc)
                            break
                        except ValueError:
                            break
                        except employee.ValueOutOfRangeError as vr:
                            print(str(vr) + " Try again.")
                elif option == 2:
                    dbCursor.execute("SELECT id, amount, description, date FROM expenses WHERE user_id == ?", (emp.id,))
                    entries = dbCursor.fetchall()
                    if not entries:
                        print("Sorry, you did not submit any expenses. Please submit an expense to view the status.")
                    else:
                        list_expenses = []
                        for column_name in dbCursor.description:
                            list_expenses.append(column_name[0])
                        #print(list_expenses)
                        for entry in entries:
                            entry = list(entry)
                            entry[1] = f"{float(entry[1]):.2f}"
                            #print(entry)
                        print(tabulate(entries, headers = list_expenses, tablefmt = "grid", floatfmt = ".2f"))
                        while True:
                            try:
                                exp_id = int(input("Select an expense id from the list in order to retrieve its status, or press -1 to return to the main menu: "))
                                if exp_id == -1:
                                    break
                                else:
                                    emp.view_expense_status(exp_id)
                                    print("The status of expense with id " + str(exp_id) + " is " + emp.view_expense_status(exp_id))                            
                                    break
                            except ValueError:
                                print("Invalid input, try again.")
                            except employee.IdNotFoundError as IDerror:
                                print(str(IDerror) + " Try again.")
                elif option == 3:
                    statement = "SELECT e.id, e.amount, e.description, e.date " \
                        "FROM expenses e JOIN approvals a " \
                        "ON e.id = a.expense_id " \
                        "WHERE e.user_id == ? AND a.status == 'pending'"
                    dbCursor.execute(statement, (emp.id,))
                    entries = dbCursor.fetchall()
                    if not entries:
                        print("Sorry, there are no available expenses for you to edit.")
                    else:
                        list_expenses = []
                        for column_name in dbCursor.description:
                            list_expenses.append(column_name[0])
                        #print(list_expenses)
                        for entry in entries:
                            entry = list(entry)
                            entry[1] = f"{float(entry[1]):.2f}"
                            #print(entry)
                        print(tabulate(entries, headers = list_expenses, tablefmt = "grid", floatfmt = ".2f"))
                        while True:
                            try:
                                exp_id = int(input("Select the id of the expense from the list you want to edit, or type any non-positive numbers or non-number characters (including the .) to return to the main menu: "))
                                if exp_id <= 0:
                                    raise ValueError
                            except ValueError:
                                break
                            dbCursor.execute("SELECT * FROM expenses WHERE id = ?", (exp_id,))
                            exp_id_entry = dbCursor.fetchone()
                            amount_change = input("Do you want to change the expense amount? Press y for yes: ")
                            if amount_change == "y":
                                while True:
                                    try:                               
                                        exp_amount = float(input("Enter the amount of the expense you want to be reimbursed for: "))
                                        break
                                    except ValueError:
                                        print("Invalid input, try again.")
                                        continue
                            else:
                                if exp_id_entry:
                                    exp_amount = exp_id_entry[2]
                            desc_change = input("Do you want to change the expense description? Press y for yes: ")
                            if desc_change == "y":
                                exp_desc = input("Enter the description of the expense: ")
                            else:
                                if exp_id_entry:
                                    exp_desc = exp_id_entry[3]
                            try:                        
                                if exp_id_entry:
                                    emp.edit_expense(exp_id, exp_amount, exp_desc)
                                else:
                                    raise employee.IdNotFoundError("Sorry, we could not find the expense you were looking for.")
                                break
                            except employee.ValueOutOfRangeError as vr:
                                print(str(vr) + " Try again.")
                                continue
                            except employee.IdNotFoundError as IDerror:
                                print(str(IDerror) + " Try again.")
                                continue
                            except expenseManager.ManagerDecisionError as mde:
                                print(str(mde) + " Try again.")
                                continue
                elif option == 4:
                    statement = "SELECT e.id, e.amount, e.description, e.date " \
                        "FROM expenses e JOIN approvals a " \
                        "ON e.id = a.expense_id " \
                        "WHERE e.user_id == ? AND a.status == 'pending'"
                    dbCursor.execute(statement, (emp.id,))
                    entries = dbCursor.fetchall()
                    if not entries:
                        print("Sorry, there are no available expenses for you to discard.")
                    else:
                        list_expenses = []
                        for column_name in dbCursor.description:
                            list_expenses.append(column_name[0])
                        #print(list_expenses)
                        for entry in entries:
                            entry = list(entry)
                            entry[1] = f"{float(entry[1]):.2f}"
                            #print(entry)
                        print(tabulate(entries, headers = list_expenses, tablefmt = "grid", floatfmt = ".2f"))
                        while True:
                            try:
                                exp_id = int(input("Select the id of the expense from the list you want to delete, or press -1 to return to the main menu): "))
                                if exp_id == -1:
                                    break
                                else:
                                    emp.delete_expense(exp_id)
                                    break
                            except ValueError:
                                print("Invalid input, try again.")
                                continue
                            except employee.IdNotFoundError as IDerror:
                                print(str(IDerror) + " Try again.")
                                continue
                            except expenseManager.ManagerDecisionError as mde:
                                print(str(mde) + " Try again.")
                                continue
                elif option == 5:
                    emp.view_expense_history()
                else:
                    print("Thank you for using the Revature Expense Manager Employee app. Goodbye!")
                    break
        emp.logout()
        dbCursor.close()
        break
    else:
        print("Sorry, we cannot verify your login credentials, try again.")

employee.dbConnect.close()