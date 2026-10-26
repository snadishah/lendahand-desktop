package com.lendahand.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.lendahand.models.Review;
import com.lendahand.utils.DatabaseConnection;

public class ReviewDAO {

    public static boolean createReview(Review review) {
        String sql = "INSERT INTO reviews (task_id, reviewer_id, reviewee_id, rating, comment) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, review.getTaskID());
            ps.setInt(2, review.getReviewerID());
            ps.setInt(3, review.getRevieweeID());
            ps.setInt(4, review.getRating());
            ps.setString(5, review.getComment());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) review.setReviewID(keys.getInt(1));
            return true;

        } catch (SQLException e) {
            System.err.println("[ReviewDAO] Create failed: " + e.getMessage());
            return false;
        }
    }

    public static List<Review> getReviewsForUser(int userID) {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT * FROM reviews WHERE reviewee_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             
            ps.setInt(1, userID);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Review r = new Review();
                r.setReviewID(rs.getInt("id")); 
                r.setTaskID(rs.getInt("task_id"));
                r.setReviewerID(rs.getInt("reviewer_id"));
                r.setRevieweeID(rs.getInt("reviewee_id"));
                r.setRating(rs.getInt("rating"));
                r.setComment(rs.getString("comment"));
                reviews.add(r);
            }
        } catch (SQLException e) {
            System.err.println("[ReviewDAO] Get reviews failed: " + e.getMessage());
        }
        return reviews;
    }
    
    public static double getAverageRating(int userID) {
        String sql = "SELECT AVG(rating) as avg_rating FROM reviews WHERE reviewee_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble("avg_rating");
            }
        } catch (SQLException e) {
            System.err.println("[ReviewDAO] Get average rating failed: " + e.getMessage());
        }
        return 0.0;
    }
}