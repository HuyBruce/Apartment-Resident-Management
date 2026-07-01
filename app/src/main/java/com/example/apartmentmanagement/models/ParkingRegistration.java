package com.example.apartmentmanagement.models;

public class ParkingRegistration {
    private String id;
    private String resident_id;
    private String vehicle_type;
    private String license_plate;
    private String vehicle_model;
    private String status;
    private String block_reason;
    private String created_at;
    private String updated_at;

    public ParkingRegistration() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getResident_id() { return resident_id; }
    public void setResident_id(String resident_id) { this.resident_id = resident_id; }

    public String getVehicle_type() { return vehicle_type; }
    public void setVehicle_type(String vehicle_type) { this.vehicle_type = vehicle_type; }

    public String getLicense_plate() { return license_plate; }
    public void setLicense_plate(String license_plate) { this.license_plate = license_plate; }

    public String getVehicle_model() { return vehicle_model; }
    public void setVehicle_model(String vehicle_model) { this.vehicle_model = vehicle_model; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBlock_reason() { return block_reason; }
    public void setBlock_reason(String block_reason) { this.block_reason = block_reason; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }

    public boolean isBlocked() { return "blocked".equals(status); }
}