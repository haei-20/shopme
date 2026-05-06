package com.example.gearshop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gearshop.model.LichSuXemSanPham;

public interface LichSuXemSanPhamRepository extends JpaRepository<LichSuXemSanPham, Long> {
    Optional<LichSuXemSanPham> findByKhachHang_IdAndSanPham_Id(Integer khachHangId, Integer sanPhamId);

    List<LichSuXemSanPham> findByKhachHang_IdOrderByThoiGianXemDesc(Integer khachHangId);
}
