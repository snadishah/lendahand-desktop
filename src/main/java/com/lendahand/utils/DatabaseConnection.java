package com.lendahand.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database connection manager.
 *
 * ── HOW TO SWITCH BETWEEN SQLite and MySQL ────────────────────────
 *  Default                 →  uses local SQLite file (lendahand.db), zero setup
 *  LENDAHAND_DB=mysql      →  connects to MySQL
 *
 * MySQL credentials come from LENDAHAND_MYSQL_USER / LENDAHAND_MYSQL_PASSWORD.
 * ─────────────────────────────────────────────────────────────────
 */
public class DatabaseConnection {

    private static final boolean USE_MYSQL = "mysql".equalsIgnoreCase(System.getenv("LENDAHAND_DB"));

    // ── MySQL settings (only used when USE_MYSQL = true) ─────────
    private static final String MYSQL_HOST = "localhost";
    private static final String MYSQL_PORT = "3306";
    private static final String MYSQL_DB   = "lendahand";
    private static final String MYSQL_USER = envOr("LENDAHAND_MYSQL_USER", "root");
    private static final String MYSQL_PASS = envOr("LENDAHAND_MYSQL_PASSWORD", "");

    private static String envOr(String key, String fallback) {
        String value = System.getenv(key);
        return (value == null || value.isEmpty()) ? fallback : value;
    }

    // ── SQLite (fallback) ─────────────────────────────────────────
    private static final String SQLITE_URL = "jdbc:sqlite:lendahand.db";

    public static Connection getConnection() throws SQLException {
        if (USE_MYSQL) {
            String url = "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT
                       + "/" + MYSQL_DB
                       + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            return DriverManager.getConnection(url, MYSQL_USER, MYSQL_PASS);
        } else {
            return DriverManager.getConnection(SQLITE_URL);
        }
    }

    /**
     * Only needed for SQLite (creates tables on first run).
     * MySQL tables are created by running lendahand_mysql.sql in Workbench.
     */
    public static void initializeDatabase() {
        if (USE_MYSQL) {
            // Test the connection and log result
            try (Connection conn = getConnection()) {
                System.out.println("[DB] Connected to MySQL successfully. DB: " + MYSQL_DB);
            } catch (SQLException e) {
                System.err.println("[DB] MySQL connection FAILED: " + e.getMessage());
                System.err.println("[DB] Check LENDAHAND_MYSQL_USER / LENDAHAND_MYSQL_PASSWORD");
            }
            return;
        }

        // SQLite table creation
        String createCategories =
            "CREATE TABLE IF NOT EXISTS categories (" +
            "  id   INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  name TEXT NOT NULL UNIQUE," +
            "  icon TEXT NOT NULL DEFAULT '📋'" +
            ");";

        String createUsers =
            "CREATE TABLE IF NOT EXISTS users (" +
            "  id             INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  name           TEXT    NOT NULL," +
            "  email          TEXT    NOT NULL UNIQUE," +
            "  password       TEXT    NOT NULL," +
            "  wallet_balance REAL    NOT NULL DEFAULT 100.0," +
            "  user_type      TEXT    NOT NULL," +
            "  city           TEXT    DEFAULT NULL," +
            "  created_at     TEXT    DEFAULT CURRENT_TIMESTAMP" +
            ");";

        String createTasks =
            "CREATE TABLE IF NOT EXISTS tasks (" +
            "  id              INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  poster_id       INTEGER NOT NULL," +
            "  category_id     INTEGER NOT NULL DEFAULT 10," +
            "  title           TEXT    NOT NULL," +
            "  description     TEXT," +
            "  address         TEXT," +
            "  latitude        REAL    DEFAULT 0.0," +
            "  longitude       REAL    DEFAULT 0.0," +
            "  budget          REAL    NOT NULL," +
            "  status          TEXT    NOT NULL DEFAULT 'OPEN'," +
            "  helper_id       INTEGER DEFAULT NULL," +
            "  accepted_amount REAL    DEFAULT 0.0," +
            "  created_at      TEXT    DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (poster_id) REFERENCES users(id)" +
            ");";

        String createBids =
            "CREATE TABLE IF NOT EXISTS bids (" +
            "  id              INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  task_id         INTEGER NOT NULL," +
            "  helper_id       INTEGER NOT NULL," +
            "  proposed_amount REAL    NOT NULL," +
            "  status          TEXT    NOT NULL DEFAULT 'PENDING'," +
            "  created_at      TEXT    DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (task_id)   REFERENCES tasks(id)," +
            "  FOREIGN KEY (helper_id) REFERENCES users(id)" +
            ");";

        String createReviews =
            "CREATE TABLE IF NOT EXISTS reviews (" +
            "  id          INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  task_id     INTEGER NOT NULL," +
            "  reviewer_id INTEGER NOT NULL," +
            "  reviewee_id INTEGER NOT NULL," +
            "  rating      INTEGER NOT NULL," +
            "  comment     TEXT," +
            "  created_at  TEXT DEFAULT CURRENT_TIMESTAMP" +
            ");";

        String createNotifications =
            "CREATE TABLE IF NOT EXISTS notifications (" +
            "  id         INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  user_id    INTEGER NOT NULL," +
            "  message    TEXT    NOT NULL," +
            "  is_read    INTEGER NOT NULL DEFAULT 0," +
            "  created_at TEXT    DEFAULT CURRENT_TIMESTAMP" +
            ");";

        String seedCategories =
            "INSERT OR IGNORE INTO categories (name, icon) VALUES" +
            "('Heavy Lifting','💪'),('Tutoring','📚'),('Cleaning','🧹')," +
            "('Need a Ride','🚗'),('Computer Help','💻'),('Delivery / Errands','📦')," +
            "('Pet Care','🐾'),('Painting','🎨'),('Gardening','🌿'),('Other','📋');";

        try (Connection conn = getConnection();
             Statement  stmt = conn.createStatement()) {
            conn.setAutoCommit(false);
            stmt.execute(createCategories);
            stmt.execute(createUsers);
            stmt.execute(createTasks);
            stmt.execute(createBids);
            stmt.execute(createReviews);
            stmt.execute(createNotifications);
            stmt.execute(seedCategories);
            conn.commit();
            System.out.println("[DB] SQLite tables initialised successfully.");
        } catch (SQLException e) {
            System.err.println("[DB] Initialisation failed: " + e.getMessage());
        }
    }
}