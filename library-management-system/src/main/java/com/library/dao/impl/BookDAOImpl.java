package com.library.dao.impl;

import com.library.dao.BookDAO;
import com.library.dao.DataAccessException;
import com.library.model.Book;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookDAOImpl implements BookDAO {

    private static final String BASE_SELECT =
            "SELECT b.*, a.name AS author_name, c.name AS category_name " +
            "FROM books b " +
            "JOIN authors a ON b.author_id = a.author_id " +
            "JOIN categories c ON b.category_id = c.category_id ";

    private final DatabaseConnection db;

    public BookDAOImpl() {
        this.db = DatabaseConnection.getInstance();
    }

    @Override
    public Optional<Book> findById(int bookId) {
        String sql = BASE_SELECT + "WHERE b.book_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load book " + bookId, e);
        }
    }

    @Override
    public List<Book> findAll() {
        String sql = BASE_SELECT + "ORDER BY b.title";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return mapAll(rs);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load books", e);
        }
    }

    @Override
    public List<Book> findPage(int page, int pageSize) {
        String sql = BASE_SELECT + "ORDER BY b.title LIMIT ? OFFSET ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, Math.max(0, page) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load book page", e);
        }
    }

    @Override
    public List<Book> search(String keyword) {
        String sql = BASE_SELECT +
                "WHERE b.title LIKE ? OR b.isbn LIKE ? OR a.name LIKE ? ORDER BY b.title";
        String like = "%" + keyword + "%";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Book search failed", e);
        }
    }

    @Override
    public List<Book> findByCategory(int categoryId) {
        String sql = BASE_SELECT + "WHERE b.category_id = ? ORDER BY b.title";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load books for category " + categoryId, e);
        }
    }

    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM books";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count books", e);
        }
    }

    @Override
    public int insert(Book book) {
        String sql = "INSERT INTO books (isbn, title, author_id, category_id, publisher, " +
                "publication_year, edition, shelf_location, total_copies, available_copies, " +
                "cover_image_path, added_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            ps.setInt(3, book.getAuthorId());
            ps.setInt(4, book.getCategoryId());
            ps.setString(5, book.getPublisher());
            setNullableInt(ps, 6, book.getPublicationYear());
            ps.setString(7, book.getEdition());
            ps.setString(8, book.getShelfLocation());
            ps.setInt(9, book.getTotalCopies());
            ps.setInt(10, book.getAvailableCopies());
            ps.setString(11, book.getCoverImagePath());
            ps.setString(12, (book.getAddedDate() != null ? book.getAddedDate() : LocalDate.now()).toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert book: " + book.getTitle(), e);
        }
    }

    @Override
    public boolean update(Book book) {
        String sql = "UPDATE books SET isbn=?, title=?, author_id=?, category_id=?, publisher=?, " +
                "publication_year=?, edition=?, shelf_location=?, total_copies=?, available_copies=?, " +
                "cover_image_path=? WHERE book_id=?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            ps.setInt(3, book.getAuthorId());
            ps.setInt(4, book.getCategoryId());
            ps.setString(5, book.getPublisher());
            setNullableInt(ps, 6, book.getPublicationYear());
            ps.setString(7, book.getEdition());
            ps.setString(8, book.getShelfLocation());
            ps.setInt(9, book.getTotalCopies());
            ps.setInt(10, book.getAvailableCopies());
            ps.setString(11, book.getCoverImagePath());
            ps.setInt(12, book.getBookId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update book " + book.getBookId(), e);
        }
    }

    @Override
    public boolean delete(int bookId) {
        String sql = "DELETE FROM books WHERE book_id=?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Most common cause: FK RESTRICT because the book has borrow history.
            throw new DataAccessException("Cannot delete this book — it may have borrow history", e);
        }
    }

    @Override
    public boolean adjustAvailableCopies(int bookId, int delta) {
        String sql = "UPDATE books SET available_copies = available_copies + ? WHERE book_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, bookId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to adjust copies for book " + bookId, e);
        }
    }

    @Override
    public boolean existsByIsbn(String isbn) {
        String sql = "SELECT 1 FROM books WHERE isbn = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to check ISBN " + isbn, e);
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private List<Book> mapAll(ResultSet rs) throws SQLException {
        List<Book> books = new ArrayList<>();
        while (rs.next()) {
            books.add(mapRow(rs));
        }
        return books;
    }

    private Book mapRow(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setBookId(rs.getInt("book_id"));
        book.setIsbn(rs.getString("isbn"));
        book.setTitle(rs.getString("title"));
        book.setAuthorId(rs.getInt("author_id"));
        book.setAuthorName(rs.getString("author_name"));
        book.setCategoryId(rs.getInt("category_id"));
        book.setCategoryName(rs.getString("category_name"));
        book.setPublisher(rs.getString("publisher"));
        int year = rs.getInt("publication_year");
        book.setPublicationYear(rs.wasNull() ? null : year);
        book.setEdition(rs.getString("edition"));
        book.setShelfLocation(rs.getString("shelf_location"));
        book.setTotalCopies(rs.getInt("total_copies"));
        book.setAvailableCopies(rs.getInt("available_copies"));
        book.setCoverImagePath(rs.getString("cover_image_path"));
        String added = rs.getString("added_date");
        if (added != null && !added.isBlank()) {
            book.setAddedDate(LocalDate.parse(added.length() > 10 ? added.substring(0, 10) : added));
        }
        return book;
    }
}
