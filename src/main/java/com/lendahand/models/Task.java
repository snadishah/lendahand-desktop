package com.lendahand.models;

/**
 * Represents a task posted on the platform.
 */
public class Task {

    public enum Status { OPEN, IN_PROGRESS, DONE, CANCELLED }

    private int    taskID;
    private int    posterID;
    private String title;
    private String description;
    private String category;
    private String address;
    private double latitude;
    private double longitude;
    private double budget;
    private Status status;
    private int    helperID;          // 0 if no helper assigned
    private double acceptedAmount;    // final agreed price (held in escrow)

    // ── Computed at runtime (not stored in DB) ────────────────────────────
    private double distanceKm;
    private String posterName;
    private String helperName;

    public Task() {}

    public Task(int posterID, String title, String description, String category,
                String address, double latitude, double longitude, double budget) {
        this.posterID    = posterID;
        this.title       = title;
        this.description = description;
        this.category    = category;
        this.address     = address;
        this.latitude    = latitude;
        this.longitude   = longitude;
        this.budget      = budget;
        this.status      = Status.OPEN;
        this.helperID    = 0;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int    getTaskID()         { return taskID; }
    public int    getPosterID()       { return posterID; }
    public String getTitle()          { return title; }
    public String getDescription()    { return description; }
    public String getCategory()       { return category; }
    public String getAddress()        { return address; }
    public double getLatitude()       { return latitude; }
    public double getLongitude()      { return longitude; }
    public double getBudget()         { return budget; }
    public Status getStatus()         { return status; }
    public int    getHelperID()       { return helperID; }
    public double getAcceptedAmount() { return acceptedAmount; }
    public double getDistanceKm()     { return distanceKm; }
    public String getPosterName()     { return posterName; }
    public String getHelperName()     { return helperName; }

    // ── Setters ──────────────────────────────────────────────────────────────
    public void setTaskID(int taskID)                 { this.taskID         = taskID; }
    public void setPosterID(int posterID)             { this.posterID       = posterID; }
    public void setTitle(String title)                { this.title          = title; }
    public void setDescription(String description)    { this.description    = description; }
    public void setCategory(String category)          { this.category       = category; }
    public void setAddress(String address)            { this.address        = address; }
    public void setLatitude(double latitude)          { this.latitude       = latitude; }
    public void setLongitude(double longitude)        { this.longitude      = longitude; }
    public void setBudget(double budget)              { this.budget         = budget; }
    public void setStatus(Status status)              { this.status         = status; }
    public void setHelperID(int helperID)             { this.helperID       = helperID; }
    public void setAcceptedAmount(double amount)      { this.acceptedAmount = amount; }
    public void setDistanceKm(double distanceKm)     { this.distanceKm    = distanceKm; }
    public void setPosterName(String posterName)      { this.posterName     = posterName; }
    public void setHelperName(String helperName)      { this.helperName     = helperName; }

    /** Display string for ListViews. */
    public String getDisplayText() {
        String dist = distanceKm > 0
                ? String.format("  •  %.1f km away", distanceKm)
                : "";
        return String.format("[%s]  %s  — %.0f LHT%s", category, title, budget, dist);
    }

    @Override
    public String toString() { return title; }
}
