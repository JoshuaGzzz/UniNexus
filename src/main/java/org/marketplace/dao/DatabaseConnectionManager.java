package org.marketplace.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DatabaseConnectionManager {

    private static DatabaseConnectionManager instance = null;
    private Connection connection = null;
    private static final String DB_URL = "jdbc:sqlite:src/main/resources/database/marketplace.db";
    private static final String SQL_SCRIPT_PATH = "src/main/resources/database/marketplace.sql";

    private DatabaseConnectionManager() {
        try {
            this.connection = DriverManager.getConnection(DB_URL);
            System.out.println("✅ Database connection established.");
            initializeDatabase();
        } catch (SQLException e) {
            System.err.println("❌ Failed to connect to database: " + e.getMessage());
        }
    }

    private void initializeDatabase() {
        try {
            String sql = new String(Files.readAllBytes(Paths.get(SQL_SCRIPT_PATH)));

            String[] statements = sql.split(";");
            try (var stmt = this.connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
                for (String s : statements) {
                    String trimmed = s.strip();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        stmt.execute(trimmed);
                    }
                }
            }
            System.out.println("✅ Database schema initialized from marketplace.sql.");
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize schema: " + e.getMessage());
        }
    }

    public static synchronized DatabaseConnectionManager getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void closeConnection() {
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                this.connection.close();
                System.out.println("🔒 Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error closing connection: " + e.getMessage());
        }
    }
}