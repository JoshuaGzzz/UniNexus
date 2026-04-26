package org.marketplace.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnectionManager {

    private static DatabaseConnectionManager instance = null;
    private Connection connection = null;
    private static final String DB_URL = "jdbc:sqlite:src/main/resources/database/marketplace.db";

    private DatabaseConnectionManager() {
        try {
            this.connection = DriverManager.getConnection(DB_URL);
            System.out.println("✅ Database connection established.");
        } catch (SQLException e) {
            System.err.println("❌ Failed to connect to database: " + e.getMessage());
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