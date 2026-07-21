package com.library.dao;

import com.library.model.Student;

import java.util.List;
import java.util.Optional;

public interface StudentDAO {

    Optional<Student> findById(int studentId);

    List<Student> findAll();

    List<Student> findPage(int page, int pageSize);

    /** Matches name, student number or email (case-insensitive, partial). */
    List<Student> search(String keyword);

    int count();

    int insert(Student student);

    boolean update(Student student);

    boolean delete(int studentId);

    boolean existsByStudentNumber(String studentNumber);

    boolean existsByEmail(String email);
}
