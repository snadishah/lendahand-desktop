package com.lendahand.models;

/**
 * Represents a bid submitted by a Helper on a Task.
 */
public class Bid {

    public enum Status { PENDING, ACCEPTED, REJECTED }

    private int    bidID;
    private int    taskID;
    private int    helperID;
    private double proposedAmount;
    private Status status;

    // ── Joined display field ──────────────────────────────────────────────────
    private String helperName;

    public Bid() {}

    public Bid(int taskID, int helperID, double proposedAmount) {
        this.taskID         = taskID;
        this.helperID       = helperID;
        this.proposedAmount = proposedAmount;
        this.status         = Status.PENDING;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int    getBidID()          { return bidID; }
    public int    getTaskID()         { return taskID; }
    public int    getHelperID()       { return helperID; }
    public double getProposedAmount() { return proposedAmount; }
    public Status getStatus()         { return status; }
    public String getHelperName()     { return helperName; }

    // ── Setters ──────────────────────────────────────────────────────────────
    public void setBidID(int bidID)                    { this.bidID          = bidID; }
    public void setTaskID(int taskID)                  { this.taskID         = taskID; }
    public void setHelperID(int helperID)              { this.helperID       = helperID; }
    public void setProposedAmount(double amount)       { this.proposedAmount = amount; }
    public void setStatus(Status status)               { this.status         = status; }
    public void setHelperName(String helperName)       { this.helperName     = helperName; }

    /** Summary string for table/list cells. */
    public String getDisplayText() {
        String name = helperName != null ? helperName : "Helper #" + helperID;
        return String.format("%s  →  %.0f LHT  [%s]", name, proposedAmount, status);
    }

    @Override
    public String toString() { return getDisplayText(); }
}
