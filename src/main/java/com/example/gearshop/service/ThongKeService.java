package com.example.gearshop.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.gearshop.dto.ThongKeXepHangRow;
import com.example.gearshop.model.HoaDon;
import com.example.gearshop.model.HoaDonChiTiet;
import com.example.gearshop.model.SanPham;
import com.example.gearshop.repository.HoaDonChiTietRepository;
import com.example.gearshop.repository.HoaDonRepository;
import com.example.gearshop.repository.KhachHangRepository;
import com.example.gearshop.repository.SanPhamRepository;

@Service
public class ThongKeService {
    private static final DateTimeFormatter NGAY_HIEN_THI = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int TOP_LIMIT = 10;

    @Autowired
    private HoaDonRepository hoaDonRepository;
    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;
    @Autowired
    private SanPhamRepository sanPhamRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;

    public Map<String, BigDecimal> getDoanhThuTheoNgayTrongKhoang(LocalDate tu, LocalDate den) {
        return getDoanhThuTheoNgayTrongKhoang(tu, den, null);
    }

    /**
     * @param loaiSanPhamId null = theo tổng hóa đơn; khác null = chỉ doanh thu dòng hàng thuộc loại đó.
     */
    public Map<String, BigDecimal> getDoanhThuTheoNgayTrongKhoang(LocalDate tu, LocalDate den, Integer loaiSanPhamId) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        if (loaiSanPhamId == null) {
            Map<LocalDate, BigDecimal> theoNgay = new TreeMap<>();
            for (HoaDon hd : hoaDonRepository.findAll()) {
                if (hd.getNgayTao() == null || hd.getTongGia() == null) {
                    continue;
                }
                LocalDate d = hd.getNgayTao().toLocalDate();
                if (d.isBefore(tu) || d.isAfter(den)) {
                    continue;
                }
                theoNgay.merge(d, hd.getTongGia(), BigDecimal::add);
            }
            for (Map.Entry<LocalDate, BigDecimal> e : theoNgay.entrySet()) {
                result.put(e.getKey().format(NGAY_HIEN_THI), e.getValue());
            }
            return result;
        }
        Map<LocalDate, BigDecimal> byDay = aggregateDoanhTuChiTietTheoNgay(tu, den, loaiSanPhamId);
        for (Map.Entry<LocalDate, BigDecimal> e : byDay.entrySet()) {
            result.put(e.getKey().format(NGAY_HIEN_THI), e.getValue());
        }
        return result;
    }

    public Map<String, BigDecimal> getDoanhThuTheoThangTrongNam(int nam) {
        return getDoanhThuTheoThangTrongNam(nam, null);
    }

    public Map<String, BigDecimal> getDoanhThuTheoThangTrongNam(int nam, Integer loaiSanPhamId) {
        if (loaiSanPhamId == null) {
            BigDecimal[] thang = new BigDecimal[12];
            Arrays.fill(thang, BigDecimal.ZERO);
            for (HoaDon hd : hoaDonRepository.findAll()) {
                if (hd.getNgayTao() == null || hd.getTongGia() == null) {
                    continue;
                }
                if (hd.getNgayTao().getYear() != nam) {
                    continue;
                }
                int m = hd.getNgayTao().getMonthValue();
                thang[m - 1] = thang[m - 1].add(hd.getTongGia());
            }
            Map<String, BigDecimal> result = new LinkedHashMap<>();
            for (int i = 0; i < 12; i++) {
                result.put("Tháng " + (i + 1), thang[i]);
            }
            return result;
        }
        LocalDate tu = LocalDate.of(nam, 1, 1);
        LocalDate den = LocalDate.of(nam, 12, 31);
        Map<LocalDate, BigDecimal> byDay = aggregateDoanhTuChiTietTheoNgay(tu, den, loaiSanPhamId);
        BigDecimal[] thang = new BigDecimal[12];
        Arrays.fill(thang, BigDecimal.ZERO);
        for (Map.Entry<LocalDate, BigDecimal> e : byDay.entrySet()) {
            LocalDate d = e.getKey();
            if (d.getYear() != nam) {
                continue;
            }
            thang[d.getMonthValue() - 1] = thang[d.getMonthValue() - 1].add(e.getValue());
        }
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            result.put("Tháng " + (i + 1), thang[i]);
        }
        return result;
    }

    public Map<String, BigDecimal> getDoanhThuTongMotThang(int nam, int thang) {
        return getDoanhThuTongMotThang(nam, thang, null);
    }

    public Map<String, BigDecimal> getDoanhThuTongMotThang(int nam, int thang, Integer loaiSanPhamId) {
        YearMonth ym = YearMonth.of(nam, thang);
        LocalDate tu = ym.atDay(1);
        LocalDate den = ym.atEndOfMonth();
        BigDecimal sum = BigDecimal.ZERO;
        if (loaiSanPhamId == null) {
            for (HoaDon hd : hoaDonRepository.findAll()) {
                if (hd.getNgayTao() == null || hd.getTongGia() == null) {
                    continue;
                }
                LocalDate d = hd.getNgayTao().toLocalDate();
                if (d.isBefore(tu) || d.isAfter(den)) {
                    continue;
                }
                sum = sum.add(hd.getTongGia());
            }
        } else {
            Map<LocalDate, BigDecimal> byDay = aggregateDoanhTuChiTietTheoNgay(tu, den, loaiSanPhamId);
            sum = byDay.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        result.put("Tháng " + thang + "/" + nam, sum);
        return result;
    }

    public Map<String, BigDecimal> getDoanhThuTheoLoaiSanPhamTrongKhoang(LocalDate tu, LocalDate den) {
        LocalDateTime tuDt = tu.atStartOfDay();
        LocalDateTime denDt = den.plusDays(1).atStartOfDay();
        List<Object[]> data = hoaDonChiTietRepository.doanhThuTheoLoaiSanPhamTrongKhoang(tuDt, denDt);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : data) {
            result.put(row[0].toString(), (BigDecimal) row[1]);
        }
        return result;
    }

    /** Đếm đơn theo trạng thái trong khoảng ngày tạo đơn. */
    public Map<String, Long> demDonTheoTrangThaiTrongKhoang(LocalDate tu, LocalDate den) {
        Map<String, Long> result = new TreeMap<>();
        for (HoaDon hd : hoaDonRepository.findAll()) {
            if (hd.getNgayTao() == null) {
                continue;
            }
            LocalDate d = hd.getNgayTao().toLocalDate();
            if (d.isBefore(tu) || d.isAfter(den)) {
                continue;
            }
            String s = hd.getTrangThaiDonHang() != null ? hd.getTrangThaiDonHang() : "Không rõ";
            result.merge(s, 1L, Long::sum);
        }
        return result;
    }

    public List<ThongKeXepHangRow> getTopSanPhamTheoDoanhThu(LocalDate tu, LocalDate den, Integer loaiSanPhamId) {
        Map<Integer, BigDecimal> bySp = new HashMap<>();
        Map<Integer, LocalDateTime> hdNgay = hoaDonIdsTrongKhoang(tu, den);
        Map<Integer, Integer> spLoai = loaiTheoSanPhamId();
        for (HoaDonChiTiet ct : hoaDonChiTietRepository.findAll()) {
            if (!hdNgay.containsKey(ct.getHoaDonID())) {
                continue;
            }
            if (loaiSanPhamId != null) {
                Integer lid = spLoai.get(ct.getSanPhamID());
                if (!Objects.equals(loaiSanPhamId, lid)) {
                    continue;
                }
            }
            bySp.merge(ct.getSanPhamID(), ct.getThanhTien(), BigDecimal::add);
        }
        Map<Integer, String> tenSp = sanPhamRepository.findAll().stream()
                .collect(Collectors.toMap(SanPham::getId, SanPham::getTenSanPham, (a, b) -> a));
        return bySp.entrySet().stream()
                .sorted(Map.Entry.<Integer, BigDecimal>comparingByValue().reversed())
                .limit(TOP_LIMIT)
                .map(e -> new ThongKeXepHangRow(
                        tenSp.getOrDefault(e.getKey(), "SP #" + e.getKey()),
                        e.getValue()))
                .collect(Collectors.toList());
    }

    public List<ThongKeXepHangRow> getTopKhachTheoDoanhThu(LocalDate tu, LocalDate den) {
        Map<Integer, BigDecimal> byKh = new HashMap<>();
        for (HoaDon hd : hoaDonRepository.findAll()) {
            if (hd.getNgayTao() == null || hd.getTongGia() == null || hd.getThongTinNhanHang() == null) {
                continue;
            }
            LocalDate d = hd.getNgayTao().toLocalDate();
            if (d.isBefore(tu) || d.isAfter(den)) {
                continue;
            }
            byKh.merge(hd.getThongTinNhanHang().getKhachHangID(), hd.getTongGia(), BigDecimal::add);
        }
        Map<Integer, String> tenKh = new HashMap<>();
        khachHangRepository.findAll().forEach(kh -> {
            String ten = kh.getNguoiDung() != null ? kh.getNguoiDung().getTenNguoiDung() : kh.getMaKhachHang();
            tenKh.put(kh.getId(), ten != null ? ten : ("KH #" + kh.getId()));
        });
        return byKh.entrySet().stream()
                .sorted(Map.Entry.<Integer, BigDecimal>comparingByValue().reversed())
                .limit(TOP_LIMIT)
                .map(e -> new ThongKeXepHangRow(
                        tenKh.getOrDefault(e.getKey(), "Khách #" + e.getKey()),
                        e.getValue()))
                .collect(Collectors.toList());
    }

    private Map<Integer, LocalDateTime> hoaDonIdsTrongKhoang(LocalDate tu, LocalDate den) {
        Map<Integer, LocalDateTime> map = new HashMap<>();
        for (HoaDon hd : hoaDonRepository.findAll()) {
            if (hd.getNgayTao() == null) {
                continue;
            }
            LocalDate d = hd.getNgayTao().toLocalDate();
            if (d.isBefore(tu) || d.isAfter(den)) {
                continue;
            }
            map.put(hd.getId(), hd.getNgayTao());
        }
        return map;
    }

    private Map<Integer, Integer> loaiTheoSanPhamId() {
        Map<Integer, Integer> m = new HashMap<>();
        for (SanPham sp : sanPhamRepository.findAll()) {
            if (sp.getLoaiSanPham() != null) {
                m.put(sp.getId(), sp.getLoaiSanPham().getId());
            }
        }
        return m;
    }

    private Map<LocalDate, BigDecimal> aggregateDoanhTuChiTietTheoNgay(LocalDate tu, LocalDate den, Integer loaiSanPhamId) {
        Map<Integer, LocalDateTime> hdNgay = hoaDonIdsTrongKhoang(tu, den);
        Map<Integer, Integer> spLoai = loaiTheoSanPhamId();
        Map<LocalDate, BigDecimal> acc = new TreeMap<>();
        for (HoaDonChiTiet ct : hoaDonChiTietRepository.findAll()) {
            if (!hdNgay.containsKey(ct.getHoaDonID())) {
                continue;
            }
            if (loaiSanPhamId != null) {
                Integer lid = spLoai.get(ct.getSanPhamID());
                if (!Objects.equals(loaiSanPhamId, lid)) {
                    continue;
                }
            }
            LocalDate day = hdNgay.get(ct.getHoaDonID()).toLocalDate();
            acc.merge(day, ct.getThanhTien(), BigDecimal::add);
        }
        return acc;
    }

    /** Giữ tương thích: toàn bộ lịch sử (theo query cũ). */
    public Map<String, BigDecimal> getDoanhThuTheoNgay() {
        List<Object[]> data = hoaDonRepository.doanhThuTheoNgay();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : data) {
            result.put(row[0].toString(), (BigDecimal) row[1]);
        }
        return result;
    }

    public Map<String, BigDecimal> getDoanhThuTheoThang() {
        List<Object[]> data = hoaDonRepository.doanhThuTheoThang();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : data) {
            result.put("Tháng " + row[0].toString(), (BigDecimal) row[1]);
        }
        return result;
    }

    public Map<String, BigDecimal> getDoanhThuTheoLoaiSanPham() {
        List<Object[]> data = hoaDonChiTietRepository.doanhThuTheoLoaiSanPham();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : data) {
            result.put((String) row[0], (BigDecimal) row[1]);
        }
        return result;
    }
}
