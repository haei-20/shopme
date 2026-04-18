package com.example.gearshop.repository;

import com.example.gearshop.model.GioHangChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GioHangChiTietRepository extends JpaRepository<GioHangChiTiet, Integer> {
    List<GioHangChiTiet> findByGioHang_Id(Integer gioHangId);

    Optional<GioHangChiTiet> findByGioHang_IdAndSanPham_Id(Integer gioHangId, Integer sanPhamId);

    void deleteByGioHang_IdAndSanPham_Id(Integer gioHangId, Integer sanPhamId);

    void deleteBySanPham(com.example.gearshop.model.SanPham sanPham);
}