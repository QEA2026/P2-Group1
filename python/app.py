

import os
import sqlite3
from pathlib import Path

from flask import Flask, jsonify, request, session

from employee import (
    Employee,
    IdNotFoundError,
    NotSignedInError,
    ValueOutOfRangeError
)
from expenseManager import ManagerDecisionError


app = Flask(__name__)


# Flask uses this key to securely sign the employee's session cookie.
# For development, the default value works.
# For production, FLASK_SECRET_KEY should be stored as an environment variable.
app.config["SECRET_KEY"] = os.environ.get(
    "FLASK_SECRET_KEY",
    "development-secret-key-change-this"
)


# app.py is inside the python folder, while the database
# is located one folder above the python folder.
BASE_DIRECTORY = Path(__file__).resolve().parent
DATABASE_PATH = BASE_DIRECTORY.parent / "revExpenseData.db"


''' Creates and returns a connection to the SQLite database.
    Rows are returned as sqlite3.Row objects so column names
    can be used instead of numeric indexes.
'''
def get_database_connection():
    connection = sqlite3.connect(DATABASE_PATH)
    connection.row_factory = sqlite3.Row
    return connection


''' Recreates the currently logged-in Employee object using
    the employee information stored in the Flask session.

    Returns None if no employee is currently logged in.
'''
def get_current_employee():
    employee_id = session.get("employee_id")
    username = session.get("username")

    if employee_id is None or username is None:
        return None

    employee = Employee(
        employee_id,
        username,
        ""
    )

    employee.signed_in = True

    return employee


''' Returns a JSON response when an employee attempts to access
    a protected endpoint without being logged in.
'''
def login_required_response():
    return jsonify({
        "success": False,
        "message": "You must be logged in to perform this action."
    }), 401


@app.get("/")
def home():
    return jsonify({
        "message": "Expense Manager Flask backend is running"
    }), 200


@app.get("/api/health")
def health():
    return jsonify({
        "success": True,
        "message": "Backend is connected"
    }), 200


''' Logs an employee into the application.

    The username and password are received as JSON.

    If successful, the employee id and username are stored
    in the Flask session so the employee remains logged in
    for later API requests.
'''
@app.post("/api/login")
def login():
    data = request.get_json(silent=True)

    if data is None:
        return jsonify({
            "success": False,
            "message": "JSON request body is required."
        }), 400

    username = str(
        data.get("username", "")
    ).strip()

    password = str(
        data.get("password", "")
    )

    if not username or not password:
        return jsonify({
            "success": False,
            "message": "Username and password are required."
        }), 400

    connection = get_database_connection()

    try:
        user = connection.execute(
            """
            SELECT
                id,
                username,
                password,
                role
            FROM users
            WHERE username = ?
            """,
            (username,)
        ).fetchone()

    except sqlite3.Error:
        return jsonify({
            "success": False,
            "message": "A database error occurred during login."
        }), 500

    finally:
        connection.close()

    if user is None or user["password"] != password:
        return jsonify({
            "success": False,
            "message": "Invalid username or password."
        }), 401

    if user["role"].lower() != "employee":
        return jsonify({
            "success": False,
            "message": "This login is only for employees."
        }), 403

    # Clears an older session before storing the new employee.
    session.clear()

    session["employee_id"] = user["id"]
    session["username"] = user["username"]
    session["role"] = user["role"]

    return jsonify({
        "success": True,
        "message": "Login successful.",
        "employee": {
            "id": user["id"],
            "username": user["username"],
            "role": user["role"]
        }
    }), 200


''' Logs the current employee out by clearing the Flask session. '''
@app.post("/api/logout")
def logout():
    employee = get_current_employee()

    if employee is None:
        return login_required_response()

    employee.logout()
    session.clear()

    return jsonify({
        "success": True,
        "message": "Logout successful."
    }), 200


''' Creates a new expense for the currently logged-in employee.

    Expected JSON:
    {
        "amount": 125.50,
        "description": "Hotel during business travel",
        "category": "Travel"
    }
'''
@app.post("/api/expenses")
def submit_expense():
    employee = get_current_employee()

    if employee is None:
        return login_required_response()

    data = request.get_json(silent=True)

    if data is None:
        return jsonify({
            "success": False,
            "message": "JSON request body is required."
        }), 400

    description = str(
        data.get("description", "")
    ).strip()

    category = str(
        data.get("category", "")
    ).strip()

    if "amount" not in data:
        return jsonify({
            "success": False,
            "message": "Expense amount is required."
        }), 400

    if not description:
        return jsonify({
            "success": False,
            "message": "Expense description is required."
        }), 400

    if not category:
        return jsonify({
            "success": False,
            "message": "Expense category is required."
        }), 400

    try:
        amount = float(data["amount"])

        created_expense = employee.submit_expense(
            amount,
            description,
            category
        )

        return jsonify({
            "success": True,
            "message": "Expense submitted successfully.",
            "expense": created_expense
        }), 201

    except (TypeError, ValueError):
        return jsonify({
            "success": False,
            "message": "Expense amount must be a valid number."
        }), 400

    except ValueOutOfRangeError as error:
        return jsonify({
            "success": False,
            "message": str(error)
        }), 400

    except NotSignedInError as error:
        return jsonify({
            "success": False,
            "message": str(error)
        }), 401

    except sqlite3.Error:
        return jsonify({
            "success": False,
            "message": "A database error occurred while submitting the expense."
        }), 500


