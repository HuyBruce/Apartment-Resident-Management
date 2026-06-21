package com.example.apartmentmanagement.models;

public class Complaint {

    private String id;
    private String resident_id;
    private String title;
    private String description;
    private String category;
    private String image_base64;
    private String status;
    private String priority;
    private String created_at;
    private String updated_at;
    private String resolved_at;
    private String admin_response;

    public Complaint() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getResident_id() { return resident_id; }
    public void setResident_id(String resident_id) { this.resident_id = resident_id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImage_base64() { return image_base64; }
    public void setImage_base64(String image_base64) { this.image_base64 = image_base64; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }

    public String getResolved_at() { return resolved_at; }
    public void setResolved_at(String resolved_at) { this.resolved_at = resolved_at; }

    public String getAdmin_response() { return admin_response; }
    public void setAdmin_response(String admin_response) { this.admin_response = admin_response; }

    // ── Helper ──────────────────────────────
    public boolean hasImage() {
        return image_base64 != null && !image_base64.isEmpty();
    }

    public String getStatusLabel() {
        switch (status != null ? status : "") {
            case "pending":    return "Chờ xử lý";
            case "processing": return "Đang xử lý";
            case "resolved":   return "Đã giải quyết";
            default:           return "";
        }
    }

    public String getCategoryLabel() {
        switch (category != null ? category : "") {
            case "vi_phạm_nội_quy": return "🚫 Vi phạm nội quy";
            case "sửa_chữa":        return "🔧 Sửa chữa";
            case "an_ninh":         return "🔒 An ninh";
            default:                return "📋 Khác";
        }
    }
}