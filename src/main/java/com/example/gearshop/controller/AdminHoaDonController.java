package com.example.gearshop.controller;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.util.HashMap;
import java.math.BigDecimal;

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
            @RequestParam(value = "maHoaDon", required = false) String maHoaDon,
            @RequestParam(value = "trangThai", required = false) String trangThai,
            @RequestParam(value = "tuNgay", required = false) LocalDate tuNgay,
            @RequestParam(value = "denNgay", required = false) LocalDate denNgay,
            Model model) {

        List<HoaDon> hoaDons = hoaDonService.getHoaDonsByFilters(
                sortOrder, maHoaDon, tenKhachHang, trangThai, tuNgay, denNgay);
        Map<Integer, String> tenKhachDatByHoaDonId = new HashMap<>();
        for (HoaDon hd : hoaDons) {
            Integer khachHangId = hd.getThongTinNhanHang() != null ? hd.getThongTinNhanHang().getKhachHangID() : null;
            String ten = khachHangId != null ? hoaDonService.getTenKhachHangByThongTinNhanHangID(khachHangId) : "—";
            tenKhachDatByHoaDonId.put(hd.getId(), ten);
        }

        model.addAttribute("hoaDons", hoaDons);
        model.addAttribute("tenKhachDatByHoaDonId", tenKhachDatByHoaDonId);
        model.addAttribute("sortOrder", sortOrder);
        model.addAttribute("tenKhachHang", tenKhachHang);
        model.addAttribute("maHoaDon", maHoaDon);
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("tuNgay", tuNgay);
        model.addAttribute("denNgay", denNgay);
        model.addAttribute("cacTrangThaiDon", TrangThaiHoaDonHang.danhSachLoc());
        return "adminTemplate/hoadon";
    }

    @GetMapping("/chitiet/{id}")
    public String chiTietHoaDon(@PathVariable Integer id, Model model) {
        HoaDon hoaDon = hoaDonService.getHoaDonById(id);
        String tenKhachHang = hoaDonService
                .getTenKhachHangByThongTinNhanHangID(hoaDon.getThongTinNhanHang().getKhachHangID());
        List<Map<String, Object>> sanPhamTrongHoaDon = hoaDonService.getSanPhamTrongHoaDon(id);
        BigDecimal tamTinh = BigDecimal.ZERO;
        for (Map<String, Object> sp : sanPhamTrongHoaDon) {
            Object thanhTienObj = sp.get("thanhTien");
            if (thanhTienObj instanceof BigDecimal thanhTien) {
                tamTinh = tamTinh.add(thanhTien);
            } else if (thanhTienObj != null) {
                tamTinh = tamTinh.add(new BigDecimal(thanhTienObj.toString()));
            }
        }
        BigDecimal tongThanhToan = hoaDon.getTongGia() != null ? hoaDon.getTongGia() : BigDecimal.ZERO;
        BigDecimal giamGia = tamTinh.subtract(tongThanhToan);
        if (giamGia.compareTo(BigDecimal.ZERO) < 0) {
            giamGia = BigDecimal.ZERO;
        }

        String chuan = HoaDonService.chuanHoaTrangThai(hoaDon.getTrangThaiDonHang());
        boolean trongDanhSach = chuan != null && !chuan.isBlank()
                && TrangThaiHoaDonHang.danhSachLoc().contains(chuan);
        List<String> trangThaiTiepTheo = hoaDonService.layTrangThaiTiepTheoHopLe(chuan);

        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("tenKhachHang", tenKhachHang);
        model.addAttribute("sanPhamList", sanPhamTrongHoaDon);
        model.addAttribute("tamTinh", tamTinh);
        model.addAttribute("giamGia", giamGia);
        model.addAttribute("tongThanhToan", tongThanhToan);
        model.addAttribute("cacTrangThaiDon", TrangThaiHoaDonHang.danhSachLoc());
        model.addAttribute("trangThaiChuan", trongDanhSach ? chuan : null);
        model.addAttribute("trangThaiTiepTheo", trangThaiTiepTheo);
        return "adminTemplate/hoadonchitiet";
    }

    /** Form admin: cập nhật trạng thái vận đơn. */
    @PostMapping(value = "/chitiet/{id}/trangthai", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String capNhatTrangThaiForm(@PathVariable Integer id,
            @RequestParam("trangThaiDonHang") String trangThaiDonHang,
            @RequestParam(value = "lyDoHuy", required = false) String lyDoHuy,
            RedirectAttributes redirectAttributes) {
        try {
            hoaDonService.capNhatTrangThaiDonHang(id, trangThaiDonHang, lyDoHuy);
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
            String lyDoHuy = body != null ? body.lyDoHuy() : null;
            hoaDonService.capNhatTrangThaiDonHang(id, tt, lyDoHuy);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã cập nhật trạng thái đơn hàng."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    public record CapNhatTrangThaiDonBody(String trangThaiDonHang, String lyDoHuy) {
    }
}