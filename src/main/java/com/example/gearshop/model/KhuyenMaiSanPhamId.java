package com.example.gearshop.model;

import java.io.Serializable;
import java.util.Objects;

public class KhuyenMaiSanPhamId implements Serializable {

    private Integer sanPham;
    private Integer khuyenMai;

    public KhuyenMaiSanPhamId() {}

    public KhuyenMaiSanPhamId(Integer sanPham, Integer khuyenMai) {
        this.sanPham = sanPham;
        this.khuyenMai = khuyenMai;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KhuyenMaiSanPhamId that = (KhuyenMaiSanPhamId) o;
        return Objects.equals(sanPham, that.sanPham)
                && Objects.equals(khuyenMai, that.khuyenMai);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sanPham, khuyenMai);
    }
}