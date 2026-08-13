package com.rev.manager.api;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;

@Feature("Expense Manager")
@DisplayName("Employee API Tests")
public class test_emp_endpoints {
    private final String BASE_URL =
        System.getenv().getOrDefault(
                "EMPLOYEE_API_URL",
                "http://127.0.0.1:5000"
        );
    private Connection conn;

    /*@BeforeAll
    private static void setup() throws SQLException{
        System.out.println("Starting API Testing");
        utilities.createTestDatabase();
    }*/

    /*@AfterAll
    private static void cleanup() throws SQLException{
        if(conn != null){
            System.out.println("Closing connection");
            conn.close();
        }
    }*/

    @BeforeEach
    void setupBefore() throws SQLException{
        utilities.resetDatabase();
        conn = utilities.getConnection();
        
    }

    @AfterEach
    void teardown() throws SQLException{
        // Clear any changes made to database.
        conn.close();
    }

    private String create_expense_body(double amount, String description, String category){
        StringBuilder expense = new StringBuilder();
        expense.append("{ \"amount\": ");
        expense.append(amount);
        expense.append(", \"description\": \"");
        expense.append(description);
        expense.append("\", \"category\": \"");
        expense.append(category);
        expense.append("\" }");
        return expense.toString();
    }

    @Nested
    @DisplayName("Login Tests")
    @Feature("Login")
    class LoginTests{

        @ParameterizedTest(name = "Login with username \"{0}\" and password \"{1}\" gives {2}")
        @CsvSource({
            "Bob, bob_22, success, Login, 200",
            "Bob, bob22, error, combination, 401",
            "Bob, , error, password, 401",
            ", bob_22, error, username, 401"
        })
        @Description("Tests with correctly formatted json input data")
        void test_login_valid_body (String username, String password, String status, String message, int statusCode){
            String requestBody = "{ \"username\": \"" + username + "\", \"password\": \"" + password + "\" }";
            //Response r =
            given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .post("/api/login")
            .then()
                .body("message", containsString(message))
                .body("status", equalTo(status))
                .statusCode(statusCode)
                .extract().response();
            //System.out.println(r.asString());
        }

        @Test
        @DisplayName("Login with no username field fails")
        @Description("Login without a username data field in json body")
        void test_login_missing_username(){
            String requestBody = "{ \"password\": \"bob_22\" }";
            given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .post("/api/login")
            .then()
                .body("status", equalTo("error"))
                .body("message", containsString("Username"))
                .statusCode(400);
        }

