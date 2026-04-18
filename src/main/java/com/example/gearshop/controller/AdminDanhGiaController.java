package com.example.gearshop.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.gearshop.model.DanhGia;
import com.example.gearshop.model.SanPham;
import com.example.gearshop.service.DanhGiaService;
import com.example.gearshop.service.SanPhamService;

@Controller
@RequestMapping("/admin/danhgia")
public class AdminDanhGiaController {

    @Autowired
    private DanhGiaService danhGiaService;

    @Autowired
    private SanPhamService sanPhamService;

    @GetMapping
    public String danhSach(
            @RequestParam(required = false) Integer sanPhamId,
            @RequestParam(required = false) String khachHang,
            @RequestParam(required = false) Integer soSao,
            @RequestParam(required = false) LocalDate tuNgay,
            @RequestParam(required = false) LocalDate denNgay,
            Model model) {

        List<DanhGia> danhGias = danhGiaService.adminLayDanhSach(
                sanPhamId,
                khachHang != null ? khachHang : "",
                soSao,
                tuNgay,
                denNgay);
        model.addAttribute("danhGias", danhGias);
        model.addAttribute("sanPhamIdLoc", sanPhamId);
        List<SanPham> dsSanPhamLoc = new ArrayList<>(sanPhamService.getAllSanPham());
        dsSanPhamLoc.sort(Comparator.comparing(SanPham::getId));
        model.addAttribute("dsSanPhamLoc", dsSanPhamLoc);
        model.addAttribute("khachHangLoc", khachHang != null ? khachHang : "");
        model.addAttribute("soSaoLoc", soSao);
        model.addAttribute("tuNgayLoc", tuNgay);
        model.addAttribute("denNgayLoc", denNgay);
        return "adminTemplate/quanlydanhgia";
    }

    @PostMapping("/xoa/{id}")
    public String xoa(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            danhGiaService.adminXoa(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa đánh giá.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/danhgia";
    }

    @PostMapping("/phanhoi/{id}")
    public String phanHoi(@PathVariable Integer id,
            @RequestParam(required = false) String phanHoi,
            @RequestParam(required = false) Integer redirectSanPhamId,
            RedirectAttributes redirectAttributes) {
        try {
            danhGiaService.adminPhanHoi(id, phanHoi);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu phản hồi cho đánh giá.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        if (redirectSanPhamId != null) {
            return "redirect:/chitietsanpham/" + redirectSanPhamId + "#danh-gia-" + id;
        }
        return "redirect:/admin/danhgia";
    }
}
