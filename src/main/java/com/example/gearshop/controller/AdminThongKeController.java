package com.example.gearshop.controller;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.gearshop.dto.ThongKeXepHangRow;
import com.example.gearshop.model.HoaDon;
import com.example.gearshop.model.LoaiSanPham;
import com.example.gearshop.repository.HoaDonRepository;
import com.example.gearshop.repository.LoaiSanPhamRepository;
import com.example.gearshop.service.ThongKeService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/admin/thongke")
public class AdminThongKeController {

    @Autowired
    private ThongKeService thongKeService;
    @Autowired
    private HoaDonRepository hoaDonRepository;
    @Autowired
    private LoaiSanPhamRepository loaiSanPhamRepository;

    @GetMapping
    public String thongKe(
            @RequestParam(name = "statNam", required = false) Integer statNam,
            @RequestParam(name = "statThang", required = false) Integer statThang,
            @RequestParam(name = "statLoaiId", required = false) Integer statLoaiId,
            Model model) {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();

        int yearParam = statNam != null ? statNam : currentYear;
        final int selectedYear = (yearParam < 2000 || yearParam > currentYear + 1) ? currentYear : yearParam;

        Integer rawThang = statThang;
        if (rawThang != null && (rawThang < 1 || rawThang > 12)) {
            rawThang = null;
        }
        final Integer selectedMonth = rawThang;

        Integer loaiLoc = statLoaiId;
        if (loaiLoc != null && loaiLoc <= 0) {
            loaiLoc = null;
        }
        if (loaiLoc != null && loaiSanPhamRepository.findById(loaiLoc) == null) {
            loaiLoc = null;
        }
        final Integer selectedLoaiId = loaiLoc;

        List<Integer> statYearOptions = buildStatYearOptions(currentYear);
        LocalDate tuKhoang;
        LocalDate denKhoang;
        if (selectedMonth != null) {
            LocalDate first = LocalDate.of(selectedYear, selectedMonth, 1);
            tuKhoang = first;
            denKhoang = first.withDayOfMonth(first.lengthOfMonth());
        } else {
            tuKhoang = LocalDate.of(selectedYear, 1, 1);
            denKhoang = LocalDate.of(selectedYear, 12, 31);
        }

        Map<String, BigDecimal> doanhThuNgay = thongKeService.getDoanhThuTheoNgayTrongKhoang(tuKhoang, denKhoang, selectedLoaiId);
        Map<String, BigDecimal> doanhThuThang = selectedMonth == null
                ? thongKeService.getDoanhThuTheoThangTrongNam(selectedYear, selectedLoaiId)
                : thongKeService.getDoanhThuTongMotThang(selectedYear, selectedMonth, selectedLoaiId);
        // Cơ cấu theo loại: luôn theo toàn bộ danh mục trong khoảng ngày đã lọc (không phụ thuộc lọc một loại SP).
        Map<String, BigDecimal> doanhThuLoaiSP = thongKeService.getDoanhThuTheoLoaiSanPhamTrongKhoang(tuKhoang, denKhoang);

        Map<String, Long> donTheoTrangThai = thongKeService.demDonTheoTrangThaiTrongKhoang(tuKhoang, denKhoang);
        List<ThongKeXepHangRow> topSanPham = thongKeService.getTopSanPhamTheoDoanhThu(tuKhoang, denKhoang, selectedLoaiId);
        List<ThongKeXepHangRow> topKhach = thongKeService.getTopKhachTheoDoanhThu(tuKhoang, denKhoang);

        BigDecimal tongDoanhThu = doanhThuNgay.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<LoaiSanPham> loaiSanPhams = loaiSanPhamRepository.findAll();

        model.addAttribute("doanhThuNgay", doanhThuNgay);
        model.addAttribute("doanhThuThang", doanhThuThang);
        model.addAttribute("doanhThuLoaiSP", doanhThuLoaiSP);
        model.addAttribute("donTheoTrangThai", donTheoTrangThai);
        model.addAttribute("topSanPham", topSanPham);
        model.addAttribute("topKhach", topKhach);
        model.addAttribute("tongDoanhThu", tongDoanhThu);
        model.addAttribute("soNgayThongKe", doanhThuNgay.size());
        model.addAttribute("soThangThongKe", doanhThuThang.size());
        model.addAttribute("soLoaiSanPham", doanhThuLoaiSP.size());
        model.addAttribute("statNam", selectedYear);
        model.addAttribute("statThang", selectedMonth);
        model.addAttribute("statLoaiId", selectedLoaiId);
        model.addAttribute("statYearOptions", statYearOptions);
        model.addAttribute("statTuNgay", tuKhoang);
        model.addAttribute("statDenNgay", denKhoang);
        model.addAttribute("loaiSanPhams", loaiSanPhams);

        return "adminTemplate/thongke";
    }