        @Test
        @DisplayName("Login with no password field fails")
        @Description("Login without a password data field in json body")
        void test_login_missing_password(){
            String requestBody = "{ \"username\": \"Bob\" }";
            given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .post("/api/login")
            .then()
                .body("status", equalTo("error"))
                .body("message", containsString("Password"))
                .statusCode(400);
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    @Feature("Logout")
    //@Disabled //TODO: Remove
    class LogoutTests{

        @Test
        @DisplayName("Logout Test after logging in")
        void test_logout_after_login(){

            System.out.println("Start test");
            String sessionKey = 
            given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body("{ \"username\": \"Bob\", \"password\": \"bob_22\" }")
            .when()
                .post("/api/login")
            .then()
                .body("status", equalTo("success"))
                .extract().cookie("session");

            //System.out.println(sessionKey);
            //System.out.println("Key to use");

            given()
                .baseUri(BASE_URL)
                .cookie("session",sessionKey)
            .when()
                .post("/api/logout")
            .then()
                .body("status", equalTo("success"))
                .statusCode(200);
        }

        @Test
        @DisplayName("Logout Test without logging in first")
        void test_logout_without_login(){
            given()
                .baseUri(BASE_URL)
            .when()
                .post("/api/logout")
            .then()
                .body("status", equalTo("error"))
                .body("message", equalTo("No employee is currently signed in."))
                .statusCode(401);
        }
    }

    @Nested
    @DisplayName("Calls to api without authorization")
    @Feature("Authorization")
    class NotLoggedInTests{

        @ParameterizedTest(name = "Get endpoint {0} without auth")
        @ValueSource(strings = {
            "/api/session", "api/expenses", "/api/expenses?only_pending=true", "/api/expenses/history"
        })
        void test_get_endpoints_without_auth_fails(String endpoint){
            given()
                .baseUri(BASE_URL)
            .when()
                .get(endpoint)
            .then()
                .body("status", equalTo("error"))
                .body("message", containsString("sign"))
                .statusCode(401);
        }

        @Test
        @DisplayName("Post endpoint /api/expenses without auth")
        void test_post_expense_without_auth_fails(){
            String requestBody = create_expense_body(30.01, "some expense", "Other");
            /*"""
                    {
                        "amount": 30.01,
                        "description": "some expense",
                        "category": "Other"
                    }
                    """;*/
            given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .post("/api/expenses")
            .then()
                .body("status", equalTo("error"))
                .body("message", containsString("sign"))
                .statusCode(401);
        }

        @Test
        @DisplayName("Put endpoint /api/expenses/<expense_id> without auth")
        void test_update_expense_without_auth_fails(){
            String requestBody = create_expense_body(45.22, "Edited expense", "Supplies");
            /*"""
                    {
                        "amount": 45.22,
                        "description": "Edited expense",
                        "category": "Supplies"
                    }
                    """;*/
            given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .put("/api/expenses/1")
            .then()
                .body("status", equalTo("error"))
                .body("message", containsString("sign"))
                .statusCode(401);
        }

        @Test
        @DisplayName("Delete endpoint /api/expenses/<expense_id> without auth")
        void test_delete_expense_without_auth_fails(){
            given()
                .baseUri(BASE_URL)
            .when()
                .delete("/api/expenses/3")
            .then()
                .body("status", equalTo("error"))
                .body("message", containsString("sign"))
                .statusCode(401);
        }
    }

    @Nested

    class TestsWithAuth{

        //authKey is the session login cookie for Bob. Inject it into each api call that requires authorization.
        String authKey =
        given()
            .baseUri(BASE_URL)
            .contentType(ContentType.JSON)
            .body("""
                { "username": "Bob", "password": "bob_22" }
                """)
        .when()
            .post("/api/login")
        .then()
            .extract()
            .cookie("session");

        @Test
        @DisplayName("Employee get expenses")
        @Feature("Get expenses")
        void test_get_expenses_lists_all_employee_expenses() throws SQLException{
            System.out.println("Connection Data:");
            System.out.println(conn.toString());
            Statement s = conn.createStatement();
            ResultSet rs = s.executeQuery("""
                    SELECT COUNT(*) FROM expenses
                    INNER JOIN approvals ON expenses.id = approvals.expense_id
                    WHERE expenses.user_id = 2;
                    """);
            rs.next();
            int exp_count = rs.getInt("COUNT(*)");
            List<LinkedHashMap<String,String>> expenses = 
            given()
                .baseUri(BASE_URL)
                .cookie("session", authKey)
            .when()
                .get("/api/expenses")
            .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("count", equalTo(exp_count))
                .extract()
                .response()
                .jsonPath()
                .getList("expenses");
            LinkedHashMap<String,String> sampleExp = expenses.get(0);
            assertAll("Verify expense has correct data fields",
                () -> assertTrue(sampleExp.containsKey("id"), "Missing id field in expenses"),
                () -> assertTrue(sampleExp.containsKey("amount"), "Missing amount field in expenses"),
                () -> assertTrue(sampleExp.containsKey("category"),"Missing category field in expense"),
                () -> assertTrue(sampleExp.containsKey("date"),"Missing date field in expense"),
                () -> assertTrue(sampleExp.containsKey("description"),"Missing description field in expense"),
                () -> assertTrue(sampleExp.containsKey("status"),"Missing status field in expense")
            );
        }

        @Test
        @DisplayName("Employee get pending expenses")
        @Feature("Get expenses")
        void test_get__pending_expenses_lists_pending_employee_expenses() throws SQLException{
            Statement s = conn.createStatement();
            ResultSet rs = s.executeQuery("""
                    SELECT COUNT(*) FROM expenses
                    INNER JOIN approvals ON expenses.id = approvals.expense_id
                    WHERE approvals.status = 'pending' AND expenses.user_id = 2;
                    """);
            rs.next();
            int pending_exp_count = rs.getInt("COUNT(*)");
            rs.close();
            s.close();
            List<LinkedHashMap<String,String>> expenses = 
            given()
                .baseUri(BASE_URL)
                .cookie("session", authKey)
            .when()
                .get("/api/expenses?only_pending=true")
            .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("count", equalTo(pending_exp_count))
                .extract()
                .response()
                .jsonPath()
                .getList("expenses");
                LinkedHashMap<String,String> sampleExp = expenses.get(0);
                assertAll("Verify expense has correct data fields",
                    () -> assertTrue(sampleExp.containsKey("id"), "Missing id field in expenses"),
                    () -> assertTrue(sampleExp.containsKey("amount"), "Missing amount field in expenses"),
                    () -> assertTrue(sampleExp.containsKey("category"),"Missing category field in expense"),
                    () -> assertTrue(sampleExp.containsKey("date"),"Missing date field in expense"),
                    () -> assertTrue(sampleExp.containsKey("description"),"Missing description field in expense"),
                    () -> assertTrue(sampleExp.get("status").equals("pending"),"Missing status field in expense, or not pending status")
                );
        }
        
        @Test
        @DisplayName("Submit expense without errors")
        @Feature("Submit expense")
        void test_post_expense_adds_expense_into_database() throws SQLException{
            String requestBody = create_expense_body(12.25, "Generic write off", "Travel");
            int id =
            given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .cookie("session", authKey)
                .body(requestBody)
            .when()
                .post("/api/expenses")
            .then()
                .body("status", equalTo("success"))
                .body("message", containsString("submit"))
                .statusCode(201)
                .extract()
                .response()
                .jsonPath()
                .get("expense.id");
            
            Statement s = conn.createStatement();
            ResultSet rs = s.executeQuery("SELECT * FROM expenses WHERE id = "+id+";");
            assertTrue(rs.next(), "New expense not added to database");
            rs.close();
            rs = s.executeQuery("SELECT * FROM approvals WHERE expense_id = "+id+";");
            assertTrue(rs.next(), "No entry found in approvals table for created expense");
            assertEquals(rs.getString("status"), "pending", "New expense does not have pending state");
        }

        @ParameterizedTest(name = "Test post expense with bad amount data {0}")
        @ValueSource(strings = {
            "{ \"amount\": 0.99, \"description\": \"Valid description\", \"category\": \"Travel\" }",
            "{ \"amount\": 1000000, \"description\": \"Valid description\", \"category\": \"Certificate\" }",
            "{ \"amount\": -3, \"description\": \"Valid description\", \"category\": \"Repairs\" }",
            "{ \"amount\": \"fifty-seven\", \"description\": \"Valid description\", \"category\": \"Repairs\" }",
            "{ \"description\": \"Valid description\", \"category\": \"Repairs\" }"
        })
        @Feature("Submit expense")
        void test_post_expense_with_bad_amount_gives_error(String requestBody){
            //System.out.println(requestBody);
            String response = 
            given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .cookie("session", authKey)
                .body(requestBody)
            .when()
                .post("/api/expenses")
            .then()
                .statusCode(400)
                .body("status", equalTo("error"))
                .extract()
                .body()
                .asString()
                .toLowerCase();
            assertTrue(response.contains("amount"), "Description should contain amount, but is \n"+response);
        }

        //Add tests for missing data fields (category, empty category, description, empty description)

        @ParameterizedTest(name = "Edit expense api, valid requests with amount {0}")
        @CsvSource({
            "20.22, Update expense, Supplies",
            "1.00, Cheep expense, Travel",
            "9999.99, Large expense, Other"
        })
        @Feature("Edit expense")
        void test_edit_expense_with_various_valid_data(double amount, String description, String category) throws SQLException{
            String requestBody = create_expense_body(amount, description, category);
            given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .cookie("session", authKey)
            .when()
                .put("/api/expenses/39")
            .then()
                .statusCode(200)
                .body("status", equalTo("success"));
            Statement s = conn.createStatement();
            ResultSet rs = s.executeQuery("SELECT * FROM expenses WHERE id = 39;");
            assertTrue(rs.next(), "No expense with id found in database after update request");
            assertAll("Edited data fields",
                () -> assertEquals(rs.getDouble("amount"), amount, "Amount mismatch, should be "+amount+" but was "+rs.getDouble("amount")),
                () -> assertEquals(rs.getString("description"), description, "Description mismatch, should be "+description+" but was "+rs.getString("description")),
                () -> assertEquals(rs.getString("category"), category, "Category mismatch, should be "+category+" but was "+rs.getString("category"))
            );
        }

        @ParameterizedTest(name = "Edit expense api, invalid requests because {3}")
        @CsvSource({
            "0.99, Too cheap, Supplies, amount is too low",
            "100000, Too expensive, Travel, amount is too high",
        })
        @Feature("Edit expense")
        void test_edit_expense_with_invalid_data(double amount, String description, String category, String denialReason){
            String requestBody = create_expense_body(amount, description, category);
            given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .cookie("session", authKey)
            .when()
                .put("/api/expenses/39")
            .then()
                .onFailMessage("Should have failed because "+denialReason)
                .body("status", equalTo("error"))
                .statusCode(400);
        }

        @Test
        @DisplayName("Edit expense on non-pending expense")
        @Feature("Edit expense")
        void test_edit_expense_on_non_pending_expense(){
            String requestBody = create_expense_body(10.10, "Some expense", "Other");
            given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .cookie("session", authKey)
            .when()
                .put("/api/expenses/5")
            .then()
                .body("status", equalTo("error"))
                .statusCode(409); //Conflict
        }

        @ParameterizedTest(name = "Delete expense api on {3}")
        @CsvSource({
            "39, success, 200, pending expense (valid)",
            "5, error, 409, expense already denied",
            "75, error, 409, expense already approved",
            "18, error, 404, expense belonging to different employee",
            "99, error, 404, non-existent expense"
        })
        void test_delete_expense_with_various_data(int id, String responseStatus, int expectedCode, String reasonForDenial){
            given()
                .baseUri(BASE_URL)
                .cookie("session", authKey)
            .when()
                .delete("api/expenses/"+id)
            .then()
                .onFailMessage("Failed on "+reasonForDenial)
                .body("status", equalTo(responseStatus))
                .statusCode(expectedCode);
        }

        @Test
        @DisplayName("Get expense history")
        @Feature("Expense History")
        @Disabled("Known bug, category and date fields missing from expense")
        void test_get_expense_history() throws SQLException{
            Statement s = conn.createStatement();
            ResultSet rs = s.executeQuery("""
                    SELECT COUNT(*)
                    FROM approvals
                    INNER JOIN expenses ON approvals.expense_id = expenses.id
                    WHERE expenses.user_id = 2 AND (approvals.status = 'approved' OR approvals.status = 'denied');
                    """);
            rs.next();
            int expenseCount = rs.getInt("COUNT(*)");
            rs.close();
            s.close();
            List<LinkedHashMap<String,String>> expense = 
            given()
                .baseUri(BASE_URL)
                .cookie("session", authKey)
            .when()
                .get("/api/expenses/history")
            .then()
                .body("status", equalTo("success"))
                .body("count", equalTo(expenseCount))
                .extract()
                .response()
                .jsonPath()
                .getList("expenses");
            LinkedHashMap<String,String> sampleExp = expense.get(0);
            System.out.println(sampleExp);
            assertAll("Verify expense has correct data fields",
                    () -> assertTrue(sampleExp.containsKey("id"), "Missing id field in expenses"),
                    () -> assertTrue(sampleExp.containsKey("amount"), "Missing amount field in expenses"),
                    () -> assertTrue(sampleExp.containsKey("category"),"Missing category field in expense"),
                    () -> assertTrue(sampleExp.containsKey("date"),"Missing date field in expense"),
                    () -> assertTrue(sampleExp.containsKey("description"),"Missing description field in expense"),
                    () -> assertTrue(sampleExp.containsKey("status"),"Missing status field in expense"),
                    () -> assertTrue(sampleExp.containsKey("comment"),"Missing comment field in expense"),
                    () -> assertTrue(sampleExp.containsKey("review_date"),"Missing review_date field in expense")
                );
        }
    }
}
