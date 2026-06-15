package com.example.apartmentmanagement.models;

public class Visitor {
    private String id;
    private String resident_id;
    private long apartment_id;
    private String visitor_name;
    private String visitor_phone;
    private String visit_date;
    private String visit_time;
    private String purpose;
    private String status; // "pending" | "approved"
    private String created_at;
    private String updated_at;

    public Visitor() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getResident_id() { return resident_id; }
    public void setResident_id(String v) { this.resident_id = v; }

    public long getApartment_id() { return apartment_id; }
    public void setApartment_id(long v) { this.apartment_id = v; }

    public String getVisitor_name() { return visitor_name; }
    public void setVisitor_name(String v) { this.visitor_name = v; }

    public String getVisitor_phone() { return visitor_phone; }
    public void setVisitor_phone(String v) { this.visitor_phone = v; }

    public String getVisit_date() { return visit_date; }
    public void setVisit_date(String v) { this.visit_date = v; }

    public String getVisit_time() { return visit_time; }
    public void setVisit_time(String v) { this.visit_time = v; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String v) { this.purpose = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String v) { this.created_at = v; }

    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String v) { this.updated_at = v; }
}