package com.example.gearshop.controller;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.gearshop.model.NguoiDung;
import com.example.gearshop.model.ThongBao;
import com.example.gearshop.model.TrangThaiThongBao;
import com.example.gearshop.repository.KhachHangRepository;
import com.example.gearshop.service.ThongBaoService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/thongbao")
public class ThongBaoController {
    private static final DateTimeFormatter UI_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    private ThongBaoService thongBaoService;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private HttpSession session;

    @ResponseBody
    @PostMapping("/markAsRead")
    public ResponseEntity<String> markAsRead(
            @RequestParam(name = "all", required = false, defaultValue = "false") boolean all,
            @RequestParam(name = "id", required = false) Integer thongBaoId) {
        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoiDung");
        if (nguoiDung != null) {
            return khachHangRepository.findByNguoiDung_Id(nguoiDung.getId())
                    .map(khachHang -> {
                        if (all) {
                            thongBaoService.danhDauTatCaDaDoc(khachHang.getId());
                            return ResponseEntity.ok("Đã đánh dấu tất cả đã đọc");
                        }
                        if (thongBaoId != null) {
                            boolean updated = thongBaoService.danhDauDaDoc(khachHang.getId(), thongBaoId);
                            return updated
                                    ? ResponseEntity.ok("Đã đánh dấu đã đọc")
                                    : ResponseEntity.badRequest().body("Thông báo không tồn tại");
                        }
                        return ResponseEntity.ok("No action");
                    })
                    .orElseGet(() -> ResponseEntity.badRequest().body("Khách hàng không tồn tại"));
        }
        return ResponseEntity.badRequest().body("Người dùng chưa đăng nhập");
    }

    @ResponseBody
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getThongBaoList() {
        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoiDung");
        if (nguoiDung == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Người dùng chưa đăng nhập"));
        }
        return khachHangRepository.findByNguoiDung_Id(nguoiDung.getId())
                .map(khachHang -> {
                    List<ThongBao> thongBaos = thongBaoService.layThongBaoTheoKhachHang(khachHang.getId());
                    List<Map<String, Object>> items = new ArrayList<>();
                    for (ThongBao tb : thongBaos) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("id", tb.getId());
                        row.put("noiDung", thongBaoService.layNoiDungHienThi(tb));
                        row.put("link", thongBaoService.layLinkThongBao(tb));
                        row.put("ngayThongBao", tb.getNgayThongBao() != null ? tb.getNgayThongBao().format(UI_TIME_FMT) : "");
                        String loai = thongBaoService.layLoaiThongBao(tb);
                        String css = "tb-default";
                        if (ThongBaoService.LOAI_DAT_HANG_THANH_CONG.equals(loai)) {
                            css = "tb-success";
                        } else if (ThongBaoService.LOAI_CAP_NHAT_TRANG_THAI.equals(loai)) {
                            css = "tb-status";
                        } else if (ThongBaoService.LOAI_HUY_DON.equals(loai)) {
                            css = "tb-cancel";
                        }
                        if (tb.getTrangThaiThongBao() == TrangThaiThongBao.CHUA_DOC) {
                            css += " tb-unread";
                        } else {
                            css += " tb-read";
                        }
                        row.put("cssClass", css);
                        items.add(row);
                    }
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("items", items);
                    response.put("unreadCount", thongBaoService.demSoThongBaoChuaDoc(khachHang.getId()));
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Khách hàng không tồn tại")));
    }
}