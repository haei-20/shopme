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
import com.example.gearshop.model.GioHangChiTiet;
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
    public ResponseEntity<Void> saveSelectedItems(@RequestBody List<Map<String, Object>> selectedItems, HttpSession session) {
        // Kiểm tra và log dữ liệu để đảm bảo rằng selectedItems chứa sanPhamID
        System.out.println("Du lieu selectedItems trong API /save-selected-items:");
        selectedItems.forEach(item -> System.out.println(item));

        session.setAttribute("selectedItems", selectedItems);
        return ResponseEntity.ok().build(); // Trả về phản hồi HTTP 200 OK
    }

    @GetMapping("/cart")
    public String showCartPage() {
        // Trả về giao diện giohang.html
        return "clientTemplate/giohang";
    }

    @GetMapping("/api/cart")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> getCart(HttpSession session) {
        KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
        List<Map<String, Object>> cart = khachHang != null
                ? buildCartPayloadFromDb(khachHang.getId())
                : getSessionCart(session);

        session.setAttribute("cart", cart);
        return ResponseEntity.ok(buildCartResponse(cart));
    }

    @PostMapping("/cart/update-quantity")
    @SuppressWarnings("unchecked")
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
        List<Map<String, Object>> cart;

        if (khachHang != null) {
            try {
                gioHangService.updateCartItemQuantity(khachHang.getId(), sanPhamID, requestedQuantity);
                cart = buildCartPayloadFromDb(khachHang.getId());
            } catch (IllegalArgumentException ex) {
                result.put("success", false);
                result.put("message", ex.getMessage());
                return ResponseEntity.badRequest().body(result);
            }
        } else {
            cart = getSessionCart(session);
            for (Map<String, Object> item : cart) {
                if (item.get("sanPhamID") != null && Integer.parseInt(item.get("sanPhamID").toString()) == sanPhamID) {
                    item.put("quantity", requestedQuantity);
                    item.put("tonKho", tonKho);
                    break;
                }
            }
        }

        session.setAttribute("cart", cart);

        result.putAll(buildCartResponse(cart));
        result.put("quantity", requestedQuantity);
        result.put("tonKho", tonKho);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/cart/remove-item")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> removeCartItem(@RequestBody Map<String, Object> payload, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (payload.get("sanPhamID") == null) {
            result.put("success", false);
            result.put("message", "Thiếu mã sản phẩm.");
            return ResponseEntity.badRequest().body(result);
        }

        int sanPhamID = Integer.parseInt(payload.get("sanPhamID").toString());
        KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
        List<Map<String, Object>> cart;

        if (khachHang != null) {
            gioHangService.removeCartItem(khachHang.getId(), sanPhamID);
            cart = buildCartPayloadFromDb(khachHang.getId());
        } else {
            cart = getSessionCart(session);
            cart.removeIf(item -> item.get("sanPhamID") != null
                    && Integer.parseInt(item.get("sanPhamID").toString()) == sanPhamID);
        }

        session.setAttribute("cart", cart);
        return ResponseEntity.ok(buildCartResponse(cart));
    }

    @PostMapping("/cart/remove-selected")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> removeSelectedItems(@RequestBody List<Integer> sanPhamIds, HttpSession session) {
        KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
        List<Map<String, Object>> cart;

        if (khachHang != null) {
            gioHangService.removeSelectedCartItems(khachHang.getId(), sanPhamIds);
            cart = buildCartPayloadFromDb(khachHang.getId());
        } else {
            cart = getSessionCart(session);
            cart.removeIf(item -> item.get("sanPhamID") != null
                    && sanPhamIds.contains(Integer.parseInt(item.get("sanPhamID").toString())));
        }

        session.setAttribute("cart", cart);
        return ResponseEntity.ok(buildCartResponse(cart));
    }

    @GetMapping("/order")
    @SuppressWarnings("unchecked")
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

        // Lấy danh sách voucher từ service
        List<Voucher> vouchers = voucherService.getAllVouchers();
        model.addAttribute("vouchers", vouchers);

        // Lấy các sản phẩm đã chọn từ session
        List<Map<String, Object>> selectedItems = (List<Map<String, Object>>) session.getAttribute("selectedItems");
        if (selectedItems == null || selectedItems.isEmpty()) {
            model.addAttribute("error", "Bạn chưa chọn sản phẩm nào.");
            return "redirect:/cart";
        }

        // Xử lý chuyển đổi giá trị price và tính tổng tiền
        double totalPrice = 0;
        for (Map<String, Object> item : selectedItems) {
            int quantity = Integer.parseInt(item.get("quantity").toString());
            String priceString = item.get("price").toString();
            double price = Double.parseDouble(priceString.replace(",", "").replace("₫", "").trim()); // Chuyển đổi giá trị để tính toán
            item.put("priceNumeric", price); // Thêm giá trị đã chuyển đổi vào map
            totalPrice += quantity * price;
        }

        // Kiểm tra dữ liệu selectedItems
        System.out.println("Du lieu selectedItems trong API order:");
        selectedItems.forEach(item -> System.out.println(item));

        model.addAttribute("cart", selectedItems);
        model.addAttribute("totalPrice", totalPrice);

        // Trả về giao diện xemhoadon.html
        return "clientTemplate/xemhoadon";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getSessionCart(HttpSession session) {
        List<Map<String, Object>> cart = (List<Map<String, Object>>) session.getAttribute("cart");
        return cart == null ? new ArrayList<>() : cart;
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

    private List<Map<String, Object>> buildCartPayloadFromDb(Integer khachHangId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (GioHangChiTiet chiTiet : gioHangService.getCartDetailsByCustomerId(khachHangId)) {
            Map<String, Object> item = new HashMap<>();
            item.put("sanPhamID", chiTiet.getSanPham().getId());
            item.put("name", chiTiet.getSanPham().getTenSanPham());
            item.put("price", chiTiet.getDonGia());
            item.put("image", "/images/product/" + chiTiet.getSanPham().getHinhAnh());
            item.put("tonKho", chiTiet.getSanPham().getTonKho());
            item.put("quantity", chiTiet.getSoLuong());
            result.add(item);
        }
        return result;
    }
}