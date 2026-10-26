package com.lendahand.models;

/**
 * Abstract base class for all user types.
 * Demonstrates Encapsulation (private fields + public getters/setters).
 */
public abstract class User {

    private int    userID;
    private String name;
    private String email;
    private String password;
    private double walletBalance;
    private String userType;   // "POSTER" or "HELPER"

    public User() {}

    public User(int userID, String name, String email,
                String password, double walletBalance, String userType) {
        this.userID        = userID;
        this.name          = name;
        this.email         = email;
        this.password      = password;
        this.walletBalance = walletBalance;
        this.userType      = userType;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int    getUserID()        { return userID; }
    public String getName()          { return name; }
    public String getEmail()         { return email; }
    public String getPassword()      { return password; }
    public double getWalletBalance() { return walletBalance; }
    public String getUserType()      { return userType; }

    // ── Setters ──────────────────────────────────────────────────────────────
    public void setUserID(int userID)               { this.userID        = userID; }
    public void setName(String name)                { this.name          = name; }
    public void setEmail(String email)              { this.email         = email; }
    public void setPassword(String password)        { this.password      = password; }
    public void setWalletBalance(double balance)    { this.walletBalance = balance; }
    public void setUserType(String userType)        { this.userType      = userType; }

    /** Polymorphic role label shown in the UI. */
    public abstract String getRole();

    @Override
    public String toString() {
        return name + " (" + getRole() + ")";
    }
}
