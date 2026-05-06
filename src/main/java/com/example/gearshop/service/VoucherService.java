package com.example.gearshop.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.gearshop.model.KhachHang;
import com.example.gearshop.model.Voucher;
import com.example.gearshop.model.VoucherKhachHang;
import com.example.gearshop.repository.VoucherKhachHangRepository;
import com.example.gearshop.repository.VoucherRepository;

import jakarta.transaction.Transactional;

@Service
public class VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;
    @Autowired
    private VoucherKhachHangRepository voucherKhachHangRepository;

    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAll();
    }

    /**
     * Voucher có thể chọn trên trang đặt hàng: còn hiệu lực, áp dụng được cho KH này, có cấu hình giảm giá.
     */
    public List<Voucher> getVouchersEligibleForCheckout(KhachHang khachHang) {
        List<Voucher> out = new ArrayList<>();
        for (Voucher v : voucherRepository.findAll()) {
            try {
                validateVoucherAvailability(v);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            if (!isVoucherAvailableForCustomer(v, khachHang)) {
                continue;
            }
            try {
                assertCoCauHinhGiamGia(v);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            out.add(v);
        }
        return out;
    }

    public void assertCoCauHinhGiamGia(Voucher voucher) {
        boolean coPt = voucher.getGiamGiaTheoPhanTram() != null && voucher.getGiamGiaTheoPhanTram() > 0;
        boolean coTien = voucher.getGiamGiaCuThe() != null
                && voucher.getGiamGiaCuThe().compareTo(BigDecimal.ZERO) > 0;
        if (!coPt && !coTien) {
            throw new IllegalArgumentException("Voucher chưa được cấu hình mức giảm giá hợp lệ.");
        }
    }

    public void assertDonHangDuDieuKien(Voucher voucher, double tongGiaTruocGiam) {
        if (voucher.getDonToiThieu() == null) {
            return;
        }
        if (tongGiaTruocGiam + 1e-6 < voucher.getDonToiThieu().doubleValue()) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng voucher ("
                    + voucher.getDonToiThieu().stripTrailingZeros().toPlainString() + " ₫).");
        }
    }

    /** Số tiền giảm (VND), không vượt quá tổng đơn trước giảm. */
    public double tinhSoTienGiam(Voucher voucher, double tongGiaTruocGiam) {
        if (voucher.getGiamGiaTheoPhanTram() != null && voucher.getGiamGiaTheoPhanTram() > 0) {
            double giam = tongGiaTruocGiam * voucher.getGiamGiaTheoPhanTram() / 100.0;
            if (voucher.getGiamGiaToiDa() != null) {
                giam = Math.min(giam, voucher.getGiamGiaToiDa().doubleValue());
            }
            return Math.min(giam, tongGiaTruocGiam);
        }
        if (voucher.getGiamGiaCuThe() != null) {
            return Math.min(Math.max(0, voucher.getGiamGiaCuThe().doubleValue()), tongGiaTruocGiam);
        }
        return 0;
    }

    public Voucher getVoucherByMaVoucher(String maVoucher) {
        Voucher voucher = voucherRepository.findByMaVoucher(maVoucher)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy voucher với mã: " + maVoucher));
        validateVoucherAvailability(voucher);
        return voucher;
    }

    public void validateVoucherAvailability(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getKichHoat() != null && !voucher.getKichHoat()) {
            throw new IllegalArgumentException("Voucher đang tạm tắt.");
        }
        if (voucher.getNgayBatDau() != null && now.isBefore(voucher.getNgayBatDau())) {
            throw new IllegalArgumentException("Voucher chưa đến thời gian bắt đầu.");
        }
        if (voucher.getThoiHan() != null && now.isAfter(voucher.getThoiHan())) {
            throw new IllegalArgumentException("Voucher đã hết hạn.");
        }
        if (voucher.getSoLuongNguoiDungToiDa() != null) {
            long daDung = voucherKhachHangRepository.countByVoucherIdAndDaDungTrue(voucher.getId());
            if (daDung >= voucher.getSoLuongNguoiDungToiDa()) {
                throw new IllegalArgumentException("Voucher đã đủ số lượng người sử dụng.");
            }
        }
    }

    public boolean isVoucherAvailableForCustomer(Voucher voucher, KhachHang khachHang) {
        if (khachHang == null) {
            return false;
        }
        VoucherKhachHang mapping = voucherKhachHangRepository
                .findByVoucherIdAndKhachHang_Id(voucher.getId(), khachHang.getId())
                .orElse(null);
        List<VoucherKhachHang> mappings = voucherKhachHangRepository.findByVoucherId(voucher.getId());
        if (mappings != null && !mappings.isEmpty()) {
            return mapping != null && !Boolean.TRUE.equals(mapping.getDaDung());
        }
        return true;
    }

    public void markVoucherAsUsed(Voucher voucher, KhachHang khachHang) {
        VoucherKhachHang mapping = voucherKhachHangRepository
                .findByVoucherIdAndKhachHang_Id(voucher.getId(), khachHang.getId())
                .orElseGet(() -> {
                    VoucherKhachHang created = new VoucherKhachHang();
                    created.setVoucher(voucher);
                    created.setKhachHang(khachHang);
                    created.setMaVoucherKhachHang("VCKH" + voucher.getId() + khachHang.getId());
                    return created;
                });
        mapping.setDaDung(true);
        voucherKhachHangRepository.save(mapping);
    }

    @Transactional
    public void xoaVoucherVaLienQuan(int id) {
        // 1. Xoá VoucherKhachHang trước
        voucherKhachHangRepository.deleteByVoucherID(id);

        // 2. Xoá Voucher
        voucherRepository.deleteById(id);
    }
}
