package com.rev.manager.cucumber.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

import com.rev.manager.repository.DatabaseConnection;

public final class DatabaseTestHelper {

    private static final String TEST_DATABASE =
        System.getenv().getOrDefault(
                "DATABASE_PATH",
                "testDatabase.db"
        );

    private DatabaseTestHelper() {
    }

    /**
     * Creates the database schema.
     *
     * Should only be called if testDatabase.db does not already exist.
     */
    public static void initializeDatabase() {
        executeSqlFile("test-schema.sql");
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
}
