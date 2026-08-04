package com.rev.manager.DAO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.rev.manager.model.Category;
import com.rev.manager.model.Expense;
import com.rev.manager.model.Status;

import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;

@Epic("Expense Management System")
@Feature("Manager DAO")
class JDBCManagerDAOTest {
    private Connection conn;
    private JDBCManagerDAO managerDAO; 

    /**
     * @BeforeEach Creates a DB connection and a temporary DB schema (empty) in memory for testing before each test.
     * @throws Exception If there is any errors establishing the connection or reading the File.
     */
    @BeforeEach
    void setup() throws Exception{
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");

        String schema =  Files.readString(
            Path.of("src/test/resources/test-schema.sql")
        );

        Statement st = conn.createStatement();
        
        for(String sql : schema.split(";")) {
            if(!sql.trim().isEmpty()) {
                st.execute(sql);
            }
        }
 
        managerDAO = new JDBCManagerDAO(conn);
    }

    /**
     * @AfterEach Closes the connection with the DB after each test.
     * @throws Exception If unable to close the connection.
     */
    @AfterEach
    void cleanup() throws Exception{
        conn.close();
    }

    /**
     * Test suite for validating the login functionality of the JDBCManagerDAO.
     */
    @Nested
    @DisplayName("Login Tests")
    @Feature("Login Tests")
    class LoginTests {
        
        /**
         * Verifies that a manager can successfully log in using valid credentials.
         */
        @Test
        @Story("Successful Authentication")
        @Severity(SeverityLevel.CRITICAL)
        @Description("""
            Verifies that a manager with valid credentials is successfully
            authenticated and their unique manager ID is returned.
        """)
        @DisplayName("Login Manager With Valid Password Succeeds")
        void login_validManager_returnsManagerId() throws Exception{
            // ARRANGE
            Allure.step("Insert manager user");
            insertUser(1L, "John", "123", "Manager");

            // ACT
            Allure.step("Attempt login with valid credentials");
            long id = managerDAO.login("John", "123");

            // ASSERT
            Allure.step("Verify returned manager id");
            assertEquals(1L, id);

        }

        /**
         * Verifies that login fails when a manager provides an incorrect password.
         * Throws InvalidLoginException if the password does not match the stored credentials.
         */
        @Test
        @Story("Authentication Failure")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that attempting to log in with an incorrect password
            throws an InvalidLoginException.    
        """)
        @DisplayName("Login Manager With Invalid Password Throws Exception")
        void login_invalidPassword_throwsException() throws Exception{
            // ARRANGE
            Allure.step("Insert user");
            insertUser(1L, "John", "123", "Manager");

            // ACT & ASSERT
            Allure.step("Attempt login with invalid password");
            assertThrows(ManagerException.InvalidLoginException.class,
                        () -> managerDAO.login("John", "321")
            );
        }

        /**
         * Verifies that employee credentials cannot be used to access manager login functionality.
         * Throws InvalidLoginException if the role does not match "Manager"
         */
        @Test
        @Story("Authorization")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that employee credentials cannot be used to authenticate
            as a manager.
        """)
        @DisplayName("Login Employee With Valid Credentials Throws Exception")
        void login_employeeCredentials_throwsException() throws Exception{
            // ARRANGE
            Allure.step("Insert user");
            insertUser(1L, "Bob", "bob_22", "Employee");

            // ACT & ASSERT
            Allure.step("Attempt manager login using employee credentials");
            assertThrows(ManagerException.InvalidLoginException.class,
                        () -> managerDAO.login("Bob", "bob_22")
            );
        }

