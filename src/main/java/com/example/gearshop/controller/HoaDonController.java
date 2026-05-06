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
import com.example.gearshop.service.ThongBaoService;
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
    @Autowired
    private ThongBaoService thongBaoService;

    @PostMapping("/save-order")
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

        List<Map<String, Object>> cart = gioHangService.buildSelectedLinesForOrder(khachHang.getId());
        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Bạn chưa chọn sản phẩm nào hoặc giỏ đã thay đổi.");
            return "redirect:/order";
        }

        double tongGia = 0;
        for (Map<String, Object> item : cart) {
            int quantity = Integer.parseInt(item.get("quantity").toString());
            double price = Double.parseDouble(item.get("priceNumeric").toString());
            tongGia += quantity * price;
        }

        // Áp dụng giảm giá từ voucher (nếu có)
        double tongGiaSauGiam = tongGia;
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            String code = voucherCode.trim();
            Voucher voucher;
            try {
                voucher = voucherService.getVoucherByMaVoucher(code);
            } catch (IllegalArgumentException ex) {
                redirectAttributes.addFlashAttribute("error", ex.getMessage());
                return "redirect:/order";
            }

            if (!voucherService.isVoucherAvailableForCustomer(voucher, khachHang)) {
                redirectAttributes.addFlashAttribute("error", "Voucher này không áp dụng cho khách hàng của bạn.");
                return "redirect:/order";
            }

            try {
                voucherService.assertCoCauHinhGiamGia(voucher);
                voucherService.assertDonHangDuDieuKien(voucher, tongGia);
            } catch (IllegalArgumentException ex) {
                redirectAttributes.addFlashAttribute("error", ex.getMessage());
                return "redirect:/order";
            }

            double discount = voucherService.tinhSoTienGiam(voucher, tongGia);
            tongGiaSauGiam = tongGia - discount;
        }

        tongGiaSauGiam = Math.max(tongGiaSauGiam, 0); // Đảm bảo tổng tiền không âm

        // Lưu hóa đơn
        if (!"BANK_TRANSFER".equalsIgnoreCase(paymentMethod) && !"COD".equalsIgnoreCase(paymentMethod)) {
            redirectAttributes.addFlashAttribute("error", "Phương thức thanh toán không hợp lệ.");
            return "redirect:/order";
        }

        final HoaDonService.DatHangKetQua ketQua;
        try {
            ketQua = hoaDonService.datHangVaTruTonKho(thongTinNhanHangID, tongGiaSauGiam, paymentMethod, cart);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/order";
        }

        HoaDon hoaDon = ketQua.hoaDon();
        List<Integer> purchasedSanPhamIds = ketQua.purchasedSanPhamIds();

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            Voucher voucher = voucherService.getVoucherByMaVoucher(voucherCode.trim());
            voucherService.markVoucherAsUsed(voucher, khachHang);
        }

        model.addAttribute("hoaDon", hoaDon);
        session.setAttribute("hoaDon", hoaDon);
        gioHangService.removeSelectedCartItems(khachHang.getId(), purchasedSanPhamIds);
        thongBaoService.taoThongBaoDonHang(
                khachHang.getId(),
                hoaDon.getId(),
                ThongBaoService.LOAI_DAT_HANG_THANH_CONG,
                "Đặt hàng thành công. Mã đơn: " + hoaDon.getMaHoaDon() + ".");
        return "redirect:/dat-hang-thanh-cong?hoaDonId=" + hoaDon.getId();
    }
}
