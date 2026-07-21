package com.library.dao;

import com.library.model.Author;

import java.util.List;
import java.util.Optional;

public interface AuthorDAO {

    Optional<Author> findById(int authorId);

    List<Author> findAll();

    List<Author> search(String keyword);

    int insert(Author author);

    boolean update(Author author);

    boolean delete(int authorId);
}
