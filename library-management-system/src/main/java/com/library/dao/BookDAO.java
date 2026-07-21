package com.library.dao;

import com.library.model.Book;

import java.util.List;
import java.util.Optional;

/** Persistence contract for Book. Impl is swappable (SQLite today, MySQL later). */
public interface BookDAO {

    Optional<Book> findById(int bookId);

    List<Book> findAll();

    List<Book> findPage(int page, int pageSize);

    /** Matches title, ISBN or author name (case-insensitive, partial). */
    List<Book> search(String keyword);

    List<Book> findByCategory(int categoryId);

    int count();

    int insert(Book book);

    boolean update(Book book);

    boolean delete(int bookId);

    /** Atomically adds {@code delta} to available_copies (negative to decrement on issue). */
    boolean adjustAvailableCopies(int bookId, int delta);

    boolean existsByIsbn(String isbn);
}
