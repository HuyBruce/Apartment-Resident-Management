package com.example.apartmentmanagement.models;

public class Resident {
    private long id;
    private String full_name;
    private String phone_number;
    private String email;
    private String date_of_birth;
    private String gender;
    private String apartment_number;
    private String members_count;
    private String avatar_url;
    private String identity_number;
    private String relationship_to_apartment;
    private long apartment_id;
    private String created_at;
    private String updated_at;

    public Resident() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFull_name() { return full_name; }
    public void setFull_name(String v) { this.full_name = v; }

    public String getPhone_number() { return phone_number; }
    public void setPhone_number(String v) { this.phone_number = v; }

    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }

    public String getDate_of_birth() { return date_of_birth; }
    public void setDate_of_birth(String v) { this.date_of_birth = v; }

    public String getGender() { return gender; }
    public void setGender(String v) { this.gender = v; }

    public String getApartment_number() { return apartment_number; }
    public void setApartment_number(String v) { this.apartment_number = v; }

    public String getMembers_count() { return members_count; }
    public void setMembers_count(String v) { this.members_count = v; }

    public String getAvatar_url() { return avatar_url; }
    public void setAvatar_url(String v) { this.avatar_url = v; }

    public String getIdentity_number() { return identity_number; }
    public void setIdentity_number(String v) { this.identity_number = v; }

    public String getRelationship_to_apartment() { return relationship_to_apartment; }
    public void setRelationship_to_apartment(String v) { this.relationship_to_apartment = v; }

    public long getApartment_id() { return apartment_id; }
    public void setApartment_id(long v) { this.apartment_id = v; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String v) { this.created_at = v; }

    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String v) { this.updated_at = v; }
}