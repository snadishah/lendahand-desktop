package com.lendahand.models;

public class Review {
    private int reviewID;
    private int taskID;
    private int reviewerID;
    private int revieweeID;
    private int rating;
    private String comment;

    public Review() {}

    public Review(int taskID, int reviewerID, int revieweeID, int rating, String comment) {
        this.taskID = taskID;
        this.reviewerID = reviewerID;
        this.revieweeID = revieweeID;
        this.rating = rating;
        this.comment = comment;
    }

    public int getReviewID() { return reviewID; }
    public void setReviewID(int reviewID) { this.reviewID = reviewID; }
    
    public int getTaskID() { return taskID; }
    public void setTaskID(int taskID) { this.taskID = taskID; }
    
    public int getReviewerID() { return reviewerID; }
    public void setReviewerID(int reviewerID) { this.reviewerID = reviewerID; }
    
    public int getRevieweeID() { return revieweeID; }
    public void setRevieweeID(int revieweeID) { this.revieweeID = revieweeID; }
    
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}