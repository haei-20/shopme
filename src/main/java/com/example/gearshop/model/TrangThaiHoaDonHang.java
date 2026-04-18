package com.example.gearshop.model;

import java.util.List;

/**
 * Trạng thái xử lý đơn hàng (lưu trong {@link HoaDon#trangThaiDonHang}).
 */
public final class TrangThaiHoaDonHang {

    private TrangThaiHoaDonHang() {
    }

    public static final String CHO_XAC_NHAN = "Chờ xác nhận";
    public static final String DANG_CHUAN_BI_HANG = "Đang chuẩn bị hàng";
    public static final String CHO_GIAO_HANG = "Chờ giao hàng";
    public static final String DA_GIAO = "Đã giao";
    public static final String TRA_HANG = "Trả hàng";
    public static final String DA_HUY = "Đã hủy";

    /** Dùng cho dropdown lọc (không lưu DB). */
    public static List<String> danhSachLoc() {
        return List.of(
                CHO_XAC_NHAN,
                DANG_CHUAN_BI_HANG,
                CHO_GIAO_HANG,
                DA_GIAO,
                TRA_HANG,
                DA_HUY);
    }

    /* --- Giá trị cũ — chỉ để ánh xạ khi lọc & hiển thị --- */
    public static final String LEGACY_CHO_LAY_HANG = "Chờ lấy hàng";
    public static final String LEGACY_UNPAID = "Unpaid";
    public static final String LEGACY_PAID = "Paid";
    public static final String LEGACY_MISSING = "Missing";
    public static final String LEGACY_EXTRA = "Extra";
    public static final String LEGACY_CHUA_TT = "Chưa thanh toán";
    public static final String LEGACY_DU = "Thanh toán đủ";
    public static final String LEGACY_THIEU = "Thanh toán thiếu";
    public static final String LEGACY_THUA = "Thanh toán thừa";
}
