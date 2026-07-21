package com.library.dao;

import com.library.model.Staff;

import java.util.List;
import java.util.Optional;

public interface StaffDAO {

    Optional<Staff> findById(int staffId);

    Optional<Staff> findByUsername(String username);

    List<Staff> findAll();

    int insert(Staff staff);

    boolean update(Staff staff);

    boolean updatePassword(int staffId, String newHash, String newSalt);

    boolean updateLastLogin(int staffId);

    boolean delete(int staffId);

    boolean existsByUsername(String username);
}
