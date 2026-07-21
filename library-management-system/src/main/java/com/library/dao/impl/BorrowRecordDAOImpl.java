package com.library.dao.impl;

import com.library.dao.BorrowRecordDAO;
import com.library.dao.DataAccessException;
import com.library.model.BorrowRecord;
import com.library.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BorrowRecordDAOImpl implements BorrowRecordDAO {

    private static final String BASE_SELECT =
            "SELECT r.*, b.title AS book_title, s.full_name AS student_name " +
            "FROM borrow_records r " +
            "JOIN books b ON r.book_id = b.book_id " +
            "JOIN students s ON r.student_id = s.student_id ";

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public Optional<BorrowRecord> findById(int recordId) {
        String sql = BASE_SELECT + "WHERE r.record_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load borrow record " + recordId, e);
        }
    }

    @Override
    public List<BorrowRecord> findAll() {
        String sql = BASE_SELECT + "ORDER BY r.issue_date DESC";
        return runListQuery(sql);
    }

    @Override
    public List<BorrowRecord> findPage(int page, int pageSize) {
        String sql = BASE_SELECT + "ORDER BY r.issue_date DESC LIMIT ? OFFSET ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, Math.max(0, page) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load borrow record page", e);
        }
    }

    @Override
    public List<BorrowRecord> findActiveByStudent(int studentId) {
        String sql = BASE_SELECT + "WHERE r.student_id = ? AND r.status = 'ISSUED' ORDER BY r.due_date";
        return runListQueryWithInt(sql, studentId);
    }

    @Override
    public List<BorrowRecord> findHistoryByStudent(int studentId) {
        String sql = BASE_SELECT + "WHERE r.student_id = ? ORDER BY r.issue_date DESC";
        return runListQueryWithInt(sql, studentId);
    }

    @Override
    public List<BorrowRecord> findActive() {
        String sql = BASE_SELECT + "WHERE r.status = 'ISSUED' ORDER BY r.due_date";
        return runListQuery(sql);
    }

    @Override
    public List<BorrowRecord> findOverdue() {
        String sql = BASE_SELECT + "WHERE r.status = 'ISSUED' AND r.due_date < ? ORDER BY r.due_date";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load overdue records", e);
        }
    }

    @Override
    public List<BorrowRecord> search(String keyword) {
        String sql = BASE_SELECT +
                "WHERE s.full_name LIKE ? OR s.student_number LIKE ? OR b.title LIKE ? " +
                "ORDER BY r.issue_date DESC";
        String like = "%" + keyword + "%";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Borrow record search failed", e);
        }
    }

    @Override
    public int insert(BorrowRecord record) {
        String sql = "INSERT INTO borrow_records (book_id, student_id, issued_by, issue_date, due_date, " +
                "return_date, fine_amount, fine_paid, renewal_count, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, record.getBookId());
            ps.setInt(2, record.getStudentId());
            setNullableInt(ps, 3, record.getIssuedBy());
            ps.setString(4, record.getIssueDate().toString());
            ps.setString(5, record.getDueDate().toString());
            ps.setString(6, record.getReturnDate() != null ? record.getReturnDate().toString() : null);
            ps.setDouble(7, record.getFineAmount());
            ps.setInt(8, record.isFinePaid() ? 1 : 0);
            ps.setInt(9, record.getRenewalCount());
            ps.setString(10, record.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create borrow record", e);
        }
    }

    @Override
    public boolean update(BorrowRecord record) {
        // book_id / student_id are intentionally not editable after creation.
        String sql = "UPDATE borrow_records SET issued_by=?, due_date=?, return_date=?, fine_amount=?, " +
                "fine_paid=?, renewal_count=?, status=? WHERE record_id=?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            setNullableInt(ps, 1, record.getIssuedBy());
            ps.setString(2, record.getDueDate().toString());
            ps.setString(3, record.getReturnDate() != null ? record.getReturnDate().toString() : null);
            ps.setDouble(4, record.getFineAmount());
            ps.setInt(5, record.isFinePaid() ? 1 : 0);
            ps.setInt(6, record.getRenewalCount());
            ps.setString(7, record.getStatus().name());
            ps.setInt(8, record.getRecordId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update borrow record " + record.getRecordId(), e);
        }
    }

    @Override
    public int countIssued() {
        return countWhere("status = 'ISSUED'");
    }

    @Override
    public int countOverdue() {
        String sql = "SELECT COUNT(*) FROM borrow_records WHERE status = 'ISSUED' AND due_date < ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count overdue records", e);
        }
    }

    @Override
    public int countReturnedToday() {
        String sql = "SELECT COUNT(*) FROM borrow_records WHERE status = 'RETURNED' AND return_date = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count today's returns", e);
        }
    }

    @Override
    public int countTotal() {
        return countWhere(null);
    }

    @Override
    public int countActiveForStudentAndBook(int studentId, int bookId) {
        String sql = "SELECT COUNT(*) FROM borrow_records WHERE student_id=? AND book_id=? AND status='ISSUED'";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to check existing checkout", e);
        }
    }

    private int countWhere(String whereClause) {
        String sql = "SELECT COUNT(*) FROM borrow_records" + (whereClause != null ? " WHERE " + whereClause : "");
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count borrow records", e);
        }
    }

    private List<BorrowRecord> runListQuery(String sql) {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return mapAll(rs);
        } catch (SQLException e) {
            throw new DataAccessException("Borrow record query failed", e);
        }
    }

    private List<BorrowRecord> runListQueryWithInt(String sql, int value) {
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Borrow record query failed", e);
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private List<BorrowRecord> mapAll(ResultSet rs) throws SQLException {
        List<BorrowRecord> records = new ArrayList<>();
        while (rs.next()) records.add(mapRow(rs));
        return records;
    }

    private BorrowRecord mapRow(ResultSet rs) throws SQLException {
        BorrowRecord r = new BorrowRecord();
        r.setRecordId(rs.getInt("record_id"));
        r.setBookId(rs.getInt("book_id"));
        r.setBookTitle(rs.getString("book_title"));
        r.setStudentId(rs.getInt("student_id"));
        r.setStudentName(rs.getString("student_name"));
        int issuedBy = rs.getInt("issued_by");
        r.setIssuedBy(rs.wasNull() ? null : issuedBy);
        r.setIssueDate(parseDate(rs.getString("issue_date")));
        r.setDueDate(parseDate(rs.getString("due_date")));
        String returnDate = rs.getString("return_date");
        if (returnDate != null && !returnDate.isBlank()) {
            r.setReturnDate(parseDate(returnDate));
        }
        r.setFineAmount(rs.getDouble("fine_amount"));
        r.setFinePaid(rs.getInt("fine_paid") == 1);
        r.setRenewalCount(rs.getInt("renewal_count"));
        r.setStatus(BorrowRecord.Status.valueOf(rs.getString("status")));
        return r;
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return LocalDate.parse(raw.length() > 10 ? raw.substring(0, 10) : raw);
    }
}
