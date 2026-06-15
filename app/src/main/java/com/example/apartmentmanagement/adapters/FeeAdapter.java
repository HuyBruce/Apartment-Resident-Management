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
import com.example.apartmentmanagement.models.Fee;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class FeeAdapter extends RecyclerView.Adapter<FeeAdapter.FeeViewHolder> {

    public interface OnPayClickListener {
        void onPayClick(Fee fee);
    }

    private final List<Fee> feeList;
    private final Context context;
    private final OnPayClickListener listener;

    public FeeAdapter(Context context, List<Fee> feeList, OnPayClickListener listener) {
        this.context = context;
        this.feeList = feeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_fee, parent, false);
        return new FeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeeViewHolder holder, int position) {
        Fee fee = feeList.get(position);

        holder.tvCategory.setText(fee.getCategory());
        holder.tvDescription.setText(fee.getDescription());
        holder.tvDueDate.setText("Hạn: " + fee.getDue_date());

        // Format tiền VNĐ
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        holder.tvAmount.setText(fmt.format(fee.getAmount()) + " đ");

        boolean isPaid = "paid".equals(fee.getStatus());

        // Badge trạng thái
        if (isPaid) {
            holder.tvStatus.setText("Đã đóng");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_paid);
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_paid));
            holder.btnPay.setVisibility(View.GONE);
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.status_paid));
        } else {
            holder.tvStatus.setText("Chưa đóng");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_unpaid);
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_unpaid));
            holder.btnPay.setVisibility(View.VISIBLE);
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.status_unpaid));
            holder.btnPay.setOnClickListener(v -> listener.onPayClick(fee));
        }
    }

    @Override
    public int getItemCount() { return feeList.size(); }

    static class FeeViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvDescription, tvDueDate, tvAmount, tvStatus;
        MaterialButton btnPay;

        FeeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory    = itemView.findViewById(R.id.tvFeeCategory);
            tvDescription = itemView.findViewById(R.id.tvFeeDescription);
            tvDueDate     = itemView.findViewById(R.id.tvFeeDueDate);
            tvAmount      = itemView.findViewById(R.id.tvFeeAmount);
            tvStatus      = itemView.findViewById(R.id.tvFeeStatus);
            btnPay        = itemView.findViewById(R.id.btnPay);
        }
    }
}