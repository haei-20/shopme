package com.example.gearshop.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.gearshop.model.KhachHang;
import com.example.gearshop.model.Voucher;
import com.example.gearshop.model.VoucherKhachHang;
import com.example.gearshop.repository.KhachHangRepository;
import com.example.gearshop.repository.VoucherKhachHangRepository;
import com.example.gearshop.repository.VoucherRepository;
import com.example.gearshop.service.VoucherService;

@Controller
@RequestMapping("/admin/voucher")
public class AdminVoucherController {

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private VoucherKhachHangRepository voucherKhachHangRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private VoucherService voucherService;

    // Trang danh sách voucher
    @GetMapping
    public String danhSachVoucher(
            @RequestParam(value = "sort", required = false, defaultValue = "") String sort,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            Model model) {
        List<Voucher> vouchers;

        if (keyword != null && !keyword.isEmpty()) {
            vouchers = voucherRepository.findByTenVoucherContainingIgnoreCase(keyword);
        } else {
            vouchers = switch (sort) {
                case "dateDesc" -> voucherRepository.findAllByOrderByThoiHanDesc();
                case "dateAsc" -> voucherRepository.findAllByOrderByThoiHanAsc();
                case "nameAsc" -> voucherRepository.findAllByOrderByTenVoucherAsc();
                case "nameDesc" -> voucherRepository.findAllByOrderByTenVoucherDesc();
                default -> voucherRepository.findAll();
            };
        }

        // Xử lý danh sách người được cấp
        Map<Integer, String> nguoiDuocCapMap = new HashMap<>();
        for (Voucher v : vouchers) {
            List<VoucherKhachHang> mappings = voucherKhachHangRepository.findByVoucherId(v.getId());
            if (mappings == null || mappings.isEmpty()) {
                nguoiDuocCapMap.put(v.getId(), "Tất cả khách hàng");
            } else {
                nguoiDuocCapMap.put(v.getId(), "Khách hàng cụ thể");
            }
        }

        model.addAttribute("vouchers", vouchers);
        model.addAttribute("keyword", keyword);
        model.addAttribute("nguoiDuocCapMap", nguoiDuocCapMap);
        return "adminTemplate/voucher"; // Thymeleaf template
    }

    // Xem chi tiết voucher
    @GetMapping("/chitiet/{id}")
    public String chiTietVoucher(@PathVariable Integer id, Model model) {
        Voucher voucher = voucherRepository.findById(id).orElseThrow();
        if (voucher.getNgayBatDau() == null) {
            voucher.setNgayBatDau(voucher.getThoiHan());
        }
        List<VoucherKhachHang> mappings = voucherKhachHangRepository.findByVoucherId(id);
        String nguoiDuocCap;
        if (mappings.isEmpty()) {
            nguoiDuocCap = "Tất cả khách hàng";
        } else {
            nguoiDuocCap = "Khách hàng cụ thể";
        }

        model.addAttribute("voucher", voucher);
        model.addAttribute("nguoiDuocCap", nguoiDuocCap);
        model.addAttribute("danhSachKhachHang", mappings);

        return "adminTemplate/chitietvoucher";
    }

    // Form thêm voucher
    @GetMapping("/them")
    public String formThemVoucher(Model model, RedirectAttributes redirectAttributes) {
        List<KhachHang> danhSachKhachHang = khachHangRepository.findAll();
        model.addAttribute("danhSachKhachHang", danhSachKhachHang);

        return "adminTemplate/themvoucher";
    }

