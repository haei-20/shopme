package com.example.gearshop.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.gearshop.model.LoaiSanPham;
import com.example.gearshop.repository.LoaiSanPhamRepository;

@Service
public class LoaiSanPhamService {

    @Autowired
    private LoaiSanPhamRepository loaiSanPhamRepository;

    // Tra ve toan bo danh muc san pham hien co trong he thong.
    public List<LoaiSanPham> findAll() {
        return loaiSanPhamRepository.findAll();
    }

    // Tim danh muc theo ten hien thi.
    public LoaiSanPham getByTenLoaiSanPham(String tenLoaiSanPham) {
        return loaiSanPhamRepository.findByTenLoaiSanPham(tenLoaiSanPham);
    }

    // Lay danh muc theo khoa chinh de phuc vu man hinh chi tiet/cap nhat.
    public LoaiSanPham getByIdLoaiSanPham(Integer maLoaiSanPham) {
        return loaiSanPhamRepository.findById(maLoaiSanPham);
    }
}