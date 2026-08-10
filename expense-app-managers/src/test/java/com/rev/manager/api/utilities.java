package com.rev.manager.api;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

import com.rev.manager.cucumber.utils.DatabaseTestHelper;
import com.rev.manager.repository.DatabaseConnection;

/**
 * Class to help setup and teardown test database for API tests.
 * utilities
 */
public class utilities {

    private static final String TEST_DATABASE =
        System.getenv().getOrDefault(
                "DATABASE_PATH",
                "testDatabase.db"
        );

    /**
     * Creates a connection to the test database.
     * @return A connection to the test database.
     * @throws SQLException
     */
    public static Connection getConnection() throws SQLException{
        DatabaseConnection database = new DatabaseConnection(TEST_DATABASE);
        return database.getConnection();
    }

    /**
     * Resets the database contents back to the default test dataset.
     *
     * Call this before every scenario.
     */
    public static void resetDatabase() {
        executeSqlFile("test-data.sql");
    }

    /**
     * Creates a fresh test database
     */
    public static void createTestDatabase() {
        executeSqlFile("test-schema.sql");
    }

    /**
     * Executes every SQL statement contained in a file located in
     * src/test/resources.
     *
     * @param fileName SQL file name.
     */
    public static void executeSqlFile(String fileName) {

        DatabaseConnection database = new DatabaseConnection(TEST_DATABASE);

        try (
                Connection conn = database.getConnection();
                Statement stmt = conn.createStatement()
        ) {

            InputStream input = DatabaseTestHelper.class
                    .getClassLoader()
                    .getResourceAsStream(fileName);

            if (input == null) {
                throw new RuntimeException(
                        "Unable to locate SQL file: " + fileName);
            }

            String sql = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            String[] statements = sql.split(";");

            for (String statement : statements) {
                statement = statement.trim();

                if (!statement.isEmpty()) {
                    stmt.execute(statement);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed executing SQL file: " + fileName, e);
        }
    }

    private static void printExpenseCount(Connection conn, String label) throws SQLException {
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM expenses")) {

        if (rs.next()) {
            System.out.println(label + ": " + rs.getInt(1));
        }
    }
}
}


