package com.lendahand.models;

/**
 * Subclass of User representing a person who posts tasks.
 * Demonstrates Inheritance.
 */
public class TaskPoster extends User {

    public TaskPoster() { super(); }

    public TaskPoster(int userID, String name, String email,
                      String password, double walletBalance) {
        super(userID, name, email, password, walletBalance, "POSTER");
    }

    /**
     * Represents the action of posting a task.
     * Actual DB persistence is handled by TaskDAO.
     */
    public String postTask(String title) {
        return getName() + " posted task: " + title;
    }

    @Override
    public String getRole() { return "Task Poster"; }
}
