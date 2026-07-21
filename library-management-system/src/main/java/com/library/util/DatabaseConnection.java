package com.library.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton connection factory. Vends a fresh {@link Connection} on every
 * call instead of sharing one long-lived connection, so background Tasks
 * and the JavaFX Application Thread never contend for the same object —
 * SQLite/JDBC connections are cheap enough that this costs nothing.
 *
 * Reads db.properties from the classpath. Switching db.type=mysql (and
 * filling in the db.mysql.* keys) is the entire migration: every DAO is
 * written against plain java.sql and has no idea which database it's on.
 */
public final class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static final DatabaseConnection INSTANCE = new DatabaseConnection();

    private final String dbType;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String sqliteFilePath;

    private DatabaseConnection() {
        Properties props = loadProperties();
        this.dbType = props.getProperty("db.type", "sqlite").trim().toLowerCase();

        if ("mysql".equals(dbType)) {
            String host = props.getProperty("db.mysql.host", "localhost");
            String port = props.getProperty("db.mysql.port", "3306");
            String database = props.getProperty("db.mysql.database", "library_db");
            this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            this.username = props.getProperty("db.mysql.user", "root");
            this.password = props.getProperty("db.mysql.password", "");
            this.sqliteFilePath = null;
        } else {
            String file = props.getProperty("db.sqlite.file", "library.db");
            this.jdbcUrl = "jdbc:sqlite:" + file;
            this.username = null;
            this.password = null;
            this.sqliteFilePath = file;
        }
    }

    public static DatabaseConnection getInstance() {
        return INSTANCE;
    }

    /** Opens a new connection. Callers must close it — always use try-with-resources. */
    public Connection getConnection() throws SQLException {
        Connection conn = (username != null)
                ? DriverManager.getConnection(jdbcUrl, username, password)
                : DriverManager.getConnection(jdbcUrl);

        if (isSqlite()) {
            try (Statement pragma = conn.createStatement()) {
                pragma.execute("PRAGMA foreign_keys = ON");
            }
        }
        return conn;
    }

    public boolean isSqlite() {
        return "sqlite".equals(dbType);
    }

    /** The SQLite database file path, or null when running against MySQL. Used by Settings' Backup/Restore. */
    public String getSqliteFilePath() {
        return sqliteFilePath;
    }

    public String getDbType() {
        return dbType;
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                LOGGER.log(Level.WARNING, "db.properties not found on classpath, using SQLite defaults");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not read db.properties, using defaults", e);
        }
        return props;
    }
}
