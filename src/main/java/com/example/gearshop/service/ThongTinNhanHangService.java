package com.example.gearshop.service;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.gearshop.model.ThongTinNhanHang;
import com.example.gearshop.repository.ThongTinNhanHangRepository;

@Service
public class ThongTinNhanHangService {

    @Autowired
    private ThongTinNhanHangRepository thongTinNhanHangRepository;

    public ThongTinNhanHang createThongTinNhanHang(int khachHangID, String tenNguoiNhan, String email, String sdt, String diachi) {
        ThongTinNhanHang thongTinNhanHang = new ThongTinNhanHang();
        thongTinNhanHang.setKhachHangID(khachHangID);
        thongTinNhanHang.setTenNguoiNhan(tenNguoiNhan);
        thongTinNhanHang.setEmail(email);
        thongTinNhanHang.setSdt(sdt);
        thongTinNhanHang.setDiachi(diachi);
        return thongTinNhanHangRepository.save(thongTinNhanHang);
    }

    public List<ThongTinNhanHang> getThongTinNhanHangByKhachHangID(int khachHangID) {
        return thongTinNhanHangRepository.findByKhachHangID(khachHangID);
    }

    public ThongTinNhanHang getThongTinNhanHangById(Integer id) {
        Integer safeId = Objects.requireNonNull(id, "ID thông tin nhận hàng không được để trống");
        return thongTinNhanHangRepository.findById(safeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin nhận hàng với ID: " + id));
    }

    public ThongTinNhanHang updateThongTinNhanHang(Integer id, String tenNguoiNhan, String email, String sdt, String diachi) {
        ThongTinNhanHang existing = getThongTinNhanHangById(id);
        existing.setTenNguoiNhan(tenNguoiNhan);
        existing.setEmail(email);
        existing.setSdt(sdt);
        existing.setDiachi(diachi);
        return thongTinNhanHangRepository.save(existing);
    }

    public void deleteThongTinNhanHang(Integer id) {
        ThongTinNhanHang existing = getThongTinNhanHangById(id);
        thongTinNhanHangRepository.delete(Objects.requireNonNull(existing));
    }
}
