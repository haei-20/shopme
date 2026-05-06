package com.example.gearshop.controller;

import com.example.gearshop.model.*;
import com.example.gearshop.service.DanhGiaService;
import com.example.gearshop.service.GioHangService;
import com.example.gearshop.service.LichSuXemSanPhamService;
import com.example.gearshop.service.SanPhamService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import java.util.Map;

@Controller
@RequestMapping("/chitietsanpham")
public class ChiTietSanPhamController {

    @Autowired
    private SanPhamService sanPhamService;

    @Autowired
    private GioHangService gioHangService;

    @Autowired
    private DanhGiaService danhGiaService;
    @Autowired
    private LichSuXemSanPhamService lichSuXemSanPhamService;

    @GetMapping("/{id}")
    public String chiTietSanPham(@PathVariable("id") Integer id, Model model, HttpSession session) {
        // Lấy thông tin sản phẩm từ cơ sở dữ liệu dựa trên ID
        SanPham sanPham = sanPhamService.getSanPhamById(id);

        // Kiểm tra nếu sản phẩm không tồn tại
        if (sanPham == null) {
            return "redirect:/error"; // Chuyển hướng đến trang lỗi nếu không tìm thấy sản phẩm
        }

        // Thêm dữ liệu sản phẩm chung vào model
        model.addAttribute("sanPham", sanPham);
        model.addAttribute("loaiSanPham", sanPham.getLoaiSanPham().getTenLoaiSanPham());
        System.out.println("Loai San Pham: " + sanPham.getLoaiSanPham().getTenLoaiSanPham());

        // Lấy thông tin chi tiết theo loại sản phẩm
        Object chiTietSanPham = sanPhamService.layChiTietTheoLoai(sanPham);
        model.addAttribute("chiTietSanPham", chiTietSanPham);

        // Ép kiểu đối tượng chi tiết sản phẩm theo loại
        if (chiTietSanPham instanceof SanPhamMainBoard) {
            SanPhamMainBoard mainBoard = (SanPhamMainBoard) chiTietSanPham;
            List<String> motaList = mainBoard.getMota() != null
                    ? Arrays.asList(mainBoard.getMota().split("\\|"))
                    : null;
            model.addAttribute("motaList", motaList);
        } else if (chiTietSanPham instanceof SanPhamCPU) {
            SanPhamCPU cpu = (SanPhamCPU) chiTietSanPham;
            List<String> motaList = cpu.getMota() != null
                    ? Arrays.asList(cpu.getMota().split("\\|"))
                    : null;
            model.addAttribute("motaList", motaList);
        } else if (chiTietSanPham instanceof SanPhamRAM) {
            SanPhamRAM ram = (SanPhamRAM) chiTietSanPham;
            List<String> motaList = ram.getMota() != null
                    ? Arrays.asList(ram.getMota().split("\\|"))
                    : null;
            model.addAttribute("motaList", motaList);
        } else if (chiTietSanPham instanceof SanPhamVGA) {
            SanPhamVGA vga = (SanPhamVGA) chiTietSanPham;
            List<String> motaList = vga.getMota() != null
                    ? Arrays.asList(vga.getMota().split("\\|"))
                    : null;
            model.addAttribute("motaList", motaList);
        } else if (chiTietSanPham instanceof SanPhamOCung) {
            SanPhamOCung oCung = (SanPhamOCung) chiTietSanPham;
            List<String> motaList = oCung.getMota() != null
                    ? Arrays.asList(oCung.getMota().split("\\|"))
                    : null;
            model.addAttribute("motaList", motaList);
        } else if (chiTietSanPham instanceof SanPhamPSU) {
            SanPhamPSU psu = (SanPhamPSU) chiTietSanPham;
            List<String> motaList = psu.getMota() != null
                    ? Arrays.asList(psu.getMota().split("\\|"))
                    : null;
            model.addAttribute("motaList", motaList);
        } else if (chiTietSanPham instanceof SanPhamCooler) {
            SanPhamCooler cooler = (SanPhamCooler) chiTietSanPham;
            List<String> motaList = cooler.getMota() != null
                    ? Arrays.asList(cooler.getMota().split("\\|"))
                    : null;
            model.addAttribute("motaList", motaList);
        } else if (chiTietSanPham instanceof SanPhamCase) {
            SanPhamCase spCase = (SanPhamCase) chiTietSanPham;
            List<String> motaList = spCase.getMota() != null
                    ? Arrays.asList(spCase.getMota().split("\\|"))
                    : null;
            model.addAttribute("motaList", motaList);
        } else if (chiTietSanPham instanceof SanPhamManHinh) {
            SanPhamManHinh manHinh = (SanPhamManHinh) chiTietSanPham;
            List<String> motaList = manHinh.getMota() != null
                    ? Arrays.asList(manHinh.getMota().split("\\|"))
                    : null;
            model.addAttribute("motaList", motaList);
        }

        List<Integer> sanPhamDaXem = (List<Integer>) session.getAttribute("sanPhamDaXem");
        if (sanPhamDaXem == null) {
            sanPhamDaXem = new ArrayList<>();
        }

        // Nếu chưa có trong danh sách thì thêm vào
        if (!sanPhamDaXem.contains(sanPham.getId())) {
            sanPhamDaXem.add(0, sanPham.getId()); // thêm vào đầu để hiển thị gần nhất trước
            // Nếu chỉ muốn lưu 10 sản phẩm gần nhất:
            if (sanPhamDaXem.size() > 10) {
                sanPhamDaXem = sanPhamDaXem.subList(0, 10);
            }
        }
        session.setAttribute("sanPhamDaXem", sanPhamDaXem);

        // Lưu riêng sản phẩm mới xem nhất để gợi ý
        session.setAttribute("sanPhamMoiXem", sanPham);

        KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
        if (khachHang != null) {
            lichSuXemSanPhamService.ghiNhanLuotXem(khachHang, sanPham.getId());
        }

        model.addAttribute("danhGias", danhGiaService.layDanhSachHienThi(id));
        long soLuongDanhGia = danhGiaService.demSoDanhGia(id);
        model.addAttribute("soLuongDanhGia", soLuongDanhGia);
        danhGiaService.tinhDiemTrungBinh(id).ifPresent(v -> model.addAttribute("diemTrungBinhDanhGia", Math.round(v * 10.0) / 10.0));

        model.addAttribute("isAdminView", session.getAttribute("nhanVien") != null);
        model.addAttribute("khachHang", khachHang);
        if (khachHang != null) {
            model.addAttribute("duocPhepDanhGia", danhGiaService.duocPhepDanhGia(khachHang.getId(), id));
            model.addAttribute("danhGiaCuaToi", danhGiaService.timCuaKhachHang(khachHang.getId(), id).orElse(null));
        } else {
            model.addAttribute("duocPhepDanhGia", false);
            model.addAttribute("danhGiaCuaToi", null);
        }

        // Trả về template chi tiết sản phẩm
        return "clientTemplate/chitietsanpham";
    }

