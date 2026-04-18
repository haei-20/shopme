package com.example.gearshop.model;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class GioHangChiTiet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String maGioHangChiTiet;
    private int soLuong;
    private BigDecimal donGia;
    
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean duocChonThanhToan = false;

    @ManyToOne
    @JoinColumn(name = "gioHangID")
    private GioHang gioHang;

    @ManyToOne
    @JoinColumn(name = "sanPhamID")
    private SanPham sanPham;
}
