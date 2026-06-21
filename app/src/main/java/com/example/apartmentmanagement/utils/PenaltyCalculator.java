package com.example.apartmentmanagement.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PenaltyCalculator {

    public static int getOverdueDays(String dueDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            Date dueDate = sdf.parse(dueDateStr);
            Date today = new Date();

            if (dueDate == null || !today.after(dueDate)) {
                return 0;
            }

            long diff = today.getTime() - dueDate.getTime();

            return (int) (diff / (1000 * 60 * 60 * 24));

        } catch (ParseException e) {
            return 0;
        }
    }

    public static long getPenaltyAmount(long originalAmount, int overdueDays) {

        if (overdueDays <= 7) {
            return 0;
        }

        if (overdueDays <= 30) {
            return (long) (originalAmount * 0.10);
        }

        if (overdueDays <= 60) {
            return (long) (originalAmount * 0.25);
        }

        if (overdueDays <= 120) {
            return (long) (originalAmount * 0.50);
        }

        return (long) (originalAmount * 1.00);
    }
    public static long getTotalAmount(long originalAmount, int overdueDays) {
        return originalAmount + getPenaltyAmount(originalAmount, overdueDays);
    }

    public static String getDebtLevel(int overdueDays) {

        if (overdueDays <= 0) {
            return "Bình thường";
        }

        if (overdueDays <= 7) {
            return "Nhắc nhở";
        }

        if (overdueDays <= 30) {
            return "Quá hạn";
        }

        if (overdueDays <= 60) {
            return "Công nợ";
        }

        if (overdueDays <= 120) {
            return "Hạn chế dịch vụ";
        }

        return "Xử lý công nợ";
    }

    public static String getReminderMessage(int overdueDays) {

        if (overdueDays <= 0) {
            return "Khoản phí đang trong thời hạn thanh toán.";
        }

        if (overdueDays <= 7) {
            return "Vui lòng thanh toán khoản phí trong thời gian sớm nhất.";
        }

        if (overdueDays <= 30) {
            return "Khoản phí đã quá hạn. Tiền phạt đã bắt đầu được áp dụng.";
        }

        if (overdueDays <= 60) {
            return "Khoản phí đang ở trạng thái công nợ. Ban quản lý sẽ liên hệ nhắc nợ.";
        }

        if (overdueDays <= 120) {
            return "Tài khoản đang bị hạn chế một số dịch vụ do công nợ kéo dài.";
        }

        return "Khoản công nợ đã được chuyển sang quy trình xử lý công nợ.";
    }

    public static boolean isServiceRestricted(int overdueDays) {
        return overdueDays > 60;
    }

    public static boolean isDebtCollectionStage(int overdueDays) {
        return overdueDays > 120;
    }
}