package com.example.gearshop.service;

import com.example.gearshop.dto.SanPhamTrongHoaDonDTO;
import com.example.gearshop.model.HoaDon;
import com.example.gearshop.model.HoaDonChiTiet;
import com.example.gearshop.model.SanPham;
import com.example.gearshop.model.TrangThaiHoaDonHang;
import com.example.gearshop.repository.HoaDonRepository;
import com.example.gearshop.repository.SanPhamRepository;
import com.example.gearshop.repository.ThongTinNhanHangRepository;
import com.example.gearshop.repository.HoaDonChiTietRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class HoaDonService {

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private ThongTinNhanHangRepository thongTinNhanHangRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    public HoaDon createHoaDon(String maHoaDonPrefix, int thongTinNhanHangID, double tongGia,
            String phuongThucThanhToan) {
        String maHoaDon = generateMaHoaDon(maHoaDonPrefix); // Tạo mã hóa đơn tự động

        HoaDon hoaDon = new HoaDon();
        hoaDon.setMaHoaDon(maHoaDon);
        hoaDon.setThongTinNhanHang(thongTinNhanHangRepository.findById(thongTinNhanHangID));
        hoaDon.setNgayTao(java.time.LocalDateTime.now());
        hoaDon.setTongGia(BigDecimal.valueOf(tongGia));
        hoaDon.setTrangThaiDonHang(TrangThaiHoaDonHang.CHO_XAC_NHAN);
        hoaDon.setPhuongThucThanhToan(
                "COD".equalsIgnoreCase(phuongThucThanhToan) ? "COD" : "BANK_TRANSFER");

        return hoaDonRepository.save(hoaDon); // Lưu vào cơ sở dữ liệu
    }

    /**
     * Kiểm tra tồn kho, tạo hóa đơn + chi tiết, trừ kho và tăng {@link SanPham#getDaBan()}.
     * Toàn bộ trong một giao dịch — lỗi sẽ rollback.
     */
    @Transactional
    public DatHangKetQua datHangVaTruTonKho(int thongTinNhanHangID, double tongGiaSauGiam, String paymentMethod,
            List<Map<String, Object>> selectedItems) {
        if (selectedItems == null || selectedItems.isEmpty()) {
            throw new IllegalArgumentException("Không có sản phẩm để đặt hàng.");
        }

        List<OrderLine> lines = new ArrayList<>();
        for (Map<String, Object> item : selectedItems) {
            Object quantityObj = item.get("quantity");
            Object priceObj = item.get("priceNumeric");
            Object sanPhamIDObj = item.get("sanPhamID");
            if (quantityObj == null || priceObj == null || sanPhamIDObj == null) {
                throw new IllegalArgumentException("Dữ liệu giỏ hàng không hợp lệ.");
            }
            int quantity = Integer.parseInt(quantityObj.toString());
            double unitPrice = Double.parseDouble(priceObj.toString().replace(",", "").replace("₫", "").trim());
            int sanPhamID = Integer.parseInt(sanPhamIDObj.toString());
            if (quantity < 1) {
                throw new IllegalArgumentException("Số lượng sản phẩm không hợp lệ.");
            }
            lines.add(new OrderLine(sanPhamID, quantity, unitPrice));
        }

        Map<Integer, Integer> tongSoLuongTheoSanPham = new LinkedHashMap<>();
        for (OrderLine line : lines) {
            tongSoLuongTheoSanPham.merge(line.sanPhamId(), line.quantity(), Integer::sum);
        }

        Map<Integer, SanPham> sanPhamDaTai = new HashMap<>();
        for (Map.Entry<Integer, Integer> e : tongSoLuongTheoSanPham.entrySet()) {
            SanPham sp = sanPhamRepository.findById(e.getKey());
            if (sp == null) {
                throw new IllegalArgumentException("Không tìm thấy sản phẩm (mã " + e.getKey() + ").");
            }
            int ton = sp.getTonKho() == null ? 0 : sp.getTonKho();
            int can = e.getValue();
            if (ton < can) {
                throw new IllegalArgumentException(String.format(
                        "Sản phẩm \"%s\" chỉ còn %d trong kho, không đủ cho số lượng đặt (%d).",
                        sp.getTenSanPham(), ton, can));
            }
            sanPhamDaTai.put(e.getKey(), sp);
        }

        HoaDon hoaDon = createHoaDon("HD", thongTinNhanHangID, tongGiaSauGiam, paymentMethod);

        Set<Integer> idDaMua = new LinkedHashSet<>();
        for (OrderLine line : lines) {
            double thanhTien = line.quantity() * line.unitPrice();
            createHoaDonChiTiet("HDCT", hoaDon.getId(), line.sanPhamId(), line.quantity(), thanhTien);
            idDaMua.add(line.sanPhamId());
        }

        for (Map.Entry<Integer, Integer> e : tongSoLuongTheoSanPham.entrySet()) {
            SanPham sp = sanPhamDaTai.get(e.getKey());
            int tru = e.getValue();
            int tonHienTai = sp.getTonKho() == null ? 0 : sp.getTonKho();
            sp.setTonKho(tonHienTai - tru);
            int daBan = sp.getDaBan() == null ? 0 : sp.getDaBan();
            sp.setDaBan(daBan + tru);
            sanPhamRepository.save(sp);
        }

        return new DatHangKetQua(hoaDon, new ArrayList<>(idDaMua));
    }

    public record DatHangKetQua(HoaDon hoaDon, List<Integer> purchasedSanPhamIds) {
    }

    private record OrderLine(int sanPhamId, int quantity, double unitPrice) {
    }

    public HoaDonChiTiet createHoaDonChiTiet(String maHoaDonChiTietPrefix, int hoaDonID, int sanPhamID, int soLuongSP,
            double thanhTien) {
        System.out.println("Dang luu chi tiet hoa don...");
        System.out.println("Ma hoa don chi tiet: " + maHoaDonChiTietPrefix);
        System.out.println("Hoa don ID: " + hoaDonID);
        System.out.println("San pham ID: " + sanPhamID);
        System.out.println("So luong: " + soLuongSP);
        System.out.println("Thanh tien: " + thanhTien);

        HoaDonChiTiet hoaDonChiTiet = new HoaDonChiTiet();
        hoaDonChiTiet.setMaHoaDonChiTiet(maHoaDonChiTietPrefix);
        hoaDonChiTiet.setHoaDonID(hoaDonID);
        hoaDonChiTiet.setSanPhamID(sanPhamID);
        hoaDonChiTiet.setSoLuongSP(soLuongSP);
        hoaDonChiTiet.setThanhTien(BigDecimal.valueOf(thanhTien));

        return hoaDonChiTietRepository.save(hoaDonChiTiet); // Lưu vào cơ sở dữ liệu
    }

    private String generateMaHoaDon(String prefix) {
        Optional<HoaDon> lastHoaDon = hoaDonRepository.findTopByOrderByIdDesc();
        if (lastHoaDon.isPresent()) {
            String lastMaHoaDon = lastHoaDon.get().getMaHoaDon();
            int lastNumber = Integer.parseInt(lastMaHoaDon.replace(prefix, ""));
            return prefix + String.format("%03d", lastNumber + 1);
        }
        return prefix + "001";
    }

    private String generateMaHoaDonChiTiet(String prefix) {
        Optional<HoaDonChiTiet> lastHoaDonChiTiet = hoaDonChiTietRepository.findTopByOrderByIdDesc();
        if (lastHoaDonChiTiet.isPresent()) {
            String lastMaHoaDonChiTiet = lastHoaDonChiTiet.get().getMaHoaDonChiTiet();
            int lastNumber = Integer.parseInt(lastMaHoaDonChiTiet.replace(prefix, ""));
            return prefix + String.format("%03d", lastNumber + 1);
        }
        return prefix + "001";
    }

    public HoaDon findById(int id) {
        return hoaDonRepository.findById(id).orElse(null);
    }

    public List<HoaDon> getHoaDonsByKhachHangID(Integer khachHangID, String sortBy, String trangThai) {
        List<HoaDon> hoaDons = hoaDonRepository.findByThongTinNhanHang_KhachHangID(khachHangID);
        return applyFiltersAndSorting(hoaDons, sortBy, trangThai);
    }

    public Page<HoaDon> getHoaDonsPageByKhachHangID(Integer khachHangID, String sortBy, String trangThai, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, size);
        Pageable pageable = PageRequest.of(safePage, safeSize, resolveSort(sortBy));

        List<String> trangThaiDb = resolveDbTrangThaiList(trangThai);
        if (trangThaiDb == null) {
            return hoaDonRepository.findByThongTinNhanHang_KhachHangID(khachHangID, pageable);
        }
        return hoaDonRepository.findByThongTinNhanHang_KhachHangIDAndTrangThaiDonHangIn(khachHangID, trangThaiDb, pageable);
    }

    public List<HoaDon> getHoaDonsByNguoiDungId(Integer nguoiDungId, String sortBy, String trangThai) {
        List<HoaDon> hoaDons = hoaDonRepository.findByNguoiDungId(nguoiDungId);
        return applyFiltersAndSorting(hoaDons, sortBy, trangThai);
    }

    private Sort resolveSort(String sortBy) {
        if ("ngayTaoAsc".equals(sortBy)) {
            return Sort.by(Sort.Direction.ASC, "ngayTao");
        }
        if ("tongGia".equals(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "tongGia");
        }
        return Sort.by(Sort.Direction.DESC, "ngayTao");
    }

    private List<String> resolveDbTrangThaiList(String trangThai) {
        if (trangThai == null || trangThai.isBlank()) {
            return null;
        }
        String loc = trangThai.trim();
        if (TrangThaiHoaDonHang.DANG_CHUAN_BI_HANG.equals(loc)) {
            return List.of(TrangThaiHoaDonHang.DANG_CHUAN_BI_HANG, TrangThaiHoaDonHang.LEGACY_CHO_LAY_HANG,
                    TrangThaiHoaDonHang.LEGACY_PAID, TrangThaiHoaDonHang.LEGACY_DU, TrangThaiHoaDonHang.LEGACY_EXTRA,
                    TrangThaiHoaDonHang.LEGACY_THUA);
        }
        if (TrangThaiHoaDonHang.CHO_XAC_NHAN.equals(loc)) {
            return List.of(TrangThaiHoaDonHang.CHO_XAC_NHAN, TrangThaiHoaDonHang.LEGACY_UNPAID,
                    TrangThaiHoaDonHang.LEGACY_CHUA_TT, TrangThaiHoaDonHang.LEGACY_MISSING,
                    TrangThaiHoaDonHang.LEGACY_THIEU);
        }
        return List.of(loc);
    }

    public boolean belongsToNguoiDung(Integer hoaDonId, Integer nguoiDungId) {
        return hoaDonRepository.existsByIdAndNguoiDungId(hoaDonId, nguoiDungId);
    }

    /** Đơn thuộc đúng khách (theo {@code ThongTinNhanHang.khachHangID}) — dùng cho trang khách. */
    public boolean belongsToKhachHang(Integer hoaDonId, Integer khachHangId) {
        if (hoaDonId == null || khachHangId == null) {
            return false;
        }
        return hoaDonRepository.existsByIdAndThongTinNhanHang_KhachHangID(hoaDonId, khachHangId);
    }

    private List<HoaDon> applyFiltersAndSorting(List<HoaDon> hoaDons, String sortBy, String trangThai) {
        if (trangThai != null && !trangThai.isEmpty()) {
            String loc = trangThai.trim();
            if (TrangThaiHoaDonHang.LEGACY_CHO_LAY_HANG.equals(loc)) {
                loc = TrangThaiHoaDonHang.DANG_CHUAN_BI_HANG;
            }
            final String trangThaiLoc = loc;
            hoaDons = hoaDons.stream()
                    .filter(hd -> matchesTrangThaiLoc(hd.getTrangThaiDonHang(), trangThaiLoc))
                    .toList();
        }

        if ("ngayTaoAsc".equals(sortBy)) {
            hoaDons.sort(Comparator.comparing(HoaDon::getNgayTao));
        } else if ("ngayTaoDesc".equals(sortBy)) {
            hoaDons.sort(Comparator.comparing(HoaDon::getNgayTao).reversed());
        } else if ("tongGia".equals(sortBy)) {
            hoaDons.sort(Comparator.comparing(HoaDon::getTongGia).reversed());
        }
        return hoaDons;
    }

    /**
     * Lọc theo trạng thái vận đơn; gom giá trị cũ (thanh toán) về nhóm tương ứng.
     */
    private boolean matchesTrangThaiLoc(String giaTriDb, String trangThaiChon) {
        return chuanHoaTrangThai(giaTriDb).equals(trangThaiChon);
    }

    /**
     * Map DB → một trong {@link TrangThaiHoaDonHang} chuẩn (để lọc / so sánh).
     */
    public static String chuanHoaTrangThai(String giaTriDb) {
        if (giaTriDb == null || giaTriDb.isBlank()) {
            return "";
        }
        String s = giaTriDb.trim();
        if (TrangThaiHoaDonHang.danhSachLoc().contains(s)) {
            return s;
        }
        if (TrangThaiHoaDonHang.LEGACY_CHO_LAY_HANG.equals(s)) {
            return TrangThaiHoaDonHang.DANG_CHUAN_BI_HANG;
        }
        if (TrangThaiHoaDonHang.LEGACY_UNPAID.equalsIgnoreCase(s)
                || TrangThaiHoaDonHang.LEGACY_CHUA_TT.equals(s)
                || TrangThaiHoaDonHang.LEGACY_MISSING.equalsIgnoreCase(s)
                || TrangThaiHoaDonHang.LEGACY_THIEU.equals(s)) {
            return TrangThaiHoaDonHang.CHO_XAC_NHAN;
        }
        if (TrangThaiHoaDonHang.LEGACY_PAID.equalsIgnoreCase(s)
                || TrangThaiHoaDonHang.LEGACY_DU.equals(s)
                || TrangThaiHoaDonHang.LEGACY_EXTRA.equalsIgnoreCase(s)
                || TrangThaiHoaDonHang.LEGACY_THUA.equals(s)) {
            return TrangThaiHoaDonHang.DANG_CHUAN_BI_HANG;
        }
        return s;
    }

    public HoaDon getHoaDonById(Integer hoaDonId) {
        return hoaDonRepository.findById(hoaDonId)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại"));
    }

    public List<SanPhamTrongHoaDonDTO> getSanPhamTrongHoaDon(Integer hoaDonID) {
        List<HoaDonChiTiet> chiTietList = hoaDonChiTietRepository.findByHoaDonID(hoaDonID);
        List<SanPhamTrongHoaDonDTO> result = new ArrayList<>();
        for (HoaDonChiTiet chiTiet : chiTietList) {
            SanPham sanPham = sanPhamRepository.findById(chiTiet.getSanPhamID());
            result.add(new SanPhamTrongHoaDonDTO(chiTiet, sanPham));
        }
        return result;
    }

    public HoaDon save(HoaDon hoaDon) {
        if (hoaDon == null || hoaDon.getId() == null) {
            throw new IllegalArgumentException("HoaDon or HoaDon ID must not be null");
        }

        Optional<HoaDon> existingHoaDon = hoaDonRepository.findById(hoaDon.getId());
        if (existingHoaDon.isPresent()) {
            HoaDon updatedHoaDon = existingHoaDon.get();
            updatedHoaDon.setTrangThaiDonHang(hoaDon.getTrangThaiDonHang()); // Cập nhật trạng thái đơn hàng
            updatedHoaDon.setTongGia(hoaDon.getTongGia()); // Cập nhật tổng giá (nếu cần)
            updatedHoaDon.setNgayTao(hoaDon.getNgayTao()); // Cập nhật ngày tạo (nếu cần)
            return hoaDonRepository.save(updatedHoaDon); // Lưu vào cơ sở dữ liệu
        } else {
            throw new IllegalArgumentException("HoaDon with ID " + hoaDon.getId() + " does not exist");
        }
    }
}