package com.example.gearshop.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.gearshop.model.KhachHang;
import com.example.gearshop.model.LichSuXemSanPham;
import com.example.gearshop.model.SanPham;
import com.example.gearshop.repository.LichSuXemSanPhamRepository;

@Service
public class LichSuXemSanPhamService {
    @Autowired
    private LichSuXemSanPhamRepository lichSuXemSanPhamRepository;

    public void ghiNhanLuotXem(KhachHang khachHang, Integer sanPhamId) {
        if (khachHang == null || khachHang.getId() == null || sanPhamId == null) {
            return;
        }
        LichSuXemSanPham ls = lichSuXemSanPhamRepository
                .findByKhachHang_IdAndSanPham_Id(khachHang.getId(), sanPhamId)
                .orElseGet(LichSuXemSanPham::new);
        ls.setKhachHang(khachHang);
        SanPham sanPham = new SanPham();
        sanPham.setId(sanPhamId);
        ls.setSanPham(sanPham);
        ls.setThoiGianXem(LocalDateTime.now());
        lichSuXemSanPhamRepository.save(ls);
    }

    public List<Integer> layDanhSachIdDaXemGanNhat(Integer khachHangId, int limit) {
        if (khachHangId == null || limit <= 0) {
            return List.of();
        }
        List<LichSuXemSanPham> rows = lichSuXemSanPhamRepository.findByKhachHang_IdOrderByThoiGianXemDesc(khachHangId);
        List<Integer> ids = new ArrayList<>();
        for (LichSuXemSanPham row : rows) {
            if (row.getSanPham() == null || row.getSanPham().getId() == null) {
                continue;
            }
            Integer id = row.getSanPham().getId();
            if (!ids.contains(id)) {
                ids.add(id);
            }
            if (ids.size() >= limit) {
                break;
            }
        }
        return ids;
    }
}
