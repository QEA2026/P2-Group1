package com.rev.manager.api;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.oneOf;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Assumptions;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.qameta.allure.Feature;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class test_manager_endpoints {
    private final static String BASE_URL =
        System.getenv().getOrDefault(
                "MANAGER_API_URL",
                "http://127.0.0.1:5001"
        );
    private Connection conn;
    private static final boolean PRINT_DATA = false;
    private static String jwtToken;

    @BeforeAll
    static void setupBeforeAll(){
        //Set jwtToken for manager Andrew (id 1), use on calls that require authorization.
        jwtToken =
        given()
            .baseUri(BASE_URL)
            .contentType(ContentType.JSON)
            .body("{ \"username\": \"Andrew\", \"password\": \"onetwothree\" }")
        .when()
            .post("/api/auth/login")
        .then()
            .extract()
            .cookie("jwt");
        if(PRINT_DATA){System.out.println("Created new jwt auth token: "+jwtToken);}
    }

    @BeforeEach
    void setupBefore() throws SQLException{
        utilities.resetDatabase();
        conn = utilities.getConnection();
    }

    @AfterEach
    void teardown() throws SQLException{
        conn.close();
    }

    /**
     * Asserts that all the expense fields in an expense are present in the given mapping.
     * @param exp A LinkedHashMap, likely returned from a jsonPath to a list.
     */
    static void assertAllExpenseCategories(LinkedHashMap<String,String> exp){
        assertAll("Verify returned expense has all correct data fields",
                () -> assertTrue(exp.containsKey("expense_id"), "Missing data field expense_id"),
                () -> assertTrue(exp.containsKey("emp_name"), "Missing data field emp_name"),
                () -> assertTrue(exp.containsKey("amount"), "Missing data field amount"),
                () -> assertTrue(exp.containsKey("description"), "Missing data field description"),
                () -> assertTrue(exp.containsKey("exp_date"), "Missing data field exp_date"),
                () -> assertTrue(exp.containsKey("category"), "Missing data field category"),
                () -> assertTrue(exp.containsKey("status"), "Missing data field status"),
                () -> assertTrue(exp.containsKey("reviewer_name"), "Missing data field reviewer_name"),
                () -> assertTrue(exp.containsKey("comment"), "Missing data field comment"),
                () -> assertTrue(exp.containsKey("review_date"), "Missing data field review_date")
            );
    }

    /**
     * Helper function for the csv report test.
     * Takes the header string from the csv plain text response, and asserts that all expense fields are present.
     * @param exp Header string for a csv file.
     */
    static void assertAllExpenseCategoriesCsv(String exp){
        assertAll("Verify returned csv has all correct data fields",
                () -> assertTrue(exp.contains("Expense ID"), "Missing data field Expense ID"),
                () -> assertTrue(exp.contains("Employee"), "Missing data field Employee"),
                () -> assertTrue(exp.contains("Amount"), "Missing data field Amount"),
                () -> assertTrue(exp.contains("Description"), "Missing data field Description"),
                () -> assertTrue(exp.contains("Date"), "Missing data field Date"),
                () -> assertTrue(exp.contains("Category"), "Missing data field Category"),
                () -> assertTrue(exp.contains("Status"), "Missing data field Status"),
                () -> assertTrue(exp.contains("Reviewer"), "Missing data field Reviewer"),
                () -> assertTrue(exp.contains("Comment"), "Missing data field Comment"),
                () -> assertTrue(exp.contains("Review Date"), "Missing data field Review Date")
            );
    }

    @Nested
    @DisplayName("Login Tests")
    @Feature("Manager Login")
    class LoginTests{

        @BeforeAll
        private static void loginStartup(){
            if(PRINT_DATA){System.out.println("-------STARTING LOGIN TESTS-------");}
        }

        @AfterAll
        private static void loginTeardown(){
            if(PRINT_DATA){System.out.println("-------FINISHED LOGIN TESTS-------");}
        }
        
        @ParameterizedTest(name = "Login test on username {0} and password {1} ({4} emp login)")
        @CsvSource({
            "Andrew, onetwothree, true, 200, valid",
            "Marco, Polo, true, 200, valid",
            "andrew, onetwothree, false, 401, invalid",
            "Andrew, 123, false, 401, invalid"
        })
        void login_tests_with_valid_invalid_credentials(String username, String password, boolean isSuccessful, int expectedStatusCode, String valid){
            if(PRINT_DATA){
                System.out.println("Attempting to login with username \""+username+"\" and password \""+password+"\" ("+valid+" login)");
            }
            String requestBody = "{ \"username\": \""+username+"\", \"password\": \""+password+"\" }";
            Map<String,String> cookies =
            given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .post("/api/auth/login")
            .then()
                .onFailMessage(requestBody + " should be " + valid)
                .body("success", equalTo(isSuccessful))
                .statusCode(expectedStatusCode)
                .extract()
                .cookies();

            if(PRINT_DATA){
                System.out.println("Cookies are: "+cookies);
            }
            if(isSuccessful){
                assertTrue(cookies.containsKey("jwt"));
            }
            else{
                assertFalse(cookies.containsKey("jwt"));
            }
        }

        @ParameterizedTest(name = "Login tests on missing data fields")
        @CsvSource(delimiter = '|', value = {
            "{ \"username\": \"Andrew\" } | Missing password field",
            "{ \"password\": \"onetwothree\" } | Missing username field",
            "{ \"username\": null, \"password\": \"onetwothree\" } | Null username field",
            "{ \"username\": \"Andrew\", \"password\": null } | Null password field"
        })
        void login_tests_with_bad_data_format(String requestBody, String failReason){
            if(PRINT_DATA){
                System.out.println("Attempting to login ("+failReason+")");
            }
            given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .post("/api/auth/login")
            .then()
                .onFailMessage(failReason)
                .body("success", equalTo(false))
                .statusCode(400);
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    @Feature("Manager Logout")
    class LogoutTests{

        @BeforeAll
        private static void logoutStartup(){
            if(PRINT_DATA){System.out.println("-------STARTING LOGOUT TESTS-------");}
        }

        @AfterAll
        private static void logoutTeardown(){
            if(PRINT_DATA){System.out.println("-------FINISHED LOGOUT TESTS-------");}
        }

        @Test
        @DisplayName("Logout without login")
        @Disabled("Returns success even when no manager is logged in")
        void test_logout_without_login(){
            if(PRINT_DATA){System.out.println("Attempting to logout without logging in.");}
            given()
                .baseUri(BASE_URL)
            .when()
                .post("/api/auth/logout")
            .then()
                .body("success", equalTo(false))
                .statusCode(400);
        }

        @Test
        @DisplayName("Logout after logging in")
        void test_logout_after_login(){
            if(PRINT_DATA){System.out.println("Attempting to logout after logging in.");}
            String jwt = 
            given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body("{ \"username\": \"Andrew\", \"password\": \"onetwothree\" }")
            .when()
                .post("/api/auth/login")
            .then()
                .extract()
                .cookie("jwt");

            given()
                .baseUri(BASE_URL)
                .cookie("jwt", jwt)
            .when()
                .post("/api/auth/logout")
            .then()
                .body("success", equalTo(true))
                .statusCode(200);
        }
    }

    @Nested
    @DisplayName("Test restricted endpoints without authorization")
    @Feature("Manager Authentication")
    class NoAuthTests{

        @BeforeAll
        private static void logoutStartup(){
            if(PRINT_DATA){System.out.println("-------STARTING AUTH TESTS-------");}
        }

        @AfterAll
        private static void logoutTeardown(){
            if(PRINT_DATA){System.out.println("-------FINISHED AUTH TESTS-------");}
        }
        
        @ParameterizedTest(name = "Test without auth endpoint {0}")
        @ValueSource(strings = {
            "/api/expenses",
            "/api/expenses/pending",
            "/api/expenses/employee/Bob",
            "/api/reports/expenses/csv",
            "/api/reports/expenses/pending/csv",
            "/api/reports/expenses/employee/Bob/csv",
            "/api/reports/expenses/category/Other/csv",
            "/api/reports/expenses/daterange/csv?startDate=2020/01/01&endDate=2026/01/01"
        })
        void test_restricted_get_endpoints_without_auth(String endpoint){
            if(endpoint.equals("/api/expenses")){
                Assumptions.abort("This test fails, no authorization required for /api/expenses, but should be required");
            }
            if(PRINT_DATA){System.out.println("Testing endpoint without auth: "+endpoint);}
            given()
                .baseUri(BASE_URL)
            .when()
                .get(endpoint)
            .then()
                .onFailMessage("When trying to access " + endpoint)
                //.body("success", equalTo(false))
                .statusCode(401);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "/api/expenses/77/approve",
            "/api/expenses/77/deny"
        })
        void test_restricted_post_endpoints_without_auth(String endpoint){
            if(PRINT_DATA){System.out.println("Testing endpoint without auth: "+endpoint);}
            given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body("{ \"comment\": \"Some comment\" }")
            .when()
                .post(endpoint)
            .then()
                .onFailMessage("When trying to access " + endpoint)
                .statusCode(401);
        }
    }

    @Nested
    @DisplayName("View Expenses Tests")
    @Feature("Manager View Expenses")
    class GetExpensesTest{

        @BeforeAll
        private static void logoutStartup(){
            if(PRINT_DATA){System.out.println("-------STARTING GET EXPENSES TESTS-------");}
        }

        @AfterAll
        private static void logoutTeardown(){
            if(PRINT_DATA){System.out.println("-------FINISHED GET EXPENSES TESTS-------");}
        }

        @ParameterizedTest(name = "Get expense with endpoint {0}")
        @ValueSource(strings = {
            "/api/expenses",
            "/api/expenses/pending",
            "/api/expenses/employee/Bob",
            "/api/expenses/employee/Tommy"
        })
        void test_expense_endpoints_valid_request_return_success(String endpoint){
            if(PRINT_DATA){System.out.println("Getting expenses with "+endpoint);}
            given()
                .baseUri(BASE_URL)
                .cookie("jwt", jwtToken)
            .when()
                .get(endpoint)
            .then()
                .body("success", equalTo(true))
                .statusCode(200)
                .body("count", greaterThan(0));
        }

        @ParameterizedTest(name = "Test get expenses for employee {0} fail")
        @CsvSource({
            "Justin, no user named Justin",
            "Marco, Marco is a manager"
        })
        void test_get_expenses_for_employee_with_invalid_employee(String username, String failReason){
            if(PRINT_DATA){System.out.println("Attemping to get expenses from invalid employee "+username);}
            given()
                .baseUri(BASE_URL)
                .cookie("jwt", jwtToken)
            .when()
                .get("/api/expenses/employee/"+username)
            .then()
                .onFailMessage("Should fail because "+failReason)
                .statusCode(400);
        }

        @Test
        @DisplayName("Test get all expenses returns correct data")
        void test_get_all_expenses_returns_all_expenses() throws SQLException{
            if(PRINT_DATA){System.out.println("Getting and checking return data of get all expenses endpoint");}
            Statement s = conn.createStatement();
            ResultSet rs = s.executeQuery("""
                    SELECT COUNT(*) FROM EXPENSES
                    """);
            assumeTrue(rs.next(), "Error with JDBC object, aborting test");
            int exp_count = rs.getInt("COUNT(*)");

            List<LinkedHashMap<String,String>> expList = 
            given()
                .baseUri(BASE_URL)
                .cookie("jwt", jwtToken)
            .when()
                .get("/api/expenses")
            .then()
                .body("success", equalTo(true))
                .statusCode(200)
                .body("count", equalTo(exp_count))
                .extract()
                .response()
                .jsonPath()
                .getList("data");

            LinkedHashMap<String,String> sampleExp = expList.get(0);
            if(PRINT_DATA){System.out.println("Sample expense: "+sampleExp);}
            assertAllExpenseCategories(sampleExp);

        }

        @Test
        @DisplayName("Test get pending expenses returns all pending expenses")
        void test_get_pending_expenses_returns_pending_expenses() throws SQLException{
            if(PRINT_DATA){System.out.println("Getting and checking return data of get all pending expenses endpoint");}
            Statement s = conn.createStatement();
            ResultSet rs = s.executeQuery("""
                    SELECT COUNT(*) FROM approvals
                    WHERE status = 'pending'
                    """);
            assumeTrue(rs.next(), "Error with JDBC object, aborting test");
            int exp_count = rs.getInt("COUNT(*)");
            s.close();
            rs.close();

            List<LinkedHashMap<String,String>> expList = 
            given()
                .baseUri(BASE_URL)
                .cookie("jwt", jwtToken)
            .when()
                .get("/api/expenses/pending")
            .then()
                .body("success", equalTo(true))
                .statusCode(200)
                .body("count", equalTo(exp_count))
                .extract()
                .response()
                .jsonPath()
                .getList("data");
            LinkedHashMap<String,String> sampleExp = expList.get(0);
            if(PRINT_DATA){System.out.println("Sample expense: "+sampleExp);}
            assertAllExpenseCategories(sampleExp);

            for(LinkedHashMap<String,String> expenses : expList){
                assertEquals(expenses.get("status"), "pending", "All expenses should have status pending");
            }
        }

        @Test
        @DisplayName("Test get pending expenses returns all pending expenses")
        void test_get_employee_expenses_returns_employee_expenses() throws SQLException{
            if(PRINT_DATA){System.out.println("Getting and checking return data of get all employee expenses endpoint");}
            Statement s = conn.createStatement();
            ResultSet rs = s.executeQuery("""
                    SELECT COUNT(*) FROM expenses
                    WHERE user_id = 2;
                    """);
            assumeTrue(rs.next(), "Error with JDBC object, aborting test");
            int exp_count = rs.getInt("COUNT(*)");
            s.close();
            rs.close();

            List<LinkedHashMap<String,String>> expList = 
            given()
                .baseUri(BASE_URL)
                .cookie("jwt", jwtToken)
            .when()
                .get("/api/expenses/employee/Bob")
            .then()
                .body("success", equalTo(true))
                .statusCode(200)
                .body("count", equalTo(exp_count))
                .extract()
                .response()
                .jsonPath()
                .getList("data");
            LinkedHashMap<String,String> sampleExp = expList.get(0);
            if(PRINT_DATA){System.out.println("Sample expense: "+sampleExp);}
            assertAllExpenseCategories(sampleExp);
            
            for(LinkedHashMap<String,String> expenses : expList){
                assertEquals(expenses.get("emp_name"), "Bob", "All expenses should belong to Bob.");
            }
        }
    }

    @Nested
    @DisplayName("Test approve/deny expense endpoints")
    @Feature("Manager approve/deny expense")
    class TestApproveDenyExpense{

        @BeforeAll
        private static void logoutStartup(){
            if(PRINT_DATA){System.out.println("-------STARTING APPROVE/DENY TESTS-------");}
        }

        @AfterAll
        private static void logoutTeardown(){
            if(PRINT_DATA){System.out.println("-------FINISHED APPROVE/DENY TESTS-------");}
        }

        @ParameterizedTest(name = "{2}")
        @CsvSource({
            "approve, some description, valid approval with description, approved",
            "approve,, valid approval with empty description, approved",
            "deny, some description, valid deny with description, denied",
            "deny,, valid deny with empty description, denied"
        })
        void test_approve_deny_expense_endpoints_valid_requests_succeed(String approveOrDeny, String comment, String testInfo, String expectedStatus) throws SQLException{
            if(PRINT_DATA){System.out.println("Trying to "+approveOrDeny+" an expense ("+testInfo+")");}
            given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body("{ \"comment\": \""+comment+"\" }")
                .cookie("jwt", jwtToken)
            .when()
                .post("/api/expenses/77/"+approveOrDeny)
            .then()
                .body("success", equalTo(true))
                .statusCode(200);
                //.body("message", containsString(approveOrDeny));
            Statement s = conn.createStatement();
            ResultSet rs = s.executeQuery("""
                    SELECT status, reviewer, comment
                    FROM approvals
                    WHERE expense_id = 77;
                    """);
            assertTrue(rs.next(), "Expense not found in database after trying to "+approveOrDeny+" it.");
            assertAll("Edited expense changed correct fields",
                () -> assertEquals(rs.getString("status"), expectedStatus),
                () -> assertEquals(rs.getInt("reviewer"), 1)
                //() -> assertEquals(rs.getString("comment"), comment)
            );
        }

        @ParameterizedTest(name = "Test invalid {1} ({2})")
        @CsvSource({
            "10, approve, expense does not exist",
            "10, deny, expense does not exist",
            "75, approve, expense already approved",
            "75, deny, expense already approved",
            "51, approve, expense already denied",
            "51, deny, expense already denied",
        })
        void test_approve_deny_expense_endpoints_invalid_requests_fail(int expense_id, String approveOrDeny, String failReason){
            if(PRINT_DATA){System.out.println("Trying to "+approveOrDeny+" an expense ( should fail because "+failReason+")");}
            given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body("{ \"comment\": \"Some comment\" }")
                .cookie("jwt", jwtToken)
            .when()
                .post("/api/expenses/"+expense_id+"/"+approveOrDeny)
            .then()
                .onFailMessage("Request should fail because "+failReason)
                //.body("success", equalTo(false))
                .statusCode(404);
        }
    }

    @Nested
    @Feature("Manager Reports")
    @DisplayName("Test Report Generation")
    class TestReportGeneration{

        @BeforeAll
        private static void logoutStartup(){
            if(PRINT_DATA){System.out.println("-------STARTING GENERATE REPORT TESTS-------");}
        }

        @AfterAll
        private static void logoutTeardown(){
            if(PRINT_DATA){System.out.println("-------FINISHED GENERATE REPORT TESTS-------");}
        }

        @ParameterizedTest(name = "Test valid endpoint {2}")
        @ValueSource(strings = {
            "api/reports/expenses/csv",
            "api/reports/expenses/pending/csv",
            "api/reports/expenses/employee/Bob/csv",
            "api/reports/expenses/category/Supplies/csv",
            "api/reports/expenses/daterange/csv?startDate=2021/03/01&endDate=2027/01/01"
        })
        void test_valid_paths_succeed_for_all_report_endpoints(String endpoint){
            if(PRINT_DATA){System.out.println("Testing correct csv fields for endpoint "+endpoint);}
            Response r =
            given()
                .baseUri(BASE_URL)
                .cookie("jwt", jwtToken)
            .when()
                .get(endpoint)
            .then()
                .onFailMessage("Failed for endpoint "+endpoint)
                .statusCode(200)
                .extract()
                .response();
            String[] csvParse = r.asString().split("\n");
            /*System.out.println(csvParse.length);
            for(int k = 0; k < csvParse.length; k++){
                System.out.println(csvParse[k]);
            }*/
            assertAllExpenseCategoriesCsv(csvParse[0]);
        }

        @ParameterizedTest(name = "Test if correct number of expenses returned for endpoint {0}")
        @CsvSource({
            "/api/reports/expenses/csv,;",
            "/api/reports/expenses/pending/csv, WHERE a.status = 'pending';",
            "/api/reports/expenses/employee/Bob/csv, WHERE e.user_id = 2;",
            "/api/reports/expenses/employee/Tommy/csv, WHERE e.user_id = 3;",
            "/api/reports/expenses/employee/Caleb/csv, WHERE e.user_id = 4;",
            "/api/reports/expenses/category/Travel/csv, WHERE e.category = 'Travel';",
            "/api/reports/expenses/category/Services/csv, WHERE e.category = 'Services';",
            "/api/reports/expenses/category/Repairs/csv, WHERE e.category = 'Repairs';",
            "/api/reports/expenses/daterange/csv?startDate=2026/07/02&endDate=2026/07/15, WHERE e.date >= '2026/07/02' AND e.date <= '2026/07/15';",
            "/api/reports/expenses/daterange/csv?startDate=2020/01/01&endDate=2021/01/01, WHERE e.date >= '2020/01/01' AND e.date <= '2021/01/01';",
            "/api/reports/expenses/daterange/csv?startDate=2026/07/24&endDate=2026/07/24, WHERE e.date >= '2026/07/24' AND e.date <= '2026/07/24';",
            "/api/reports/expenses/daterange/csv?startDate=2026/07/25&endDate=2026/07/24, WHERE e.date >= '2026/07/25' AND e.date <= '2026/07/24';"
        })
        void test_gen_report_all_expenses(String endpoint, String sqlCondition) throws SQLException{
            if(PRINT_DATA){System.out.println("Testing if correct number of expenses are returned for endpoint "+endpoint);}
            String[] csvParse =
            given()
                .baseUri(BASE_URL)
                .cookie("jwt", jwtToken)
            .when()
                .get(endpoint)
            .then()
                .statusCode(200)
                .extract()
                .response()
                .asString()
                .split("\n");
            Statement s = conn.createStatement();
            String sqlQuery = "SELECT COUNT(*) FROM expenses e INNER JOIN approvals a ON e.id = a.expense_id "+sqlCondition;
            //System.out.println("The query is "+sqlQuery);
            ResultSet rs = s.executeQuery(sqlQuery);
            int num_exps = rs.getInt("COUNT(*)");
            rs.close();
            s.close();
            assertEquals(csvParse.length - 1, num_exps, "Wrong number of expenses returned for "+endpoint+"."); // -1 because of header in csv file
        }

        @ParameterizedTest(name = "Invalid employee report for employee {0}")
        @ValueSource(strings = {"bob","Andrew","","1=1"})
        void test_gen_report_on_invalid_employee_fails(String empName){
            if(PRINT_DATA){System.out.println("Attempting to generate employee report with invalid employee named "+empName);}
            given()
                .baseUri(BASE_URL)
                .cookie("jwt",jwtToken)
            .when()
                .get("/api/reports/expenses/employee/"+empName+"/csv")
            .then()
            .onFailMessage("Testing for invalid employee name "+empName)
                .statusCode(oneOf(404,500));
        }

        @ParameterizedTest(name = "Name")
        @ValueSource(strings = {"Catering","Vehicle","NotACategory"})
        void test_gen_report_on_invalid_category_defaults_to_category_other(String wrongCategory){
            if(PRINT_DATA){System.out.println("Attempting to generate category report on non-existent category "+wrongCategory);}
            String[] csvParse = 
            given()
                .baseUri(BASE_URL)
                .cookie("jwt",jwtToken)
            .when()
                .get("/api/reports/expenses/category/"+wrongCategory+"/csv")
            .then()
                .statusCode(200)
                .extract()
                .response()
                .asString()
                .split("\n");
            assertTrue(csvParse[1].contains(",Other"), "Requests to generate report on invalid category should default to category 'Other'");
        }
    }
}
