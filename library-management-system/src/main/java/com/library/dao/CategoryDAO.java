package com.library.dao;

import com.library.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryDAO {

    Optional<Category> findById(int categoryId);

    List<Category> findAll();

    int insert(Category category);

    boolean update(Category category);

    boolean delete(int categoryId);
}
