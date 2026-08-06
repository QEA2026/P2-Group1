from pathlib import Path
import os
import sqlite3
import sys

from flask import Flask, jsonify, request, session, render_template
from flask_cors import CORS


# Make employee.py and expenseManager.py importable from the parent folder.
PYTHON_DIRECTORY = Path(__file__).resolve().parent.parent

if str(PYTHON_DIRECTORY) not in sys.path:
    sys.path.insert(0, str(PYTHON_DIRECTORY))


from employee import (
    Employee,
    IdNotFoundError,
    InvalidLoginError,
    NotSignedInError,
    ValueOutOfRangeError,
)
from expenseManager import ManagerDecisionError


app = Flask(__name__)

# For local development.
app.secret_key = os.environ.get(
    "FLASK_SECRET_KEY",
    "development-secret-key-change-before-deployment",
)

# Allows the frontend to send the Flask session cookie.
CORS(
    app,
    supports_credentials=True,
    origins=[
        "http://localhost:3000",
        "http://127.0.0.1:3000",
        "http://localhost:4200",
        "http://127.0.0.1:4200",
        "http://localhost:5173",
        "http://127.0.0.1:5173",
    ],
)

# revExpenseData.db is stored in the P1_Group1 folder.
if len(sys.argv) < 2:
    DATABASE_PATH = PYTHON_DIRECTORY.parent / "revExpenseData.db"
else: #sys.argv[1] is the name of the test database to use in the resources folder of the expense-app-managers resources files, for testing usage only
    DATABASE_PATH = PYTHON_DIRECTORY.parent / "expense-app-managers" / "src" / "test" / "resources" / sys.argv[1]


def create_authenticated_employee():
    """
    Recreates an Employee from the authenticated Flask session.

    This helper does not change employee.py. It prepares the Employee object
    so its existing signed-in business methods can be called by API routes.
    """
    employee_id = session.get("employee_id")
    username = session.get("username")

    if employee_id is None or username is None:
        raise NotSignedInError(
            "Sorry, you are not signed in. Please sign in first."
        )

    connection = sqlite3.connect(DATABASE_PATH)

    employee = Employee(
        id=employee_id,
        username=username,
        password="",
        connection=connection,
    )

    employee.signed_in = True

    return employee


def serialize_expense(expense):
    """
    Converts a tuple returned by Employee.list_expenses() into JSON data.

    Tuple order:
    id, amount, description, date, category, status
    """
    return {
        "id": expense[0],
        "amount": expense[1],
        "description": expense[2],
        "date": expense[3],
        "category": expense[4],
        "status": expense[5],
    }


def serialize_history_entry(entry):
    """
    Converts a tuple returned by Employee.view_expense_history() into JSON.

    Tuple order:
    expense ID, user ID, amount, description, status, comment, review date
    """
    return {
        "id": entry[0],
        "employee_id": entry[1],
        "amount": entry[2],
        "description": entry[3],
        "status": entry[4],
        "comment": entry[5],
        "review_date": entry[6],
    }


@app.get("/")
def index():
    return jsonify({
        "status": "success",
        "message": "Revature Employee Expense API",
    }), 200


@app.get("/app")
def frontend():
    return render_template('index.html')

@app.get("/api/health")
def health():
    return jsonify({
        "status": "success",
        "message": "Employee API is running.",
    }), 200


@app.post("/api/login")
def login():
    data = request.get_json(silent=True) or {}

    username = data.get("username")
    password = data.get("password")

    if not isinstance(username, str) or not username.strip():
        return jsonify({
            "status": "error",
            "message": "Username is required.",
        }), 400

    if not isinstance(password, str) or not password:
        return jsonify({
            "status": "error",
            "message": "Password is required.",
        }), 400

    connection = sqlite3.connect(DATABASE_PATH)

    try:
        employee = Employee(
            id=-1,
            username=username.strip(),
            password=password,
            connection=connection,
        )

        employee.login(username.strip(), password)

        # Clear any old session before saving the new authenticated user.
        session.clear()
        session["employee_id"] = employee.id
        session["username"] = username.strip()

        return jsonify({
            "status": "success",
            "message": "Login successful.",
            "employee": {
                "id": employee.id,
                "username": username.strip(),
            },
        }), 200

    except InvalidLoginError as error:
        return jsonify({
            "status": "error",
            "message": str(error),
        }), 401

    finally:
        connection.close()


@app.post("/api/logout")
def logout():
    if "employee_id" not in session:
        return jsonify({
            "status": "error",
            "message": "No employee is currently signed in.",
        }), 401

    session.clear()

    return jsonify({
        "status": "success",
        "message": "Logout successful.",
    }), 200


@app.get("/api/session")
def get_session():
    employee_id = session.get("employee_id")
    username = session.get("username")

    if employee_id is None:
        return jsonify({
            "status": "error",
            "message": "No employee is currently signed in.",
        }), 401

    return jsonify({
        "status": "success",
        "employee": {
            "id": employee_id,
            "username": username,
        },
    }), 200


