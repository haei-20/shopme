package com.example.gearshop.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.gearshop.dto.NguoiDungDTO;
import com.example.gearshop.model.HoaDon;
import com.example.gearshop.model.KhachHang;
import com.example.gearshop.model.NguoiDung;
import com.example.gearshop.model.NhanVien;
import com.example.gearshop.model.SanPham;
import com.example.gearshop.model.TrangThaiHoaDonHang;
import com.example.gearshop.repository.HoaDonRepository;
import com.example.gearshop.repository.KhachHangRepository;
import com.example.gearshop.repository.NguoiDungRepository;
import com.example.gearshop.repository.NhanVienRepository;
import com.example.gearshop.repository.SanPhamRepository;
import com.example.gearshop.service.NguoiDungService;

import jakarta.transaction.Transactional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final int DASHBOARD_TREND_DAYS = 7;
    private static final int DASHBOARD_RECENT_ORDERS = 5;
    private static final DateTimeFormatter TREND_LABEL = DateTimeFormatter.ofPattern("dd/MM");

    @Autowired
    private NguoiDungRepository nguoiDungRepo;
    @Autowired
    private KhachHangRepository khachHangRepo;
    @Autowired
    private NhanVienRepository nhanVienRepo;
    @Autowired
    private SanPhamRepository sanPhamRepository;
    @Autowired
    private NguoiDungService nguoiDungService;
    @Autowired
    private HoaDonRepository hoaDonRepository;

    /**
     * Dashboard: tổng quan nhanh — KPI, xu hướng 7 ngày, cảnh báo, đơn gần đây.
     * Phân tích theo năm/tháng/lọc sâu nằm ở /admin/thongke.
     */
    @GetMapping("/trangchu")
    public String adminHome(Model model) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.plusDays(1).atStartOfDay();
        int y = today.getYear();
        int m = today.getMonthValue();
        LocalDateTime startOfMonth = LocalDate.of(y, m, 1).atStartOfDay();
        LocalDateTime startOfNextMonth = (m == 12 ? LocalDate.of(y + 1, 1, 1) : LocalDate.of(y, m + 1, 1)).atStartOfDay();

        List<HoaDon> allHoaDons = hoaDonRepository.findAll();
        allHoaDons.sort(Comparator.comparing(HoaDon::getNgayTao, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        BigDecimal doanhThuHomNay = BigDecimal.ZERO;
        int donHangHomNay = 0;
        BigDecimal doanhThuThangNay = BigDecimal.ZERO;
        int donHangThangNay = 0;

        long donCanXuLy = 0;
        long donTrong30Ngay = 0;
        long donDaGiao30Ngay = 0;
        LocalDateTime cutoff30 = today.minusDays(30).atStartOfDay();

        Set<Integer> khachCoDon7Ngay = new HashSet<>();
        LocalDate ngayBatDau7 = today.minusDays(DASHBOARD_TREND_DAYS - 1);

        for (HoaDon hd : allHoaDons) {
            if (hd.getNgayTao() == null) {
                continue;
            }
            LocalDateTime nt = hd.getNgayTao();
            LocalDate nd = nt.toLocalDate();

            if (!nt.isBefore(startOfToday) && nt.isBefore(endOfToday)) {
                donHangHomNay++;
                if (hd.getTongGia() != null) {
                    doanhThuHomNay = doanhThuHomNay.add(hd.getTongGia());
                }
            }
            if (!nt.isBefore(startOfMonth) && nt.isBefore(startOfNextMonth)) {
                donHangThangNay++;
                if (hd.getTongGia() != null) {
                    doanhThuThangNay = doanhThuThangNay.add(hd.getTongGia());
                }
            }

            if (!nt.isBefore(cutoff30)) {
                donTrong30Ngay++;
                if (TrangThaiHoaDonHang.DA_GIAO.equals(hd.getTrangThaiDonHang())) {
                    donDaGiao30Ngay++;
                }
            }

            if (laDonDangCanXuLy(hd)) {
                donCanXuLy++;
            }

            if (!nd.isBefore(ngayBatDau7) && !nd.isAfter(today)
                    && hd.getThongTinNhanHang() != null) {
                khachCoDon7Ngay.add(hd.getThongTinNhanHang().getKhachHangID());
            }
        }

        Integer tyLeHoanThanh30Ngay = null;
        if (donTrong30Ngay > 0) {
            tyLeHoanThanh30Ngay = BigDecimal.valueOf(100L * donDaGiao30Ngay)
                    .divide(BigDecimal.valueOf(donTrong30Ngay), 0, RoundingMode.HALF_UP)
                    .intValue();
        }

        List<String> trendLabels = new ArrayList<>(DASHBOARD_TREND_DAYS);
        BigDecimal[] trendDoanhThu = new BigDecimal[DASHBOARD_TREND_DAYS];
        int[] trendDonHang = new int[DASHBOARD_TREND_DAYS];
        for (int i = 0; i < DASHBOARD_TREND_DAYS; i++) {
            trendDoanhThu[i] = BigDecimal.ZERO;
        }
        for (int i = 0; i < DASHBOARD_TREND_DAYS; i++) {
            LocalDate d = ngayBatDau7.plusDays(i);
            trendLabels.add(d.format(TREND_LABEL));
            LocalDateTime d0 = d.atStartOfDay();
            LocalDateTime d1 = d.plusDays(1).atStartOfDay();
            for (HoaDon hd : allHoaDons) {
                if (hd.getNgayTao() == null) {
                    continue;
                }
                LocalDateTime nt = hd.getNgayTao();
                if (!nt.isBefore(d0) && nt.isBefore(d1)) {
                    trendDonHang[i]++;
                    if (hd.getTongGia() != null) {
                        trendDoanhThu[i] = trendDoanhThu[i].add(hd.getTongGia());
                    }
                }
            }
        }

        List<HoaDon> donGanDay = allHoaDons.size() > DASHBOARD_RECENT_ORDERS
                ? allHoaDons.subList(0, DASHBOARD_RECENT_ORDERS)
                : allHoaDons;

        List<SanPham> topSanPham = sanPhamRepository.findTop10ByOrderByDaBanDesc();
        SanPham sanPhamBanChay = topSanPham.isEmpty() ? null : topSanPham.get(0);

        long tongDonHang = allHoaDons.size();
        long tongKhachHang = khachHangRepo.count();

        long sanPhamSapHet = sanPhamRepository.findAll().stream()
                .filter(sp -> sp.getTonKho() != null && sp.getTonKho() <= 5)
                .count();

        List<BigDecimal> trendDoanhThuList = new ArrayList<>(DASHBOARD_TREND_DAYS);
        for (BigDecimal b : trendDoanhThu) {
            trendDoanhThuList.add(b);
        }
        List<Integer> trendDonHangList = new ArrayList<>(DASHBOARD_TREND_DAYS);
        for (int c : trendDonHang) {
            trendDonHangList.add(c);
        }

        model.addAttribute("doanhThuHomNay", doanhThuHomNay);
        model.addAttribute("donHangHomNay", donHangHomNay);
        model.addAttribute("doanhThuThangNay", doanhThuThangNay);
        model.addAttribute("donHangThangNay", donHangThangNay);
        model.addAttribute("donCanXuLy", donCanXuLy);
        model.addAttribute("tyLeHoanThanh30Ngay", tyLeHoanThanh30Ngay);
        model.addAttribute("donTrong30Ngay", donTrong30Ngay);
        model.addAttribute("donDaGiao30Ngay", donDaGiao30Ngay);
        model.addAttribute("khachCoDon7Ngay", khachCoDon7Ngay.size());

        model.addAttribute("trendLabels", trendLabels);
        model.addAttribute("trendDoanhThu", trendDoanhThuList);
        model.addAttribute("trendDonHang", trendDonHangList);

        model.addAttribute("tongDonHang", tongDonHang);
        model.addAttribute("tongKhachHang", tongKhachHang);
        model.addAttribute("sanPhamBanChay", sanPhamBanChay);
        model.addAttribute("donGanDay", donGanDay);
        model.addAttribute("sanPhamSapHet", sanPhamSapHet);

        return "adminTemplate/trangchuadmin";
    }

    private static boolean laDonDangCanXuLy(HoaDon hd) {
        String s = hd.getTrangThaiDonHang();
        if (s == null) {
            return true;
        }
        return TrangThaiHoaDonHang.CHO_XAC_NHAN.equals(s)
                || TrangThaiHoaDonHang.DANG_CHUAN_BI_HANG.equals(s)
                || TrangThaiHoaDonHang.CHO_GIAO_HANG.equals(s)
                || TrangThaiHoaDonHang.LEGACY_CHO_LAY_HANG.equals(s);
    }

    @GetMapping("/nguoidung")
    public String danhSachNguoiDung(
            @RequestParam(name = "roleFilter", defaultValue = "all") String roleFilter,
            Model model) {

        List<NguoiDung> nguoiDungs = nguoiDungRepo.findAll();
        List<NguoiDungDTO> danhSach = new ArrayList<>();

        for (NguoiDung nd : nguoiDungs) {
            Optional<KhachHang> khachHang = khachHangRepo.findByNguoiDung_Id(nd.getId());
            Optional<NhanVien> nhanVien = nhanVienRepo.findByNguoiDung_Id(nd.getId());

            if (roleFilter.equals("khachhang") && khachHang.isEmpty()) {
                continue;
            }
            if (roleFilter.equals("nhanvien") && nhanVien.isEmpty()) {
                continue;
            }

            NguoiDungDTO dto = new NguoiDungDTO();
            dto.setId(nd.getId());
            dto.setTenNguoiDung(nd.getTenNguoiDung());
            dto.setEmail(nd.getEmail());

            if (khachHang.isPresent()) {
                dto.setVaiTro("Khách hàng");
                dto.setGhiChu(khachHang.get().getGhiChu());
            } else if (nhanVien.isPresent()) {
                dto.setVaiTro("Nhân viên");
                dto.setGhiChu(nhanVien.get().getGhiChu());
            } else {
                dto.setVaiTro("Không xác định");
                dto.setGhiChu("");
            }

            danhSach.add(dto);
        }

        model.addAttribute("danhSachNguoiDung", danhSach);
        model.addAttribute("roleFilter", roleFilter);
        return "adminTemplate/nguoidung";
    }

    @GetMapping("/nguoidung/{id}")
    public String xemChiTietNguoiDung(@PathVariable("id") Integer id, Model model) {
        Optional<NguoiDung> optional = nguoiDungRepo.findById(id);

        if (optional.isPresent()) {
            NguoiDung nd = optional.get();
            boolean isKhachHang = khachHangRepo.findByNguoiDung_Id(nd.getId()).isPresent();
            boolean isNhanVien = nhanVienRepo.findByNguoiDung_Id(nd.getId()).isPresent();

            String vaiTro = isKhachHang ? "Khách hàng" : isNhanVien ? "Nhân viên" : "Không xác định";
            String ghiChu = isKhachHang
                    ? khachHangRepo.findByNguoiDung_Id(nd.getId()).get().getGhiChu()
                    : isNhanVien
                            ? nhanVienRepo.findByNguoiDung_Id(nd.getId()).get().getGhiChu()
                            : "";

            model.addAttribute("nguoiDung", nd);
            model.addAttribute("vaiTro", vaiTro);
            model.addAttribute("ghiChu", ghiChu);
            model.addAttribute("isKhachHang", isKhachHang);
            model.addAttribute("isNhanVien", isNhanVien);

            return "adminTemplate/chitietnguoidung";
        } else {
            return "redirect:/admin/nguoidung?error=notfound";
        }
    }

    @PostMapping("/nguoidung/xoa/{id}")
    @Transactional
    public String xoaNguoiDung(@PathVariable("id") Integer id) {
        khachHangRepo.deleteByNguoiDung_Id(id);
        nhanVienRepo.deleteByNguoiDung_Id(id);
        nguoiDungRepo.deleteById(id);
        return "redirect:/admin/nguoidung";
    }

    @PostMapping("/nguoidung/capquyen/{id}")
    public String capQuyenAdmin(@PathVariable("id") Integer id) {
        nguoiDungService.capNhatQuyenAdmin(id);
        return "redirect:/admin/nguoidung/" + id;
    }

    @PostMapping("/nguoidung/goquyen/{id}")
    public String goQuyenAdmin(@PathVariable("id") Integer id) {
        nguoiDungService.goQuyenAdmin(id);
        return "redirect:/admin/nguoidung/" + id;
    }

    @PostMapping("/nguoidung/capnhatghichu")
    public String capNhatGhiChu(@RequestParam("nguoiDungId") Integer id,
            @RequestParam("ghiChu") String ghiChu) {
        Optional<NguoiDung> nguoiDungOpt = nguoiDungRepo.findById(id);
        if (nguoiDungOpt.isPresent()) {
            if (khachHangRepo.existsByNguoiDung_Id(id)) {
                KhachHang kh = khachHangRepo.findByNguoiDung_Id(id).get();
                kh.setGhiChu(ghiChu);
                khachHangRepo.save(kh);
            } else if (nhanVienRepo.existsByNguoiDung_Id(id)) {
                NhanVien nv = nhanVienRepo.findByNguoiDung_Id(id).get();
                nv.setGhiChu(ghiChu);
                nhanVienRepo.save(nv);
            }
        }
        return "redirect:/admin/nguoidung/" + id;
    }
}
