package com.app.dto;

public class SummaryDto {
    private long totalUsers;
    private long activePolicies;
    private long alerts;
    private long openTasks;

    public SummaryDto(long totalUsers, long activePolicies, long alerts, long openTasks) {
        this.totalUsers    = totalUsers;
        this.activePolicies = activePolicies;
        this.alerts        = alerts;
        this.openTasks     = openTasks;
    }

    public long getTotalUsers()     { return totalUsers; }
    public long getActivePolicies() { return activePolicies; }
    public long getAlerts()         { return alerts; }
    public long getOpenTasks()      { return openTasks; }
}
