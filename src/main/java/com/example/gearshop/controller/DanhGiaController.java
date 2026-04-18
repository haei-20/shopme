package com.example.gearshop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.gearshop.model.KhachHang;
import com.example.gearshop.model.NguoiDung;
import com.example.gearshop.service.DanhGiaService;

import jakarta.servlet.http.HttpSession;

@Controller
public class DanhGiaController {

    @Autowired
    private DanhGiaService danhGiaService;

    @PostMapping("/danhgia")
    public String guiDanhGia(
            @RequestParam Integer sanPhamId,
            @RequestParam int soSao,
            @RequestParam String noiDung,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoiDung");
        if (nguoiDung == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để đánh giá sản phẩm.");
            return "redirect:/dangnhap";
        }

        KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
        if (khachHang == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ tài khoản khách hàng mới có thể đánh giá.");
            return "redirect:/chitietsanpham/" + sanPhamId;
        }

        try {
            danhGiaService.luuHoacCapNhat(khachHang, sanPhamId, soSao, noiDung);
            redirectAttributes.addFlashAttribute("successMessage", "Cảm ơn bạn đã đánh giá sản phẩm!");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/chitietsanpham/" + sanPhamId + "#danh-gia";
    }
}
