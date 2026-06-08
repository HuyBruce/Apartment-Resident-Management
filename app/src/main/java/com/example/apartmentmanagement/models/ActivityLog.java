package com.example.apartmentmanagement.models;

public class ActivityLog {
    private String id;
    private String user_id;
    private String action;       // "CREATE_REQUEST", "PAY_FEE", "REGISTER_VISITOR", ...
    private String description;
    private String created_at;

    public ActivityLog() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUser_id() { return user_id; }
    public void setUser_id(String v) { this.user_id = v; }

    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }

    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String v) { this.created_at = v; }
}