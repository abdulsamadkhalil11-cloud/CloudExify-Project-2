package com.library.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Creates every table/index (idempotent — CREATE ... IF NOT EXISTS) and,
 * on a genuinely empty database, seeds default accounts and demo data.
 * Called once from Main during splash-screen load.
 */
public final class SchemaInitializer {

    private static final Logger LOGGER = Logger.getLogger(SchemaInitializer.class.getName());

    private SchemaInitializer() {
    }

    public static void initialize(DatabaseConnection db) throws SQLException {
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            for (String ddl : SchemaScript.STATEMENTS) {
                stmt.executeUpdate(ddl);
            }
        }
        LOGGER.info("Schema ready");
        SeedDataLoader.seedIfEmpty(db);
    }
}
