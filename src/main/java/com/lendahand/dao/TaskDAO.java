package com.lendahand.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.lendahand.models.Task;
import com.lendahand.utils.DatabaseConnection;

public class TaskDAO {

    public static boolean createTask(Task task) {
        // Resolve category name → id
        int catId = getCategoryId(task.getCategory());
        String sql =
            "INSERT INTO tasks (poster_id, category_id, title, description, " +
            "address, latitude, longitude, budget, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'OPEN')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, task.getPosterID());
            ps.setInt(2, catId);
            ps.setString(3, task.getTitle());
            ps.setString(4, task.getDescription());
            ps.setString(5, task.getAddress());
            ps.setDouble(6, task.getLatitude());
            ps.setDouble(7, task.getLongitude());
            ps.setDouble(8, task.getBudget());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) task.setTaskID(keys.getInt(1));
            return true;
        } catch (SQLException e) {
            System.err.println("[TaskDAO] Create failed: " + e.getMessage());
            return false;
        }
    }

    /** ALL open tasks — visible to every Helper, sorted newest first */
    public static List<Task> getOpenTasks() {
        String sql =
            "SELECT t.*, c.name AS cat_name, c.icon AS cat_icon, " +
            "       u.name AS poster_name " +
            "FROM tasks t " +
            "JOIN categories c ON t.category_id = c.id " +
            "JOIN users u      ON t.poster_id   = u.id " +
            "WHERE t.status = 'OPEN' ORDER BY t.created_at DESC";
        return queryTasks(sql);
    }

    public static List<Task> getTasksByPoster(int posterID) {
        String sql =
            "SELECT t.*, c.name AS cat_name, c.icon AS cat_icon, " +
            "       u.name AS poster_name " +
            "FROM tasks t " +
            "JOIN categories c ON t.category_id = c.id " +
            "JOIN users u      ON t.poster_id   = u.id " +
            "WHERE t.poster_id = ? ORDER BY t.created_at DESC";
        return queryTasksWithParam(sql, posterID);
    }

    public static List<Task> getTasksByHelper(int helperID) {
        String sql =
            "SELECT t.*, c.name AS cat_name, c.icon AS cat_icon, " +
            "       u.name AS poster_name " +
            "FROM tasks t " +
            "JOIN categories c ON t.category_id = c.id " +
            "JOIN users u      ON t.poster_id   = u.id " +
            "WHERE t.helper_id = ? ORDER BY t.created_at DESC";
        return queryTasksWithParam(sql, helperID);
    }

    public static Task getTaskById(int taskID) {
        String sql =
            "SELECT t.*, c.name AS cat_name, c.icon AS cat_icon, " +
            "       u.name AS poster_name " +
            "FROM tasks t " +
            "JOIN categories c ON t.category_id = c.id " +
            "JOIN users u      ON t.poster_id   = u.id " +
            "WHERE t.id = ?";
        List<Task> list = queryTasksWithParam(sql, taskID);
        return list.isEmpty() ? null : list.get(0);
    }

    public static void acceptBid(int taskID, int helperID, double acceptedAmount) {
        String sql = "UPDATE tasks SET status='IN_PROGRESS', helper_id=?, accepted_amount=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, helperID);
            ps.setDouble(2, acceptedAmount);
            ps.setInt(3, taskID);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[TaskDAO] AcceptBid: " + e.getMessage());
        }
    }

    public static void markDone(int taskID) { updateStatus(taskID, "DONE"); }
    public static void cancelTask(int taskID) { updateStatus(taskID, "CANCELLED"); }

    private static void updateStatus(int taskID, String status) {
        String sql = "UPDATE tasks SET status=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, taskID);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[TaskDAO] UpdateStatus: " + e.getMessage());
        }
    }

    private static int getCategoryId(String categoryName) {
        String sql = "SELECT id FROM categories WHERE name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoryName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            System.err.println("[TaskDAO] getCategoryId: " + e.getMessage());
        }
        return 10; // fallback to "Other"
    }

    private static List<Task> queryTasks(String sql) {
        List<Task> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[TaskDAO] Query: " + e.getMessage());
        }
        return list;
    }

    private static List<Task> queryTasksWithParam(String sql, int param) {
        List<Task> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[TaskDAO] QueryParam: " + e.getMessage());
        }
        return list;
    }

    private static Task mapRow(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setTaskID(rs.getInt("id"));
        t.setPosterID(rs.getInt("poster_id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        // Combine icon + category name for display
        String icon = rs.getString("cat_icon");
        String name = rs.getString("cat_name");
        t.setCategory((icon != null ? icon + " " : "") + name);
        t.setAddress(rs.getString("address"));
        t.setLatitude(rs.getDouble("latitude"));
        t.setLongitude(rs.getDouble("longitude"));
        t.setBudget(rs.getDouble("budget"));
        t.setStatus(Task.Status.valueOf(rs.getString("status")));
        t.setHelperID(rs.getInt("helper_id"));
        t.setAcceptedAmount(rs.getDouble("accepted_amount"));
        t.setPosterName(rs.getString("poster_name"));
        return t;
    }

    /** Returns all tasks (any status) that have GPS coords — used by MapController */
    public static List<Task> getAllTasksForMap() {
        String sql =
            "SELECT t.*, c.name AS cat_name, c.icon AS cat_icon, " +
            "       u.name AS poster_name " +
            "FROM tasks t " +
            "JOIN categories c ON t.category_id = c.id " +
            "JOIN users u      ON t.poster_id   = u.id " +
            "WHERE t.latitude != 0 OR t.longitude != 0 " +
            "ORDER BY t.created_at DESC";
        return queryTasks(sql);
    }
}