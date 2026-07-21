package com.library.dao.impl;

import com.library.dao.AuthorDAO;
import com.library.dao.DataAccessException;
import com.library.model.Author;
import com.library.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuthorDAOImpl implements AuthorDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public Optional<Author> findById(int authorId) {
        String sql = "SELECT * FROM authors WHERE author_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, authorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load author " + authorId, e);
        }
    }

    @Override
    public List<Author> findAll() {
        String sql = "SELECT * FROM authors ORDER BY name";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<Author> authors = new ArrayList<>();
            while (rs.next()) authors.add(mapRow(rs));
            return authors;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load authors", e);
        }
    }

    @Override
    public List<Author> search(String keyword) {
        String sql = "SELECT * FROM authors WHERE name LIKE ? ORDER BY name";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<Author> authors = new ArrayList<>();
                while (rs.next()) authors.add(mapRow(rs));
                return authors;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Author search failed", e);
        }
    }

    @Override
    public int insert(Author author) {
        String sql = "INSERT INTO authors (name, bio) VALUES (?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, author.getName());
            ps.setString(2, author.getBio());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert author: " + author.getName(), e);
        }
    }

    @Override
    public boolean update(Author author) {
        String sql = "UPDATE authors SET name=?, bio=? WHERE author_id=?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, author.getName());
            ps.setString(2, author.getBio());
            ps.setInt(3, author.getAuthorId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update author " + author.getAuthorId(), e);
        }
    }

    @Override
    public boolean delete(int authorId) {
        String sql = "DELETE FROM authors WHERE author_id=?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, authorId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Cannot delete this author — they may have books on file", e);
        }
    }

    private Author mapRow(ResultSet rs) throws SQLException {
        return new Author(rs.getInt("author_id"), rs.getString("name"), rs.getString("bio"));
    }
}
