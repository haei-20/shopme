package com.example.gearshop.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class KhuyenMai {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String maKM;
    private String tenKM;
    private int giamGiaPhanTram;
    private BigDecimal giamGiaCuThe;
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;
    private String mota;
}
