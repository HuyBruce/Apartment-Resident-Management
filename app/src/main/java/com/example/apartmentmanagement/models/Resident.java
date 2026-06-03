package com.example.apartmentmanagement.models;

public class Resident {

    private String full_name;
    private String phone_number;
    private String email;
    private String date_of_birth;
    private String gender;
    private String apartment_number;
    private String members_count;

    public Resident() {}

    // --- Getters & Setters ---

    public String getFull_name() { return full_name; }
    public void setFull_name(String full_name) { this.full_name = full_name; }

    public String getPhone_number() { return phone_number; }
    public void setPhone_number(String phone_number) { this.phone_number = phone_number; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDate_of_birth() { return date_of_birth; }
    public void setDate_of_birth(String date_of_birth) { this.date_of_birth = date_of_birth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getApartment_number() { return apartment_number; }
    public void setApartment_number(String apartment_number) { this.apartment_number = apartment_number; }

    public String getMembers_count() { return members_count; }
    public void setMembers_count(String members_count) { this.members_count = members_count; }
}