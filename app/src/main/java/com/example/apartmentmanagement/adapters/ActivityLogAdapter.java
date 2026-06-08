package com.example.apartmentmanagement.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.models.ActivityLog;

import java.util.List;

public class ActivityLogAdapter extends RecyclerView.Adapter<ActivityLogAdapter.VH> {

    private final List<ActivityLog> list;
    private final Context ctx;

    public ActivityLogAdapter(Context ctx, List<ActivityLog> list) {
        this.ctx = ctx;
        this.list = list;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_activity_log, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ActivityLog log = list.get(position);

        // Icon + color based on action type
        String action = log.getAction() != null ? log.getAction() : "";
        String emoji;
        int bgColor;

        switch (action) {
            case "CREATE_REQUEST":
                emoji = "📋"; bgColor = 0xFFE3F2FD; break;
            case "PAY_FEE":
                emoji = "💰"; bgColor = 0xFFE8F5E9; break;
            case "REGISTER_VISITOR":
                emoji = "👥"; bgColor = 0xFFFFF3E0; break;
            case "UPDATE_PROFILE":
                emoji = "👤"; bgColor = 0xFFF3E5F5; break;
            case "UPDATE_REQUEST":
                emoji = "🔄"; bgColor = 0xFFE0F7FA; break;
            default:
                emoji = "📌"; bgColor = 0xFFF5F5F5; break;
        }

        h.tvEmoji.setText(emoji);
        h.tvEmojiBg.setBackgroundColor(bgColor);
        h.tvDescription.setText(log.getDescription() != null ? log.getDescription() : "—");
        h.tvTime.setText(log.getCreated_at() != null ? log.getCreated_at() : "—");

        // Timeline line — hide for last item
        h.viewLine.setVisibility(
                position == list.size() - 1 ? View.INVISIBLE : View.VISIBLE);
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvDescription, tvTime;
        View tvEmojiBg, viewLine;

        VH(@NonNull View v) {
            super(v);
            tvEmojiBg    = v.findViewById(R.id.viewEmojiBackground);
            tvEmoji      = v.findViewById(R.id.tvLogEmoji);
            tvDescription = v.findViewById(R.id.tvLogDescription);
            tvTime       = v.findViewById(R.id.tvLogTime);
            viewLine     = v.findViewById(R.id.viewTimelineLine);
        }
    }
}