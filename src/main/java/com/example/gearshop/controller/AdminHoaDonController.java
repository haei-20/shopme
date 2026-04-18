package com.example.gearshop.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.gearshop.model.HoaDon;
import com.example.gearshop.model.TrangThaiHoaDonHang;
import com.example.gearshop.service.HoaDonAdminService;
import com.example.gearshop.service.HoaDonService;

@Controller
@RequestMapping("/admin/hoadon")
public class AdminHoaDonController {
    @Autowired
    private HoaDonAdminService hoaDonService;

    @GetMapping
    public String danhSachHoaDon(
            @RequestParam(value = "sortOrder", required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(value = "tenKhachHang", required = false) String tenKhachHang,
            Model model) {

        List<HoaDon> hoaDons;

        if (tenKhachHang != null && !tenKhachHang.isEmpty()) {
            hoaDons = hoaDonService.getHoaDonByTenKhachHang(tenKhachHang);
        } else {
            hoaDons = hoaDonService.getAllHoaDonsSorted(sortOrder);
        }

        model.addAttribute("hoaDons", hoaDons);
        model.addAttribute("sortOrder", sortOrder);
        model.addAttribute("tenKhachHang", tenKhachHang);
        return "adminTemplate/hoadon";
    }

    @GetMapping("/chitiet/{id}")
    public String chiTietHoaDon(@PathVariable Integer id, Model model) {
        HoaDon hoaDon = hoaDonService.getHoaDonById(id);
        String tenKhachHang = hoaDonService
                .getTenKhachHangByThongTinNhanHangID(hoaDon.getThongTinNhanHang().getKhachHangID());
        List<Map<String, Object>> sanPhamTrongHoaDon = hoaDonService.getSanPhamTrongHoaDon(id);

        String chuan = HoaDonService.chuanHoaTrangThai(hoaDon.getTrangThaiDonHang());
        boolean trongDanhSach = chuan != null && !chuan.isBlank()
                && TrangThaiHoaDonHang.danhSachLoc().contains(chuan);

        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("tenKhachHang", tenKhachHang);
        model.addAttribute("sanPhamList", sanPhamTrongHoaDon);
        model.addAttribute("cacTrangThaiDon", TrangThaiHoaDonHang.danhSachLoc());
        model.addAttribute("trangThaiChuan", trongDanhSach ? chuan : null);
        return "adminTemplate/hoadonchitiet";
    }

    /** Form admin: cập nhật trạng thái vận đơn. */
    @PostMapping(value = "/chitiet/{id}/trangthai", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String capNhatTrangThaiForm(@PathVariable Integer id,
            @RequestParam("trangThaiDonHang") String trangThaiDonHang,
            RedirectAttributes redirectAttributes) {
        try {
            hoaDonService.capNhatTrangThaiDonHang(id, trangThaiDonHang);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đơn hàng.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/hoadon/chitiet/" + id;
    }

    /** API JSON (tích hợp ngoài / script): POST body {@code {"trangThaiDonHang":"Chờ giao hàng"}} */
    @PostMapping(value = "/chitiet/{id}/trangthai", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> capNhatTrangThaiJson(@PathVariable Integer id,
            @RequestBody(required = false) CapNhatTrangThaiDonBody body) {
        try {
            String tt = body != null ? body.trangThaiDonHang() : null;
            hoaDonService.capNhatTrangThaiDonHang(id, tt);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã cập nhật trạng thái đơn hàng."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    public record CapNhatTrangThaiDonBody(String trangThaiDonHang) {
    }
}