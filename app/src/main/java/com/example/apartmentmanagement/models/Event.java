package com.example.apartmentmanagement.models;

public class Event {

    private String id;
    private String title;
    private String description;
    private String type;           // gift, activity, service
    private String target_group;   // children, elderly, all
    private String event_date;
    private String event_time;
    private String location;
    private int max_participants;
    private String status;         // upcoming, ongoing, completed
    private boolean requires_registration;
    private String created_at;

    public Event() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTarget_group() { return target_group; }
    public void setTarget_group(String target_group) { this.target_group = target_group; }

    public String getEvent_date() { return event_date; }
    public void setEvent_date(String event_date) { this.event_date = event_date; }

    public String getEvent_time() { return event_time; }
    public void setEvent_time(String event_time) { this.event_time = event_time; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getMax_participants() { return max_participants; }
    public void setMax_participants(int max_participants) { this.max_participants = max_participants; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isRequires_registration() { return requires_registration; }
    public void setRequires_registration(boolean r) { this.requires_registration = r; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getTypeEmoji() {
        switch (type != null ? type : "") {
            case "gift":     return "🎁";
            case "activity": return "🎉";
            case "service":  return "🔧";
            default:         return "📋";
        }
    }

    public String getTargetGroupLabel() {
        switch (target_group != null ? target_group : "") {
            case "children": return "👶 Trẻ em";
            case "elderly":  return "👴 Người cao tuổi";
            default:         return "👥 Tất cả";
        }
    }

    public String getStatusLabel() {
        switch (status != null ? status : "") {
            case "upcoming":  return "Sắp diễn ra";
            case "ongoing":   return "Đang diễn ra";
            case "completed": return "Đã kết thúc";
            default:          return "";
        }
    }
}