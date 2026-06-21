package com.example.apartmentmanagement.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.models.Event;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Set;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.VH> {

    public interface OnRegisterClick { void onClick(Event event); }

    private final Context context;
    private final List<Event> events;
    private final Set<String> registeredEventIds; // event đã đăng ký
    private final OnRegisterClick onRegister;

    public EventAdapter(Context ctx, List<Event> events,
                        Set<String> registeredIds, OnRegisterClick onRegister) {
        this.context = ctx;
        this.events = events;
        this.registeredEventIds = registeredIds;
        this.onRegister = onRegister;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_event, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Event e = events.get(pos);

        h.tvEmoji.setText(e.getTypeEmoji());
        h.tvTitle.setText(e.getTitle());
        h.tvTargetGroup.setText(e.getTargetGroupLabel());
        h.tvDate.setText(e.getEvent_date() + " • " + e.getEvent_time());
        h.tvLocation.setText("📍 " + e.getLocation());
        h.tvDesc.setText(e.getDescription());
        h.tvStatus.setText(e.getStatusLabel());

        boolean isRegistered = registeredEventIds.contains(e.getId());
        boolean isCompleted  = "completed".equals(e.getStatus());

        if (!e.isRequires_registration()) {
            h.btnRegister.setVisibility(View.GONE);
        } else if (isCompleted) {
            h.btnRegister.setVisibility(View.GONE);
        } else if (isRegistered) {
            h.btnRegister.setText("✓ Đã đăng ký");
            h.btnRegister.setEnabled(false);
            h.btnRegister.setAlpha(0.6f);
        } else {
            h.btnRegister.setText("Đăng ký tham gia");
            h.btnRegister.setEnabled(true);
            h.btnRegister.setAlpha(1f);
            h.btnRegister.setOnClickListener(v -> onRegister.onClick(e));
        }
    }

    @Override public int getItemCount() { return events.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvTitle, tvTargetGroup, tvDate,
                tvLocation, tvDesc, tvStatus;
        MaterialButton btnRegister;

        VH(View v) {
            super(v);
            tvEmoji       = v.findViewById(R.id.tvEventEmoji);
            tvTitle       = v.findViewById(R.id.tvEventTitle);
            tvTargetGroup = v.findViewById(R.id.tvEventTargetGroup);
            tvDate        = v.findViewById(R.id.tvEventDate);
            tvLocation    = v.findViewById(R.id.tvEventLocation);
            tvDesc        = v.findViewById(R.id.tvEventDesc);
            tvStatus      = v.findViewById(R.id.tvEventStatus);
            btnRegister   = v.findViewById(R.id.btnRegisterEvent);
        }
    }
}