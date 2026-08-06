package com.rev.manager.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

class DatabaseConnectionTest {

    @Test
    void initializesSchemaAndDefaultManagerUser() throws Exception {
        Path tempDb = Files.createTempFile("manager-test-db", ".db");
        Files.deleteIfExists(tempDb);

        try {
            DatabaseConnection databaseConnection = new DatabaseConnection(tempDb.toString());

            try (Connection conn = databaseConnection.getConnection()) {
                assertTrue(tableExists(conn, "users"));
                assertTrue(tableExists(conn, "expenses"));
                assertTrue(tableExists(conn, "approvals"));

                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT username, password, role FROM users WHERE username = ?")) {
                    stmt.setString(1, "Andrew");
                    try (ResultSet rs = stmt.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals("onetwothree", rs.getString("password"));
                        assertEquals("Manager", rs.getString("role"));
                    }
                }
            }
        } finally {
            Files.deleteIfExists(tempDb);
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }
}
