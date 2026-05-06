package com.example.gearshop.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.example.gearshop.dto.SanPhamTrongHoaDonDTO;
import com.example.gearshop.model.HoaDon;
import com.example.gearshop.model.KhachHang;
import com.example.gearshop.model.NguoiDung;
import com.example.gearshop.service.DanhGiaService;
import com.example.gearshop.service.HoaDonService;

import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GiaoDichController {

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private DanhGiaService danhGiaService;

    @GetMapping("/lichsugiaodich")
    public String lichSuGiaoDich(HttpSession session,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoiDung");
        if (nguoiDung == null) {
            return "redirect:/dangnhap";
        }

        KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
        List<HoaDon> hoaDoncuaKhachHang;
        int totalPages = 0;
        long totalItems = 0;
        if (khachHang == null) {
            hoaDoncuaKhachHang = Collections.emptyList();
        } else {
            // Theo khachHangID trên ThongTinNhanHang, có phân trang bằng Pageable.
            Page<HoaDon> hoaDonPage = hoaDonService.getHoaDonsPageByKhachHangID(khachHang.getId(), sortBy, trangThai,
                    page, size);
            hoaDoncuaKhachHang = hoaDonPage.getContent();
            totalPages = hoaDonPage.getTotalPages();
            totalItems = hoaDonPage.getTotalElements();
        }
        model.addAttribute("hoaDonKhachHang", hoaDoncuaKhachHang);
        model.addAttribute("selectedSort", sortBy);
        model.addAttribute("selectedTrangThai", trangThai);
        model.addAttribute("currentPage", Math.max(page, 0));
        model.addAttribute("pageSize", Math.max(size, 1));
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);

        return "clientTemplate/lichsugiaodich";
    }

    /**
     * Trang xác nhận đặt hàng thành công (giống TMĐT: tóm tắt đơn, bước tiếp theo COD / chuyển khoản).
     */
    @GetMapping("/dat-hang-thanh-cong")
    public String datHangThanhCong(@RequestParam(required = false) Integer hoaDonId,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoiDung");
        if (nguoiDung == null) {
            return "redirect:/dangnhap";
        }
        model.addAttribute("nguoiDung", nguoiDung);

        if (hoaDonId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thiếu mã đơn hàng.");
            return "redirect:/lichsugiaodich";
        }

        KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
        if (khachHang == null || !hoaDonService.belongsToKhachHang(hoaDonId, khachHang.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng hoặc bạn không có quyền xem.");
            return "redirect:/lichsugiaodich";
        }

        HoaDon hoaDon = hoaDonService.getHoaDonById(hoaDonId);
        model.addAttribute("hoaDon", hoaDon);
        return "clientTemplate/dathangthanhcong";
    }

    @GetMapping("/lichsugiaodich/{id}")
    public String chiTietGiaoDich(@PathVariable Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoiDung");
        if (nguoiDung == null) {
            return "redirect:/dangnhap";
        }

        KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
        if (khachHang == null || !hoaDonService.belongsToKhachHang(id, khachHang.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không tìm thấy đơn hàng hoặc bạn không có quyền xem.");
            return "redirect:/lichsugiaodich";
        }

        HoaDon hoaDon = hoaDonService.getHoaDonById(id);

        List<SanPhamTrongHoaDonDTO> danhSachSanPham = hoaDonService.getSanPhamTrongHoaDon(id);

        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("danhSachSanPham", danhSachSanPham);

        Map<Integer, Boolean> coTheDanhGiaSanPham = new HashMap<>();
        Map<Integer, Boolean> daDanhGiaSanPham = new HashMap<>();
        for (var item : danhSachSanPham) {
            int spId = item.getSanPham() != null ? item.getSanPham().getId() : item.getHoaDonChiTiet().getSanPhamID();
            coTheDanhGiaSanPham.put(spId, danhGiaService.duocPhepDanhGia(khachHang.getId(), spId));
            daDanhGiaSanPham.put(spId, danhGiaService.daCoDanhGia(khachHang.getId(), spId));
        }
        model.addAttribute("coTheDanhGiaSanPham", coTheDanhGiaSanPham);
        model.addAttribute("daDanhGiaSanPham", daDanhGiaSanPham);

        return "clientTemplate/chitietgiaodich";
    }
}
