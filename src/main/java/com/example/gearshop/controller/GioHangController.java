package com.example.gearshop.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.gearshop.model.KhachHang;
import com.example.gearshop.model.NguoiDung;
import com.example.gearshop.model.SanPham;
import com.example.gearshop.model.ThongTinNhanHang;
import com.example.gearshop.model.Voucher;
import com.example.gearshop.repository.SanPhamRepository;
import com.example.gearshop.service.GioHangService;
import com.example.gearshop.service.ThongTinNhanHangService;
import com.example.gearshop.service.VoucherService;

import jakarta.servlet.http.HttpSession;

@Controller
public class GioHangController {

    @Autowired
    private ThongTinNhanHangService thongTinNhanHangService;
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private SanPhamRepository sanPhamRepository;
    @Autowired
    private GioHangService gioHangService;

    @PostMapping("/save-selected-items")
    public ResponseEntity<Void> saveSelectedItems(@RequestBody(required = false) List<Map<String, Object>> selectedItems, HttpSession session) {
        KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
        if (khachHang == null) {
            return ResponseEntity.status(401).build();
        }
        if (selectedItems != null) {
            List<Integer> ids = new ArrayList<>();
            for (Map<String, Object> row : selectedItems) {
                if (row.get("sanPhamID") != null) {
                    ids.add(Integer.parseInt(row.get("sanPhamID").toString()));
                }
            }
            gioHangService.syncDuocChonThanhToan(khachHang.getId(), ids);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cart")
    public String showCartPage(HttpSession session) {
        if (session.getAttribute("khachHang") == null) {
            return "redirect:/dangnhap";
        }
        // Trả về giao diện giohang.html
        return "clientTemplate/giohang";
    }

    @GetMapping("/api/cart")
    public ResponseEntity<Map<String, Object>> getCart(HttpSession session) {
        KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
        if (khachHang == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng đăng nhập để xem giỏ hàng.");
            return ResponseEntity.status(401).body(error);
        }
        List<Map<String, Object>> cart = gioHangService.buildSessionCartPayload(khachHang.getId());
        return ResponseEntity.ok(buildCartResponse(cart));
    }

    @PostMapping("/cart/update-quantity")
    public ResponseEntity<Map<String, Object>> updateCartQuantity(@RequestBody Map<String, Object> payload, HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        if (payload.get("sanPhamID") == null || payload.get("quantity") == null) {
            result.put("success", false);
            result.put("message", "Thiếu dữ liệu cập nhật số lượng.");
            return ResponseEntity.badRequest().body(result);
        }

        int sanPhamID;
        int requestedQuantity;
        try {
            sanPhamID = Integer.parseInt(payload.get("sanPhamID").toString());
            requestedQuantity = Integer.parseInt(payload.get("quantity").toString());
        } catch (NumberFormatException ex) {
            result.put("success", false);
            result.put("message", "Dữ liệu số lượng không hợp lệ.");
            return ResponseEntity.badRequest().body(result);
        }

        if (requestedQuantity < 1) {
            result.put("success", false);
            result.put("message", "Số lượng phải lớn hơn hoặc bằng 1.");
            return ResponseEntity.badRequest().body(result);
        }

        SanPham sanPham = sanPhamRepository.findById(sanPhamID);
        if (sanPham == null) {
            result.put("success", false);
            result.put("message", "Không tìm thấy sản phẩm.");
            return ResponseEntity.badRequest().body(result);
        }

        Integer tonKhoValue = sanPham.getTonKho();
        int tonKho = tonKhoValue == null ? 0 : tonKhoValue;
        if (requestedQuantity > tonKho) {
            result.put("success", false);
            result.put("message", "Số lượng vượt quá tồn kho. Hiện còn " + tonKho + " sản phẩm.");
            result.put("quantity", tonKho > 0 ? tonKho : 1);
            result.put("tonKho", tonKho);
            return ResponseEntity.badRequest().body(result);
        }

        KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
        if (khachHang == null) {
            result.put("success", false);
            result.put("message", "Vui lòng đăng nhập để cập nhật giỏ hàng.");
            return ResponseEntity.status(401).body(result);
        }
        final List<Map<String, Object>> cart;
        try {
            gioHangService.updateCartItemQuantity(khachHang.getId(), sanPhamID, requestedQuantity);
            cart = gioHangService.buildSessionCartPayload(khachHang.getId());
        } catch (IllegalArgumentException ex) {
            result.put("success", false);
            result.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(result);
        }

        result.putAll(buildCartResponse(cart));
        result.put("quantity", requestedQuantity);
        result.put("tonKho", tonKho);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/cart/remove-item")
    public ResponseEntity<Map<String, Object>> removeCartItem(@RequestBody Map<String, Object> payload, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (payload.get("sanPhamID") == null) {
            result.put("success", false);
            result.put("message", "Thiếu mã sản phẩm.");
            return ResponseEntity.badRequest().body(result);
        }

        int sanPhamID = Integer.parseInt(payload.get("sanPhamID").toString());
        KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
        if (khachHang == null) {
            result.put("success", false);
            result.put("message", "Vui lòng đăng nhập để cập nhật giỏ hàng.");
            return ResponseEntity.status(401).body(result);
        }
        gioHangService.removeCartItem(khachHang.getId(), sanPhamID);
        List<Map<String, Object>> cart = gioHangService.buildSessionCartPayload(khachHang.getId());
        return ResponseEntity.ok(buildCartResponse(cart));
    }

    @PostMapping("/cart/remove-selected")
    public ResponseEntity<Map<String, Object>> removeSelectedItems(@RequestBody List<Integer> sanPhamIds, HttpSession session) {
        KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
        if (khachHang == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Vui lòng đăng nhập để cập nhật giỏ hàng.");
            return ResponseEntity.status(401).body(result);
        }
        gioHangService.removeSelectedCartItems(khachHang.getId(), sanPhamIds);
        List<Map<String, Object>> cart = gioHangService.buildSessionCartPayload(khachHang.getId());
        return ResponseEntity.ok(buildCartResponse(cart));
    }

    @GetMapping("/order")
    public String showOrderPage(HttpSession session, Model model) {
        // Lấy thông tin người dùng từ session
        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoiDung");
        if (nguoiDung == null) {
            model.addAttribute("error", "Bạn cần đăng nhập để đặt hàng.");
            return "redirect:/dangnhap";
        }
        model.addAttribute("nguoiDung", nguoiDung);

        // Lấy thông tin khách hàng từ session
        KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
        if (khachHang == null) {
            model.addAttribute("error", "Không tìm thấy thông tin khách hàng.");
            return "redirect:/cart";
        }
        model.addAttribute("khachHang", khachHang);

        // Lấy danh sách thông tin nhận hàng ứng với khachHangID
        List<ThongTinNhanHang> thongTinNhanHangList = thongTinNhanHangService.getThongTinNhanHangByKhachHangID(khachHang.getId());
        model.addAttribute("receivers", thongTinNhanHangList);

        // Tìm đối tượng thông tin nhận hàng có tên giống tên người dùng
        ThongTinNhanHang matchedReceiver = thongTinNhanHangList.stream()
            .filter(receiver -> receiver.getTenNguoiNhan().equalsIgnoreCase(nguoiDung.getTenNguoiDung()))
            .findFirst()
            .orElse(null);

        model.addAttribute("matchedReceiver", matchedReceiver);

        // Chỉ hiển thị voucher còn dùng được cho khách hàng này
        List<Voucher> vouchers = voucherService.getVouchersEligibleForCheckout(khachHang);
        model.addAttribute("vouchers", vouchers);

        List<Map<String, Object>> selectedItems = gioHangService.buildSelectedLinesForOrder(khachHang.getId());
        if (selectedItems.isEmpty()) {
            model.addAttribute("error", "Bạn chưa chọn sản phẩm nào.");
            return "redirect:/cart";
        }

        double totalPrice = 0;
        for (Map<String, Object> item : selectedItems) {
            int quantity = Integer.parseInt(item.get("quantity").toString());
            double price = Double.parseDouble(item.get("priceNumeric").toString());
            totalPrice += quantity * price;
        }

        model.addAttribute("cart", selectedItems);
        model.addAttribute("totalPrice", totalPrice);

        // Trả về giao diện xemhoadon.html
        return "clientTemplate/xemhoadon";
    }

    private Map<String, Object> buildCartResponse(List<Map<String, Object>> cart) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cart", cart);
        response.put("cartItemCount", cart.stream()
                .mapToInt(item -> Integer.parseInt(item.get("quantity").toString()))
                .sum());
        return response;
    }

}