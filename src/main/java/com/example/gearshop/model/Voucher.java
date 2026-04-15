package com.example.gearshop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "voucher")
@Entity
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String maVoucher;

    private String tenVoucher;

    private String moTa;

    private Boolean kichHoat;

    private Integer giamGiaTheoPhanTram;

    private BigDecimal giamGiaCuThe;

    private BigDecimal giamGiaToiDa;

    private LocalDateTime ngayBatDau;

    private LocalDateTime thoiHan;

    private Integer soLuongNguoiDungToiDa;

    private BigDecimal donToiThieu;

    // Getters và Setters
}
