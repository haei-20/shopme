package com.example.gearshop.controller;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.example.gearshop.dto.SanPhamTrongHoaDonDTO;
import com.example.gearshop.model.HoaDon;
import com.example.gearshop.model.HoaDonChiTiet;
import com.example.gearshop.model.NguoiDung;
import com.example.gearshop.repository.HoaDonChiTietRepository;
import com.example.gearshop.service.HoaDonService;
import com.example.gearshop.service.YeuCauHoanTienService;

import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GiaoDichController {

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private YeuCauHoanTienService yeuCauHoanTienService;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @GetMapping("/lichsugiaodich")
    public String lichSuGiaoDich(HttpSession session,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String trangThai,
            Model model) {

        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoiDung");
        if (nguoiDung == null) {
            return "redirect:/dangnhap"; // Hoặc trả về lỗi 401
        }

        List<HoaDon> hoaDoncuaKhachHang = hoaDonService.getHoaDonsByNguoiDungId(nguoiDung.getId(), sortBy, trangThai);
        model.addAttribute("hoaDonKhachHang", hoaDoncuaKhachHang);
        model.addAttribute("selectedSort", sortBy);
        model.addAttribute("selectedTrangThai", trangThai);

        return "clientTemplate/lichsugiaodich";
    }

    @GetMapping("/lichsugiaodich/{id}")
    public String chiTietGiaoDich(@PathVariable Integer id,
            HttpSession session,
            Model model) throws AccessDeniedException {

        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoiDung");
        if (nguoiDung == null) {
            return "redirect:/dangnhap"; // Hoặc trả về lỗi 401
        }

        HoaDon hoaDon = hoaDonService.getHoaDonById(id);
        if (!hoaDonService.belongsToNguoiDung(id, nguoiDung.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xem giao dịch này.");
        }

        List<SanPhamTrongHoaDonDTO> danhSachSanPham = hoaDonService.getSanPhamTrongHoaDon(id);

        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("danhSachSanPham", danhSachSanPham);

        return "clientTemplate/chitietgiaodich";
    }

    @PostMapping("/yeucauhoantien")
    public String yeuCauHoanTien(@RequestParam int hoaDonChiTietId,
            @RequestParam String loiNhan,
            RedirectAttributes redirectAttributes) {
        // Lưu yêu cầu hoàn tiền vào CSDL
        Optional<HoaDonChiTiet> chiTietOpt = hoaDonChiTietRepository.findById(hoaDonChiTietId);
        if (chiTietOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy chi tiết hóa đơn để gửi yêu cầu hoàn tiền.");
            return "redirect:/lichsugiaodich";
        }

        HoaDonChiTiet chiTiet = chiTietOpt.get();
        yeuCauHoanTienService.createYeuCauHoanTien(chiTiet, loiNhan);
        redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu hoàn tiền đã được gửi thành công!");
        return "redirect:/lichsugiaodich/" + chiTiet.getHoaDonID();
    }
}
