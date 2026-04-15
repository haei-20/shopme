package com.example.gearshop.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gearshop.model.KhachHang;
import com.example.gearshop.model.ThongTinNhanHang;
import com.example.gearshop.service.ThongTinNhanHangService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/receivers")
public class ThongTinNhanHangController {

    @Autowired
    private ThongTinNhanHangService thongTinNhanHangService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> addReceiver(@RequestBody ThongTinNhanHang receiver,
                                        HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
            if (khachHang == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy thông tin khách hàng. Vui lòng đăng nhập lại.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            ThongTinNhanHang saved = thongTinNhanHangService.createThongTinNhanHang(
                khachHang.getId(),
                receiver.getTenNguoiNhan(),
                receiver.getEmail(),
                receiver.getSdt(),
                receiver.getDiachi()
            );

            response.put("success", true);
            response.put("message", "Thêm thông tin nhận hàng thành công.");
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", "Không thể lưu thông tin nhận hàng: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ThongTinNhanHang> getReceiverById(@PathVariable Integer id) {
        return ResponseEntity.ok(thongTinNhanHangService.getThongTinNhanHangById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateReceiver(@PathVariable Integer id,
                                                              @RequestBody ThongTinNhanHang receiver,
                                                              HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
            if (khachHang == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy thông tin khách hàng. Vui lòng đăng nhập lại.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            ThongTinNhanHang existing = thongTinNhanHangService.getThongTinNhanHangById(id);
            if (existing.getKhachHangID() != khachHang.getId()) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền sửa địa chỉ này.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            ThongTinNhanHang updated = thongTinNhanHangService.updateThongTinNhanHang(
                id,
                receiver.getTenNguoiNhan(),
                receiver.getEmail(),
                receiver.getSdt(),
                receiver.getDiachi()
            );

            response.put("success", true);
            response.put("message", "Cập nhật địa chỉ nhận hàng thành công.");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", "Không thể cập nhật địa chỉ: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteReceiver(@PathVariable Integer id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
            if (khachHang == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy thông tin khách hàng. Vui lòng đăng nhập lại.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            ThongTinNhanHang existing = thongTinNhanHangService.getThongTinNhanHangById(id);
            if (existing.getKhachHangID() != khachHang.getId()) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền xóa địa chỉ này.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            thongTinNhanHangService.deleteThongTinNhanHang(id);
            response.put("success", true);
            response.put("message", "Xóa địa chỉ thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", "Không thể xóa địa chỉ: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