    // Xử lý thêm voucher
    @PostMapping("/them")
    public String themVoucher(
            @RequestParam String maVoucher,
            @RequestParam String tenVoucher,
            @RequestParam(required = false) String moTa,
            @RequestParam(required = false) String kichHoat,
            @RequestParam(required = false) Integer giamGiaTheoPhanTram,
            @RequestParam(required = false) BigDecimal giamGiaCuThe,
            @RequestParam(required = false) BigDecimal giamGiaToiDa,
            @RequestParam String ngayBatDau,
            @RequestParam String thoiHan,
            @RequestParam(required = false) Integer soLuongNguoiDungToiDa,
            @RequestParam BigDecimal donToiThieu,
            @RequestParam(value = "khachHangIds", required = false) String khachHangIdsString,
            RedirectAttributes redirectAttributes) {

        LocalDateTime startDate = parseDateTime(ngayBatDau);
        LocalDateTime endDate = parseDateTime(thoiHan);
        if (!endDate.isAfter(startDate)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thời hạn kết thúc phải sau thời gian bắt đầu.");
            return "redirect:/admin/voucher/them";
        }

        String voucherCode = maVoucher.trim();
        if (voucherCode.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập mã voucher.");
            return "redirect:/admin/voucher/them";
        }
        if (voucherRepository.existsByMaVoucher(voucherCode)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã voucher đã tồn tại.");
            return "redirect:/admin/voucher/them";
        }

        // Validate giảm giá theo phần trăm
        if (giamGiaTheoPhanTram != null) {
            if (giamGiaTheoPhanTram <= 0 || giamGiaTheoPhanTram > 100) {
                redirectAttributes.addFlashAttribute("errorMessage", "Giảm giá theo phần trăm (%) phải lớn hơn 0 và nhỏ hơn hoặc bằng 100.");
                return "redirect:/admin/voucher/them";
            }
        }

        Voucher voucher = new Voucher();
        voucher.setMaVoucher(voucherCode);
        voucher.setTenVoucher(tenVoucher);
        voucher.setMoTa(moTa);
        voucher.setKichHoat(kichHoat != null);
        voucher.setGiamGiaTheoPhanTram(giamGiaTheoPhanTram);
        voucher.setGiamGiaCuThe(giamGiaCuThe);
        voucher.setGiamGiaToiDa(giamGiaToiDa);
        voucher.setNgayBatDau(startDate);
        voucher.setDonToiThieu(donToiThieu);
        voucher.setThoiHan(endDate);
        voucher.setSoLuongNguoiDungToiDa(soLuongNguoiDungToiDa);

        voucherRepository.save(voucher);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm voucher thành công.");
        // Mapping với khách hàng
        if (khachHangIdsString != null && !khachHangIdsString.isEmpty()) {
            String[] ids = khachHangIdsString.split(",");
            for (String idStr : ids) {
                Integer id = Integer.valueOf(idStr.trim());
                VoucherKhachHang vkh = new VoucherKhachHang();
                vkh.setVoucher(voucher);
                vkh.setKhachHang(khachHangRepository.findById(id).orElseThrow());
                vkh.setMaVoucherKhachHang("VCKH" + voucher.getId() + id);
                vkh.setDaDung(false);
                voucherKhachHangRepository.save(vkh);
            }
        }

        return "redirect:/admin/voucher";
    }

    // Xoá voucher
    @GetMapping("/xoa/{id}")
    public String xoaVoucher(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            voucherService.xoaVoucherVaLienQuan(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa voucher thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa voucher: " + e.getMessage());
        }
        return "redirect:/admin/voucher"; // redirect về trang danh sách voucher
    }

    // Form sửa voucher
    @GetMapping("/sua/{id}")
    public String formSuaVoucher(@PathVariable Integer id, Model model) {
        Voucher voucher = voucherRepository.findById(id).orElseThrow();
        if (voucher.getNgayBatDau() == null) {
            voucher.setNgayBatDau(voucher.getThoiHan() != null ? voucher.getThoiHan() : LocalDateTime.now());
        }
        List<KhachHang> danhSachKhachHang = khachHangRepository.findAll();
        List<VoucherKhachHang> mappings = voucherKhachHangRepository.findByVoucherId(id);
        List<Integer> selectedKhachHangIds = mappings.stream()
                .map(vkh -> vkh.getKhachHang() != null ? vkh.getKhachHang().getId() : null)
                .filter(khId -> khId != null)
                .toList();
        model.addAttribute("voucher", voucher);
        model.addAttribute("danhSachKhachHang", danhSachKhachHang);
        model.addAttribute("selectedKhachHangIds", selectedKhachHangIds);

        return "adminTemplate/suavoucher";
    }

