package com.library.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * First-run only. If the staff table is empty (fresh database), creates
 * the default login accounts, default Settings rows, and a small demo
 * catalog so the app is usable immediately instead of opening on an
 * empty shell. Runs once — every statement after the emptiness check
 * only happens the very first time the app is launched.
 */
final class SeedDataLoader {

    private static final Logger LOGGER = Logger.getLogger(SeedDataLoader.class.getName());

    private SeedDataLoader() {
    }

    static void seedIfEmpty(DatabaseConnection db) throws SQLException {
        try (Connection conn = db.getConnection()) {
            if (!isEmpty(conn, "staff")) {
                return; // already seeded on a previous run
            }
            LOGGER.info("Empty database detected — seeding default accounts and demo data");

            seedSettings(conn);
            seedStaff(conn);
            Map<String, Integer> categoryIds = seedCategories(conn);
            Map<String, Integer> authorIds = seedAuthors(conn);
            seedBooks(conn, categoryIds, authorIds);
            seedStudents(conn);
        }
    }

    private static boolean isEmpty(Connection conn, String table) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    private static void seedSettings(Connection conn) throws SQLException {
        String sql = "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            insertSetting(ps, "fine_per_day", "10");
            insertSetting(ps, "loan_period_days", "14");
            insertSetting(ps, "max_renewals", "2");
            insertSetting(ps, "theme", "light");
            insertSetting(ps, "currency_symbol", "Rs.");
            insertSetting(ps, "library_name", "COMSATS Library");
        }
    }

    private static void insertSetting(PreparedStatement ps, String key, String value) throws SQLException {
        ps.setString(1, key);
        ps.setString(2, value);
        ps.executeUpdate();
    }

    private static void seedStaff(Connection conn) throws SQLException {
        String sql = "INSERT INTO staff (username, password_hash, password_salt, role, full_name, "
                + "email, security_question, security_answer_hash, created_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            insertStaffAccount(ps, "admin", "admin123", "ADMIN", "System Administrator",
                    "admin@library.local", "What city were you born in?", "islamabad");
            insertStaffAccount(ps, "librarian", "librarian123", "LIBRARIAN", "Head Librarian",
                    "librarian@library.local", "What city were you born in?", "islamabad");
        }
    }

    private static void insertStaffAccount(PreparedStatement ps, String username, String plainPassword,
            String role, String fullName, String email, String securityQuestion, String securityAnswer)
            throws SQLException {
        String[] passwordHashAndSalt = PasswordUtil.hashNew(plainPassword);
        // Security answers are normalized (trimmed + lowercased) before hashing so the
        // reset flow can do the same normalization when checking what the user types back.
        String[] answerHashAndSalt = PasswordUtil.hashNew(securityAnswer.trim().toLowerCase());

        ps.setString(1, username);
        ps.setString(2, passwordHashAndSalt[0]);
        ps.setString(3, passwordHashAndSalt[1]);
        ps.setString(4, role);
        ps.setString(5, fullName);
        ps.setString(6, email);
        ps.setString(7, securityQuestion);
        // Store "answerHash:answerSalt" together since staff table has one column for this;
        // AuthService splits on ':' when verifying. Keeps schema flat (no extra salt column).
        ps.setString(8, answerHashAndSalt[0] + ":" + answerHashAndSalt[1]);
        ps.setString(9, LocalDate.now().toString());
        ps.executeUpdate();
    }

    private static Map<String, Integer> seedCategories(Connection conn) throws SQLException {
        String[][] categories = {
            {"Computer Science", "Programming, software engineering and computing theory"},
            {"Fiction", "Novels and short stories"},
            {"Mathematics", "Pure and applied mathematics"},
            {"Physics", "Classical and modern physics"},
            {"History", "World and regional history"}
        };
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] c : categories) {
                ps.setString(1, c[0]);
                ps.setString(2, c[1]);
                ps.executeUpdate();
            }
        }
        return fetchIdMap(conn, "categories", "category_id", "name");
    }

    private static Map<String, Integer> seedAuthors(Connection conn) throws SQLException {
        String[][] authors = {
            {"Robert C. Martin", "Software engineer and author of Clean Code and Clean Architecture."},
            {"George Orwell", "English novelist and essayist, author of 1984 and Animal Farm."},
            {"Jane Austen", "English novelist known for Pride and Prejudice."},
            {"Stephen Hawking", "Theoretical physicist and author of A Brief History of Time."},
            {"Yuval Noah Harari", "Historian and author of Sapiens."},
            {"James Stewart", "Mathematician known for widely used calculus textbooks."}
        };
        String sql = "INSERT INTO authors (name, bio) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] a : authors) {
                ps.setString(1, a[0]);
                ps.setString(2, a[1]);
                ps.executeUpdate();
            }
        }
        return fetchIdMap(conn, "authors", "author_id", "name");
    }

    private static void seedBooks(Connection conn, Map<String, Integer> categoryIds,
            Map<String, Integer> authorIds) throws SQLException {
        // title, author, category, isbn, publisher, year, edition, shelf, totalCopies
        String[][] books = {
            {"Clean Code", "Robert C. Martin", "Computer Science", "9780132350884",
                "Prentice Hall", "2008", "1st", "CS-101", "4"},
            {"Clean Architecture", "Robert C. Martin", "Computer Science", "9780134494166",
                "Prentice Hall", "2017", "1st", "CS-102", "3"},
            {"1984", "George Orwell", "Fiction", "9780451524935",
                "Signet Classics", "1949", "1st", "FIC-201", "5"},
            {"Pride and Prejudice", "Jane Austen", "Fiction", "9780141439518",
                "Penguin Classics", "1813", "1st", "FIC-202", "3"},
            {"A Brief History of Time", "Stephen Hawking", "Physics", "9780553380163",
                "Bantam", "1988", "10th Anniv.", "PHY-301", "2"},
            {"Sapiens: A Brief History of Humankind", "Yuval Noah Harari", "History", "9780062316097",
                "Harper", "2015", "1st", "HIS-401", "4"},
            {"Calculus: Early Transcendentals", "James Stewart", "Mathematics", "9781285741550",
                "Cengage", "2015", "8th", "MATH-501", "6"}
        };
        String sql = "INSERT INTO books (isbn, title, author_id, category_id, publisher, "
                + "publication_year, edition, shelf_location, total_copies, available_copies, added_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] b : books) {
                int copies = Integer.parseInt(b[8]);
                ps.setString(1, b[3]);
                ps.setString(2, b[0]);
                ps.setInt(3, authorIds.get(b[1]));
                ps.setInt(4, categoryIds.get(b[2]));
                ps.setString(5, b[4]);
                ps.setInt(6, Integer.parseInt(b[5]));
                ps.setString(7, b[6]);
                ps.setString(8, b[7]);
                ps.setInt(9, copies);
                ps.setInt(10, copies);
                ps.setString(11, LocalDate.now().toString());
                ps.executeUpdate();
            }
        }
    }

    private static void seedStudents(Connection conn) throws SQLException {
        String[][] students = {
            {"FA25-BSE-001", "Ahmed Raza", "Software Engineering", "3", "03001234567", "ahmed.raza@student.edu"},
            {"FA25-BSE-002", "Sara Khan", "Software Engineering", "3", "03011234567", "sara.khan@student.edu"},
            {"FA24-CS-014", "Bilal Hussain", "Computer Science", "5", "03021234567", "bilal.hussain@student.edu"},
            {"FA24-EE-022", "Ayesha Malik", "Electrical Engineering", "5", "03031234567", "ayesha.malik@student.edu"}
        };
        String sql = "INSERT INTO students (student_number, full_name, department, semester, phone, "
                + "email, status, registered_date) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] s : students) {
                ps.setString(1, s[0]);
                ps.setString(2, s[1]);
                ps.setString(3, s[2]);
                ps.setInt(4, Integer.parseInt(s[3]));
                ps.setString(5, s[4]);
                ps.setString(6, s[5]);
                ps.setString(7, LocalDate.now().toString());
                ps.executeUpdate();
            }
        }
    }

    private static Map<String, Integer> fetchIdMap(Connection conn, String table, String idCol, String nameCol)
            throws SQLException {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT " + idCol + ", " + nameCol + " FROM " + table;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString(nameCol), rs.getInt(idCol));
            }
        }
        return map;
    }
}
