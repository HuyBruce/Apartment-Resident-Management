package com.example.apartmentmanagement.models;

public class Fee {
    private String id;
    private String resident_id;
    private long apartment_id;
    private String category;
    private long amount;
    private String description;
    private String due_date;
    private String status; // "unpaid" | "paid"
    private String created_at;
    private String updated_at;

    public Fee() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getResident_id() { return resident_id; }
    public void setResident_id(String v) { this.resident_id = v; }

    public long getApartment_id() { return apartment_id; }
    public void setApartment_id(long v) { this.apartment_id = v; }

    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }

    public long getAmount() { return amount; }
    public void setAmount(long v) { this.amount = v; }

    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }

    public String getDue_date() { return due_date; }
    public void setDue_date(String v) { this.due_date = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String v) { this.created_at = v; }

    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String v) { this.updated_at = v; }
}