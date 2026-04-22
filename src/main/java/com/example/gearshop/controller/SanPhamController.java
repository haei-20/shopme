package com.example.gearshop.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.gearshop.model.SanPham;
import com.example.gearshop.model.SanPhamCPU;
import com.example.gearshop.model.SanPhamCase;
import com.example.gearshop.model.SanPhamCooler;
import com.example.gearshop.model.SanPhamMainBoard;
import com.example.gearshop.model.SanPhamManHinh;
import com.example.gearshop.model.SanPhamOCung;
import com.example.gearshop.model.SanPhamPSU;
import com.example.gearshop.model.SanPhamRAM;
import com.example.gearshop.model.SanPhamVGA;
import com.example.gearshop.service.MainboardService;
import com.example.gearshop.service.SanPhamCPUService;
import com.example.gearshop.service.SanPhamCaseService;
import com.example.gearshop.service.SanPhamCoolerService;
import com.example.gearshop.service.SanPhamManHinhService;
import com.example.gearshop.service.SanPhamOCungService;
import com.example.gearshop.service.SanPhamPSUService;
import com.example.gearshop.service.SanPhamRAMService;
import com.example.gearshop.service.SanPhamService;
import com.example.gearshop.service.SanPhamVGAService;

@Controller
public class SanPhamController {

    @Autowired
    private SanPhamService sanPhamService;
    @Autowired
    private MainboardService mainboardService;
    @Autowired
    private SanPhamCPUService cpuService;
    @Autowired
    private SanPhamRAMService RService;
    @Autowired
    private SanPhamVGAService vgaService;
    @Autowired
    private SanPhamOCungService ocungService;
    @Autowired
    private SanPhamCoolerService coolerService;
    @Autowired
    private SanPhamPSUService psuService;
    @Autowired
    private SanPhamCaseService caseService;
    @Autowired
    private SanPhamManHinhService manHinhService;

    @GetMapping("/sanpham")
    public String danhSachSanPham(Model model) {
        List<SanPham> list = sanPhamService.getAllSanPham();
        model.addAttribute("sanphams", list);
        return "sanpham"; // Trả về tên view Thymeleaf: sanpham.html
    }

    @GetMapping("/sanphammainboard")
    public String listMainboards(
            @RequestParam(required = false) String[] thuongHieu,
            @RequestParam(required = false) String[] modelMain,
            @RequestParam(required = false) String[] chipset,
            @RequestParam(required = false) String[] socketMain,
            @RequestParam(required = false) String[] kichThuoc,
            @RequestParam(required = false) String[] soKheRAM,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String sort,
            Model model) {
        String normalizedSort = normalizeSort(sort);
        List<SanPhamMainBoard> mainboards = mainboardService.findFiltered(
                thuongHieu, modelMain, chipset, socketMain, kichThuoc, soKheRAM, minPrice, maxPrice, normalizedSort);
        System.out.println("Tìm thấy " + mainboards.size() + " mainboard");
        List<SanPhamMainBoard> thuongHieuOptions = mainboardService.findFiltered(
                null, modelMain, chipset, socketMain, kichThuoc, soKheRAM, minPrice, maxPrice, null);
        List<SanPhamMainBoard> modelOptions = mainboardService.findFiltered(
                thuongHieu, null, chipset, socketMain, kichThuoc, soKheRAM, minPrice, maxPrice, null);
        List<SanPhamMainBoard> chipsetOptions = mainboardService.findFiltered(
                thuongHieu, modelMain, null, socketMain, kichThuoc, soKheRAM, minPrice, maxPrice, null);
        List<SanPhamMainBoard> socketOptions = mainboardService.findFiltered(
                thuongHieu, modelMain, chipset, null, kichThuoc, soKheRAM, minPrice, maxPrice, null);
        List<SanPhamMainBoard> kichThuocOptions = mainboardService.findFiltered(
                thuongHieu, modelMain, chipset, socketMain, null, soKheRAM, minPrice, maxPrice, null);
        List<SanPhamMainBoard> soKheRamOptions = mainboardService.findFiltered(
                thuongHieu, modelMain, chipset, socketMain, kichThuoc, null, minPrice, maxPrice, null);

        model.addAttribute("mainboards", mainboards);
        model.addAttribute("thuongHieuSet",
                extractSet(thuongHieuOptions, mb -> mb.getSanPham().getThuongHieu().getTenThuongHieu()));
        model.addAttribute("modelMainSet", extractSet(modelOptions, SanPhamMainBoard::getModelMain));
        model.addAttribute("chipsetSet", extractSet(chipsetOptions, SanPhamMainBoard::getChipset));
        model.addAttribute("socketMainSet", extractSet(socketOptions, SanPhamMainBoard::getSocketMain));
        model.addAttribute("kichThuocSet", extractSet(kichThuocOptions, SanPhamMainBoard::getKichThuoc));
        model.addAttribute("soKheRAMSet",
                extractSet(soKheRamOptions, mb -> mb.getSoKheRAM() == null ? null : mb.getSoKheRAM().toString()));

        model.addAttribute("selectedThuongHieu", thuongHieu != null ? thuongHieu : new String[0]);
        model.addAttribute("selectedModelMain", modelMain != null ? modelMain : new String[0]);
        model.addAttribute("selectedChipset", chipset != null ? chipset : new String[0]);
        model.addAttribute("selectedSocketMain", socketMain != null ? socketMain : new String[0]);
        model.addAttribute("selectedKichThuoc", kichThuoc != null ? kichThuoc : new String[0]);
        model.addAttribute("selectedSoKheRAM", soKheRAM != null ? soKheRAM : new String[0]);
        applyPriceSortState(model, minPrice, maxPrice, normalizedSort);

        return "clientTemplate/sanphammainboard";
    }

