package com.library.dao.impl;

import com.library.dao.DataAccessException;
import com.library.dao.StudentDAO;
import com.library.model.Student;
import com.library.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StudentDAOImpl implements StudentDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public Optional<Student> findById(int studentId) {
        String sql = "SELECT * FROM students WHERE student_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load student " + studentId, e);
        }
    }

    @Override
    public List<Student> findAll() {
        String sql = "SELECT * FROM students ORDER BY full_name";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return mapAll(rs);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load students", e);
        }
    }

    @Override
    public List<Student> findPage(int page, int pageSize) {
        String sql = "SELECT * FROM students ORDER BY full_name LIMIT ? OFFSET ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, Math.max(0, page) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load student page", e);
        }
    }

    @Override
    public List<Student> search(String keyword) {
        String sql = "SELECT * FROM students WHERE full_name LIKE ? OR student_number LIKE ? " +
                "OR email LIKE ? ORDER BY full_name";
        String like = "%" + keyword + "%";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Student search failed", e);
        }
    }

    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM students";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count students", e);
        }
    }

    @Override
    public int insert(Student student) {
        String sql = "INSERT INTO students (student_number, full_name, department, semester, phone, " +
                "email, photo_path, status, registered_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, student.getStudentNumber());
            ps.setString(2, student.getFullName());
            ps.setString(3, student.getDepartment());
            ps.setInt(4, student.getSemester());
            ps.setString(5, student.getPhone());
            ps.setString(6, student.getEmail());
            ps.setString(7, student.getPhotoPath());
            ps.setString(8, student.getStatus().name());
            LocalDate reg = student.getRegisteredDate() != null ? student.getRegisteredDate() : LocalDate.now();
            ps.setString(9, reg.toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to register student: " + student.getFullName(), e);
        }
    }

    @Override
    public boolean update(Student student) {
        String sql = "UPDATE students SET student_number=?, full_name=?, department=?, semester=?, " +
                "phone=?, email=?, photo_path=?, status=? WHERE student_id=?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student.getStudentNumber());
            ps.setString(2, student.getFullName());
            ps.setString(3, student.getDepartment());
            ps.setInt(4, student.getSemester());
            ps.setString(5, student.getPhone());
            ps.setString(6, student.getEmail());
            ps.setString(7, student.getPhotoPath());
            ps.setString(8, student.getStatus().name());
            ps.setInt(9, student.getStudentId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update student " + student.getStudentId(), e);
        }
    }

    @Override
    public boolean delete(int studentId) {
        String sql = "DELETE FROM students WHERE student_id=?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Cannot delete this student — they may have borrow history", e);
        }
    }

    @Override
    public boolean existsByStudentNumber(String studentNumber) {
        return existsWhere("student_number", studentNumber);
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return existsWhere("email", email);
    }

    private boolean existsWhere(String column, String value) {
        String sql = "SELECT 1 FROM students WHERE " + column + " = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lookup failed on students." + column, e);
        }
    }

    private List<Student> mapAll(ResultSet rs) throws SQLException {
        List<Student> students = new ArrayList<>();
        while (rs.next()) students.add(mapRow(rs));
        return students;
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setStudentId(rs.getInt("student_id"));
        s.setStudentNumber(rs.getString("student_number"));
        s.setFullName(rs.getString("full_name"));
        s.setDepartment(rs.getString("department"));
        s.setSemester(rs.getInt("semester"));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        s.setPhotoPath(rs.getString("photo_path"));
        s.setStatus(Student.Status.valueOf(rs.getString("status")));
        String reg = rs.getString("registered_date");
        if (reg != null && !reg.isBlank()) {
            s.setRegisteredDate(LocalDate.parse(reg.length() > 10 ? reg.substring(0, 10) : reg));
        }
        return s;
    }
}
