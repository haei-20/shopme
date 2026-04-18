package com.example.gearshop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "hoadon")
public class HoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String maHoaDon;

    @ManyToOne
    @JoinColumn(name = "thongTinNhanHangID", nullable = false)
    private ThongTinNhanHang thongTinNhanHang;

    private LocalDateTime ngayTao;

    private BigDecimal tongGia;

    private String trangThaiDonHang;

    private String phuongThucThanhToan;
    private Integer nguoiDungId;

    // Getters và Setters
}
