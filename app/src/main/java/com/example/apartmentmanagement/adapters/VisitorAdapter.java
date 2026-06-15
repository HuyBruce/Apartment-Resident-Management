package com.example.apartmentmanagement.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.models.Visitor;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class VisitorAdapter extends RecyclerView.Adapter<VisitorAdapter.VH> {

    public interface OnDeleteListener { void onDelete(Visitor v); }

    private final List<Visitor> list;
    private final Context ctx;
    private final OnDeleteListener listener;

    public VisitorAdapter(Context ctx, List<Visitor> list, OnDeleteListener listener) {
        this.ctx = ctx; this.list = list; this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_visitor, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Visitor v = list.get(position);

        // Avatar initials
        String name = v.getVisitor_name() != null ? v.getVisitor_name() : "?";
        h.tvInitial.setText(name.length() > 0 ? String.valueOf(name.charAt(0)).toUpperCase() : "?");

        h.tvName.setText(name);
        h.tvPhone.setText(v.getVisitor_phone() != null ? v.getVisitor_phone() : "—");
        h.tvPurpose.setText(v.getPurpose() != null ? v.getPurpose() : "—");
        h.tvDateTime.setText((v.getVisit_date() != null ? v.getVisit_date() : "") +
                (v.getVisit_time() != null ? "  •  " + v.getVisit_time() : ""));

        boolean approved = "approved".equals(v.getStatus());
        if (approved) {
            h.tvStatus.setText("✓  Đã duyệt");
            h.tvStatus.setBackgroundResource(R.drawable.bg_badge_paid);
            h.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_paid));
            h.tvInitial.setBackgroundResource(R.drawable.bg_avatar_green);
        } else {
            h.tvStatus.setText("⏳  Chờ duyệt");
            h.tvStatus.setBackgroundResource(R.drawable.bg_badge_unpaid);
            h.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_unpaid));
            h.tvInitial.setBackgroundResource(R.drawable.bg_avatar_blue);
        }

        h.btnDelete.setOnClickListener(x -> listener.onDelete(v));
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvInitial, tvName, tvPhone, tvPurpose, tvDateTime, tvStatus;
        MaterialButton btnDelete;
        VH(@NonNull View v) {
            super(v);
            tvInitial  = v.findViewById(R.id.tvVisitorInitial);
            tvName     = v.findViewById(R.id.tvVisitorName);
            tvPhone    = v.findViewById(R.id.tvVisitorPhone);
            tvPurpose  = v.findViewById(R.id.tvVisitorPurpose);
            tvDateTime = v.findViewById(R.id.tvVisitDateTime);
            tvStatus   = v.findViewById(R.id.tvVisitorStatus);
            btnDelete  = v.findViewById(R.id.btnDeleteVisitor);
        }
    }
}