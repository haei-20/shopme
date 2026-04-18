package com.example.gearshop.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
import com.example.gearshop.repository.HoaDonRepository;
import com.example.gearshop.repository.KhachHangRepository;
import com.example.gearshop.repository.NguoiDungRepository;
import com.example.gearshop.repository.NhanVienRepository;
import com.example.gearshop.repository.SanPhamRepository;
import com.example.gearshop.repository.YeuCauHoanTienRepository;
import com.example.gearshop.service.NguoiDungService;

import jakarta.transaction.Transactional;

@Controller
@RequestMapping("/admin")
public class AdminController {

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
    @Autowired
    private YeuCauHoanTienRepository yeuCauHoanTienRepository;

    @GetMapping("/trangchu")
    public String adminHome(Model model) {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();

        List<HoaDon> allHoaDons = hoaDonRepository.findAll();
        allHoaDons.sort(Comparator.comparing(HoaDon::getNgayTao, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        List<HoaDon> hoaDonsTheoNamHienTai = allHoaDons.stream()
            .filter(hd -> hd.getNgayTao() != null && hd.getNgayTao().getYear() == currentYear)
            .toList();

        BigDecimal doanhThuHomNay = BigDecimal.ZERO;
        int donHangHomNay = 0;

        for (HoaDon hd : allHoaDons) {
            if (hd.getNgayTao() != null && hd.getNgayTao().toLocalDate().equals(today)) {
                donHangHomNay++;
                if (hd.getTongGia() != null) {
                    doanhThuHomNay = doanhThuHomNay.add(hd.getTongGia());
                }
            }
        }

        List<HoaDon> donGanDay = allHoaDons.size() > 8 ? allHoaDons.subList(0, 8) : allHoaDons;

        List<SanPham> topSanPham = sanPhamRepository.findTop10ByOrderByDaBanDesc();
        SanPham sanPhamBanChay = topSanPham.isEmpty() ? null : topSanPham.get(0);

        long tongDonHang = allHoaDons.size();
        long tongKhachHang = khachHangRepo.count();
        long tongSanPham = sanPhamRepository.count();

        long sanPhamSapHet = sanPhamRepository.findAll().stream()
                .filter(sp -> sp.getTonKho() != null && sp.getTonKho() <= 5)
                .count();

        long yeuCauHoanTienMoi = yeuCauHoanTienRepository.findAll().stream()
                .filter(yc -> yc.getTrangThai() != null && yc.getTrangThai().toLowerCase().contains("chua"))
                .count();

        int[] donTheoThang = new int[12];
        for (HoaDon hd : hoaDonsTheoNamHienTai) {
            if (hd.getNgayTao() != null) {
                int month = hd.getNgayTao().getMonthValue();
                donTheoThang[month - 1]++;
            }
        }

        BigDecimal[] doanhThuTheoThang = new BigDecimal[12];
        Arrays.fill(doanhThuTheoThang, BigDecimal.ZERO);
        for (HoaDon hd : hoaDonsTheoNamHienTai) {
            if (hd.getNgayTao() != null && hd.getTongGia() != null) {
                int month = hd.getNgayTao().getMonthValue();
                doanhThuTheoThang[month - 1] = doanhThuTheoThang[month - 1].add(hd.getTongGia());
            }
        }

        List<String> monthLabels = Arrays.asList("T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12");
        List<Integer> donTheoThangList = Arrays.stream(donTheoThang).boxed().toList();
        List<BigDecimal> doanhThuTheoThangList = Arrays.asList(doanhThuTheoThang);

        model.addAttribute("doanhThuHomNay", doanhThuHomNay);
        model.addAttribute("donHangHomNay", donHangHomNay);
        model.addAttribute("tongDonHang", tongDonHang);
        model.addAttribute("tongKhachHang", tongKhachHang);
        model.addAttribute("tongSanPham", tongSanPham);
        model.addAttribute("sanPhamBanChay", sanPhamBanChay);
        model.addAttribute("donGanDay", donGanDay);
        model.addAttribute("monthLabels", monthLabels);
        model.addAttribute("donTheoThang", donTheoThangList);
        model.addAttribute("doanhThuTheoThang", doanhThuTheoThangList);
        model.addAttribute("chartNam", currentYear);
        model.addAttribute("yeuCauHoanTienMoi", yeuCauHoanTienMoi);
        model.addAttribute("sanPhamSapHet", sanPhamSapHet);

        return "adminTemplate/trangchuadmin";
    }

    @GetMapping("/hoantien")
    public String hoanTienRedirect() {
        return "redirect:/admin/yeucauhoantien";
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

            // Kiểm tra roleFilter
            if (roleFilter.equals("khachhang") && khachHang.isEmpty())
                continue;
            if (roleFilter.equals("nhanvien") && nhanVien.isEmpty())
                continue;

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
        model.addAttribute("roleFilter", roleFilter); // Để giữ lựa chọn đã chọn
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
        // Xóa bản ghi trong bảng khachhang nếu tồn tại
        khachHangRepo.deleteByNguoiDung_Id(id);

        // Xóa bản ghi trong bảng nhanvien nếu tồn tại
        nhanVienRepo.deleteByNguoiDung_Id(id);

        nguoiDungRepo.deleteById(id); // bạn cần xoá cascade trong DB hoặc code
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
