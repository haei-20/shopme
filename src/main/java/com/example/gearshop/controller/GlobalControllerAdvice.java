package com.example.gearshop.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.gearshop.model.NguoiDung;
import com.example.gearshop.model.ThongTinShop;
import com.example.gearshop.model.TrangThaiThongBao;
import com.example.gearshop.repository.KhachHangRepository;
import com.example.gearshop.repository.NguoiDungRepository;
import com.example.gearshop.service.ThongBaoService;
import com.example.gearshop.service.ThongTinShopService;

import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalControllerAdvice {
    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private ThongBaoService thongBaoService;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private ThongTinShopService thongTinShopService;

    @ModelAttribute("nguoiDung")
    public NguoiDung thongTinNguoiDungDangNhap(HttpSession session) {
        return (NguoiDung) session.getAttribute("nguoiDung");
    }

    @ModelAttribute
    public void thongTinThongBaoMoi(Model model, @ModelAttribute("nguoiDung") NguoiDung nguoiDung) {
        if (nguoiDung == null) {
            return;
        }
        var khOpt = khachHangRepository.findByNguoiDung_Id(nguoiDung.getId());
        if (khOpt.isPresent()) {
            Integer khachHangId = khOpt.get().getId();
            var thongBaos = thongBaoService.layThongBaoTheoKhachHang(khachHangId);
            Map<Integer, String> thongBaoNoiDungById = new HashMap<>();
            Map<Integer, String> thongBaoLinkById = new HashMap<>();
            Map<Integer, String> thongBaoLoaiCssById = new HashMap<>();
            for (var tb : thongBaos) {
                thongBaoNoiDungById.put(tb.getId(), thongBaoService.layNoiDungHienThi(tb));
                thongBaoLinkById.put(tb.getId(), thongBaoService.layLinkThongBao(tb));
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
                thongBaoLoaiCssById.put(tb.getId(), css);
            }
            model.addAttribute("thongBaos", thongBaos);
            model.addAttribute("thongBaoNoiDungById", thongBaoNoiDungById);
            model.addAttribute("thongBaoLinkById", thongBaoLinkById);
            model.addAttribute("thongBaoLoaiCssById", thongBaoLoaiCssById);
            model.addAttribute("soLuongThongBaoChuaDoc", thongBaoService.demSoThongBaoChuaDoc(khachHangId));
        } else {
            model.addAttribute("thongBaos", Collections.emptyList());
            model.addAttribute("soLuongThongBaoChuaDoc", 0L);
            model.addAttribute("thongBaoNoiDungById", Collections.emptyMap());
            model.addAttribute("thongBaoLinkById", Collections.emptyMap());
            model.addAttribute("thongBaoLoaiCssById", Collections.emptyMap());
        }
    }

    @ModelAttribute("shopInfo")
    public ThongTinShop thongTinShop() {
        return thongTinShopService.getOrCreateThongTinShop();
    }
}
