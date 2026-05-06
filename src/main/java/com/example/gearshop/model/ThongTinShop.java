package com.example.gearshop.model;

import java.math.BigDecimal;

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
@Entity
@Table(name = "thongTinShop")
public class ThongTinShop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String tenShop;

    private String diaChiShop;

    private String soDienThoaiShop;

    private String emailShop;

    /** Ngưỡng miễn phí ship (VND). */
    private BigDecimal freeShipThreshold;

    /** Phí ship cơ bản (VND). */
    private BigDecimal shippingBaseFee;

    /** Phí ship theo mỗi km (VND/km). */
    private BigDecimal shippingFeePerKm;

    /** Mã giảm riêng cho phí ship. */
    private String shippingDiscountCode;

    /** Số tiền giảm phí ship khi nhập đúng mã (VND). */
    private BigDecimal shippingDiscountAmount;
}