    @PostMapping("/add-to-cart")
    public ResponseEntity<Map<String, Object>> addToCart(@RequestBody Map<String, Object> product, HttpSession session) {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("success", false);

        int sanPhamID;
        int quantity;
        try {
            sanPhamID = Integer.parseInt(product.get("sanPhamID").toString());
            quantity = Integer.parseInt(product.get("quantity").toString());
        } catch (RuntimeException ex) {
            errorBody.put("message", "Dữ liệu sản phẩm không hợp lệ.");
            return ResponseEntity.badRequest().body(errorBody);
        }

        if (quantity < 1) {
            errorBody.put("message", "Số lượng phải ít nhất là 1.");
            return ResponseEntity.badRequest().body(errorBody);
        }

        SanPham sanPham = sanPhamService.getSanPhamById(sanPhamID);
        if (sanPham == null) {
            errorBody.put("message", "Không tìm thấy sản phẩm.");
            return ResponseEntity.badRequest().body(errorBody);
        }

        KhachHang khachHang = (KhachHang) session.getAttribute("khachHang");
        if (khachHang == null) {
            errorBody.put("message", "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.");
            return ResponseEntity.status(401).body(errorBody);
        }

        final List<Map<String, Object>> cart;
        try {
            gioHangService.addOrUpdateCartItem(khachHang.getId(), sanPham, quantity);
            cart = gioHangService.buildSessionCartPayload(khachHang.getId());
        } catch (IllegalArgumentException ex) {
            errorBody.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(errorBody);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cart", cart);
        response.put("cartItemCount", cart.stream()
                .mapToInt(item -> Integer.parseInt(item.get("quantity").toString()))
                .sum());
        return ResponseEntity.ok(response);
    }

}
