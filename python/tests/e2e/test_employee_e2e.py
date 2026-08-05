import pytest

from api.app import app


@pytest.fixture
def client():
    app.config.update(
        TESTING=True,
        SECRET_KEY="test-secret-key"
    )

    with app.test_client() as test_client:
        yield test_client


def test_complete_employee_expense_workflow(client):
    # Step 1: Log in
    login_response = client.post(
        "/api/login",
        json={
            "username": "Bob",
            "password": "bob_22"
        }
    )

    assert login_response.status_code == 200

    login_data = login_response.get_json()
    assert login_data["success"] is True

    # Flask's test client automatically keeps the session cookie.

    # Step 2: Submit an expense
    submit_response = client.post(
        "/api/expenses",
        json={
            "amount": 45.75,
            "description": "E2E test lunch",
            "category": "Food"
        }
    )

    assert submit_response.status_code == 201

    submit_data = submit_response.get_json()
    assert submit_data["success"] is True

    expense_id = submit_data["expense"]["id"]

    # Step 3: Check the expense status
    status_response = client.get(
        f"/api/expenses/{expense_id}/status"
    )

    assert status_response.status_code == 200

    status_data = status_response.get_json()
    assert status_data["success"] is True
    assert status_data["status"].lower() == "pending"

    # Step 4: View history
    history_response = client.get("/api/expense-history")

    assert history_response.status_code == 200

    history_data = history_response.get_json()
    assert history_data["success"] is True

    matching_expenses = [
        expense
        for expense in history_data["expenses"]
        if expense["id"] == expense_id
    ]

    assert len(matching_expenses) == 1
    assert matching_expenses[0]["description"] == "E2E test lunch"

    # Step 5: Edit the pending expense
    edit_response = client.put(
        f"/api/expenses/{expense_id}",
        json={
            "amount": 50.00,
            "description": "Updated E2E test lunch",
            "category": "Food"
        }
    )

    assert edit_response.status_code == 200

    edit_data = edit_response.get_json()
    assert edit_data["success"] is True
    assert edit_data["expense"]["amount"] == 50.00

    # Step 6: Delete the pending expense
    delete_response = client.delete(
        f"/api/expenses/{expense_id}"
    )

    assert delete_response.status_code == 200

    delete_data = delete_response.get_json()
    assert delete_data["success"] is True

    # Step 7: Log out
    logout_response = client.post("/api/logout")

    assert logout_response.status_code == 200

    # Step 8: Protected endpoint should now fail
    protected_response = client.get("/api/expense-history")

    assert protected_response.status_code == 401