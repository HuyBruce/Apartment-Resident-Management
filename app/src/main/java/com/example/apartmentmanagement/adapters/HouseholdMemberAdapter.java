package com.example.apartmentmanagement.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.models.HouseholdMember;

import java.util.List;

public class HouseholdMemberAdapter
        extends RecyclerView.Adapter<HouseholdMemberAdapter.VH> {

    public interface OnDeleteClick { void onClick(HouseholdMember member); }

    private final Context context;
    private final List<HouseholdMember> members;
    private final OnDeleteClick onDelete;

    public HouseholdMemberAdapter(Context ctx,
                                  List<HouseholdMember> members,
                                  OnDeleteClick onDelete) {
        this.context  = ctx;
        this.members  = members;
        this.onDelete = onDelete;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_household_member, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        HouseholdMember m = members.get(pos);
        h.tvEmoji.setText(m.getAgeGroupEmoji());
        h.tvName.setText(m.getFull_name());
        h.tvRelationship.setText(m.getRelationship());
        h.tvAge.setText(m.getAge() + " tuổi");
        h.tvAgeGroup.setText(m.getAgeGroup());

        // Ẩn nút xóa với Chủ hộ
        if ("Chủ hộ".equals(m.getRelationship())) {
            h.btnDelete.setVisibility(View.GONE);
        } else {
            h.btnDelete.setVisibility(View.VISIBLE);
            h.btnDelete.setOnClickListener(v -> onDelete.onClick(m));
        }
    }

    @Override public int getItemCount() { return members.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvName, tvRelationship, tvAge, tvAgeGroup;
        ImageButton btnDelete;

        VH(View v) {
            super(v);
            tvEmoji        = v.findViewById(R.id.tvMemberEmoji);
            tvName         = v.findViewById(R.id.tvMemberName);
            tvRelationship = v.findViewById(R.id.tvMemberRelationship);
            tvAge          = v.findViewById(R.id.tvMemberAge);
            tvAgeGroup     = v.findViewById(R.id.tvMemberAgeGroup);
            btnDelete      = v.findViewById(R.id.btnDeleteMember);
        }
    }
}