    private Set<String> extractSet(List<SanPhamMainBoard> list, Function<SanPhamMainBoard, String> getter) {
        return list.stream().map(getter).filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new));
    }

    private <T> List<String> extractDistinctStrings(List<T> list, Function<T, String> getter) {
        return list.stream().map(getter).filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
    }

    private <T> List<Integer> extractDistinctIntegers(List<T> list, Function<T, Integer> getter) {
        return list.stream().map(getter).filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
    }

    private <T> List<BigDecimal> extractDistinctBigDecimals(List<T> list, Function<T, BigDecimal> getter) {
        return list.stream().map(getter).filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
    }

    private <T> List<Boolean> extractDistinctBooleans(List<T> list, Function<T, Boolean> getter) {
        return list.stream().map(getter).filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
    }

    @GetMapping("/sanphamcpu")
    public String hienThiCPU(Model model,
            @RequestParam(required = false) String loaiCPU,
            @RequestParam(required = false) String soNhanSoLuong,
            @RequestParam(required = false) Long giaMin,
            @RequestParam(required = false) Long giaMax,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String thuongHieu) {
        String normalizedSort = normalizeSort(sort);

        List<SanPhamCPU> danhSachCPU = cpuService.filterCPUs(loaiCPU, soNhanSoLuong, giaMin, giaMax, normalizedSort,
                thuongHieu);
        List<SanPhamCPU> thuongHieuOptions = cpuService.filterCPUs(loaiCPU, soNhanSoLuong, giaMin, giaMax, null, null);
        List<SanPhamCPU> loaiCpuOptions = cpuService.filterCPUs(null, soNhanSoLuong, giaMin, giaMax, null, thuongHieu);
        List<SanPhamCPU> soNhanSoLuongOptions = cpuService.filterCPUs(loaiCPU, null, giaMin, giaMax, null, thuongHieu);
        System.out.println("Tìm thấy " + danhSachCPU.size() + " mainboard");
        model.addAttribute("dsCPU", danhSachCPU);
        model.addAttribute("dsLoaiCPU", extractDistinctStrings(loaiCpuOptions, SanPhamCPU::getLoaiCPU));
        model.addAttribute("dsSoNhanSoLuong", extractDistinctStrings(soNhanSoLuongOptions, SanPhamCPU::getSoNhansoLuong));
        model.addAttribute("dsThuongHieu", extractDistinctStrings(thuongHieuOptions,
                cpu -> cpu.getSanPham().getThuongHieu().getTenThuongHieu()));

        model.addAttribute("dsCPU", danhSachCPU);
        model.addAttribute("loaiCPU", loaiCPU);
        model.addAttribute("soNhanSoLuong", soNhanSoLuong);
        applyPriceSortState(model, giaMin, giaMax, normalizedSort);

        return "clientTemplate/sanphamcpu";
    }

    @GetMapping("/sanphamram")
    public String hienThiSanPhamRAM(Model model,
            @RequestParam(required = false) String thuongHieu,
            @RequestParam(required = false) String chuanRAM,
            @RequestParam(required = false) String dungLuong,
            @RequestParam(required = false) Long giaMin,
            @RequestParam(required = false) Long giaMax,
            @RequestParam(required = false) String sort) {
        String normalizedSort = normalizeSort(sort);

        List<SanPhamRAM> danhSach = RService.locSanPham(thuongHieu, chuanRAM, dungLuong, giaMin, giaMax,
                normalizedSort);
        List<SanPhamRAM> thuongHieuOptions = RService.locSanPham(null, chuanRAM, dungLuong, giaMin, giaMax, null);
        List<SanPhamRAM> chuanRamOptions = RService.locSanPham(thuongHieu, null, dungLuong, giaMin, giaMax, null);
        List<SanPhamRAM> dungLuongOptions = RService.locSanPham(thuongHieu, chuanRAM, null, giaMin, giaMax, null);
        model.addAttribute("dsSanPhamRAM", danhSach);

        model.addAttribute("dsThuongHieu", extractDistinctStrings(thuongHieuOptions,
                ram -> ram.getSanPham().getThuongHieu().getTenThuongHieu()));
        model.addAttribute("dsChuanRAM", extractDistinctStrings(chuanRamOptions, SanPhamRAM::getChuanRAM));
        model.addAttribute("dsDungLuong", extractDistinctStrings(dungLuongOptions, SanPhamRAM::getDungLuong));

        model.addAttribute("dsSanPhamRAM", danhSach);
        model.addAttribute("thuongHieu", thuongHieu);
        model.addAttribute("chuanRAM", chuanRAM);
        model.addAttribute("dungLuong", dungLuong);
        applyPriceSortState(model, giaMin, giaMax, normalizedSort);

        return "clientTemplate/sanphamram";
    }

    @GetMapping("/sanphamvga")
    public String hienThiVGA(Model model,
            @RequestParam(required = false) String kieuBoNho,
            @RequestParam(required = false) String dungLuongBoNho,
            @RequestParam(required = false) String chipGPU,
            @RequestParam(required = false) Long giaMin,
            @RequestParam(required = false) Long giaMax,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String thuongHieu) {
        String normalizedSort = normalizeSort(sort);

        List<SanPhamVGA> danhSachVGA = vgaService.filterVGAs(kieuBoNho, dungLuongBoNho, chipGPU, giaMin, giaMax,
                normalizedSort, thuongHieu);
        List<SanPhamVGA> thuongHieuOptions = vgaService.filterVGAs(kieuBoNho, dungLuongBoNho, chipGPU, giaMin, giaMax,
                null, null);
        List<SanPhamVGA> kieuBoNhoOptions = vgaService.filterVGAs(null, dungLuongBoNho, chipGPU, giaMin, giaMax,
                null, thuongHieu);
        List<SanPhamVGA> dungLuongBoNhoOptions = vgaService.filterVGAs(kieuBoNho, null, chipGPU, giaMin, giaMax,
                null, thuongHieu);
        List<SanPhamVGA> chipGpuOptions = vgaService.filterVGAs(kieuBoNho, dungLuongBoNho, null, giaMin, giaMax,
                null, thuongHieu);

        model.addAttribute("dsVGA", danhSachVGA);
        model.addAttribute("dsKieuBoNho", extractDistinctStrings(kieuBoNhoOptions, SanPhamVGA::getKieuBoNho));
        model.addAttribute("dsDungLuongBoNho",
                extractDistinctStrings(dungLuongBoNhoOptions, SanPhamVGA::getDungLuongBoNho));
        model.addAttribute("dsChipGPU", extractDistinctStrings(chipGpuOptions, SanPhamVGA::getChipGPU));
        model.addAttribute("dsThuongHieu", extractDistinctStrings(thuongHieuOptions,
                vga -> vga.getSanPham().getThuongHieu().getTenThuongHieu()));

        model.addAttribute("kieuBoNho", kieuBoNho);
        model.addAttribute("dungLuongBoNho", dungLuongBoNho);
        model.addAttribute("chipGPU", chipGPU);
        applyPriceSortState(model, giaMin, giaMax, normalizedSort);
        model.addAttribute("thuongHieu", thuongHieu);

        return "clientTemplate/sanphamvga";
    }

    @GetMapping("/sanphamocung")
    public String hienThiOCung(Model model,
            @RequestParam(required = false) String loaiOCung,
            @RequestParam(required = false) String dungLuong,
            @RequestParam(required = false) Long giaMin,
            @RequestParam(required = false) Long giaMax,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String thuongHieu) {
        String normalizedSort = normalizeSort(sort);

        List<SanPhamOCung> dsOCung = ocungService.filterOCung(loaiOCung, dungLuong, giaMin, giaMax, normalizedSort,
                thuongHieu);
        List<SanPhamOCung> thuongHieuOptions = ocungService.filterOCung(loaiOCung, dungLuong, giaMin, giaMax, null,
                null);
        List<SanPhamOCung> loaiOCungOptions = ocungService.filterOCung(null, dungLuong, giaMin, giaMax, null,
                thuongHieu);
        List<SanPhamOCung> dungLuongOptions = ocungService.filterOCung(loaiOCung, null, giaMin, giaMax, null,
                thuongHieu);

        model.addAttribute("dsOCung", dsOCung);
        model.addAttribute("dsLoaiOCung", extractDistinctStrings(loaiOCungOptions, SanPhamOCung::getLoaiOCung));
        model.addAttribute("dsDungLuong", extractDistinctStrings(dungLuongOptions, SanPhamOCung::getDungLuong));
        model.addAttribute("dsThuongHieu", extractDistinctStrings(thuongHieuOptions,
                o -> o.getSanPham().getThuongHieu().getTenThuongHieu()));

        model.addAttribute("loaiOCung", loaiOCung);
        model.addAttribute("dungLuong", dungLuong);
        applyPriceSortState(model, giaMin, giaMax, normalizedSort);
        model.addAttribute("thuongHieu", thuongHieu);

        return "clientTemplate/sanphamocung";
    }

    @GetMapping("/sanphamcooler")
    public String hienThiCooler(Model model,
            @RequestParam(required = false) String loaiTan,
            @RequestParam(required = false) Boolean coLED,
            @RequestParam(required = false) Long giaMin,
            @RequestParam(required = false) Long giaMax,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String thuongHieu) {
        String normalizedSort = normalizeSort(sort);

        List<SanPhamCooler> danhSachCooler = coolerService.filterCooler(loaiTan, coLED, giaMin, giaMax, normalizedSort,
                thuongHieu);
        List<SanPhamCooler> thuongHieuOptions = coolerService.filterCooler(loaiTan, coLED, giaMin, giaMax, null, null);
        List<SanPhamCooler> loaiTanOptions = coolerService.filterCooler(null, coLED, giaMin, giaMax, null, thuongHieu);
        List<SanPhamCooler> coLedOptions = coolerService.filterCooler(loaiTan, null, giaMin, giaMax, null, thuongHieu);

        model.addAttribute("dsCooler", danhSachCooler);
        model.addAttribute("dsLoaiTan", extractDistinctStrings(loaiTanOptions, SanPhamCooler::getLoaiTan));
        model.addAttribute("dsCoLED", extractDistinctBooleans(coLedOptions, SanPhamCooler::getCoLED));
        model.addAttribute("dsThuongHieu", extractDistinctStrings(thuongHieuOptions,
                cooler -> cooler.getSanPham().getThuongHieu().getTenThuongHieu()));

        model.addAttribute("loaiTan", loaiTan);
        model.addAttribute("coLED", coLED);
        applyPriceSortState(model, giaMin, giaMax, normalizedSort);
        model.addAttribute("thuongHieu", thuongHieu);

        return "clientTemplate/sanphamcooler";
    }

    @GetMapping("/sanphampsu")
    public String hienThiPSU(Model model,
            @RequestParam(required = false) Integer dienApVao,
            @RequestParam(required = false) Integer congSuat,
            @RequestParam(required = false) Long giaMin,
            @RequestParam(required = false) Long giaMax,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String thuongHieu) {
        String normalizedSort = normalizeSort(sort);

        List<SanPhamPSU> danhSachPSU = psuService.filterPSUs(dienApVao, congSuat, giaMin, giaMax, normalizedSort,
                thuongHieu);
        List<SanPhamPSU> thuongHieuOptions = psuService.filterPSUs(dienApVao, congSuat, giaMin, giaMax, null, null);
        List<SanPhamPSU> dienApOptions = psuService.filterPSUs(null, congSuat, giaMin, giaMax, null, thuongHieu);
        List<SanPhamPSU> congSuatOptions = psuService.filterPSUs(dienApVao, null, giaMin, giaMax, null, thuongHieu);

        model.addAttribute("dsPSU", danhSachPSU);
        model.addAttribute("dsDienApVao", extractDistinctIntegers(dienApOptions, SanPhamPSU::getDienApVao));
        model.addAttribute("dsCongSuat", extractDistinctIntegers(congSuatOptions, SanPhamPSU::getCongSuat));
        model.addAttribute("dsThuongHieu", extractDistinctStrings(thuongHieuOptions,
                psu -> psu.getSanPham().getThuongHieu().getTenThuongHieu()));

        model.addAttribute("dienApVao", dienApVao);
        model.addAttribute("congSuat", congSuat);
        applyPriceSortState(model, giaMin, giaMax, normalizedSort);
        model.addAttribute("thuongHieu", thuongHieu);

        return "clientTemplate/sanphampsu";
    }

    @GetMapping("/sanphamcase")
    public String hienThiCase(Model model,
            @RequestParam(required = false) String hoTroMain,
            @RequestParam(required = false) String mauCase,
            @RequestParam(required = false) Long giaMin,
            @RequestParam(required = false) Long giaMax,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String thuongHieu) {
        String normalizedSort = normalizeSort(sort);

        List<SanPhamCase> danhSachCase = caseService.filterCases(hoTroMain, mauCase, giaMin, giaMax, normalizedSort,
                thuongHieu);
        List<SanPhamCase> thuongHieuOptions = caseService.filterCases(hoTroMain, mauCase, giaMin, giaMax, null, null);
        List<SanPhamCase> hoTroMainOptions = caseService.filterCases(null, mauCase, giaMin, giaMax, null, thuongHieu);
        List<SanPhamCase> mauCaseOptions = caseService.filterCases(hoTroMain, null, giaMin, giaMax, null, thuongHieu);

        model.addAttribute("dsCase", danhSachCase);
        model.addAttribute("dsHoTroMain", extractDistinctStrings(hoTroMainOptions, SanPhamCase::getHoTroMain));
        model.addAttribute("dsMauCase", extractDistinctStrings(mauCaseOptions, SanPhamCase::getMauCase));
        model.addAttribute("dsThuongHieu", extractDistinctStrings(thuongHieuOptions,
                item -> item.getSanPham().getThuongHieu().getTenThuongHieu()));

        // Trả lại giá trị lọc cho giao diện
        model.addAttribute("hoTroMain", hoTroMain);
        model.addAttribute("mauCase", mauCase);
        applyPriceSortState(model, giaMin, giaMax, normalizedSort);
        model.addAttribute("thuongHieu", thuongHieu);

        return "clientTemplate/sanphamcase";
    }

    @GetMapping("/sanphammanhinh")
    public String hienThiManHinh(Model model,
            @RequestParam(required = false) String thuongHieu,
            @RequestParam(required = false) BigDecimal kichThuoc,
            @RequestParam(required = false) String beMat,
            @RequestParam(required = false) Integer tanSoQuet,
            @RequestParam(required = false) String tamNen,
            @RequestParam(required = false) String doPhanGiai,
            @RequestParam(required = false) Long giaMin,
            @RequestParam(required = false) Long giaMax,
            @RequestParam(required = false) String sort) {
        String normalizedSort = normalizeSort(sort);

        List<SanPhamManHinh> danhSachManHinh = manHinhService.filterManHinh(
                thuongHieu, kichThuoc, beMat, tanSoQuet, tamNen, doPhanGiai, giaMin, giaMax, normalizedSort);
        List<SanPhamManHinh> thuongHieuOptions = manHinhService.filterManHinh(
                null, kichThuoc, beMat, tanSoQuet, tamNen, doPhanGiai, giaMin, giaMax, null);
        List<SanPhamManHinh> kichThuocOptions = manHinhService.filterManHinh(
                thuongHieu, null, beMat, tanSoQuet, tamNen, doPhanGiai, giaMin, giaMax, null);
        List<SanPhamManHinh> beMatOptions = manHinhService.filterManHinh(
                thuongHieu, kichThuoc, null, tanSoQuet, tamNen, doPhanGiai, giaMin, giaMax, null);
        List<SanPhamManHinh> tanSoQuetOptions = manHinhService.filterManHinh(
                thuongHieu, kichThuoc, beMat, null, tamNen, doPhanGiai, giaMin, giaMax, null);
        List<SanPhamManHinh> tamNenOptions = manHinhService.filterManHinh(
                thuongHieu, kichThuoc, beMat, tanSoQuet, null, doPhanGiai, giaMin, giaMax, null);
        List<SanPhamManHinh> doPhanGiaiOptions = manHinhService.filterManHinh(
                thuongHieu, kichThuoc, beMat, tanSoQuet, tamNen, null, giaMin, giaMax, null);

        model.addAttribute("dsManHinh", danhSachManHinh);
        model.addAttribute("dsThuongHieu", extractDistinctStrings(thuongHieuOptions,
                mh -> mh.getSanPham().getThuongHieu().getTenThuongHieu()));
        model.addAttribute("dsKichThuoc", extractDistinctBigDecimals(kichThuocOptions, SanPhamManHinh::getKichThuoc));
        model.addAttribute("dsBeMat", extractDistinctStrings(beMatOptions, SanPhamManHinh::getBeMat));
        model.addAttribute("dsTanSoQuet", extractDistinctIntegers(tanSoQuetOptions, SanPhamManHinh::getTanSoQuet));
        model.addAttribute("dsTamNen", extractDistinctStrings(tamNenOptions, SanPhamManHinh::getTamNen));
        model.addAttribute("dsDoPhanGiai", extractDistinctStrings(doPhanGiaiOptions, SanPhamManHinh::getDoPhanGiai));

        model.addAttribute("thuongHieu", thuongHieu);
        model.addAttribute("kichThuoc", kichThuoc);
        model.addAttribute("beMat", beMat);
        model.addAttribute("tanSoQuet", tanSoQuet);
        model.addAttribute("tamNen", tamNen);
        model.addAttribute("doPhanGiai", doPhanGiai);
        applyPriceSortState(model, giaMin, giaMax, normalizedSort);

        return "clientTemplate/sanphammanhinh";
    }

    private void applyPriceSortState(Model model, Integer minPrice, Integer maxPrice, String sort) {
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);
    }

    private void applyPriceSortState(Model model, Long giaMin, Long giaMax, String sort) {
        model.addAttribute("giaMin", giaMin);
        model.addAttribute("giaMax", giaMax);
        model.addAttribute("sort", sort);
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return null;
        }
        if ("priceAsc".equals(sort) || "giaTangDan".equals(sort)) {
            return "giaAsc";
        }
        if ("priceDesc".equals(sort) || "giaGiamDan".equals(sort)) {
            return "giaDesc";
        }
        return sort;
    }
}
