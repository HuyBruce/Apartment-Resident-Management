package com.example.apartmentmanagement.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.models.ParkingRegistration;

import java.util.List;

public class ParkingAdapter extends RecyclerView.Adapter<ParkingAdapter.VH> {

    private final Context context;
    private final List<ParkingRegistration> list;

    public ParkingAdapter(Context ctx, List<ParkingRegistration> list) {
        this.context = ctx;
        this.list = list;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_parking, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ParkingRegistration p = list.get(pos);
        h.tvEmoji.setText("Xe máy".equals(p.getVehicle_type()) ? "🏍️" : "🚗");
        h.tvPlate.setText(p.getLicense_plate());
        h.tvModel.setText(p.getVehicle_model());

        if (p.isBlocked()) {
            h.tvStatus.setText("Bị khóa");
            h.tvStatus.setBackgroundResource(R.drawable.bg_badge_unpaid);
            h.tvStatus.setTextColor(0xFFE74C3C);
        } else {
            h.tvStatus.setText("Hoạt động");
            h.tvStatus.setBackgroundResource(R.drawable.bg_badge_green);
            h.tvStatus.setTextColor(0xFF0F6E56);
        }
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvPlate, tvModel, tvStatus;
        VH(View v) {
            super(v);
            tvEmoji  = v.findViewById(R.id.tvVehicleEmoji);
            tvPlate  = v.findViewById(R.id.tvLicensePlate);
            tvModel  = v.findViewById(R.id.tvVehicleModel);
            tvStatus = v.findViewById(R.id.tvParkingStatus);
        }
    }
}