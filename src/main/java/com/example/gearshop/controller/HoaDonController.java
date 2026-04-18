package com.example.gearshop.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.gearshop.model.HoaDon;
import com.example.gearshop.model.KhachHang;
import com.example.gearshop.model.ThongTinNhanHang;
import com.example.gearshop.model.Voucher;
import com.example.gearshop.service.GioHangService;
import com.example.gearshop.service.HoaDonService;
import com.example.gearshop.service.ThongTinNhanHangService;
import com.example.gearshop.service.VoucherService;

import jakarta.servlet.http.HttpSession;

@Controller
public class HoaDonController {

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private ThongTinNhanHangService thongTinNhanHangService;

    @Autowired
    private GioHangService gioHangService;

    @PostMapping("/save-order")
    @SuppressWarnings("unchecked")
    public String saveOrder(HttpSession session,
                            @RequestParam(required = false) Integer thongTinNhanHangID,
                            @RequestParam(required = false) String voucherCode,
                            @RequestParam(defaultValue = "BANK_TRANSFER") String paymentMethod,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
        if (khachHang == null) {
            model.addAttribute("error", "Không tìm thấy thông tin khách hàng.");
            return "redirect:/order";
        }

        if (thongTinNhanHangID == null || thongTinNhanHangID <= 0) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn thông tin người nhận hàng trước khi lưu hóa đơn.");
            return "redirect:/order";
        }

        ThongTinNhanHang thongTinNhanHang;
        try {
            thongTinNhanHang = thongTinNhanHangService.getThongTinNhanHangById(thongTinNhanHangID);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/order";
        }

        if (thongTinNhanHang.getKhachHangID() != khachHang.getId()) {
            redirectAttributes.addFlashAttribute("error", "Thông tin nhận hàng không thuộc tài khoản hiện tại.");
            return "redirect:/order";
        }

        List<Map<String, Object>> cart = (List<Map<String, Object>>) session.getAttribute("selectedItems");
        if (cart == null || cart.isEmpty()) {
            model.addAttribute("error", "Giỏ hàng của bạn đang trống.");
            return "redirect:/order";
        }

        double tongGia = 0;
        for (Map<String, Object> item : cart) {
            int quantity = Integer.parseInt(item.get("quantity").toString());
            double price = Double.parseDouble(item.get("price").toString().replace(",", "").replace("₫", "").trim());
            tongGia += quantity * price;
        }

        // Áp dụng giảm giá từ voucher (nếu có)
        double tongGiaSauGiam = tongGia;
        if (voucherCode != null && !voucherCode.isEmpty()) {
            Voucher voucher;
            try {
                voucher = voucherService.getVoucherByMaVoucher(voucherCode);
            } catch (IllegalArgumentException ex) {
                redirectAttributes.addFlashAttribute("error", ex.getMessage());
                return "redirect:/order";
            }

            if (!voucherService.isVoucherAvailableForCustomer(voucher, khachHang)) {
                redirectAttributes.addFlashAttribute("error", "Voucher này không áp dụng cho khách hàng của bạn.");
                return "redirect:/order";
            }

            if (voucher.getGiamGiaTheoPhanTram() != null) {
                tongGiaSauGiam -= (tongGia * voucher.getGiamGiaTheoPhanTram() / 100);
            } else if (voucher.getGiamGiaCuThe() != null) {
                tongGiaSauGiam -= voucher.getGiamGiaCuThe().doubleValue();
            }
        }

        tongGiaSauGiam = Math.max(tongGiaSauGiam, 0); // Đảm bảo tổng tiền không âm

        // Lưu hóa đơn
        if (!"BANK_TRANSFER".equalsIgnoreCase(paymentMethod) && !"COD".equalsIgnoreCase(paymentMethod)) {
            redirectAttributes.addFlashAttribute("error", "Phương thức thanh toán không hợp lệ.");
            return "redirect:/order";
        }

        HoaDon hoaDon = hoaDonService.createHoaDon("HD", thongTinNhanHangID, tongGiaSauGiam, paymentMethod);
        System.out.println("Đã tạo hóa đơn với ID: " + hoaDon.getId());
        System.out.println("Tổng tiền sau giảm: " + hoaDon.getTongGia());

        // Lưu chi tiết hóa đơn
        for (Map<String, Object> item : cart) {
            Object quantityObj = item.get("quantity");
            Object priceObj = item.get("priceNumeric");
            Object sanPhamIDObj = item.get("sanPhamID");

            if (quantityObj == null || priceObj == null || sanPhamIDObj == null) {
                model.addAttribute("error", "Dữ liệu giỏ hàng không hợp lệ.");
                return "redirect:/order";
            }

            int quantity = Integer.parseInt(quantityObj.toString());
            double price = Double.parseDouble(priceObj.toString().replace(",", "").replace("₫", "").trim());
            int sanPhamID = Integer.parseInt(sanPhamIDObj.toString());

            hoaDonService.createHoaDonChiTiet("HDCT", hoaDon.getId(), sanPhamID, quantity, quantity * price);
        }

        if (voucherCode != null && !voucherCode.isEmpty()) {
            Voucher voucher = voucherService.getVoucherByMaVoucher(voucherCode);
            voucherService.markVoucherAsUsed(voucher, khachHang);
        }
        System.out.println("Đã lưu chi tiết hóa đơn với ID: " + hoaDon.getId());

        model.addAttribute("hoaDon", hoaDon);
        session.setAttribute("hoaDon", hoaDon);
        session.removeAttribute("selectedItems");
        session.removeAttribute("cart");
        gioHangService.clearCartByCustomerId(khachHang.getId());
        return "redirect:/checkout";
    }
}
