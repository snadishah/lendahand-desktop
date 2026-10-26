package com.lendahand.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.lendahand.models.Helper;
import com.lendahand.models.TaskPoster;
import com.lendahand.models.User;
import com.lendahand.utils.DatabaseConnection;

public class UserDAO {

    public static boolean registerUser(User user) {
        String sql = "INSERT INTO users (name, email, password, wallet_balance, user_type) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());   // already lowercased by controller
            ps.setString(3, user.getPassword()); // stored exactly as typed
            ps.setDouble(4, user.getWalletBalance());
            ps.setString(5, user.getUserType());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) user.setUserID(keys.getInt(1));

            System.out.println("[UserDAO] Registered: " + user.getEmail()
                + " pass='" + user.getPassword() + "' type=" + user.getUserType());
            return true;

        } catch (SQLException e) {
            System.err.println("[UserDAO] Register failed: " + e.getMessage());
            return false;
        }
    }

    public static User login(String email, String password) {
        // Use LOWER() on the stored email so casing never matters
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?) AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim());
            ps.setString(2, password);   // never trim — must match exactly
            ResultSet rs = ps.executeQuery();

            System.out.println("[UserDAO] Login attempt: email='" + email.trim()
                + "' pass='" + password + "'");

            if (rs.next()) {
                User u = mapRow(rs);
                System.out.println("[UserDAO] Login SUCCESS: " + u.getName());
                return u;
            } else {
                System.out.println("[UserDAO] Login FAILED — no matching row.");
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO] Login error: " + e.getMessage());
        }
        return null;
    }

    public static User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[UserDAO] GetById failed: " + e.getMessage());
        }
        return null;
    }

    public static void updateWallet(int userID, double newBalance) {
        String sql = "UPDATE users SET wallet_balance = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setInt(2, userID);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[UserDAO] UpdateWallet failed: " + e.getMessage());
        }
    }

    public static boolean emailExists(String email) {
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            return ps.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    private static User mapRow(ResultSet rs) throws SQLException {
        int    id     = rs.getInt("id");
        String name   = rs.getString("name");
        String email  = rs.getString("email");
        String pass   = rs.getString("password");
        double wallet = rs.getDouble("wallet_balance");
        String type   = rs.getString("user_type");

        if ("POSTER".equals(type)) {
            return new TaskPoster(id, name, email, pass, wallet);
        } else {
            return new Helper(id, name, email, pass, wallet);
        }
    }
}