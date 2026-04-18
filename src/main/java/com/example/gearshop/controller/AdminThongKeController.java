package com.example.gearshop.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.gearshop.service.ThongKeService;

@Controller
@RequestMapping("/admin/thongke")
public class AdminThongKeController {
    @Autowired
    private ThongKeService thongKeService;

    @GetMapping
    public String thongKe(Model model) {
        Map<String, BigDecimal> doanhThuNgay = thongKeService.getDoanhThuTheoNgay();
        Map<String, BigDecimal> doanhThuThang = thongKeService.getDoanhThuTheoThang();
        Map<String, BigDecimal> doanhThuLoaiSP = thongKeService.getDoanhThuTheoLoaiSanPham();
        BigDecimal tongDoanhThu = doanhThuNgay.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("doanhThuNgay", doanhThuNgay);
        model.addAttribute("doanhThuThang", doanhThuThang);
        model.addAttribute("doanhThuLoaiSP", doanhThuLoaiSP);
        model.addAttribute("tongDoanhThu", tongDoanhThu);
        model.addAttribute("soNgayThongKe", doanhThuNgay.size());
        model.addAttribute("soThangThongKe", doanhThuThang.size());
        model.addAttribute("soLoaiSanPham", doanhThuLoaiSP.size());

        return "adminTemplate/thongke";
    }
}