@app.get("/api/expenses")
def list_expenses():
    employee = None

    try:
        employee = create_authenticated_employee()

        only_pending_text = request.args.get(
            "only_pending",
            default="false",
        ).lower()

        if only_pending_text not in ("true", "false"):
            return jsonify({
                "status": "error",
                "message": "only_pending must be true or false.",
            }), 400

        only_pending = only_pending_text == "true"
        expenses = employee.list_expenses(only_pending=only_pending)

        return jsonify({
            "status": "success",
            "count": len(expenses),
            "expenses": [
                serialize_expense(expense)
                for expense in expenses
            ],
        }), 200

    except NotSignedInError as error:
        return jsonify({
            "status": "error",
            "message": str(error),
        }), 401

    finally:
        if employee is not None:
            employee.connection.close()


@app.post("/api/expenses")
def submit_expense():
    employee = None
    data = request.get_json(silent=True) or {}

    amount = data.get("amount")
    description = data.get("description")
    category = data.get("category", "Other")

    if amount is None:
        return jsonify({
            "status": "error",
            "message": "Amount is required.",
        }), 400

    try:
        amount = float(amount)
    except (TypeError, ValueError):
        return jsonify({
            "status": "error",
            "message": "Amount must be a number.",
        }), 400

    if not isinstance(description, str) or not description.strip():
        return jsonify({
            "status": "error",
            "message": "Description is required.",
        }), 400

    if not isinstance(category, str) or not category.strip():
        return jsonify({
            "status": "error",
            "message": "Category must be a non-empty string.",
        }), 400

    try:
        employee = create_authenticated_employee()

        expense_id = employee.submit_expense(
            amount,
            description.strip(),
            category.strip(),
        )

        return jsonify({
            "status": "success",
            "message": "Expense submitted successfully.",
            "expense": {
                "id": expense_id,
                "amount": amount,
                "description": description.strip(),
                "category": category.strip(),
                "status": "pending",
            },
        }), 201

    except NotSignedInError as error:
        return jsonify({
            "status": "error",
            "message": str(error),
        }), 401

    except ValueOutOfRangeError as error:
        return jsonify({
            "status": "error",
            "message": str(error),
        }), 400

    finally:
        if employee is not None:
            employee.connection.close()


@app.put("/api/expenses/<int:expense_id>")
def edit_expense(expense_id):
    """
    Updates an existing pending expense.

    Example request:

    PUT /api/expenses/927

    {
        "amount": 45.75,
        "description": "Updated parking expense",
        "category": "Travel"
    }
    """
    employee = None
    data = request.get_json(silent=True) or {}

    amount = data.get("amount")
    description = data.get("description")
    category = data.get("category", "Other")

    if amount is None:
        return jsonify({
            "status": "error",
            "message": "Amount is required.",
        }), 400

    try:
        amount = float(amount)
    except (TypeError, ValueError):
        return jsonify({
            "status": "error",
            "message": "Amount must be a number.",
        }), 400

    if not isinstance(description, str) or not description.strip():
        return jsonify({
            "status": "error",
            "message": "Description is required.",
        }), 400

    if not isinstance(category, str) or not category.strip():
        return jsonify({
            "status": "error",
            "message": "Category must be a non-empty string.",
        }), 400

    try:
        employee = create_authenticated_employee()

        employee.edit_expense(
            expense_id,
            amount,
            description.strip(),
            category.strip(),
        )

        return jsonify({
            "status": "success",
            "message": f"Expense {expense_id} was updated successfully.",
            "expense": {
                "id": expense_id,
                "amount": amount,
                "description": description.strip(),
                "category": category.strip(),
                "status": "pending",
            },
        }), 200

    except NotSignedInError as error:
        return jsonify({
            "status": "error",
            "message": str(error),
        }), 401

    except ValueOutOfRangeError as error:
        return jsonify({
            "status": "error",
            "message": str(error),
        }), 400

    except IdNotFoundError as error:
        return jsonify({
            "status": "error",
            "message": str(error),
        }), 404

    except ManagerDecisionError as error:
        return jsonify({
            "status": "error",
            "message": str(error),
        }), 409

    finally:
        if employee is not None:
            employee.connection.close()


@app.delete("/api/expenses/<int:expense_id>")
def delete_expense(expense_id):
    employee = None

    try:
        employee = create_authenticated_employee()
        employee.delete_expense(expense_id)

        return jsonify({
            "status": "success",
            "message": f"Expense {expense_id} was deleted successfully.",
        }), 200

    except NotSignedInError as error:
        return jsonify({
            "status": "error",
            "message": str(error),
        }), 401

    except IdNotFoundError as error:
        return jsonify({
            "status": "error",
            "message": str(error),
        }), 404

    except ManagerDecisionError as error:
        return jsonify({
            "status": "error",
            "message": str(error),
        }), 409

    finally:
        if employee is not None:
            employee.connection.close()


@app.get("/api/expenses/history")
def expense_history():
    employee = None

    try:
        employee = create_authenticated_employee()
        entries = employee.view_expense_history()

        return jsonify({
            "status": "success",
            "count": len(entries),
            "expenses": [
                serialize_history_entry(entry)
                for entry in entries
            ],
        }), 200

    except NotSignedInError as error:
        return jsonify({
            "status": "error",
            "message": str(error),
        }), 401

    finally:
        if employee is not None:
            employee.connection.close()


if __name__ == "__main__":
    app.run(debug=False, host="0.0.0.0", port=5000)