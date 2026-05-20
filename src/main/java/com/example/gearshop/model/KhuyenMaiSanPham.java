package com.example.gearshop.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@IdClass(KhuyenMaiSanPhamId.class)
public class KhuyenMaiSanPham {

    @Id
    @ManyToOne
    @JoinColumn(name = "sanPhamID")
    private SanPham sanPham;

    @Id
    @ManyToOne
    @JoinColumn(name = "khuyenMaiID")
    private KhuyenMai khuyenMai;

    public KhuyenMaiSanPham() {}

    public KhuyenMaiSanPham(SanPham sanPham, KhuyenMai khuyenMai) {
        this.sanPham = sanPham;
        this.khuyenMai = khuyenMai;
    }

    public SanPham getSanPham() {
        return sanPham;
    }

    public void setSanPham(SanPham sanPham) {
        this.sanPham = sanPham;
    }

    public KhuyenMai getKhuyenMai() {
        return khuyenMai;
    }

    public void setKhuyenMai(KhuyenMai khuyenMai) {
        this.khuyenMai = khuyenMai;
    }
}