package com.library.dao.impl;

import com.library.dao.DataAccessException;
import com.library.dao.SettingsDAO;
import com.library.util.DatabaseConnection;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsDAOImpl implements SettingsDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public String getValue(String key, String defaultValue) {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : defaultValue;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to read setting " + key, e);
        }
    }

    @Override
    public void setValue(String key, String value) {
        String sql = "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?) " +
                "ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save setting " + key, e);
        }
    }

    @Override
    public Map<String, String> getAll() {
        String sql = "SELECT setting_key, setting_value FROM settings ORDER BY setting_key";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            Map<String, String> map = new LinkedHashMap<>();
            while (rs.next()) {
                map.put(rs.getString(1), rs.getString(2));
            }
            return map;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load settings", e);
        }
    }
}