    // Xử lý sửa voucher
    @PostMapping("/sua/{id}")
    public String suaVoucher(@PathVariable Integer id,
            @RequestParam String maVoucher,
            @RequestParam String tenVoucher,
            @RequestParam(required = false) String moTa,
            @RequestParam(required = false) String kichHoat,
            @RequestParam(required = false) Integer giamGiaTheoPhanTram,
            @RequestParam(required = false) BigDecimal giamGiaCuThe,
            @RequestParam(required = false) BigDecimal giamGiaToiDa,
            @RequestParam String ngayBatDau,
            @RequestParam String thoiHan,
            @RequestParam(required = false) Integer soLuongNguoiDungToiDa,
            @RequestParam BigDecimal donToiThieu,
            @RequestParam(value = "khachHangIds", required = false) String khachHangIdsString,
            RedirectAttributes redirectAttributes) {
        Voucher voucher = voucherRepository.findById(id).orElseThrow();
        LocalDateTime startDate = parseDateTime(ngayBatDau);
        LocalDateTime endDate = parseDateTime(thoiHan);
        if (!endDate.isAfter(startDate)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thời hạn kết thúc phải sau thời gian bắt đầu.");
            return "redirect:/admin/voucher/sua/" + id;
        }

        String voucherCode = maVoucher.trim();
        if (voucherCode.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập mã voucher.");
            return "redirect:/admin/voucher/sua/" + id;
        }
        Optional<Voucher> existingVoucher = voucherRepository.findByMaVoucher(voucherCode);
        if (existingVoucher.isPresent() && !existingVoucher.get().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã voucher đã tồn tại.");
            return "redirect:/admin/voucher/sua/" + id;
        }

        // Validate giảm giá theo phần trăm
        if (giamGiaTheoPhanTram != null) {
            if (giamGiaTheoPhanTram <= 0 || giamGiaTheoPhanTram > 100) {
                redirectAttributes.addFlashAttribute("errorMessage", "Giảm giá theo phần trăm (%) phải lớn hơn 0 và nhỏ hơn hoặc bằng 100.");
                return "redirect:/admin/voucher/sua/" + id;
            }
        }

        voucher.setMaVoucher(voucherCode);
        voucher.setTenVoucher(tenVoucher);
        voucher.setMoTa(moTa);
        voucher.setKichHoat(kichHoat != null);
        voucher.setGiamGiaTheoPhanTram(giamGiaTheoPhanTram);
        voucher.setGiamGiaCuThe(giamGiaCuThe);
        voucher.setGiamGiaToiDa(giamGiaToiDa);
        voucher.setNgayBatDau(startDate);
        voucher.setThoiHan(endDate);
        voucher.setSoLuongNguoiDungToiDa(soLuongNguoiDungToiDa);
        voucher.setDonToiThieu(donToiThieu);
        voucherRepository.save(voucher);

        voucherKhachHangRepository.deleteByVoucherID(id);
        if (khachHangIdsString != null && !khachHangIdsString.isBlank()) {
            String[] ids = khachHangIdsString.split(",");
            for (String idStr : ids) {
                Integer khachHangId = Integer.valueOf(idStr.trim());
                VoucherKhachHang vkh = new VoucherKhachHang();
                vkh.setVoucher(voucher);
                vkh.setKhachHang(khachHangRepository.findById(khachHangId).orElseThrow());
                vkh.setMaVoucherKhachHang("VCKH" + voucher.getId() + khachHangId);
                vkh.setDaDung(false);
                voucherKhachHangRepository.save(vkh);
            }
        }

        redirectAttributes.addFlashAttribute("successMessage", "Sửa voucher thành công.");
        return "redirect:/admin/voucher";
    }

    private LocalDateTime parseDateTime(String value) {
        return LocalDateTime.parse(value);
    }
}
