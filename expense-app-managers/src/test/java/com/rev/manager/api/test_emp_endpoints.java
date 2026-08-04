package com.rev.manager.api;

import static io.restassured.RestAssured.*;
import static io.restassured.matcher.RestAssuredMatchers.*;
import static org.hamcrest.Matchers.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.lu.ugeholl;
// Standard imports
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.http.ContentType;

import io.restassured.path.json.mapper.factory.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class test_emp_endpoints {
    private final String baseUrl = "http://127.0.0.1:5000";
    private Connection conn;

    @BeforeAll
    void setup() throws SQLException{
        conn = utilities.getConnection();
    }

    @AfterAll
    void cleanup() throws SQLException{
        if(conn != null){
            conn.close();
        }
    }

    @BeforeEach
    void setupBefore(){
        utilities.resetDatabase();
    }

    @AfterEach
    void teardown(){
        // Clear any changes made to database.
    }

    @ParameterizedTest
    @CsvSource({
        "Bob, bob_22, success, Login, 200",
        "Bob, bob22, error, combination, 401",
        "Bob, , error, password, 401",
        ", bob_22, error, username, 401"
    })
    void test_login_valid_body (String username, String password, String status, String message, int statusCode){
        String requestBody = "{ \"username\": \"" + username + "\", \"password\": \"" + password + "\" }";
        //Response r =
        given()
            .baseUri(baseUrl)
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
    void test_login_missing_username(){
        String requestBody = "{ \"password\": \"bob_22\" }";
        given()
            .baseUri(baseUrl)
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
    void test_login_missing_password(){
        String requestBody = "{ \"username\": \"Bob\" }";
        given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/login")
        .then()
            .body("status", equalTo("error"))
            .body("message", containsString("Password"))
            .statusCode(400);
    }

    @Test
    void create_database_test(){

    }
}
