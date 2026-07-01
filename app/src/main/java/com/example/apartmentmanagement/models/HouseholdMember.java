package com.example.apartmentmanagement.models;

public class HouseholdMember {

    private String id;
    private String resident_id;
    private int apartment_id;
    private String full_name;
    private String date_of_birth;
    private String gender;
    private String relationship;  // Chủ hộ, Vợ/Chồng, Con cái, Ông/Bà
    private String identity_number;
    private boolean is_active;
    private String created_at;

    public HouseholdMember() {}

    // ── Getters & Setters ──────────────────────────────────────
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getResident_id() { return resident_id; }
    public void setResident_id(String resident_id) { this.resident_id = resident_id; }

    public int getApartment_id() { return apartment_id; }
    public void setApartment_id(int apartment_id) { this.apartment_id = apartment_id; }

    public String getFull_name() { return full_name; }
    public void setFull_name(String full_name) { this.full_name = full_name; }

    public String getDate_of_birth() { return date_of_birth; }
    public void setDate_of_birth(String date_of_birth) { this.date_of_birth = date_of_birth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public String getIdentity_number() { return identity_number; }
    public void setIdentity_number(String identity_number) { this.identity_number = identity_number; }

    public boolean isIs_active() { return is_active; }
    public void setIs_active(boolean is_active) { this.is_active = is_active; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    // ── Helper ──────────────────────────────────────────────────
    public int getAge() {
        try {
            String[] parts = date_of_birth.split("-");
            int birthYear = Integer.parseInt(parts[0]);
            return 2026 - birthYear;
        } catch (Exception e) {
            return 0;
        }
    }

    public String getAgeGroup() {
        int age = getAge();
        if (age < 15)  return "Trẻ em";
        if (age >= 60) return "Người cao tuổi";
        return "Người lớn";
    }

    public String getAgeGroupEmoji() {
        int age = getAge();
        if (age < 15)  return "👶";
        if (age >= 60) return "👴";
        return "👤";
    }
}