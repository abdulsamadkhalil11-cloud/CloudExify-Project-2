package com.library.service;

import com.library.dao.StudentDAO;
import com.library.dao.impl.StudentDAOImpl;
import com.library.model.Student;
import com.library.util.ValidationUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class StudentService {

    private final StudentDAO studentDAO;

    public StudentService() {
        this(new StudentDAOImpl());
    }

    public StudentService(StudentDAO studentDAO) {
        this.studentDAO = studentDAO;
    }

    public List<Student> getAll() {
        return studentDAO.findAll();
    }

    public List<Student> getPage(int page, int size) {
        return studentDAO.findPage(page, size);
    }

    public List<Student> search(String keyword) {
        return ValidationUtil.isNotBlank(keyword) ? studentDAO.search(keyword.trim()) : studentDAO.findAll();
    }

    public Optional<Student> getById(int id) {
        return studentDAO.findById(id);
    }

    public int totalCount() {
        return studentDAO.count();
    }

    public Student register(Student student) {
        validate(student);
        if (studentDAO.existsByStudentNumber(student.getStudentNumber())) {
            throw new IllegalArgumentException(
                    "Student number " + student.getStudentNumber() + " is already registered.");
        }
        if (ValidationUtil.isNotBlank(student.getEmail()) && studentDAO.existsByEmail(student.getEmail())) {
            throw new IllegalArgumentException("Email " + student.getEmail() + " is already registered.");
        }
        student.setStatus(Student.Status.ACTIVE);
        student.setRegisteredDate(LocalDate.now());
        int id = studentDAO.insert(student);
        student.setStudentId(id);
        return student;
    }

    public void update(Student updated) {
        validate(updated);
        Student existing = studentDAO.findById(updated.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student no longer exists."));
        if (!existing.getStudentNumber().equals(updated.getStudentNumber())
                && studentDAO.existsByStudentNumber(updated.getStudentNumber())) {
            throw new IllegalArgumentException(
                    "Student number " + updated.getStudentNumber() + " is already registered.");
        }
        if (ValidationUtil.isNotBlank(updated.getEmail())
                && !updated.getEmail().equalsIgnoreCase(existing.getEmail())
                && studentDAO.existsByEmail(updated.getEmail())) {
            throw new IllegalArgumentException("Email " + updated.getEmail() + " is already registered.");
        }
        studentDAO.update(updated);
    }

    public void delete(int studentId) {
        studentDAO.delete(studentId);
    }

    private void validate(Student s) {
        if (!ValidationUtil.isNotBlank(s.getFullName())) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (!ValidationUtil.isNotBlank(s.getStudentNumber())) {
            throw new IllegalArgumentException("Student number is required.");
        }
        if (!ValidationUtil.isNotBlank(s.getDepartment())) {
            throw new IllegalArgumentException("Department is required.");
        }
        if (!ValidationUtil.isValidSemester(s.getSemester())) {
            throw new IllegalArgumentException("Semester must be between 1 and 12.");
        }
        if (ValidationUtil.isNotBlank(s.getPhone()) && !ValidationUtil.isValidPhone(s.getPhone())) {
            throw new IllegalArgumentException("Phone number looks invalid.");
        }
        if (ValidationUtil.isNotBlank(s.getEmail()) && !ValidationUtil.isValidEmail(s.getEmail())) {
            throw new IllegalArgumentException("Email address looks invalid.");
        }
    }
}
