package com.example.gearshop.service;

import java.time.LocalDateTime;
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
