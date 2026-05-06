package com.example.gearshop.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.gearshop.model.KhachHang;
import com.example.gearshop.model.ThongBao;
import com.example.gearshop.model.TrangThaiThongBao;
import com.example.gearshop.repository.KhachHangRepository;
import com.example.gearshop.repository.ThongBaoRepository;

@Service
public class ThongBaoService {
    public static final String LOAI_DAT_HANG_THANH_CONG = "ORDER_SUCCESS";
    public static final String LOAI_CAP_NHAT_TRANG_THAI = "ORDER_STATUS";
    public static final String LOAI_HUY_DON = "ORDER_CANCEL";

    @Autowired
    private ThongBaoRepository thongBaoRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;

    public List<ThongBao> layThongBaoTheoKhachHang(Integer khachHangId) {
        return thongBaoRepository.findByKhachHangIdOrderByNgayThongBaoDesc(khachHangId);
    }

    public long demSoThongBaoChuaDoc(Integer khachHangId) {
        return thongBaoRepository.countByKhachHangIdAndTrangThaiThongBao(khachHangId, TrangThaiThongBao.CHUA_DOC);
    }

    public void danhDauTatCaDaDoc(Integer khachHangId) {
        List<ThongBao> thongBaos = thongBaoRepository.findByKhachHangIdOrderByNgayThongBaoDesc(khachHangId);
        for (ThongBao tb : thongBaos) {
            if (tb.getTrangThaiThongBao() == TrangThaiThongBao.CHUA_DOC) {
                tb.setTrangThaiThongBao(TrangThaiThongBao.DA_DOC);
            }
        }
        thongBaoRepository.saveAll(thongBaos);
    }

    public boolean danhDauDaDoc(Integer khachHangId, Integer thongBaoId) {
        if (khachHangId == null || thongBaoId == null) {
            return false;
        }
        return thongBaoRepository.findByIdAndKhachHangId(thongBaoId, khachHangId)
                .map(tb -> {
                    if (tb.getTrangThaiThongBao() != TrangThaiThongBao.DA_DOC) {
                        tb.setTrangThaiThongBao(TrangThaiThongBao.DA_DOC);
                        thongBaoRepository.save(tb);
                    }
                    return true;
                })
                .orElse(false);
    }

    public void taoThongBaoChoKhachHang(Integer khachHangId, String noiDung) {
        if (khachHangId == null || noiDung == null || noiDung.isBlank()) {
            return;
        }
        KhachHang khachHang = khachHangRepository.findById(khachHangId).orElse(null);
        if (khachHang == null) {
            return;
        }
        ThongBao tb = new ThongBao();
        tb.setMaThongBao("TB" + System.currentTimeMillis());
        tb.setNoiDung(noiDung.trim());
        tb.setNgayThongBao(LocalDateTime.now());
        tb.setTrangThaiThongBao(TrangThaiThongBao.CHUA_DOC);
        tb.setKhachHang(khachHang);
        thongBaoRepository.save(tb);
    }

    public void taoThongBaoDonHang(Integer khachHangId, Integer hoaDonId, String loai, String noiDungHienThi) {
        if (khachHangId == null || hoaDonId == null || noiDungHienThi == null || noiDungHienThi.isBlank()) {
            return;
        }
        String payload = loai + "|" + hoaDonId + "|" + noiDungHienThi.trim();
        taoThongBaoChoKhachHang(khachHangId, payload);
    }

    public String layNoiDungHienThi(ThongBao tb) {
        String raw = tb != null ? tb.getNoiDung() : null;
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String[] parts = raw.split("\\|", 3);
        if (parts.length == 3 && parts[0].startsWith("ORDER_")) {
            return parts[2];
        }
        return raw;
    }

    public String layLoaiThongBao(ThongBao tb) {
        String raw = tb != null ? tb.getNoiDung() : null;
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String[] parts = raw.split("\\|", 3);
        if (parts.length == 3 && parts[0].startsWith("ORDER_")) {
            return parts[0];
        }
        return "";
    }

    public String layLinkThongBao(ThongBao tb) {
        String raw = tb != null ? tb.getNoiDung() : null;
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split("\\|", 3);
        if (parts.length == 3 && parts[0].startsWith("ORDER_")) {
            try {
                int hoaDonId = Integer.parseInt(parts[1]);
                return "/lichsugiaodich/" + hoaDonId;
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

}
