package com.rev.manager.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database connection utility for SQLite database.
 * Handles connection management for the shared expense manager database.
 */
public class DatabaseConnection {
    private final String databasePath;
    private final boolean allowCreateIfMissing;

    public DatabaseConnection() {
        this(null, false);
    }

    /**
     * Sets the database to the passed in database. For testing use only.
     * @param databasePath The name of the database in the test resources folder.
     */
    public DatabaseConnection(String databasePath) {
        this(databasePath, true);
    }

    private DatabaseConnection(String requestedPath, boolean allowCreateIfMissing) {
        this.allowCreateIfMissing = allowCreateIfMissing;
        this.databasePath = resolvePath(requestedPath);
        System.setProperty("databasePath", this.databasePath);
    }

    /**
     * Get a database connection.
     * @return SQLite database connection
     * @throws SQLException if connection fails
     */
    public Connection getConnection() throws SQLException {
        String resolvedPath = resolvePath(databasePath);
        Path resolvedFile = Paths.get(resolvedPath).toAbsolutePath().normalize();
        Path parent = resolvedFile.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new SQLException("Unable to create database directory", e);
            }
        }

        if (!Files.exists(resolvedFile)) {
            if (!allowCreateIfMissing) {
                throw new SQLException("Database file not found at expected path: " + resolvedFile
                    + "\nPlease ensure the existing project database is present at the project root.");
            }
        }

        String url = "jdbc:sqlite:" + resolvedFile;
        Connection connection = DriverManager.getConnection(url);
        initializeDatabase(connection);
        return connection;
    }

    private void initializeDatabase(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
            statement.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY,
                    username TEXT UNIQUE,
                    password TEXT,
                    role TEXT
                );
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS expenses (
                    id INTEGER PRIMARY KEY,
                    user_id INTEGER,
                    amount REAL,
                    description TEXT,
                    date TEXT,
                    category TEXT,
                    FOREIGN KEY(user_id) REFERENCES users(id)
                );
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS approvals (
                    id INTEGER PRIMARY KEY,
                    expense_id INTEGER,
                    status TEXT,
                    reviewer INTEGER,
                    comment TEXT,
                    review_date TEXT,
                    FOREIGN KEY(expense_id) REFERENCES expenses(id)
                );
                """);
            statement.execute("""
                INSERT OR IGNORE INTO users (id, username, password, role)
                VALUES (1, 'Andrew', 'onetwothree', 'Manager');
                """);
        }
    }

    private String resolvePath(String requestedPath) {
        String configuredPath = System.getenv("DATABASE_PATH");
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = System.getProperty("databasePath");
        }
        if (requestedPath == null || requestedPath.isBlank()) {
            if (configuredPath != null && !configuredPath.isBlank()) {
                return Paths.get(configuredPath).normalize().toString();
            }

            Path projectRootCandidate = findExistingDatabaseInAncestors(Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize());
            if (projectRootCandidate != null) {
                return projectRootCandidate.toString();
            }

            return Paths.get(System.getProperty("user.dir"))
                .resolve("..").resolve("revExpenseData.db")
                .normalize()
                .toString();
        }

        Path requested = Paths.get(requestedPath);
        if (requested.isAbsolute()) {
            return requested.normalize().toString();
        }

        Path fromWorkingDir = Paths.get(System.getProperty("user.dir")).resolve(requested).normalize();
        if (Files.exists(fromWorkingDir)) {
            return fromWorkingDir.toString();
        }

        Path fromTestResources = Paths.get(System.getProperty("user.dir"))
            .resolve("src")
            .resolve("test")
            .resolve("resources")
            .resolve(requested)
            .normalize();
        if (Files.exists(fromTestResources)) {
            return fromTestResources.toString();
        }

        return fromWorkingDir.toString();
    }

    private Path findExistingDatabaseInAncestors(Path startPath) {
        Path current = startPath;
        while (current != null) {
            Path candidate = current.resolve("revExpenseData.db");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }
}