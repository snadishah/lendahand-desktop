package com.lendahand.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.lendahand.models.Bid;
import com.lendahand.utils.DatabaseConnection;

/**
 * Data Access Object for the bids table.
 */
public class BidDAO {

    /** Inserts a new bid and sets the generated ID. */
    public static boolean createBid(Bid bid) {
        String sql = "INSERT INTO bids (task_id, helper_id, proposed_amount, status) " +
                     "VALUES (?, ?, ?, 'PENDING')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, bid.getTaskID());
            ps.setInt(2, bid.getHelperID());
            ps.setDouble(3, bid.getProposedAmount());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) bid.setBidID(keys.getInt(1));
            return true;

        } catch (SQLException e) {
            System.err.println("[BidDAO] Create failed: " + e.getMessage());
            return false;
        }
    }

    /** Returns all bids for a given task, including helper names. */
    public static List<Bid> getBidsForTask(int taskID) {
        String sql =
            "SELECT b.*, u.name AS helper_name " +
            "FROM bids b JOIN users u ON b.helper_id = u.id " +
            "WHERE b.task_id = ? ORDER BY b.proposed_amount ASC";
        List<Bid> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            System.err.println("[BidDAO] GetBidsForTask failed: " + e.getMessage());
        }
        return list;
    }

    /** Checks if a helper has already bid on this task. */
    public static boolean hasHelperBid(int taskID, int helperID) {
        String sql = "SELECT 1 FROM bids WHERE task_id = ? AND helper_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskID);
            ps.setInt(2, helperID);
            return ps.executeQuery().next();

        } catch (SQLException e) {
            return false;
        }
    }

    /** Sets a specific bid to ACCEPTED and all others on same task to REJECTED. */
    public static void acceptBid(int bidID, int taskID) {
        String acceptSql = "UPDATE bids SET status = 'ACCEPTED' WHERE id = ?";
        String rejectSql = "UPDATE bids SET status = 'REJECTED' WHERE task_id = ? AND id != ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(acceptSql);
                 PreparedStatement ps2 = conn.prepareStatement(rejectSql)) {

                ps1.setInt(1, bidID);
                ps1.executeUpdate();

                ps2.setInt(1, taskID);
                ps2.setInt(2, bidID);
                ps2.executeUpdate();

                conn.commit();
            }
        } catch (SQLException e) {
            System.err.println("[BidDAO] AcceptBid failed: " + e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static Bid mapRow(ResultSet rs) throws SQLException {
        Bid b = new Bid();
        b.setBidID(rs.getInt("id"));
        b.setTaskID(rs.getInt("task_id"));
        b.setHelperID(rs.getInt("helper_id"));
        b.setProposedAmount(rs.getDouble("proposed_amount"));
        b.setStatus(Bid.Status.valueOf(rs.getString("status")));
        b.setHelperName(rs.getString("helper_name"));
        return b;
    }

    /** Returns all bids placed by a specific helper. */
    public static List<Bid> getBidsForHelper(int helperID) {
        List<Bid> bids = new ArrayList<>();
        // Joining with users table because your mapRow method expects "helper_name"
        String sql = "SELECT b.*, u.name AS helper_name " +
                     "FROM bids b JOIN users u ON b.helper_id = u.id " +
                     "WHERE b.helper_id = ?";
                     
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             
            ps.setInt(1, helperID);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                bids.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[BidDAO] getBidsForHelper failed: " + e.getMessage());
        }
        return bids;
    }
}
