package com.example.apartmentmanagement.adapters;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagement.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminRecordAdapter extends RecyclerView.Adapter<AdminRecordAdapter.VH> {

    public interface OnActionClick {
        void onAction(AdminRecord record, String action);
    }

    private final List<AdminRecord> records = new ArrayList<>();
    private final OnActionClick listener;

    public AdminRecordAdapter(OnActionClick listener) {
        this.listener = listener;
    }

    public void submitList(List<AdminRecord> items) {
        records.clear();
        records.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_record, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        AdminRecord record = records.get(position);
        holder.icon.setText(record.icon);
        holder.title.setText(record.title);
        holder.subtitle.setText(record.subtitle);
        holder.body.setText(record.body);
        holder.status.setText(record.status);

        if (record.positiveStatus) {
            holder.status.setBackgroundResource(R.drawable.bg_badge_paid);
            holder.status.setTextColor(0xFF2E7D32);
            holder.icon.setBackgroundResource(R.drawable.bg_avatar_green);
        } else {
            holder.status.setBackgroundResource(R.drawable.bg_badge_unpaid);
            holder.status.setTextColor(0xFFC62828);
            holder.icon.setBackgroundResource(R.drawable.bg_avatar_blue);
        }

        holder.actions.removeAllViews();
        holder.actions.setGravity(Gravity.END);
        holder.actions.setOrientation(LinearLayout.HORIZONTAL);

        for (int i = 0; i < record.actions.size(); i++) {
            String action = record.actions.get(i);
            MaterialButton button = createActionButton(holder.itemView, action);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(holder.itemView, 38)
            );

            if (i > 0) {
                params.setMargins(dp(holder.itemView, 8), 0, 0, 0);
            }

            button.setLayoutParams(params);
            button.setOnClickListener(v -> listener.onAction(record, action));
            holder.actions.addView(button);
        }

        holder.actions.setVisibility(record.actions.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private MaterialButton createActionButton(View parent, String action) {
        MaterialButton button = new MaterialButton(parent.getContext());
        String normalized = action == null ? "" : action.trim().toLowerCase(Locale.ROOT);

        button.setText(action);
        button.setTextSize(12);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);

        button.setCornerRadius(dp(parent, 12));
        button.setMinHeight(dp(parent, 38));
        button.setMinimumHeight(dp(parent, 38));
        button.setMinWidth(getMinButtonWidth(parent, normalized));
        button.setMinimumWidth(getMinButtonWidth(parent, normalized));

        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setInsetLeft(0);
        button.setInsetRight(0);
        button.setPadding(dp(parent, 14), 0, dp(parent, 14), 0);

        button.setTextColor(getTextColor(normalized));
        button.setBackgroundTintList(ColorStateList.valueOf(getBackgroundColor(normalized)));
        button.setStrokeWidth(dp(parent, 1));
        button.setStrokeColor(ColorStateList.valueOf(getStrokeColor(normalized)));

        return button;
    }

    private int getMinButtonWidth(View parent, String normalizedAction) {
        if (normalizedAction.startsWith("xác nhận")) {
            return dp(parent, 132);
        }
        if (normalizedAction.contains("nhắc")) {
            return dp(parent, 112);
        }
        if (normalizedAction.contains("từ chối") || normalizedAction.contains("hủy")) {
            return dp(parent, 92);
        }
        if (normalizedAction.contains("chặn")) {
            return dp(parent, 104);
        }
        if (normalizedAction.contains("duyệt")) {
            return dp(parent, 92);
        }
        if (normalizedAction.startsWith("xem") || normalizedAction.contains("ds đăng ký")) {
            return dp(parent, 104);
        }
        return dp(parent, 100);
    }

    private int getBackgroundColor(String normalizedAction) {
        if (normalizedAction.startsWith("xác nhận")) {
            return 0xFF173B70;
        }
        if (normalizedAction.contains("bỏ chặn")) {
            return 0xFF2E7D32;
        }
        if (normalizedAction.contains("duyệt")) {
            return 0xFF173B70;
        }
        if (normalizedAction.startsWith("xem")) {
            return 0xFF2F5F9E;
        }
        if (normalizedAction.contains("nhắc") || normalizedAction.contains("từ chối") || normalizedAction.contains("hủy") || normalizedAction.startsWith("chặn")) {
            return 0xFFD32F2F;
        }
        return 0xFF173B70;
    }

    private int getStrokeColor(String normalizedAction) {
        if (normalizedAction.startsWith("xác nhận")) {
            return 0xFF173B70;
        }
        if (normalizedAction.contains("bỏ chặn")) {
            return 0xFF2E7D32;
        }
        if (normalizedAction.contains("duyệt")) {
            return 0xFF173B70;
        }
        if (normalizedAction.startsWith("xem")) {
            return 0xFF2F5F9E;
        }
        if (normalizedAction.contains("nhắc") || normalizedAction.contains("từ chối") || normalizedAction.contains("hủy") || normalizedAction.startsWith("chặn")) {
            return 0xFFD32F2F;
        }
        return 0xFF173B70;
    }

    private int getTextColor(String normalizedAction) {
        return 0xFFFFFFFF;
    }

    private int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public static class AdminRecord {
        public String documentId;
        public String icon;
        public String title;
        public String subtitle;
        public String body;
        public String status;
        public boolean positiveStatus;
        public List<String> actions = new ArrayList<>();
        public Map<String, String> extras = new HashMap<>();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView icon, title, subtitle, body, status;
        LinearLayout actions;

        VH(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.tvAdminRecordIcon);
            title = itemView.findViewById(R.id.tvAdminRecordTitle);
            subtitle = itemView.findViewById(R.id.tvAdminRecordSubtitle);
            body = itemView.findViewById(R.id.tvAdminRecordBody);
            status = itemView.findViewById(R.id.tvAdminRecordStatus);
            actions = itemView.findViewById(R.id.layoutAdminRecordActions);
        }
    }
}
