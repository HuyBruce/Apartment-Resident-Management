package com.example.apartmentmanagement.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.models.Complaint;

import java.util.List;

public class ComplaintAdapter extends RecyclerView.Adapter<ComplaintAdapter.VH> {

    private final Context context;
    private final List<Complaint> complaints;

    public ComplaintAdapter(Context ctx, List<Complaint> complaints) {
        this.context = ctx;
        this.complaints = complaints;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_complaint, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Complaint c = complaints.get(pos);

        h.tvCategory.setText(c.getCategoryLabel());
        h.tvTitle.setText(c.getTitle());
        h.tvDesc.setText(c.getDescription());
        h.tvDate.setText(c.getCreated_at());
        h.tvStatus.setText(c.getStatusLabel());

        // Status color
        switch (c.getStatus()) {
            case "resolved":
                h.tvStatus.setBackgroundResource(R.drawable.bg_badge_green);
                h.tvStatus.setTextColor(0xFF0F6E56);
                break;
            case "processing":
                h.tvStatus.setBackgroundResource(R.drawable.bg_badge_blue);
                h.tvStatus.setTextColor(0xFF185FA5);
                break;
            default:
                h.tvStatus.setBackgroundResource(R.drawable.bg_badge_unpaid);
                h.tvStatus.setTextColor(0xFFE74C3C);
        }

        // Image
        if (c.hasImage()) {
            try {
                byte[] bytes = Base64.decode(c.getImage_base64(), Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                h.ivImage.setImageBitmap(bmp);
                h.ivImage.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                h.ivImage.setVisibility(View.GONE);
            }
        } else {
            h.ivImage.setVisibility(View.GONE);
        }

        // Admin response
        if (c.getAdmin_response() != null && !c.getAdmin_response().isEmpty()) {
            h.layoutResponse.setVisibility(View.VISIBLE);
            h.tvResponse.setText(c.getAdmin_response());
        } else {
            h.layoutResponse.setVisibility(View.GONE);
        }
    }

    @Override public int getItemCount() { return complaints.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCategory, tvTitle, tvDesc, tvStatus, tvDate, tvResponse;
        ImageView ivImage;
        LinearLayout layoutResponse;

        VH(View v) {
            super(v);
            tvCategory     = v.findViewById(R.id.tvComplaintCategory);
            tvTitle        = v.findViewById(R.id.tvComplaintTitle);
            tvDesc         = v.findViewById(R.id.tvComplaintDesc);
            tvStatus       = v.findViewById(R.id.tvComplaintStatus);
            tvDate         = v.findViewById(R.id.tvComplaintDate);
            ivImage        = v.findViewById(R.id.ivComplaintImage);
            layoutResponse = v.findViewById(R.id.layoutAdminResponse);
            tvResponse     = v.findViewById(R.id.tvAdminResponse);
        }
    }
}