package com.example.gearshop.service;

import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gearshop.model.HoaDon;
import com.example.gearshop.model.TrangThaiHoaDonHang;
import com.example.gearshop.model.HoaDonChiTiet;
import com.example.gearshop.model.KhachHang;
import com.example.gearshop.model.SanPham;
import com.example.gearshop.repository.HoaDonChiTietRepository;
import com.example.gearshop.repository.HoaDonRepository;
import com.example.gearshop.repository.KhachHangRepository;
import com.example.gearshop.repository.NguoiDungRepository;
import com.example.gearshop.repository.SanPhamRepository;

@Service
public class HoaDonAdminService {
    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    @Autowired
    private ThongBaoService thongBaoService;

    public List<HoaDon> getAllHoaDons() {
        return hoaDonRepository.findAll();
    }

    public HoaDon getHoaDonById(Integer id) {
        return hoaDonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn ID: " + id));
    }

    public String getTenKhachHangByThongTinNhanHangID(Integer khachHangID) {
        KhachHang khachHang = khachHangRepository.findById(khachHangID)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng ID: " + khachHangID));
        return khachHang.getNguoiDung().getTenNguoiDung();
    }

    public List<Map<String, Object>> getSanPhamTrongHoaDon(Integer hoaDonID) {
        List<HoaDonChiTiet> chiTietList = hoaDonChiTietRepository.findByHoaDonID(hoaDonID);
        List<Map<String, Object>> result = new ArrayList<>();

        for (HoaDonChiTiet chiTiet : chiTietList) {
            SanPham sanPham = sanPhamRepository.findById(chiTiet.getSanPhamID());
            if (sanPham == null) {
                throw new RuntimeException("Không tìm thấy sản phẩm ID: " + chiTiet.getSanPhamID());
            }

            Map<String, Object> item = new HashMap<>();
            item.put("tenSanPham", sanPham.getTenSanPham());
            item.put("soLuong", chiTiet.getSoLuongSP());
            item.put("thanhTien", chiTiet.getThanhTien());
            result.add(item);
        }
        return result;
    }

    public List<HoaDon> getAllHoaDonsSorted(String sortOrder) {
        Sort sort = Sort.by("ngayTao");
        if ("desc".equalsIgnoreCase(sortOrder)) {
            sort = sort.descending();
        } else {
            sort = sort.ascending();
        }
        return hoaDonRepository.findAll(sort);
    }

    public List<HoaDon> getHoaDonByTenKhachHang(String tenNguoiDung) {
        return hoaDonRepository.findByTenNguoiDungContainingIgnoreCase(tenNguoiDung);
    }

    public List<HoaDon> getHoaDonsByFilters(String sortOrder, String maHoaDon, String tenKhachHang,
            String trangThai, LocalDate tuNgay, LocalDate denNgay) {
        String ma = chuanHoa(maHoaDon);
        String tenKh = chuanHoa(tenKhachHang);
        String tt = chuanHoa(trangThai);

        LocalDateTime from = tuNgay != null ? tuNgay.atStartOfDay() : null;
        LocalDateTime to = denNgay != null ? denNgay.atTime(LocalTime.MAX) : null;

        List<HoaDon> ds = hoaDonRepository.findAllForAdminFilters(ma, tenKh, tt, from, to);
        Comparator<HoaDon> byNgay = Comparator.comparing(HoaDon::getNgayTao, Comparator.nullsLast(Comparator.naturalOrder()));
        if ("asc".equalsIgnoreCase(sortOrder)) {
            ds.sort(byNgay);
        } else {
            ds.sort(byNgay.reversed());
        }
        return ds;
    }

    private String chuanHoa(String s) {
        if (s == null) {
            return null;
        }
        String x = s.trim();
        return x.isEmpty() ? null : x;
    }

    /**
     * Shop cập nhật trạng thái vận đơn (một trong {@link TrangThaiHoaDonHang#danhSachLoc()}).
     */
    @Transactional
    public void capNhatTrangThaiDonHang(Integer hoaDonId, String trangThaiMoi, String lyDoHuy) {
        if (trangThaiMoi == null || trangThaiMoi.isBlank()) {
            throw new IllegalArgumentException("Trạng thái không được để trống.");
        }
        String trimmed = trangThaiMoi.trim();
        if (!TrangThaiHoaDonHang.danhSachLoc().contains(trimmed)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ: " + trimmed);
        }
        HoaDon hoaDon = hoaDonRepository.findById(hoaDonId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn ID: " + hoaDonId));
        String hienTai = HoaDonService.chuanHoaTrangThai(hoaDon.getTrangThaiDonHang());
        List<String> trangThaiHopLe = layTrangThaiTiepTheoHopLe(hienTai);
        if (!trangThaiHopLe.contains(trimmed)) {
            throw new IllegalArgumentException(
                    "Chỉ được cập nhật tuần tự. Trạng thái hợp lệ tiếp theo: " + String.join(", ", trangThaiHopLe));
        }
        if (TrangThaiHoaDonHang.DA_HUY.equals(trimmed)) {
            String lyDo = lyDoHuy != null ? lyDoHuy.trim() : "";
            if (lyDo.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập lý do hủy đơn.");
            }
            hoaDon.setLyDoHuy(lyDo);
        } else {
            hoaDon.setLyDoHuy(null);
        }
        hoaDon.setTrangThaiDonHang(trimmed);
        hoaDonRepository.save(hoaDon);

        Integer khachHangId = hoaDon.getThongTinNhanHang() != null ? hoaDon.getThongTinNhanHang().getKhachHangID() : null;
        if (khachHangId != null) {
            String noiDung = "Trạng thái đơn " + hoaDon.getMaHoaDon() + " đã cập nhật: " + trimmed + ".";
            String loai = ThongBaoService.LOAI_CAP_NHAT_TRANG_THAI;
            if (TrangThaiHoaDonHang.DA_HUY.equals(trimmed) && hoaDon.getLyDoHuy() != null && !hoaDon.getLyDoHuy().isBlank()) {
                noiDung += " Lý do: " + hoaDon.getLyDoHuy();
                loai = ThongBaoService.LOAI_HUY_DON;
            }
            thongBaoService.taoThongBaoDonHang(khachHangId, hoaDon.getId(), loai, noiDung);
        }
    }

    public List<String> layTrangThaiTiepTheoHopLe(String trangThaiHienTai) {
        if (trangThaiHienTai == null || trangThaiHienTai.isBlank()) {
            return TrangThaiHoaDonHang.danhSachLoc();
        }
        return switch (trangThaiHienTai) {
            case TrangThaiHoaDonHang.CHO_XAC_NHAN -> List.of(
                    TrangThaiHoaDonHang.DANG_CHUAN_BI_HANG,
                    TrangThaiHoaDonHang.DA_HUY);
            case TrangThaiHoaDonHang.DANG_CHUAN_BI_HANG -> List.of(
                    TrangThaiHoaDonHang.CHO_GIAO_HANG,
                    TrangThaiHoaDonHang.DA_HUY);
            case TrangThaiHoaDonHang.CHO_GIAO_HANG -> List.of(TrangThaiHoaDonHang.DA_GIAO);
            case TrangThaiHoaDonHang.DA_GIAO -> List.of(TrangThaiHoaDonHang.TRA_HANG);
            default -> List.of();
        };
    }
}
