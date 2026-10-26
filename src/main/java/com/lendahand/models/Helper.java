package com.lendahand.models;

/**
 * Subclass of User representing a person who fulfils tasks.
 * Demonstrates Inheritance.
 */
public class Helper extends User {

    public Helper() { super(); }

    public Helper(int userID, String name, String email,
                  String password, double walletBalance) {
        super(userID, name, email, password, walletBalance, "HELPER");
    }

    /**
     * Represents the action of submitting a bid on a task.
     * Actual DB persistence is handled by BidDAO.
     */
    public String submitBid(int taskID, double amount) {
        return getName() + " bid " + amount + " LHT on task #" + taskID;
    }

    /**
     * Represents marking a task as completed.
     */
    public String completeTask(int taskID) {
        return getName() + " completed task #" + taskID;
    }

    @Override
    public String getRole() { return "Helper"; }
}