    @GetMapping(value = "/export-csv", produces = "text/csv;charset=UTF-8")
    public void exportDoanhThuNgayCsv(
            @RequestParam(name = "statNam", required = false) Integer statNam,
            @RequestParam(name = "statThang", required = false) Integer statThang,
            @RequestParam(name = "statLoaiId", required = false) Integer statLoaiId,
            HttpServletResponse response) throws IOException {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int yearParam = statNam != null ? statNam : currentYear;
        final int selectedYear = (yearParam < 2000 || yearParam > currentYear + 1) ? currentYear : yearParam;
        Integer rawThang = statThang;
        if (rawThang != null && (rawThang < 1 || rawThang > 12)) {
            rawThang = null;
        }
        final Integer selectedMonth = rawThang;
        Integer loaiLoc = statLoaiId;
        if (loaiLoc != null && loaiLoc <= 0) {
            loaiLoc = null;
        }
        if (loaiLoc != null && loaiSanPhamRepository.findById(loaiLoc) == null) {
            loaiLoc = null;
        }
        final Integer selectedLoaiId = loaiLoc;

        LocalDate tuKhoang;
        LocalDate denKhoang;
        if (selectedMonth != null) {
            LocalDate first = LocalDate.of(selectedYear, selectedMonth, 1);
            tuKhoang = first;
            denKhoang = first.withDayOfMonth(first.lengthOfMonth());
        } else {
            tuKhoang = LocalDate.of(selectedYear, 1, 1);
            denKhoang = LocalDate.of(selectedYear, 12, 31);
        }

        Map<String, BigDecimal> doanhThuNgay = thongKeService.getDoanhThuTheoNgayTrongKhoang(tuKhoang, denKhoang, selectedLoaiId);

        String filename = "thong-ke-doanh-thu-" + selectedYear
                + (selectedMonth != null ? "-" + selectedMonth : "")
                + ".csv";
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (Writer w = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            w.write('\ufeff');
            w.write("Ngay,DoanhThuVND\n");
            for (Map.Entry<String, BigDecimal> e : doanhThuNgay.entrySet()) {
                w.write(csvEscape(e.getKey()) + "," + e.getValue().toPlainString() + "\n");
            }
            w.flush();
        }
    }

    private static String csvEscape(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private List<Integer> buildStatYearOptions(int currentYear) {
        List<HoaDon> allHoaDons = hoaDonRepository.findAll();
        TreeSet<Integer> yearsInData = allHoaDons.stream()
                .map(hd -> hd.getNgayTao() != null ? hd.getNgayTao().getYear() : null)
                .filter(y -> y != null)
                .collect(Collectors.toCollection(TreeSet::new));
        if (yearsInData.isEmpty()) {
            yearsInData.add(currentYear);
        }
        int minYear = yearsInData.first();
        int maxYearOption = Math.max(currentYear, yearsInData.last());
        List<Integer> statYearOptions = new ArrayList<>();
        for (int y = minYear; y <= maxYearOption; y++) {
            statYearOptions.add(y);
        }
        return statYearOptions;
    }
}
