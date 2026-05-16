package com.studentmarketplace.database;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.util.Optional;

/**
 * DatabaseManager Singleton for managing SQLite database connections and operations.
 * Implements thread-safe singleton pattern with connection pooling.
 *
 * Usage: DatabaseManager.getInstance().getConnection()
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DATABASE_FILE = "uninexus.db";
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE;
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_CLIENT_USERNAME = "client";
    private static final String DEFAULT_PASSWORD = "password123";
    private static DatabaseManager instance;
    private Connection connection;

    /**
     * Private constructor to enforce singleton pattern
     */
    private DatabaseManager() {
        initializeDatabase();
    }

    /**
     * Thread-safe singleton getter using eager initialization
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Initialize database: create file if needed and run schema
     */
    private void initializeDatabase() {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Check if database file exists
            File dbFile = new File(DATABASE_FILE);
            boolean isNewDatabase = !dbFile.exists();

            // Create connection
            this.connection = DriverManager.getConnection(DATABASE_URL);
            this.connection.setAutoCommit(true);

            logger.info("Database connection established at: {}", DATABASE_URL);
            if (isNewDatabase) {
                logger.info("New database created. Running schema initialization...");
            } else {
                logger.info("Connected to existing database");
            }

            // Keep schema migrations idempotent so older DB files can still run newer app versions.
            initializeSchema();
            seedDefaultUsers();
        } catch (ClassNotFoundException e) {
            logger.error("SQLite JDBC driver not found", e);
            throw new RuntimeException("SQLite driver initialization failed", e);
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Initialize database schema from SQL file or embedded SQL
     */
    private void initializeSchema() {
        try {
            // Core schema used by current implemented flows (auth + rental posts).
            String[] sqlStatements = {
                "CREATE TABLE IF NOT EXISTS users (" +
                "    user_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    username TEXT UNIQUE NOT NULL," +
                "    email TEXT UNIQUE NOT NULL," +
                "    password_hash TEXT NOT NULL," +
                "    full_name TEXT NOT NULL," +
                "    student_id TEXT UNIQUE NOT NULL," +
                "    university TEXT NOT NULL," +
                "    phone TEXT," +
                "    profile_image_path TEXT," +
                "    bio TEXT," +
                "    rating REAL DEFAULT 5.0 CHECK (rating >= 0.0 AND rating <= 5.0)," +
                "    total_ratings INTEGER DEFAULT 0," +
                "    user_role TEXT DEFAULT 'CLIENT' CHECK (user_role IN ('CLIENT', 'ADMIN'))," +
                "    account_status TEXT DEFAULT 'ACTIVE' CHECK (account_status IN ('ACTIVE', 'SUSPENDED', 'BANNED'))," +
                "    created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ");",
                "CREATE TABLE IF NOT EXISTS posts (" +
                "    post_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    seller_id INTEGER NOT NULL," +
                "    post_type TEXT NOT NULL CHECK (post_type IN ('SALE', 'RENTAL', 'RESOURCE'))," +
                "    title TEXT NOT NULL," +
                "    description TEXT," +
                "    image_path TEXT," +
                "    flagged INTEGER DEFAULT 0," +
                "    status TEXT DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SOLD', 'RENTED', 'ARCHIVED', 'DELETED'))," +
                "    created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "    FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                ");",
                "CREATE TABLE IF NOT EXISTS rentals (" +
                "    rental_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    post_id INTEGER UNIQUE NOT NULL," +
                "    rental_type TEXT NOT NULL CHECK (rental_type IN ('DORMITORY', 'APARTMENT', 'ROOM', 'HOUSE'))," +
                "    location TEXT NOT NULL," +
                "    latitude REAL," +
                "    longitude REAL," +
                "    price_per_month REAL NOT NULL CHECK (price_per_month > 0)," +
                "    bedrooms INTEGER DEFAULT 1," +
                "    bathrooms INTEGER DEFAULT 1," +
                "    area_sqm REAL," +
                "    furnished BOOLEAN DEFAULT 0," +
                "    amenities TEXT," +
                "    available_from DATE," +
                "    main_image_path TEXT," +
                "    thumbnail_image_path TEXT," +
                "    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE" +
                ");",
                "CREATE TABLE IF NOT EXISTS products (" +
                "    product_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    post_id INTEGER UNIQUE NOT NULL," +
                "    category TEXT NOT NULL CHECK (category IN ('ELECTRONICS', 'TEXTBOOKS', 'FURNITURE', 'CLOTHING', 'ACCESSORIES', 'OTHER'))," +
                "    price REAL NOT NULL CHECK (price > 0)," +
                "    condition TEXT DEFAULT 'GOOD' CHECK (condition IN ('LIKE_NEW', 'GOOD', 'FAIR', 'POOR'))," +
                "    quantity INTEGER DEFAULT 1 CHECK (quantity > 0)," +
                "    location TEXT NOT NULL," +
                "    main_image_path TEXT," +
                "    thumbnail_image_path TEXT," +
                "    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE" +
                ");",
                "CREATE TABLE IF NOT EXISTS academic_resources (" +
                "    resource_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    post_id INTEGER UNIQUE NOT NULL," +
                "    resource_type TEXT NOT NULL CHECK (resource_type IN ('SOFTWARE', 'PDF', 'RESEARCH_PAPER', 'NOTES', 'OTHER'))," +
                "    file_path TEXT NOT NULL," +
                "    file_size_mb REAL," +
                "    price REAL DEFAULT 0.0 CHECK (price >= 0)," +
                "    download_count INTEGER DEFAULT 0," +
                "    subject_area TEXT," +
                "    course_code TEXT," +
                "    university TEXT," +
                "    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE" +
                ");",
                "CREATE TABLE IF NOT EXISTS transactions (" +
                "    transaction_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    buyer_id INTEGER NOT NULL," +
                "    seller_id INTEGER NOT NULL," +
                "    post_id INTEGER NOT NULL," +
                "    amount REAL NOT NULL," +
                "    transaction_type TEXT NOT NULL CHECK (transaction_type IN ('SALE', 'RENTAL_PAYMENT', 'RESOURCE_PURCHASE'))," +
                "    status TEXT DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED'))," +
                "    payment_method VARCHAR(40) DEFAULT 'CASH' CHECK (payment_method IN ('CASH', 'ONLINE_TRANSFER', 'CREDIT_CARD'))," +
                "    transaction_date DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "    completion_date DATETIME," +
                "    FOREIGN KEY (buyer_id) REFERENCES users(user_id) ON DELETE CASCADE," +
                "    FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE," +
                "    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE" +
                ");",
                "CREATE TABLE IF NOT EXISTS reports (" +
                "    report_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    reporter_id INTEGER NOT NULL," +
                "    post_id INTEGER NOT NULL," +
                "    reason TEXT," +
                "    status TEXT DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED'))," +
                "    created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "    resolved_at DATETIME," +
                "    FOREIGN KEY (reporter_id) REFERENCES users(user_id) ON DELETE CASCADE," +
                "    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE" +
                ");",
                "CREATE TABLE IF NOT EXISTS refund_requests (" +
                "    refund_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    buyer_id INTEGER NOT NULL," +
                "    transaction_id INTEGER NOT NULL," +
                "    reason TEXT NOT NULL," +
                "    status TEXT DEFAULT 'PENDING'," +
                "    created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "    FOREIGN KEY (buyer_id) REFERENCES users(user_id)," +
                "    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)" +
                ");",
                "CREATE INDEX IF NOT EXISTS idx_posts_seller_id ON posts(seller_id);",
                "CREATE INDEX IF NOT EXISTS idx_posts_status ON posts(status);",
                "CREATE INDEX IF NOT EXISTS idx_rentals_location ON rentals(location);",
                "CREATE INDEX IF NOT EXISTS idx_transactions_buyer_id ON transactions(buyer_id);",
                "CREATE INDEX IF NOT EXISTS idx_reports_post_id ON reports(post_id);"
            };

            String[] migrationStatements = {
                "ALTER TABLE users ADD COLUMN user_role TEXT DEFAULT 'CLIENT'",
                "ALTER TABLE posts ADD COLUMN flagged INTEGER DEFAULT 0",
                "ALTER TABLE posts ADD COLUMN image_path TEXT",
                "ALTER TABLE transactions ADD COLUMN payment_method VARCHAR(40) DEFAULT 'CASH'",
                "CREATE INDEX IF NOT EXISTS idx_posts_flagged ON posts(flagged)"
            };

            try (Statement stmt = connection.createStatement()) {
                for (String sql : sqlStatements) {
                    if (!sql.trim().isEmpty()) {
                        stmt.execute(sql);
                    }
                }

                // Ignore "duplicate column" errors for older databases that already have the column.
                for (String sql : migrationStatements) {
                    try {
                        stmt.execute(sql);
                    } catch (SQLException ignored) {
                        // no-op
                    }
                }
            }
            logger.info("Database schema initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException("Schema initialization failed", e);
        }
    }

    /**
     * Get a database connection with automatic resource management
     * Usage in try-with-resources:
     *   try (Connection conn = DatabaseManager.getInstance().getConnection()) {
     *       // use connection
     *   }
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                this.connection = DriverManager.getConnection(DATABASE_URL);
                this.connection.setAutoCommit(true);
                logger.info("New database connection established");
            } catch (SQLException e) {
                logger.error("Failed to establish database connection", e);
                throw e;
            }
        }
        return connection;
    }

    /**
     * Execute a query and return results
     * Usage: Optional<ResultSet> results = executeQuery("SELECT * FROM users WHERE user_id = ?", userId);
     */
    public Optional<ResultSet> executeQuery(String sql, Object... params) {
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            setParameters(stmt, params);
            return Optional.of(stmt.executeQuery());
        } catch (SQLException e) {
            logger.error("Query execution failed: {}", sql, e);
            return Optional.empty();
        }
    }

    /**
     * Execute an insert/update/delete operation
     * Returns number of affected rows
     */
    public int executeUpdate(String sql, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, params);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Update execution failed: {}", sql, e);
            return 0;
        }
    }

    /**
     * Execute an insert operation and return generated keys
     */
    public Optional<Long> executeInsert(String sql, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, params);
            stmt.executeUpdate();

            // SQLite JDBC in this environment does not support getGeneratedKeys().
            try (PreparedStatement keyStmt = connection.prepareStatement("SELECT last_insert_rowid()");
                 ResultSet rs = keyStmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            logger.error("Insert execution failed: {}", sql, e);
        }
        return Optional.empty();
    }

    /**
     * Seed default demo users for immediate login testing.
     */
    private void seedDefaultUsers() {
        String checkSql = "SELECT user_id, password_hash, user_role FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users (username, email, password_hash, full_name, student_id, university, user_role) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?)";

        seedUserIfMissing(checkSql, insertSql,
                DEFAULT_ADMIN_USERNAME,
                "admin@studentmarketplace.local",
                hashPasswordBCrypt(DEFAULT_PASSWORD),
                "System Administrator",
                "ADMIN-0001",
                "UniNexus",
                "ADMIN");

        seedUserIfMissing(checkSql, insertSql,
                DEFAULT_CLIENT_USERNAME,
                "client@studentmarketplace.local",
                hashPasswordBCrypt(DEFAULT_PASSWORD),
                "Demo Client",
                "CLIENT-0001",
                "UniNexus",
                "CLIENT");
    }

    private void seedUserIfMissing(String checkSql, String insertSql,
                                   String username, String email, String passwordHash,
                                   String fullName, String studentId, String university, String role) {
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("user_id");
                    String existingHash = rs.getString("password_hash");
                    String existingRole = rs.getString("user_role");

                    if (!isBCryptHash(existingHash)) {
                        executeUpdate("UPDATE users SET password_hash = ? WHERE user_id = ?",
                                hashPasswordBCrypt(DEFAULT_PASSWORD), userId);
                    }

                    if (existingRole == null || !existingRole.equalsIgnoreCase(role)) {
                        executeUpdate("UPDATE users SET user_role = ? WHERE user_id = ?", role, userId);
                    }

                    return;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed checking default user: {}", username, e);
            return;
        }

        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            insertStmt.setString(1, username);
            insertStmt.setString(2, email);
            insertStmt.setString(3, passwordHash);
            insertStmt.setString(4, fullName);
            insertStmt.setString(5, studentId);
            insertStmt.setString(6, university);
            insertStmt.setString(7, role);
            insertStmt.executeUpdate();
            logger.info("Seeded default {} account", role.toLowerCase());
        } catch (SQLException e) {
            logger.error("Failed inserting default user: {}", username, e);
        }
    }

    private String hashPasswordBCrypt(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    private boolean isBCryptHash(String hash) {
        if (hash == null) {
            return false;
        }
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
    }

    /**
     * Begin a transaction (set autocommit to false)
     */
    public void beginTransaction() throws SQLException {
        connection.setAutoCommit(false);
        logger.debug("Transaction started");
    }

    /**
     * Commit current transaction
     */
    public void commit() throws SQLException {
        try {
            connection.commit();
            connection.setAutoCommit(true);
            logger.debug("Transaction committed");
        } catch (SQLException e) {
            logger.error("Commit failed, rolling back", e);
            rollback();
            throw e;
        }
    }

    /**
     * Rollback current transaction
     */
    public void rollback() throws SQLException {
        try {
            connection.rollback();
            connection.setAutoCommit(true);
            logger.debug("Transaction rolled back");
        } catch (SQLException e) {
            logger.error("Rollback failed", e);
        }
    }

    /**
     * Set parameters in a PreparedStatement
     */
    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                stmt.setNull(i + 1, Types.NULL);
            } else if (params[i] instanceof String) {
                stmt.setString(i + 1, (String) params[i]);
            } else if (params[i] instanceof Integer) {
                stmt.setInt(i + 1, (Integer) params[i]);
            } else if (params[i] instanceof Long) {
                stmt.setLong(i + 1, (Long) params[i]);
            } else if (params[i] instanceof Double) {
                stmt.setDouble(i + 1, (Double) params[i]);
            } else if (params[i] instanceof Boolean) {
                stmt.setBoolean(i + 1, (Boolean) params[i]);
            } else if (params[i] instanceof java.time.LocalDateTime) {
                stmt.setString(i + 1, params[i].toString());
            } else {
                stmt.setObject(i + 1, params[i]);
            }
        }
    }

    /**
     * Close database connection (call on application shutdown)
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Database connection closed");
            } catch (SQLException e) {
                logger.error("Error closing database connection", e);
            }
        }
    }

    /**
     * Get database version
     */
    public String getDatabaseVersion() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT sqlite_version()")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to get database version", e);
        }
        return "Unknown";
    }
}
