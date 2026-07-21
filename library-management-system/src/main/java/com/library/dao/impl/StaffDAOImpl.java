package com.library.dao.impl;

import com.library.dao.DataAccessException;
import com.library.dao.StaffDAO;
import com.library.model.Staff;
import com.library.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StaffDAOImpl implements StaffDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public Optional<Staff> findById(int staffId) {
        String sql = "SELECT * FROM staff WHERE staff_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load staff " + staffId, e);
        }
    }

    @Override
    public Optional<Staff> findByUsername(String username) {
        String sql = "SELECT * FROM staff WHERE username = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Login lookup failed for " + username, e);
        }
    }

    @Override
    public List<Staff> findAll() {
        String sql = "SELECT * FROM staff ORDER BY full_name";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<Staff> staffList = new ArrayList<>();
            while (rs.next()) staffList.add(mapRow(rs));
            return staffList;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load staff list", e);
        }
    }

    @Override
    public int insert(Staff staff) {
        String sql = "INSERT INTO staff (username, password_hash, password_salt, role, full_name, " +
                "email, security_question, security_answer_hash, created_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, staff.getUsername());
            ps.setString(2, staff.getPasswordHash());
            ps.setString(3, staff.getPasswordSalt());
            ps.setString(4, staff.getRole().name());
            ps.setString(5, staff.getFullName());
            ps.setString(6, staff.getEmail());
            ps.setString(7, staff.getSecurityQuestion());
            ps.setString(8, staff.getSecurityAnswerHash());
            ps.setString(9, LocalDate.now().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create staff account: " + staff.getUsername(), e);
        }
    }

    @Override
    public boolean update(Staff staff) {
        String sql = "UPDATE staff SET username=?, role=?, full_name=?, email=?, " +
                "security_question=?, security_answer_hash=? WHERE staff_id=?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, staff.getUsername());
            ps.setString(2, staff.getRole().name());
            ps.setString(3, staff.getFullName());
            ps.setString(4, staff.getEmail());
            ps.setString(5, staff.getSecurityQuestion());
            ps.setString(6, staff.getSecurityAnswerHash());
            ps.setInt(7, staff.getStaffId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update staff " + staff.getStaffId(), e);
        }
    }

    @Override
    public boolean updatePassword(int staffId, String newHash, String newSalt) {
        String sql = "UPDATE staff SET password_hash=?, password_salt=? WHERE staff_id=?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, newSalt);
            ps.setInt(3, staffId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update password for staff " + staffId, e);
        }
    }

    @Override
    public boolean updateLastLogin(int staffId) {
        String sql = "UPDATE staff SET last_login=? WHERE staff_id=?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            ps.setInt(2, staffId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to record last login for staff " + staffId, e);
        }
    }

    @Override
    public boolean delete(int staffId) {
        String sql = "DELETE FROM staff WHERE staff_id=?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Cannot delete this staff account", e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM staff WHERE username = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Username lookup failed", e);
        }
    }

    private Staff mapRow(ResultSet rs) throws SQLException {
        Staff staff = new Staff();
        staff.setStaffId(rs.getInt("staff_id"));
        staff.setUsername(rs.getString("username"));
        staff.setPasswordHash(rs.getString("password_hash"));
        staff.setPasswordSalt(rs.getString("password_salt"));
        staff.setRole(Staff.Role.valueOf(rs.getString("role")));
        staff.setFullName(rs.getString("full_name"));
        staff.setEmail(rs.getString("email"));
        staff.setSecurityQuestion(rs.getString("security_question"));
        staff.setSecurityAnswerHash(rs.getString("security_answer_hash"));
        String created = rs.getString("created_date");
        if (created != null && !created.isBlank()) {
            staff.setCreatedDate(LocalDate.parse(created.length() > 10 ? created.substring(0, 10) : created));
        }
        String lastLogin = rs.getString("last_login");
        if (lastLogin != null && !lastLogin.isBlank()) {
            staff.setLastLogin(LocalDate.parse(lastLogin.length() > 10 ? lastLogin.substring(0, 10) : lastLogin));
        }
        return staff;
    }
}
