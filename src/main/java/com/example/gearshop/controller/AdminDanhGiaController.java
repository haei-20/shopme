package com.example.gearshop.controller;

import java.util.List;

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
import com.example.gearshop.service.DanhGiaService;

@Controller
@RequestMapping("/admin/danhgia")
public class AdminDanhGiaController {

    @Autowired
    private DanhGiaService danhGiaService;

    @GetMapping
    public String danhSach(
            @RequestParam(required = false) Integer sanPhamId,
            @RequestParam(required = false) String tuKhoa,
            Model model) {

        List<DanhGia> danhGias = danhGiaService.adminLayDanhSach(sanPhamId, tuKhoa != null ? tuKhoa : "");
        model.addAttribute("danhGias", danhGias);
        model.addAttribute("sanPhamIdLoc", sanPhamId);
        model.addAttribute("tuKhoa", tuKhoa != null ? tuKhoa : "");
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
}
