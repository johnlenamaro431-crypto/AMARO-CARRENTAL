package com.carrental.model;

/** Maps to one row of the activity_logs table (the system audit trail). */
public record ActivityLog(int logId, int userId, String actionType, String description, String timestamp) {
}
