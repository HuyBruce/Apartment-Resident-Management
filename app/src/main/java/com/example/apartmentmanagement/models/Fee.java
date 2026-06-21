package com.example.apartmentmanagement.models;

import com.example.apartmentmanagement.utils.PenaltyCalculator;

public class Fee {
    private String id;
    private String resident_id;
    private String category;
    private String title;
    private String description;
    private long amount;
    private String due_date;
    private String status;

    public Fee() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getResident_id() { return resident_id; }
    public void setResident_id(String resident_id) { this.resident_id = resident_id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description != null ? description : title; }
    public void setDescription(String description) { this.description = description; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public String getDue_date() { return due_date; }
    public void setDue_date(String due_date) { this.due_date = due_date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }


    public boolean isOverdue() {
        return "unpaid".equals(status) && PenaltyCalculator.getOverdueDays(due_date) > 0;
    }

    public int getOverdueDays() {
        return PenaltyCalculator.getOverdueDays(due_date);
    }

    public long getPenaltyAmount() {
        return PenaltyCalculator.getPenaltyAmount(amount, getOverdueDays());
    }

    public long getTotalAmount() {
        return PenaltyCalculator.getTotalAmount(amount, getOverdueDays());
    }

    public String getDebtLevel() {
        return PenaltyCalculator.getDebtLevel(getOverdueDays());
    }

    public String getReminderMessage() {
        return PenaltyCalculator.getReminderMessage(getOverdueDays());
    }
}