        /**
         * Verifies that login fails when credentials do not match any user in the database.
         * Throws InvalidLoginException.
         */
        @Test
        @Step("Authentication Failure")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that logging in with credentials belonging to a
            non-existent user throws an InvalidLoginException.    
        """)
        @DisplayName("Unknown User Credentials Throws Exception")
        void login_unknownUser_throwsException() throws Exception{
            // NO ARRANGE
            // ACT & ASSERT
            Allure.step("Attempt login with unknown credentials");
            assertThrows(ManagerException.InvalidLoginException.class,
                        () -> managerDAO.login("Sarah", "123")
            );
        }

    }

    /**
     * Test suite for validating the view expenses functionality of the JDBCManagerDAO.
     */
    @Nested
    @DisplayName("View Expenses Tests")
    @Feature("Expense View")
    class ViewExpensesTests {

        /**
         * Verifies that view_expenses() returns a list of pending expenses.
         */
        @Test
        @Step("View Pending Expenses")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that viewing expenses returns all expenses
            currently pending approval.        
        """)
        @DisplayName("View Expenses Returns Pending Expenses")
        void viewExpenses_returnsPendingExpenses() throws Exception {
            // ARRANGE
            Allure.step("Insert user");
            insertUser(1L, "John", "123", "Manager");
            insertExpense(100L, 1L, 20.00, "Lunch", "2026/07/01", Category.Meals);
            insertApproval(200L, 100L, "pending");

            // ACT
            Allure.step("Retrieve Pending Expenses");
            List<Expense> expenses = managerDAO.view_expenses();
            attachExpenses("Returned Expenses", expenses);
            // ASSERT
            Allure.step("Verify one pending expense is returned");
            assertAll(
                () -> assertEquals(1, expenses.size()),
                () -> assertEquals(100L, expenses.get(0).getExpense_id()),
                () -> assertEquals(Status.pending, expenses.get(0).getStatus())
            );
        }
        
        /**
         * Verifies that view_expenses() returns only pending expenses from a database containing
         * approved and pending expenses.
         */
        @Test
        @Story("View Pending Expenses")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that expenses with an approved status
            are not returned when viewing pending expenses.   
        """)
        @DisplayName("View Expenses Returns List Without Approved Expenses")
        void viewExpenses_approvedExpenses_returnsListWithoutApproved() throws Exception {
            // ARRANGE
            Allure.step("Insert user");
            insertUser(1L, "John", "123", "Employee");
            // Pending Expense
            insertExpense(100L, 1L, 20.00, "Lunch", "2026/07/01", Category.Meals);
            insertApproval(300L, 100L, "pending");
            // Approved Expense
            insertExpense(200L, 1L, 50.00, "Dinner", "2026/07/01", Category.Meals);
            insertApproval(400L, 200L, "approved", 2L, "Nice", "2026/07/03");

            // ACT
            Allure.step("Retrieve pending expenses");
            List<Expense> expenses = managerDAO.view_expenses();
            attachExpenses("Returned Expenses", expenses);
            // ASSERT
            Allure.step("Verify approved expense is excluded");
            assertEquals(1, expenses.size());
            assertEquals(100L, expenses.get(0).getExpense_id());
        }
        
        /**
         * Verifies that view_expenses() returns an empty list when there are only approved expenses in
         * the database.
         */
        @Test
        @Story("View Pending Expenses")
        @Severity(SeverityLevel.MINOR)
        @Description("""
            Verifies that an empty list is returned when
            there are no pending expenses.
        """)
        @DisplayName("View Expenses With No Pending Expenses Returns Empty List")
        void viewExpenses_noPendingExpenses_returnsEmptyList() throws Exception{
            // ARRANGE
            Allure.step("Insert user");
            insertUser(1L, "John", "123", "Employee");
            insertExpense(100L, 1L, 20.00, "Lunch", "2026/07/01", Category.Meals);
            insertApproval(200L, 100L, "approved", 2L, "Nice", "2026/07/03");

            // ACT
            Allure.step("Retrieve pending expenses");
            List<Expense> expenses = managerDAO.view_expenses();
            attachExpenses("Returned Expenses", expenses);

            // ASSERT
            Allure.step("Verify returned list is empty");
            assertTrue(expenses.isEmpty());
        }
    
        /**
         * Verifies that view_expenses() returns multiple pending expenses from a database
         * containing only pending expenses.
         */
        @Test
        @Story("View Pending Expenses")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
        Verifies that all pending expenses are returned when multiple
        expenses are awaiting manager approval.
        """)
        @DisplayName("View Expenses Returns Multiple Pending Expenses")
        void viewExpenses_multiplePendingExpenses_returnsPendingExpenses() throws Exception {
            // ARRANGE
            Allure.step("Insert user");
            insertUser(1L, "John", "123", "Employee");
            // Pending Expense #1
            insertExpense(100L, 1L, 20.00, "Lunch", "2026/07/01", Category.Meals);
            insertApproval(300L, 100L, "pending");
            // Pending Expense #2
            insertExpense(200L, 1L, 50.00, "Dinner", "2026/07/01", Category.Meals);
            insertApproval(400L, 200L, "pending");

            // ACT
            Allure.step("Retrieve multiple pending expenses");
            List<Expense> expenses = managerDAO.view_expenses();
            attachExpenses("Returned Expenses", expenses);

            // ASSERT
            Allure.step("Retrieve returned list contains multiple pending expenses");
            assertEquals(2, expenses.size());
        }
    
        /**
         * Verifies that view_expenses() returns pedning expenses ordered by
         * date (descending) and amount (descending) from a database containing
         * multiple pending expenses.
         */
        @Test
        @Story("View Pending Expenses")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
        Verifies that pending expenses are sorted by expense date in
        descending order and, when dates are equal, by expense amount
        in descending order.
        """)
        @DisplayName("View Expenses Returns Expenses Ordered By Date & Amount") 
        void viewExpenses_orderByDateAndAmount_returnsOrderedExpenses() throws Exception {
            // ARRANGE
            Allure.step("Insert user");
            insertUser(1L, "John", "123", "Employee");
            // Pending Expense #1
            insertExpense(100L, 1L, 20.00, "Older", "2026/07/01", Category.Meals);
            insertApproval(300L, 100L, "pending");
            // Pending Expense #2
            insertExpense(200L, 1L, 50.00, "Newer", "2026/07/05", Category.Meals);
            insertApproval(400L, 200L, "pending");

            // ACT
            Allure.step("Retrieve multiple pending expenses (ordered)");
            List<Expense> expenses = managerDAO.view_expenses();
            attachExpenses("Ordered Expenses", expenses);

            // ASSERT
            Allure.step("Verify pending expenses are ordered");
            assertEquals(200L, expenses.get(0).getExpense_id());
        }
    }

    /**
     * Test suite for validating the approve expenses functionality of the JDBCManagerDAO.
     */
    @Nested
    @DisplayName("Approve Expenses Tests")
    @Feature("Expense Review")
    class ApproveExpensesTests {

        /**
         * Verifies that approve_exp() updates an expense and returns the updated expense.
         */
        @Test
        @Story("Approve Expense")
        @Severity(SeverityLevel.CRITICAL)
        @Description("""
            Verifies that a manager can successfully approve a pending expense.
            The expense status, reviewer, comment, and review date are updated,
            and the updated Expense object is returned.
        """)
        @DisplayName("Approving Pending Expense Returns Updated Expense")
        void approveExp_pendingExpense_returnsExpense() throws Exception {
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            insertExpense(100L, 1L, 20.00, "Lunch", "2026/07/01", Category.Meals);
            insertApproval(200L, 100L, "pending");
            Allure.step("Insert user #2");
            insertUser(2L, "Bob", "345", "Manager");
            
            // ACT
            Allure.step("Approve expense #100");
            Expense updatedExpense = managerDAO.approve_exp(2L, 100L, "Nice");
            attachExpense("Approved Expense", updatedExpense);

            // ASSERT
            Allure.step("Verify expense was approved");
            assertAll(
                () -> assertEquals(100L, updatedExpense.getExpense_id()),
                () -> assertEquals(Status.approved, updatedExpense.getStatus()),
                () -> assertEquals("Bob", updatedExpense.getReviewer_name()),
                () -> assertEquals("Nice", updatedExpense.getComment()),
                () -> assertEquals(LocalDate.now().toString().replace('-', '/'), updatedExpense.getReview_date())
            );
            
        }
        
        /**
         * Verifies that approve_exp() throws ExpenseNotFoundException when trying to
         * find an expense using a non-existent id.
        */
        @Test
        @Story("Approve Expense")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that attempting to approve a non-existent expense
            throws an ExpenseNotFoundException.
        """)
        @DisplayName("Approving Non-Existent Expense throws ExpenseNotFoundException")
        void approveExp_nonExistentId_throwsException() throws Exception{
            // ARRANGE
            Allure.step("Insert user#1");
            insertUser(1L, "John", "123", "Employee");
            insertExpense(100L, 1L, 20.00, "Lunch", "2026/07/01", Category.Meals);
            insertApproval(200L, 100L, "pending");
            Allure.step("Insert user#2");
            insertUser(2L, "Bob", "345", "Manager");
            // ACT & Assert
            Allure.step("Attempt to approve non-existent expense");
            assertThrows(ManagerException.ExpenseNotFoundException.class,
                         () -> managerDAO.approve_exp(2L, 300L, "Nice")
            );
        }

        /**
         * Verifies that approve_exp() throws ExpenseNotPendingException when trying to
         * approve an already approved expense.
         */
        @Test
        @Story("Approve Expense")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that an expense that has already been approved
            cannot be approved again.
        """)
        @DisplayName("Approving Approved Expense Throws ExpenseNotPendingException") 
        void approveExp_approvedExpense_throwsException() throws Exception{
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            insertExpense(100L, 1L, 20.00, "Lunch", "2026/07/01", Category.Meals);
            insertApproval(200L, 100L, "approved", 2L, "Nice", "2026/07/03");
            Allure.step("Insert user #2");
            insertUser(2L, "Bob", "345", "Manager");
            // ACT & Assert
            Allure.step("Attempt to approve approved expense");
            assertThrows(ManagerException.ExpenseNotPendingException.class,
                         () -> managerDAO.approve_exp(2L, 100L, "Nice")
            );
        }
    
        /**
         * Verifies that approve_exp() throws ExpenseNotPendingException when trying to
         * approve an already denied expense.
         */
        @Test
        @Story("Approve Expense")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that an expense that has already been denied
            cannot be approved.
        """)
        @DisplayName("Approving Denied Expense Throws ExpenseNotPendingException")
        void approveExp_deniedExpense_throwsException() throws Exception{
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            insertExpense(100L, 1L, 20.00, "Lunch", "2026/07/01", Category.Meals);
            insertApproval(200L, 100L, "denied", 2L, "Not Nice", "2026/07/03");
            Allure.step("Insert user #2");
            insertUser(2L, "Bob", "345", "Manager");
            // ACT & Assert
            Allure.step("Attempt to approve denied expense");
            assertThrows(ManagerException.ExpenseNotPendingException.class,
                         () -> managerDAO.approve_exp(2L, 100L, "Nice")
            );
        }  
    } 

    /** 
     * Test suite for validating the deny expenses functionality of the JDBCManagerDAO.
     */
    @Nested
    @DisplayName("Deny Expenses Tests")
    @Feature("Expense Review")
    class DenyExpensesTests {
        /**
         * Verifies that deny_exp() updates an expense and returns the updated expense.
         */
        @Test
        @Story("Deny Expense")
        @Severity(SeverityLevel.CRITICAL)
        @Description("""
            Verifies that a manager can successfully deny a pending expense.
            The expense status, reviewer, comment, and review date are updated,
            and the updated Expense object is returned.
        """)
        @DisplayName("Denying Pending Expense Returns Updated Expense")
        void denyExp_pendingExpense_returnsExpense() throws Exception {
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            insertExpense(100L, 1L, 500.00, "Lunch", "2026/07/01", Category.Meals);
            insertApproval(200L, 100L, "pending");
            Allure.step("Insert user #2");
            insertUser(2L, "Bob", "345", "Manager");
            
            // ACT
            Allure.step("Deny expense #100");
            Expense updatedExpense = managerDAO.deny_exp(2L, 100L, "Not Nice");
            attachExpense("Denied Expense", updatedExpense);
            // ASSERT
            Allure.step("Verify expense was denied");
            assertAll(
                () -> assertEquals(100L, updatedExpense.getExpense_id()),
                () -> assertEquals(Status.denied, updatedExpense.getStatus()),
                () -> assertEquals("Bob", updatedExpense.getReviewer_name()),
                () -> assertEquals("Not Nice", updatedExpense.getComment()),
                () -> assertEquals(LocalDate.now().toString().replace('-', '/'), updatedExpense.getReview_date())
            );
        }
        
        /**
         * Verifies that deny_exp() throws ExpenseNotFoundException when trying to
         * find an expense using a non-existent id.
        */
        @Test
        @Story("Deny Expense")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that denying an expense that does not exist
            throws an ExpenseNotFoundException.
        """)
        @DisplayName("Denying Non-Existent Expense throws ExpenseNotFoundException")
        void denyExp_nonExistentId_throwsException() throws Exception{
            // ARRANGE
            Allure.step("Insert manager user #1");
            insertUser(1L, "John", "123", "Employee");
            insertExpense(100L, 1L, 20.00, "Lunch", "2026/07/01", Category.Meals);
            insertApproval(200L, 100L, "pending");
            Allure.step("Insert manager user #2");
            insertUser(2L, "Bob", "345", "Manager");
            // ACT & Assert
            Allure.step("Attempt to deny non-existent expense");
            assertThrows(ManagerException.ExpenseNotFoundException.class,
                         () -> managerDAO.deny_exp(2L, 300L, "Not Nice")
            );
        }

        /**
         * Verifies that deny_exp() throws ExpenseNotPendingException when trying to
         * deny an already approved expense.
         */
        @Test
        @Story("Deny Expense")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that attempting to deny an expense that has
            already been approved throws an ExpenseNotPendingException.
        """)
        @DisplayName("Denying Approved Expense Throws ExpenseNotPendingException")
        void denyExp_approvedExpense_throwsException() throws Exception{
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            insertExpense(100L, 1L, 20.00, "Lunch", "2026/07/01", Category.Meals);
            insertApproval(200L, 100L, "approved", 2L, "Nice", "2026/07/03");
            Allure.step("Insert user #2");
            insertUser(2L, "Bob", "345", "Manager");
            // ACT & Assert
            Allure.step("Attempt to deny approved expense");
            assertThrows(ManagerException.ExpenseNotPendingException.class,
                         () -> managerDAO.deny_exp(2L, 100L, "Not Nice")
            );
        }
    
        /**
         * Verifies that deny_exp() throws ExpenseNotPendingException when trying to
         * deny an already denied expense.
         */
        @Test
        @Story("Deny Expense")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that attempting to deny an expense that has
            already been denied throws an ExpenseNotPendingException.
        """)
        @DisplayName("Denying Denied Expense Throws ExpenseNotPendingException")
        void denyExp_deniedExpense_throwsException() throws Exception{
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            insertExpense(100L, 1L, 20.00, "Lunch", "2026/07/01", Category.Meals);
            insertApproval(200L, 100L, "denied", 2L, "Not Nice", "2026/07/03");
            Allure.step("Insert user #2");
            insertUser(2L, "Bob", "345", "Manager");
            // ACT & Assert
            Allure.step("Attempt to deny already denied expense");
            assertThrows(ManagerException.ExpenseNotPendingException.class,
                         () -> managerDAO.deny_exp(2L, 100L, "Not Nice")
            );
        }  
    
    }

    /**
     * Test suite for validating the generate report by given user functionality of the JDBCManagerDAO.
     */
    @Nested
    @DisplayName("Generate Report By Given User Tests")
    @Feature("Employee Reports")
    class GenReportEmpTests {

        /**
         * Verifies that gen_report_emp() returns an expense list containing
         * all the expenses belonging to the given user.
         * The expenses can have any status (pending, approved, denied)
         */
        @Test
        @Story("Generate Employee Report")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that generating a report for a valid employee
            returns all expenses submitted by that employee.
        """)
        @DisplayName("Generate Employee Report Returns Employee Expense List")
        void genReportEmp_validEmployeeUsername_returnsExpensesList() throws Exception {
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            insertExpense(100L, 1L, 500.00, "Dinner", "2026/07/01", Category.Meals);
            insertApproval(200L, 100L, "pending");
            insertExpense(300L, 1L, 25.00, "Lunch", "2026/07/01", Category.Meals);
            insertApproval(400L, 300L, "approved", 3L, "Nice", "2026/07/03");
            insertExpense(500L, 1L, 50.00, "Dessert", "2026/07/01", Category.Meals);
            insertApproval(600L, 500L, "denied", 3L, "Not Nice", "2026/07/03");
            Allure.step("Insert user #2");
            insertUser(2L, "Bob", "345", "Employee");
            insertExpense(700L, 2L, 500.00, "Dinner", "2026/07/01", Category.Meals);
            insertApproval(800L, 700L, "pending");
            Allure.step("Insert user #3");
            insertUser(3L, "Pablo", "678", "Manager");

            // ACT
            Allure.step("Generate report for user #1");
            List<Expense> expenses = managerDAO.gen_report_emp("John");
            attachExpenses("Employee Report", expenses);

            // ASSERT
            Allure.step("Verify returned expenses belong to 'John'");
            assertEquals(3, expenses.size());
            assertEquals("John", expenses.get(0).getEmp_name());
            assertTrue(expenses.stream().anyMatch(e -> e.getStatus() == Status.pending));
            assertTrue(expenses.stream().anyMatch(e -> e.getStatus() == Status.approved));
            assertTrue(expenses.stream().anyMatch(e -> e.getStatus() == Status.denied));
        }
        
        /**
         * Verifies that generating a report for a valid employee username
         * returns the employee's expenses sorted by status (descending),
         * date (descending), and amount (descending).
         */
        @Test
        @Story("Generate Employee Report")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that an employee report is ordered by status,
            expense date, and expense amount.
        """)
        @DisplayName("Generate Employee Report Returns Expenses In Correct Order")
        void genReportEmp_validEmployeeUsername_returnsOrderedExpensesList() throws Exception {
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            // Expense #1
            insertExpense(100L, 1L, 500.00, "Dinner", "2026/07/01", Category.Meals);
            insertApproval(200L, 100L, "pending");
            // Expense #2
            insertExpense(300L, 1L, 25.00, "Lunch", "2026/07/01", Category.Meals);
            insertApproval(400L, 300L, "pending");
            // Expense #3
            insertExpense(500L, 1L, 25.00, "Dessert", "2026/06/30", Category.Meals);
            insertApproval(600L, 500L, "pending");
            // Expense #4
            insertExpense(700L, 1L, 50.00, "Dessert", "2026/07/01", Category.Meals);
            insertApproval(800L, 700L, "denied", 3L, "Not Nice", "2026/07/03");
            // Expense #5
            insertExpense(900L, 1L, 50.00, "Dessert", "2026/07/01", Category.Meals);
            insertApproval(1000L, 900L, "approved", 3L, "Nice", "2026/07/03");
            Allure.step("Insert user #3");
            insertUser(3L, "Pablo", "678", "Manager");
            // ACT
            Allure.step("Generate report for user #1");
            List<Expense> expenses = managerDAO.gen_report_emp("John");
            attachExpenses("Employee Report", expenses);

            // ASSERT
            Allure.step("Verify expense ordering");
            assertEquals(5, expenses.size());
            assertIterableEquals(List.of(100L,300L,500L,700L,900L),
                                 expenses.stream()
                                         .map(Expense::getExpense_id)
                                         .toList()
            );  
        }

        /**
         * Verifies that generating a report for a valid employee who has
         * submitted no expenses returns an empty list. 
         */
        @Test
        @Story("Generate Employee Report")
        @Severity(SeverityLevel.MINOR)
        @Description("""
            Verifies that an empty list is returned when the employee
            has not submitted any expenses.
        """)
        @DisplayName("Generate Employee Report For Employee With No Expenses Returns Empty List")
        void genReportEmp_validEmployeeWithNoExpenses_returnsEmptyList() throws Exception {
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");

            // ACT
            Allure.step("Generate report for user #1");
            List<Expense> expenses =  managerDAO.gen_report_emp("John");
            attachExpenses("Employee Report", expenses);

            // ASSERT
            Allure.step("Verify no expenses returned");
            assertTrue(expenses.isEmpty());
        }
        
        /**
         * Verifies that generating a report for a username that does not
         * belong to an existing employee throws a UserNotFoundException.
         */
        @Test
        @Story("Generate Employee Report")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that requesting a report for a non-existent employee
            throws a UserNotFoundException.
        """)
        @DisplayName("Generate Employee Report With Unknown Employee Username Throws Exception")
        void genReportEmp_NonExistentEmployeeUsername_throwsException() throws Exception {
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            insertExpense(100L, 1L, 500.00, "Dinner", "2026/07/01", Category.Meals);
            insertApproval(200L, 100L, "pending");
            insertExpense(300L, 1L, 25.00, "Lunch", "2026/07/01", Category.Meals);
            insertApproval(400L, 300L, "approved", 3L, "Nice", "2026/07/03");
            insertExpense(500L, 1L, 50.00, "Dessert", "2026/07/01", Category.Meals);
            insertApproval(600L, 500L, "denied", 3L, "Not Nice", "2026/07/03");
            Allure.step("Insert user #3");
            insertUser(3L, "Pablo", "678", "Manager");
            // ACT & ASSERT
            Allure.step("Attempt to generate report for non-existent user");
            assertThrows(ManagerException.UserNotFoundException.class, 
                         () -> managerDAO.gen_report_emp("Bob")
            );
        }

        /**
         * Verifies that generating a report using a manager's username
         * throws a UserNotFoundException because reports may only be
         * generated for employees.
         */
        @Test
        @Story("Generate Employee Report")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that requesting an employee report for a manager
            throws a UserNotFoundException.
        """)
        @DisplayName("Generate Employee Report With Manager Username Throws Exception")
        void genReportEmp_validManagerUsername_throwsException() throws Exception {
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            insertExpense(100L, 1L, 500.00, "Dinner", "2026/07/01", Category.Meals);
            insertApproval(200L, 100L, "pending");
            insertExpense(300L, 1L, 25.00, "Lunch", "2026/07/01", Category.Meals);
            insertApproval(400L, 300L, "approved", 3L, "Nice", "2026/07/03");
            insertExpense(500L, 1L, 50.00, "Dessert", "2026/07/01", Category.Meals);
            insertApproval(600L, 500L, "denied", 3L, "Not Nice", "2026/07/03");
            Allure.step("Insert user #3");
            insertUser(3L, "Pablo", "678", "Manager");
            // ACT & ASSERT
            Allure.step("Attempt to generate report for non-existent user");
            assertThrows(ManagerException.UserNotFoundException.class,
                         () -> managerDAO.gen_report_emp("Pablo")
            );
        }
    
    }

    /**
     * Test suite for validating the generate report by given user functionality of the JDBCManagerDAO.
     */
    @Nested
    @DisplayName("Generate Report By Category Tests")
    @Feature("Category Reports")
    class GenReportCatTests {

        /**
         * Verifies that generating a report for a valid category returns
         * all expenses associated with that category.
         * This test runs for each value in the Category enum to ensure
         * all categories are handled correctly.
         */
        @ParameterizedTest
        @EnumSource(Category.class)
        @Story("Generate Category Report")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that generating a report for a category
            returns only expenses belonging to that category.
        """)
        @DisplayName("Generate Category Report Returns Expenses For Each Category")
        void genReportCat_givenCategory_returnsExpensesList(Category category) throws Exception {
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            // Different Categories
            insertExpense(100L, 1L, 20.00, "Backpack", "2026/07/01", Category.Supplies);
            insertApproval(101L, 100L, "pending");
            insertExpense(200L, 1L, 150.00, "Flight", "2026/07/01", Category.Travel);
            insertApproval(201L, 200L, "pending");
            insertExpense(300L, 1L, 30.00, "Service", "2026/07/01", Category.Services);
            insertApproval(301L, 300L, "pending");
            insertExpense(400L, 1L, 200.00, "Laptop", "2026/07/01", Category.Repairs);
            insertApproval(401L, 400L, "pending");
            insertExpense(500L, 1L, 25.00, "Lunch", "2026/07/01", Category.Meals);
            insertApproval(501L, 500L, "pending");
            insertExpense(600L, 1L, 500.00, "CompTIA A+", "2026/07/01", Category.Certifications);
            insertApproval(601L, 600L, "pending");
            insertExpense(700L, 1L, 70.00, "Shoes", "2026/07/01", Category.Other);
            insertApproval(701L, 700L, "pending");

            // ACT
            Allure.step("Generate report for category " + category.toString());
            List<Expense> expenses = managerDAO.gen_report_cat(category);
            attachExpenses(category.toString() + " Report", expenses);

            // ASSERT
            Allure.step("Verify all returned expenses belong to category " + category.toString());
            for(Expense expense : expenses){
                assertEquals(category, expense.getCategory());
            }
        }

        /**
         * Verifies that generating a report for a category returns expenses
         * in the correct order according to the query sorting rules:
         * status descending, date descending, and amount descending.
         */
        @Test
        @Story("Generate Category Report")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that expenses within a category report are ordered by
            expense date (descending), approval status, and expense amount
            (descending).
        """)
        @DisplayName("Generate Category Report Returns Expenses In Correct Order")
        void genReportCat_givenCategory_returnsOrderedExpensesList() throws Exception {
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            // Expenses #1
            insertExpense(100L, 1L, 20.00, "Notepad", "2026/07/01", Category.Supplies);
            insertApproval(200L, 100L, "pending");
            // Expenses #2
            insertExpense(300L, 1L, 150.00, "Laptop", "2026/07/01", Category.Supplies);
            insertApproval(400L, 300L, "pending");
            // Expenses #3
            insertExpense(500L, 1L, 30.00, "Backpack", "2026/06/30", Category.Supplies);
            insertApproval(600L, 500L, "pending");
            // Expenses #4
            insertExpense(700L, 1L, 15.00, "Pencils", "2026/07/01", Category.Supplies);
            insertApproval(800L, 700L, "approved", 3L, "Nice", "2026/07/03");
            // Expenses #5
            insertExpense(900L, 1L, 13.00, "Stapler", "2026/07/01", Category.Supplies);
            insertApproval(1000L, 900L, "denied", 3L, "Not Nice", "2026/07/03");
            Allure.step("Insert user #2");
            insertUser(3L, "Bob", "123", "Manager");

            // ACT
            Allure.step("Generate report for category Supplies");
            List<Expense> expenses = managerDAO.gen_report_cat(Category.Supplies);
            attachExpenses("Category Report", expenses);

            // ASSERT
            Allure.step("Verify expenses are returned in the expected order");
            assertIterableEquals(List.of(300L,100L,500L,900L,700L),
                                 expenses.stream()
                                         .map(Expense::getExpense_id)
                                         .toList());
        }

        /**
         * Verifies that generating a report with a null category is rejected
         * by throwing an appropriate exception.
         */
        @Test
        @Story("Generate Category Report")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that passing a null category throws
            a NullPointerException.
        """)
        @DisplayName("Generate Category Report With Null Category Throws Exception")
        void genReportCat_nullCategory_throwsException() {
            // NO ARRANGE
            // ACT & ASSERT
            Allure.step("Attempt to generate report using a null category");
            assertThrows(NullPointerException.class,
                         () -> managerDAO.gen_report_cat(null)
            );
        }
    
        /**
         * Verifies that generating a report for a valid category with no
         * matching expenses returns an empty list.
         */
        @Test
        @Story("Generate Category Report")
        @Severity(SeverityLevel.MINOR)
        @Description("""
            Verifies that an empty list is returned when no expenses
            exist for the requested category.
        """)
        @DisplayName("Generate Category Report With No Expenses Returns Empty List")
        void genReportCat_categoryHasNoExpenses_returnsEmptyList() throws Exception {
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            insertExpense(200L, 1L, 150.00, "Flight", "2026/07/01", Category.Travel);
            insertApproval(300L, 200L, "approved", 3L, "Nice", "2026/07/03");
            // ACT
            Allure.step("Generate report for category Meals");
            List<Expense> expenses = managerDAO.gen_report_cat(Category.Meals);
            attachExpenses("Category Report", expenses);

            // ASSERT
            Allure.step("Verify no expenses are returned");
            assertTrue(expenses.isEmpty(), "Expected no expenses for Meals category");
        }
    
    }

    /**
     * Test suite for validating the generate report by given date range functionality of the JDBCManagerDAO.
     */
    @Nested
    @DisplayName("Generate Report By Date Range Tests")
    @Feature("Date Reports")
    class GenReportDate {

        /**
         * Verifies that generating a report with valid start and end dates
         * returns all expenses whose dates fall within the specified range.
         */
        @Test
        @Story("Generate Date Report")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that generating a report with a valid start and end date
            returns all expenses within the specified date range.
        """)
        @DisplayName("Generate Date Report With Valid Date Range Returns Expenses")
        void genReportDate_validDateRange_returnsExpensesList() throws Exception {
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            // Expense inside range
            Allure.step("Insert expenses inside date range");
            insertExpense(100L, 1L, 200.00, "Dinner", "2026/07/03", Category.Meals);
            insertApproval(200L, 100L, "pending");
            // Expense outside range
            Allure.step("Insert expenses outside date range");
            insertExpense(300L, 1L, 20.00, "Lunch", "2026/07/27", Category.Meals);
            insertApproval(400L, 300L, "pending");
            // ACT
            Allure.step("Generate report from 2026/07/01 to 2026/07/31");
            List<Expense> expenses = managerDAO.gen_report_date("2026/07/01", "2026/07/06");
            attachExpenses("Date Report", expenses);
            // ASSERT
            Allure.step("Verify only expenses inside date range are returned");
            assertEquals(1, expenses.size());
            assertEquals(100L, expenses.get(0).getExpense_id());
        }
    
        /**
         * Verifies that generating a report for a valid date range returns
         * expenses in the correct order according to the query sorting rules:
         * date descending, status descending, and amount descending.
         */
        @Test
        @Story("Generate Date Report")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that expenses returned by a date report are ordered
            by expense date descending, approval status descending,
            and amount descending.
        """)
        @DisplayName("Generate Date Report Returns Expenses In Correct Order")
        void genReportDate_validDateRange_returnsOrderedExpensesList() throws Exception {
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            // Expenses #1
            insertExpense(100L, 1L, 150.00, "Laptop", "2026/07/25", Category.Supplies);
            insertApproval(200L, 100L, "pending");
            // Expenses #2
            insertExpense(300L, 1L, 20.00, "Notepad", "2026/07/25", Category.Supplies);
            insertApproval(400L, 300L, "pending");
            // Expenses #3
            insertExpense(500L, 1L, 30.00, "Backpack", "2026/07/25", Category.Supplies);
            insertApproval(600L, 500L, "approved", 3L, "Nice", "2026/07/25");
            // Expenses #4
            insertExpense(700L, 1L, 15.00, "Pencils", "2026/07/15", Category.Supplies);
            insertApproval(800L, 700L, "denied", 3L, "Not Nice", "2026/07/15");
            // Expenses #5
            insertExpense(900L, 1L, 13.00, "Stapler", "2026/06/17", Category.Supplies);
            insertApproval(1000L, 900L, "denied", 3L, "Not Nice", "2026/06/17");
            Allure.step("Insert user #2");
            insertUser(3L, "Bob", "456", "Manager");
            // ACT
            Allure.step("Generate report from 2026/07/01 to 2026/07/31");
            List<Expense> expenses = managerDAO.gen_report_date("2026/07/01", "2026/07/31");
            attachExpenses("Ordered Date Report", expenses);

            // ASSERT
            Allure.step("Verify expenses are returned in expected order");
            assertEquals(4, expenses.size());
            assertIterableEquals(List.of(100L,300L,500L,700L),
                                 expenses.stream()
                                         .map(Expense::getExpense_id)
                                         .toList()
            );
        }
        
        /**
         * Verifies that generating a report for a valid date range with no
         * matching expenses returns an empty list.
         */
        @Test
        @Story("Generate Date Report")
        @Severity(SeverityLevel.MINOR)
        @Description("""
            Verifies that an empty list is returned when no expenses exist
            within the requested date range.
        """)
        @DisplayName("Generate Date Report With No Matching Expenses Returns Empty List")
        void genReportDate_noExpensesInsideDateRange_returnsEmptyList() throws Exception {
            // ASSERT
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            insertExpense(100L, 1L, 150.00, "Laptop", "2026/07/25", Category.Supplies);
            insertApproval(200L, 100L, "pending");
            insertExpense(300L, 1L, 20.00, "Notepad", "2026/05/25", Category.Supplies);
            insertApproval(400L, 300L, "pending");
            // ACT
            Allure.step("Generate report from 2026/06/01 to 2026/06/30");
            List<Expense> expenses = managerDAO.gen_report_date("2026/06/01", "2026/06/30");
            attachExpenses("Empty Date Report", expenses);
            // ASSERT
            Allure.step("Verify no expenses are returned");
            assertTrue(expenses.isEmpty(), "No Expenses Inside Date Range Returns Empty List");
        }
    
        /**
         * Verifies that an expense occurring exactly on the start date of the
         * specified range is included in the generated report.
         */
        @Test
        @Story("Generate Date Report")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that an expense occurring exactly on the start date
            is included in the generated report.
        """)
        @DisplayName("Generate Date Report Includes Expense On Start Date")
        void genReportDate_expenseOnStartDate_returnsExpense() throws Exception {
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            // Expenses #1
            insertExpense(100L, 1L, 150.00, "Laptop", "2026/07/01", Category.Supplies);
            insertApproval(200L, 100L, "pending");
            // Expenses #2
            insertExpense(300L, 1L, 20.00, "Notepad", "2026/07/02", Category.Supplies);
            insertApproval(400L, 300L, "denied", 3L, "Nice", "2026/07/02");
            // Expenses #3
            insertExpense(500L, 1L, 30.00, "Backpack", "2026/07/05", Category.Supplies);
            insertApproval(600L, 500L, "approved", 3L, "Nice", "2026/07/05");
            Allure.step("Insert user #2");
            insertUser(3L, "Bob", "456", "Manager");

            // ACT
            Allure.step("Generate report starting on 2026/07/02");
            List<Expense> expenses = managerDAO.gen_report_date("2026/07/02", "2026/07/04");
            attachExpenses("Start Date Boundary Report", expenses);
            // ASSERT
            Allure.step("Verify start date expense is included");
            assertEquals(1, expenses.size());
            assertEquals(300L, expenses.get(0).getExpense_id());
        }
    
        /**
         * Verifies that an expense occurring exactly on the end date of the
         * specified range is included in the generated report.
         */
        @Test
        @Story("Generate Date Report")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that an expense occurring exactly on the end date
            is included in the generated report.
        """)
        @DisplayName("Generate Date Report Includes Expense On End Date")
        void genReportDate_expenseOnEndDate_returnsExpense() throws Exception {
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            // Expenses #1
            insertExpense(100L, 1L, 150.00, "Laptop", "2026/07/01", Category.Supplies);
            insertApproval(200L, 100L, "pending");
            // Expenses #2
            insertExpense(300L, 1L, 20.00, "Notepad", "2026/07/02", Category.Supplies);
            insertApproval(400L, 300L, "denied", 3L, "Nice", "2026/07/02");
            // Expenses #3
            insertExpense(500L, 1L, 30.00, "Backpack", "2026/07/05", Category.Supplies);
            insertApproval(600L, 500L, "approved", 3L, "Nice", "2026/07/05");
            Allure.step("Insert user #2");
            insertUser(3L, "Bob", "456", "Manager");
            
            // ACT
            Allure.step("Generate report ending on 2026/07/05");
            List<Expense> expenses = managerDAO.gen_report_date("2026/07/03", "2026/07/05");
            attachExpenses("End Date Boundary Report", expenses);
            // ASSERT
            Allure.step("Verify end date expense is included");
            assertEquals(1, expenses.size());
            assertEquals(500L, expenses.get(0).getExpense_id());
        }
    
        /**
         * Verifies that generating a report where the start and end dates are
         * the same returns only the expenses that occurred on that date.
         */
        @Test
        @Story("Generate Date Report")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that when the start date and end date are the same,
            only expenses from that specific day are returned.
        """)
        @DisplayName("Generate Date Report For Single Day Returns Matching Expenses")
        void genReportDate_expenseSameStartAndEndDate_returnsExpensesForSingleDay() throws Exception {
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            // Expenses #1
            insertExpense(100L, 1L, 150.00, "Laptop", "2026/07/01", Category.Supplies);
            insertApproval(200L, 100L, "pending");
            // Expenses #2
            insertExpense(300L, 1L, 20.00, "Notepad", "2026/07/02", Category.Supplies);
            insertApproval(400L, 300L, "denied", 3L, "Nice", "2026/07/02");
            // Expenses #3
            insertExpense(500L, 1L, 30.00, "Backpack", "2026/07/03", Category.Supplies);
            insertApproval(600L, 500L, "approved", 3L, "Nice", "2026/07/03");
            Allure.step("Insert user #2");
            insertUser(3L, "Bob", "456", "Manager");
            
            // ACT
            Allure.step("Generate report for 2026/07/02 only");
            List<Expense> expenses = managerDAO.gen_report_date("2026/07/02", "2026/07/02");
            attachExpenses("Single Day Report", expenses);
            // ASSERT
            Allure.step("Verify only single day expenses are returned");
            assertEquals(1, expenses.size());
            assertEquals(300L, expenses.get(0).getExpense_id());
        }
    }
    
    /**
     * Test suite for validating the find expense by given id functionality of the JDBCManagerDAO.
     */
    @Nested
    @DisplayName("Find Expense By Id Tests")
    @Feature("Expense Lookup")
    class FindExpenseById {

        /**
         * Verifies that finding an expense with a valid ID returns a correctly
         * mapped Expense object when the expense has a pending approval status.
         * Ensures fields specific to pending expenses are populated correctly.
         */
        @Test
        @Story("Find Expense By ID")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that searching for an existing pending expense ID
            returns the correctly mapped Expense object.
        """)
        @DisplayName("Find Pending Expense By ID Returns Mapped Expense")
        void findExpenseById_pendingExpense_returnsMappedExpense() throws Exception {
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            insertExpense(100L, 1L, 150.00, "Laptop", "2026/07/25", Category.Supplies);
            insertApproval(200L, 100L, "pending");
            // ACT
            Allure.step("Find expense by ID #100");
            Optional<Expense> result = managerDAO.findExpenseById(100L);
            // ASSERT
            Allure.step("Check if expense by ID #100 was found");
            assertTrue(result.isPresent(), "The Optional Contains An Expense");
            Expense expense = result.get();
            attachExpense("Pending Expense", expense);

            Allure.step("Verify returned expense is correctly mapped");
            assertAll("Mapped Expense:",
                    () -> assertEquals(100L, expense.getExpense_id()),
                    () -> assertEquals("John", expense.getEmp_name()),
                    () -> assertEquals(150.00, expense.getAmount()),
                    () -> assertEquals("Laptop", expense.getDescription()),
                    () -> assertEquals(Category.Supplies, expense.getCategory()),
                    () -> assertEquals(Status.pending, expense.getStatus())
            );
        }

        /**
         * Verifies that finding an expense with a valid ID returns a correctly
         * mapped Expense object when the expense has been reviewed.
         * Ensures reviewed expense fields such as status, reviewer, comment,
         * and review date are populated correctly.
         */
        @Test
        @Story("Find Expense By ID")
        @Severity(SeverityLevel.NORMAL)
        @Description("""
            Verifies that searching for an approved or denied expense ID
            returns the Expense object with review information mapped correctly.
        """)
        @DisplayName("Find Reviewed Expense By ID Returns Mapped Expense")
        void findExpenseById_reviewedExpense_returnsMappedExpense() throws Exception {
            // ARRANGE
            Allure.step("Insert user #1");
            insertUser(1L, "John", "123", "Employee");
            Allure.step("Insert user #2");
            insertUser(2L, "Bob", "123", "Manager");
            insertExpense(100L, 1L, 150.00, "Laptop", "2026/07/25", Category.Supplies);
            insertApproval(200L, 100L, "approved", 2L, "Nice", "2026/07/26");
            // ACT
            Allure.step("Find expense by ID #100");
            Optional<Expense> result = managerDAO.findExpenseById(100L);
            // ASSERT
            Allure.step("Check if expense by ID #100 was found");
            assertTrue(result.isPresent(), "The Optional Contains An Expense");
            Expense expense = result.get();
            attachExpense("Reviewed Expense", expense);

            Allure.step("Verify reviewed expense fields are mapped");
            assertAll("Mapped Expense:",
                    () -> assertEquals(Status.approved, expense.getStatus()),
                    () -> assertEquals("Bob", expense.getReviewer_name()),
                    () -> assertEquals("Nice", expense.getComment()),
                    () -> assertEquals("2026/07/26", expense.getReview_date())
            );
        }

        /**
         * Verifies that searching for an expense ID that does not exist in the
         * database returns an empty Optional rather than an Expense object.
         */
        @Test
        @Story("Find Expense By ID")
        @Severity(SeverityLevel.MINOR)
        @Description("""
            Verifies that searching for an expense ID that does not exist
            returns an empty Optional.
        """)
        @DisplayName("Find Non-Existent Expense By ID Returns Empty Optional")
        void findExpenseById_nonExistentExpenseId_returnsEmptyOptional() throws Exception {
            // NO ARRANGE
            // ACT
            Allure.step("Search for non-existent expense ID #999");
            Optional<Expense> expense = managerDAO.findExpenseById(999L);
            // ASSERT
            Allure.step("Verify no expense was found");
            assertTrue(expense.isEmpty(), "No Expense Found With Given Id");
        }
    }
    
    // ==========================
    // Test Data Helpers
    // ==========================

    private void insertUser(long id, String username, String password, String role) throws SQLException {
        Allure.parameter("id", id);
        Allure.parameter("username", username);
        Allure.parameter("role", role);

        String sql = """
            INSERT INTO users(id, username, password, role)
            VALUES (?, ?, ?, ?)
            """;

        try(PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, username);
            ps.setString(3, password);
            ps.setString(4, role);

            ps.executeUpdate();
        }
    }
    @Step("Insert expense #{id} with category {category}")
    private void insertExpense(long id, long userId, double amount, String description, String date, Category category) throws SQLException {
        String sql = """
            INSERT INTO expenses
            (id, user_id, amount, description, date, category)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try(PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setLong(2, userId);
            ps.setDouble(3, amount);
            ps.setString(4, description);
            ps.setString(5, date);
            ps.setString(6, category.toString());

            ps.executeUpdate();
        }
    }
    @Step("Insert approval with status {status} for expense #{expenseId}")
    private void insertApproval(long id, long expenseId, String status) throws SQLException {

        String sql = """
            INSERT INTO approvals
            (id, expense_id, status)
            VALUES (?, ?, ?)
            """;

        try(PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setLong(2, expenseId);
            ps.setString(3, status);

            ps.executeUpdate();
        }
    }
    @Step("Insert approval with status {status} and reviewer {reviewer}")
    private void insertApproval(long id, long expenseId, String status, Long reviewer, String comment, String reviewDate) throws SQLException {

        String sql = """
                INSERT INTO approvals
                (id, expense_id, status, reviewer, comment, review_date)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement p = conn.prepareStatement(sql)) {

            p.setLong(1, id);
            p.setLong(2, expenseId);
            p.setString(3, status);

            if (reviewer == null) {
                p.setNull(4, Types.INTEGER);
            } else {
                p.setLong(4, reviewer);
            }

            p.setString(5, comment);
            p.setString(6, reviewDate);

            p.executeUpdate();
        }
    }
    @Attachment(value = "{name}", type = "text/plain")
    private String attachExpenses(String name, List<Expense> expenses) {

        return expenses.stream()
                .map(Expense::toString)
                .collect(Collectors.joining(System.lineSeparator()));
    }
    @Attachment(value = "{name}", type = "text/plain")
    private String attachExpense(String name, Expense expense) {
        return expense.toString();
    }
}
