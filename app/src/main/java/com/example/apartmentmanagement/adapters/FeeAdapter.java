package com.example.apartmentmanagement.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.models.Fee;
import com.example.apartmentmanagement.utils.PenaltyCalculator;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class FeeAdapter extends RecyclerView.Adapter<FeeAdapter.VH> {

    public interface OnPayClick {
        void onClick(Fee fee);
    }

    private final Context context;
    private final List<Fee> fees;
    private final OnPayClick onPayClick;
    private final NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));

    public FeeAdapter(Context context, List<Fee> fees, OnPayClick onPayClick) {
        this.context = context;
        this.fees = fees;
        this.onPayClick = onPayClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_fee, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Fee fee = fees.get(pos);

        boolean isPaid = "paid".equals(fee.getStatus());

        int overdueDays = PenaltyCalculator.getOverdueDays(fee.getDue_date());
        boolean isOverdue = overdueDays > 0;

        long penaltyAmount = PenaltyCalculator.getPenaltyAmount(fee.getAmount(), overdueDays);
        long totalAmount = PenaltyCalculator.getTotalAmount(fee.getAmount(), overdueDays);
        String debtLevel = PenaltyCalculator.getDebtLevel(overdueDays);

        h.tvCategory.setText(fee.getCategory());
        h.tvDesc.setText(fee.getDescription());
        h.tvDueDate.setText("Hạn: " + fee.getDue_date());

        if (isPaid) {
            h.tvStatus.setText("Đã đóng");
            h.tvStatus.setBackgroundResource(R.drawable.bg_badge_paid);
            h.tvStatus.setTextColor(context.getColor(android.R.color.holo_green_dark));

            h.tvAmount.setText(fmt.format(fee.getAmount()) + " đ");
            h.tvAmount.setTextColor(context.getColor(android.R.color.holo_green_dark));

            h.layoutOverdue.setVisibility(View.GONE);
            h.tvTotalAmount.setVisibility(View.GONE);
            h.btnPay.setVisibility(View.GONE);

        } else if (isOverdue) {
            h.tvStatus.setText(debtLevel);
            h.tvStatus.setBackgroundResource(R.drawable.bg_badge_unpaid);
            h.tvStatus.setTextColor(0xFFC0392B);

            h.tvAmount.setText(fmt.format(fee.getAmount()) + " đ");
            h.tvAmount.setTextColor(0xFFC0392B);

            h.layoutOverdue.setVisibility(View.VISIBLE);
            h.tvOverdueDays.setText("Quá hạn " + overdueDays + " ngày");
            h.tvPenaltyAmount.setText("Tiền phạt: +" + fmt.format(penaltyAmount) + " đ");

            h.tvTotalAmount.setVisibility(View.VISIBLE);
            h.tvTotalAmount.setText("Tổng phải đóng: " + fmt.format(totalAmount) + " đ");

            h.btnPay.setVisibility(View.VISIBLE);
            h.btnPay.setOnClickListener(v -> onPayClick.onClick(fee));

        } else {
            h.tvStatus.setText("Chưa đóng");
            h.tvStatus.setBackgroundResource(R.drawable.bg_badge_unpaid);
            h.tvStatus.setTextColor(0xFFE74C3C);

            h.tvAmount.setText(fmt.format(fee.getAmount()) + " đ");
            h.tvAmount.setTextColor(0xFF1A2744);

            h.layoutOverdue.setVisibility(View.GONE);
            h.tvTotalAmount.setVisibility(View.GONE);

            h.btnPay.setVisibility(View.VISIBLE);
            h.btnPay.setOnClickListener(v -> onPayClick.onClick(fee));
        }
    }

    @Override
    public int getItemCount() {
        return fees.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCategory, tvDesc, tvStatus, tvAmount,
                tvDueDate, tvOverdueDays, tvPenaltyAmount, tvTotalAmount;
        LinearLayout layoutOverdue;
        MaterialButton btnPay;

        VH(View v) {
            super(v);
            tvCategory = v.findViewById(R.id.tvFeeCategory);
            tvDesc = v.findViewById(R.id.tvFeeDescription);
            tvStatus = v.findViewById(R.id.tvFeeStatus);
            tvAmount = v.findViewById(R.id.tvFeeAmount);
            tvDueDate = v.findViewById(R.id.tvFeeDueDate);
            tvOverdueDays = v.findViewById(R.id.tvOverdueDays);
            tvPenaltyAmount = v.findViewById(R.id.tvPenaltyAmount);
            tvTotalAmount = v.findViewById(R.id.tvFeeTotalAmount);
            layoutOverdue = v.findViewById(R.id.layoutOverdue);
            btnPay = v.findViewById(R.id.btnPayFee);
        }
    }
}