''' Returns the status of one expense belonging to the
    currently logged-in employee.
'''
@app.get("/api/expenses/<int:expense_id>/status")
def view_expense_status(expense_id):
    employee = get_current_employee()

    if employee is None:
        return login_required_response()

    try:
        status = employee.view_expense_status(expense_id)

        return jsonify({
            "success": True,
            "expense_id": expense_id,
            "status": status
        }), 200

    except IdNotFoundError as error:
        return jsonify({
            "success": False,
            "message": str(error)
        }), 404

    except NotSignedInError as error:
        return jsonify({
            "success": False,
            "message": str(error)
        }), 401

    except sqlite3.Error:
        return jsonify({
            "success": False,
            "message": "A database error occurred while retrieving the status."
        }), 500


''' Edits a pending expense belonging to the currently
    logged-in employee.

    Expected JSON:
    {
        "amount": 150.00,
        "description": "Updated hotel expense",
        "category": "Business Travel"
    }

    Category is optional. If category is not included,
    the existing category remains unchanged.
'''
@app.put("/api/expenses/<int:expense_id>")
def edit_expense(expense_id):
    employee = get_current_employee()

    if employee is None:
        return login_required_response()

    data = request.get_json(silent=True)

    if data is None:
        return jsonify({
            "success": False,
            "message": "JSON request body is required."
        }), 400

    if "amount" not in data:
        return jsonify({
            "success": False,
            "message": "Expense amount is required."
        }), 400

    description = str(
        data.get("description", "")
    ).strip()

    if not description:
        return jsonify({
            "success": False,
            "message": "Expense description is required."
        }), 400

    category = data.get("category")

    if category is not None:
        category = str(category).strip()

        if not category:
            return jsonify({
                "success": False,
                "message": "Expense category cannot be empty."
            }), 400

    try:
        amount = float(data["amount"])

        updated_expense = employee.edit_expense(
            expense_id,
            amount,
            description,
            category
        )

        return jsonify({
            "success": True,
            "message": "Expense updated successfully.",
            "expense": updated_expense
        }), 200

    except (TypeError, ValueError) as error:
        return jsonify({
            "success": False,
            "message": str(error)
        }), 400

    except ValueOutOfRangeError as error:
        return jsonify({
            "success": False,
            "message": str(error)
        }), 400

    except IdNotFoundError as error:
        return jsonify({
            "success": False,
            "message": str(error)
        }), 404

    except ManagerDecisionError as error:
        return jsonify({
            "success": False,
            "message": str(error)
        }), 409

    except NotSignedInError as error:
        return jsonify({
            "success": False,
            "message": str(error)
        }), 401

    except sqlite3.Error:
        return jsonify({
            "success": False,
            "message": "A database error occurred while updating the expense."
        }), 500


''' Deletes a pending expense belonging to the currently
    logged-in employee.
'''
@app.delete("/api/expenses/<int:expense_id>")
def delete_expense(expense_id):
    employee = get_current_employee()

    if employee is None:
        return login_required_response()

    try:
        deleted_expense = employee.delete_expense(
            expense_id
        )

        return jsonify({
            "success": True,
            "message": "Expense deleted successfully.",
            "expense": deleted_expense
        }), 200

    except IdNotFoundError as error:
        return jsonify({
            "success": False,
            "message": str(error)
        }), 404

    except ManagerDecisionError as error:
        return jsonify({
            "success": False,
            "message": str(error)
        }), 409

    except NotSignedInError as error:
        return jsonify({
            "success": False,
            "message": str(error)
        }), 401

    except ValueError as error:
        return jsonify({
            "success": False,
            "message": str(error)
        }), 404

    except sqlite3.Error:
        return jsonify({
            "success": False,
            "message": "A database error occurred while deleting the expense."
        }), 500


''' Returns all approved and denied expenses belonging
    to the currently logged-in employee.
'''
@app.get("/api/expense-history")
def view_expense_history():
    employee = get_current_employee()

    if employee is None:
        return login_required_response()

    try:
        expenses = employee.view_expense_history()

        return jsonify({
            "success": True,
            "count": len(expenses),
            "expenses": expenses
        }), 200

    except NotSignedInError as error:
        return jsonify({
            "success": False,
            "message": str(error)
        }), 401

    except sqlite3.Error:
        return jsonify({
            "success": False,
            "message": "A database error occurred while retrieving expense history."
        }), 500


if __name__ == "__main__":
    app.run(debug=True)