package com.example.gearshop.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.gearshop.model.KhachHang;
import com.example.gearshop.model.LoaiSanPham;
import com.example.gearshop.model.HomeDisplayConfig;
import com.example.gearshop.model.NguoiDung;
import com.example.gearshop.model.NhanVien;
import com.example.gearshop.model.SanPham;
import com.example.gearshop.repository.KhachHangRepository;
import com.example.gearshop.repository.LoaiSanPhamRepository;
import com.example.gearshop.repository.NguoiDungRepository;
import com.example.gearshop.repository.NhanVienRepository;
import com.example.gearshop.repository.SanPhamRepository;
import com.example.gearshop.service.DangKyService;
import com.example.gearshop.service.GioHangService;
import com.example.gearshop.service.HomeDisplayConfigService;
import com.example.gearshop.service.NguoiDungService;
import com.example.gearshop.service.PasswordResetService;
import com.example.gearshop.service.SanPhamService;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    @Autowired
    private SanPhamRepository sanPhamRepo;

    @Autowired
    private LoaiSanPhamRepository loaiSPRepo;

    @Autowired
    private NguoiDungRepository nguoiDungRepo;

    @Autowired
    private KhachHangRepository khachHangRepo;
    @Autowired
    private NhanVienRepository nhanVienRepo;

    @Autowired
    private SanPhamService sanPhamService;

    @Autowired
    private DangKyService dangKyService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private HomeDisplayConfigService homeDisplayConfigService;

    @Autowired
    private GioHangService gioHangService;

    @Value("${app.forgot-password.otp-expiration-ms:300000}")
    private long otpExpirationMs;

    @GetMapping("/")
    public String homePage(Model model, HttpSession session) {
        HomeDisplayConfig displayConfig = homeDisplayConfigService.getOrCreateConfig();
        model.addAttribute("displayConfig", displayConfig);
        model.addAttribute("bannerImageUrl", homeDisplayConfigService.resolveBannerImageUrl(displayConfig));
        List<String> bannerSliderUrls = homeDisplayConfigService.getBannerSliderImageUrls(displayConfig);
        model.addAttribute("bannerSliderUrls", bannerSliderUrls);
        model.addAttribute("useBannerSlider",
                displayConfig.getBannerSourceType() != null
                        && "SLIDER".equalsIgnoreCase(displayConfig.getBannerSourceType())
                        && !bannerSliderUrls.isEmpty());
        int bannerSliderMs = displayConfig.getBannerSliderIntervalMs() != null
                ? displayConfig.getBannerSliderIntervalMs()
                : 5000;
        bannerSliderMs = Math.max(2000, Math.min(60000, bannerSliderMs));
        model.addAttribute("bannerSliderIntervalMs", bannerSliderMs);
        int homeBannerH = displayConfig.getBannerHeightPx() != null ? displayConfig.getBannerHeightPx() : 300;
        homeBannerH = Math.max(150, Math.min(400, homeBannerH));
        model.addAttribute("homeBannerHeightPx", homeBannerH);
        model.addAttribute("homeTitleFeatured",
                HomeDisplayConfigService.chonTieuDe(displayConfig.getTitleSectionFeatured(), "Sản phẩm nổi bật"));
        model.addAttribute("homeTitleRecommended",
                HomeDisplayConfigService.chonTieuDe(displayConfig.getTitleSectionRecommended(), "Danh mục sản phẩm dành cho bạn"));
        model.addAttribute("homeTitleRecentlyViewed",
                HomeDisplayConfigService.chonTieuDe(displayConfig.getTitleSectionRecentlyViewed(), "Danh mục sản phẩm vừa xem"));
        model.addAttribute("homeTitleByCategory",
                HomeDisplayConfigService.chonTieuDe(displayConfig.getTitleSectionByCategory(), "Linh kiện ngon bổ rẻ"));
        String btc = displayConfig.getBannerTitleCustom();
        model.addAttribute("homeBannerTitleCustom", btc != null && !btc.isBlank() ? btc.trim() : null);
        String bst = displayConfig.getBannerSubtitleCustom();
        model.addAttribute("homeBannerSubtitleCustom", bst != null && !bst.isBlank() ? bst.trim() : null);
        model.addAttribute("homeSectionOrder", homeDisplayConfigService.sectionKeysSortedByDisplayOrder(displayConfig));
        int featuredProductsPerRow = getPositiveOrDefault(displayConfig.getFeaturedProductsPerRow(),
                displayConfig.getProductsPerRow(), 4);
        int featuredNumberOfRows = getPositiveOrDefault(displayConfig.getFeaturedNumberOfRows(),
                displayConfig.getNumberOfRows(), 2);
        int recommendedProductsPerRow = getPositiveOrDefault(displayConfig.getRecommendedProductsPerRow(),
                displayConfig.getProductsPerRow(), 4);
        int recommendedNumberOfRows = getPositiveOrDefault(displayConfig.getRecommendedNumberOfRows(),
                displayConfig.getNumberOfRows(), 2);
        int recentlyViewedProductsPerRow = getPositiveOrDefault(displayConfig.getRecentlyViewedProductsPerRow(),
                displayConfig.getProductsPerRow(), 4);
        int recentlyViewedNumberOfRows = getPositiveOrDefault(displayConfig.getRecentlyViewedNumberOfRows(),
                displayConfig.getNumberOfRows(), 2);
        int byCategoryProductsPerRow = getPositiveOrDefault(displayConfig.getByCategoryProductsPerRow(),
                displayConfig.getProductsPerRow(), 4);
        int byCategoryNumberOfRows = getPositiveOrDefault(displayConfig.getByCategoryNumberOfRows(),
                displayConfig.getNumberOfRows(), 2);
        model.addAttribute("featuredProductsPerRow", featuredProductsPerRow);
        model.addAttribute("recommendedProductsPerRow", recommendedProductsPerRow);
        model.addAttribute("recentlyViewedProductsPerRow", recentlyViewedProductsPerRow);
        model.addAttribute("byCategoryProductsPerRow", byCategoryProductsPerRow);

        // Lấy thông tin người dùng từ session
        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoiDung");
        if (nguoiDung != null) {
            model.addAttribute("nguoiDung", nguoiDung); // Thêm thông tin người dùng vào model
        }

        List<Integer> sanPhamDaXem = (List<Integer>) session.getAttribute("sanPhamDaXem");

        List<SanPham> danhSachSanPhamDaXem = new ArrayList<>();
        if (sanPhamDaXem != null && !sanPhamDaXem.isEmpty()) {
            List<Long> sanPhamDaXemLong = new ArrayList<>();
            for (Integer id : sanPhamDaXem) {
                sanPhamDaXemLong.add(id.longValue());
            }
            danhSachSanPhamDaXem = new ArrayList<>(sanPhamRepo.findAllById(sanPhamDaXemLong));
            // Nếu cần giữ thứ tự như Session lưu (id mới nhất ở đầu)
            danhSachSanPhamDaXem.sort(Comparator.comparingInt(sp -> sanPhamDaXem.indexOf(sp.getId().intValue())));
        }

        model.addAttribute("danhSachSanPhamDaXem",
                homeDisplayConfigService.limitProducts(danhSachSanPhamDaXem, recentlyViewedProductsPerRow,
                        recentlyViewedNumberOfRows));

        SanPham sanPhamMoiXem = (SanPham) session.getAttribute("sanPhamMoiXem");
        List<SanPham> sanPhamGoiY = new ArrayList<>();

        if (sanPhamMoiXem != null) {
            sanPhamGoiY = sanPhamService.getSanPhamTuongTu(sanPhamMoiXem);
        }

        model.addAttribute("sanPhamGoiY",
                homeDisplayConfigService.limitProducts(sanPhamGoiY, recommendedProductsPerRow, recommendedNumberOfRows));
        // Thêm các sản phẩm bán chạy vào model
        model.addAttribute("sanPhamBanChay",
                homeDisplayConfigService.sortAndLimitProducts(sanPhamRepo.findAll(), displayConfig.getProductDisplayOrder(),
                        featuredProductsPerRow, featuredNumberOfRows));

        // Thêm danh mục sản phẩm theo loại vào model
        List<String> orderedCategoryKeys = homeDisplayConfigService.getOrderedCategoryKeys(displayConfig);
        java.util.Set<String> visibleCategoryKeys = homeDisplayConfigService.getVisibleCategoryKeys(displayConfig);
        Map<String, List<SanPham>> sanPhamTheoLoai = new LinkedHashMap<>();
        for (String categoryKey : orderedCategoryKeys) {
            if (!visibleCategoryKeys.contains(categoryKey)) {
                continue;
            }
            String tenLoai = HomeDisplayConfigService.CATEGORY_KEY_TO_LABEL.get(categoryKey);
            LoaiSanPham loai = loaiSPRepo.findByTenLoaiSanPham(tenLoai);
            if (loai != null) {
                sanPhamTheoLoai.put(tenLoai, homeDisplayConfigService.sortAndLimitProducts(
                        sanPhamRepo.findByLoaiSanPham_TenLoaiSanPham(tenLoai), displayConfig.getProductDisplayOrder(),
                        byCategoryProductsPerRow, byCategoryNumberOfRows));
            }
        }
        model.addAttribute("sanPhamTheoLoai", sanPhamTheoLoai);

        return "clientTemplate/trangchu"; // Trả về giao diện trang chủ
    }

    private int getPositiveOrDefault(Integer value, Integer fallback, int defaultValue) {
        Integer candidate = value != null ? value : fallback;
        int resolved = candidate != null ? candidate.intValue() : defaultValue;
        return Math.max(1, resolved);
    }

    @GetMapping("/dangnhap")
    public String showLoginPage() {
        return "clientTemplate/dangnhap"; // login.html trong templates
    }

    @PostMapping("/dangky/gui-ma")
    public String sendRegisterOtp(@RequestParam String tenNguoiDung,
            @RequestParam String tenDangNhap,
            @RequestParam String matKhau,
            @RequestParam String nhapLaiMatKhau,
            @RequestParam String email,
            @RequestParam String sdt,
            @RequestParam String diaChi,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String validationError = dangKyService.validateThongTinDangKy(tenDangNhap, matKhau, nhapLaiMatKhau,
            email, sdt, diaChi, tenNguoiDung);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("error", validationError);
            redirectAttributes.addFlashAttribute("registerStep", "input");
            return "redirect:/dangnhap";
        }

        try {
            String otp = passwordResetService.generateVerificationCode();
            passwordResetService.sendVerificationCode(email, otp);

            session.setAttribute("pendingRegisterTenNguoiDung", tenNguoiDung);
            session.setAttribute("pendingRegisterTenDangNhap", tenDangNhap);
            session.setAttribute("pendingRegisterMatKhau", matKhau);
            session.setAttribute("pendingRegisterEmail", email);
            session.setAttribute("pendingRegisterSdt", sdt);
            session.setAttribute("pendingRegisterDiaChi", diaChi);
            session.setAttribute("pendingRegisterOtp", otp);
            session.setAttribute("pendingRegisterOtpExpiry", System.currentTimeMillis() + otpExpirationMs);

            redirectAttributes.addFlashAttribute("success", "Da gui OTP den email cua ban. Hay nhap OTP de hoan tat dang ky.");
            redirectAttributes.addFlashAttribute("registerStep", "verify");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Khong gui duoc OTP. Kiem tra lai cau hinh SMTP.");
            redirectAttributes.addFlashAttribute("registerStep", "input");
        }
        return "redirect:/dangnhap";
    }

    @PostMapping("/dangky/xac-nhan")
    public String confirmRegisterOtp(@RequestParam String otp,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String pendingOtp = (String) session.getAttribute("pendingRegisterOtp");
        Long pendingOtpExpiry = (Long) session.getAttribute("pendingRegisterOtpExpiry");

        if (pendingOtp == null || pendingOtpExpiry == null) {
            redirectAttributes.addFlashAttribute("error", "Phien dang ky da het han. Vui long dang ky lai.");
            redirectAttributes.addFlashAttribute("registerStep", "input");
            return "redirect:/dangnhap";
        }

        if (System.currentTimeMillis() > pendingOtpExpiry) {
            clearPendingRegister(session);
            redirectAttributes.addFlashAttribute("error", "OTP da het han. Vui long gui lai OTP.");
            redirectAttributes.addFlashAttribute("registerStep", "input");
            return "redirect:/dangnhap";
        }

        if (!pendingOtp.equals(otp)) {
            redirectAttributes.addFlashAttribute("error", "OTP khong dung.");
            redirectAttributes.addFlashAttribute("registerStep", "verify");
            return "redirect:/dangnhap";
        }

        String tenNguoiDung = (String) session.getAttribute("pendingRegisterTenNguoiDung");
        String tenDangNhap = (String) session.getAttribute("pendingRegisterTenDangNhap");
        String matKhau = (String) session.getAttribute("pendingRegisterMatKhau");
        String email = (String) session.getAttribute("pendingRegisterEmail");
        String sdt = (String) session.getAttribute("pendingRegisterSdt");
        String diaChi = (String) session.getAttribute("pendingRegisterDiaChi");

        if (tenNguoiDung == null || tenDangNhap == null || matKhau == null || email == null || sdt == null || diaChi == null) {
            clearPendingRegister(session);
            redirectAttributes.addFlashAttribute("error", "Khong tim thay du lieu dang ky tam thoi. Vui long dang ky lai.");
            redirectAttributes.addFlashAttribute("registerStep", "input");
            return "redirect:/dangnhap";
        }

        StringBuilder thongBao = new StringBuilder();
        boolean thanhCong = dangKyService.dangKyTaiKhoan(
                tenDangNhap, matKhau, matKhau,
                email, sdt, diaChi, tenNguoiDung, thongBao);

        clearPendingRegister(session);

        if (thanhCong) {
            redirectAttributes.addFlashAttribute("success", "Dang ky thanh cong. Vui long dang nhap de tiep tuc.");
            return "redirect:/dangnhap";
        }

        redirectAttributes.addFlashAttribute("error", thongBao.toString());
        redirectAttributes.addFlashAttribute("registerStep", "input");
        return "redirect:/dangnhap";
    }

    private void clearPendingRegister(HttpSession session) {
        session.removeAttribute("pendingRegisterTenNguoiDung");
        session.removeAttribute("pendingRegisterTenDangNhap");
        session.removeAttribute("pendingRegisterMatKhau");
        session.removeAttribute("pendingRegisterEmail");
        session.removeAttribute("pendingRegisterSdt");
        session.removeAttribute("pendingRegisterDiaChi");
        session.removeAttribute("pendingRegisterOtp");
        session.removeAttribute("pendingRegisterOtpExpiry");
    }

    @GetMapping("/quen-mat-khau")
    public String showForgotPasswordPage(@RequestParam(value = "step", required = false) String step, Model model) {
        if (!model.containsAttribute("step")) {
            model.addAttribute("step", step == null ? "send" : step);
        }
        return "clientTemplate/quen-mat-khau";
    }

    @PostMapping("/quen-mat-khau/gui-ma")
    public String sendForgotPasswordCode(@RequestParam String email,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Optional<NguoiDung> optionalNguoiDung = nguoiDungRepo.findByEmail(email);
        if (optionalNguoiDung.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email khong ton tai trong he thong.");
            redirectAttributes.addFlashAttribute("step", "send");
            return "redirect:/quen-mat-khau";
        }

        try {
            String verificationCode = passwordResetService.generateVerificationCode();
            passwordResetService.sendVerificationCode(email, verificationCode);

            session.setAttribute("resetEmail", email);
            session.setAttribute("resetCode", verificationCode);
            session.setAttribute("resetCodeExpiry", System.currentTimeMillis() + otpExpirationMs);

            redirectAttributes.addFlashAttribute("success", "Da gui ma xac nhan den email cua ban. Vui long kiem tra ca thu muc spam.");
            redirectAttributes.addFlashAttribute("step", "verify");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Khong gui duoc email xac nhan. Kiem tra lai cau hinh SMTP.");
            redirectAttributes.addFlashAttribute("step", "send");
        }
        return "redirect:/quen-mat-khau";
    }

    @PostMapping("/quen-mat-khau/xac-nhan")
    public String confirmForgotPassword(@RequestParam String maXacNhan,
            @RequestParam String matKhauMoi,
            @RequestParam String xacNhanMatKhauMoi,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String resetEmail = (String) session.getAttribute("resetEmail");
        String resetCode = (String) session.getAttribute("resetCode");
        Long resetCodeExpiry = (Long) session.getAttribute("resetCodeExpiry");

        if (resetEmail == null || resetCode == null || resetCodeExpiry == null) {
            redirectAttributes.addFlashAttribute("error", "Phien dat lai mat khau da het han. Vui long yeu cau ma moi.");
            redirectAttributes.addFlashAttribute("step", "send");
            return "redirect:/quen-mat-khau";
        }

        if (System.currentTimeMillis() > resetCodeExpiry) {
            session.removeAttribute("resetEmail");
            session.removeAttribute("resetCode");
            session.removeAttribute("resetCodeExpiry");
            redirectAttributes.addFlashAttribute("error", "Ma xac nhan da het han. Vui long yeu cau ma moi.");
            redirectAttributes.addFlashAttribute("step", "send");
            return "redirect:/quen-mat-khau";
        }

        if (!resetCode.equals(maXacNhan)) {
            redirectAttributes.addFlashAttribute("error", "Ma xac nhan khong dung.");
            redirectAttributes.addFlashAttribute("step", "verify");
            return "redirect:/quen-mat-khau";
        }

        if (!matKhauMoi.equals(xacNhanMatKhauMoi)) {
            redirectAttributes.addFlashAttribute("error", "Xac nhan mat khau moi khong khop.");
            redirectAttributes.addFlashAttribute("step", "verify");
            return "redirect:/quen-mat-khau";
        }

        Optional<NguoiDung> optionalNguoiDung = nguoiDungRepo.findByEmail(resetEmail);
        if (optionalNguoiDung.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Khong tim thay tai khoan can cap nhat.");
            redirectAttributes.addFlashAttribute("step", "send");
            return "redirect:/quen-mat-khau";
        }

        NguoiDung nguoiDung = optionalNguoiDung.get();
        nguoiDung.setMatKhau(matKhauMoi);
        nguoiDungRepo.save(nguoiDung);

        session.removeAttribute("resetEmail");
        session.removeAttribute("resetCode");
        session.removeAttribute("resetCodeExpiry");

        redirectAttributes.addFlashAttribute("success", "Dat lai mat khau thanh cong. Vui long dang nhap lai.");
        return "redirect:/dangnhap";
    }

    @PostMapping("/dangnhap")
    @SuppressWarnings("unchecked")
    public String login(@RequestParam String tenDangNhap,
            @RequestParam String matKhau,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Optional<NguoiDung> optionalNguoiDung = nguoiDungRepo.findByTenDangNhapAndMatKhau(tenDangNhap, matKhau);
        if (optionalNguoiDung.isPresent()) {
            NguoiDung nguoiDung = optionalNguoiDung.get();
            session.setAttribute("nguoiDung", nguoiDung); // Lưu thông tin người dùng vào session

            // Kiểm tra vai trò của người dùng
            Optional<KhachHang> optionalKhachHang = khachHangRepo.findByNguoiDung_Id(nguoiDung.getId());
            Optional<NhanVien> optionalNhanVien = nhanVienRepo.findByNguoiDung_Id(nguoiDung.getId());

            if (optionalKhachHang.isPresent()) {
                KhachHang khachHang = optionalKhachHang.get();
                session.setAttribute("khachHang", khachHang);

                List<Map<String, Object>> sessionCart = (List<Map<String, Object>>) session.getAttribute("cart");
                if (sessionCart != null && !sessionCart.isEmpty()) {
                    List<String> loiGopGio = new ArrayList<>();
                    for (Map<String, Object> item : sessionCart) {
                        Object idObj = item.get("sanPhamID");
                        Object qtyObj = item.get("quantity");
                        if (idObj == null || qtyObj == null) {
                            continue;
                        }
                        int spId = Integer.parseInt(idObj.toString());
                        int quantity = Integer.parseInt(qtyObj.toString());
                        SanPham sp = sanPhamService.getSanPhamById(spId);
                        if (sp != null) {
                            try {
                                gioHangService.addOrUpdateCartItem(khachHang.getId(), sp, quantity);
                            } catch (IllegalArgumentException ex) {
                                loiGopGio.add(ex.getMessage());
                            }
                        }
                    }
                    // Không hiển thị cảnh báo gộp giỏ hàng sau khi đăng nhập.
                }
                session.setAttribute("cart", gioHangService.buildSessionCartPayload(khachHang.getId()));
                return "redirect:/";
            } else if (optionalNhanVien.isPresent()) {
                session.setAttribute("nhanVien", optionalNhanVien.get()); // Tạo session nhân viên
                return "redirect:/admin/trangchu"; // Chuyển đến trang admin
            } else {
                model.addAttribute("error", "Tài khoản không thuộc vai trò hợp lệ.");
                return "clientTemplate/dangnhap";
            }
        } else {
            model.addAttribute("error", "Sai tên đăng nhập hoặc mật khẩu.");
            return "clientTemplate/dangnhap";
        }
    }

    // Tim kiem san pham
    @GetMapping("/timkiem")
    public String timKiem(@RequestParam("q") String keyword,
            @RequestParam(value = "sort", required = false) String sort,
            Model model) {
        List<SanPham> ketQua;

        if ("asc".equals(sort)) {
            ketQua = sanPhamService.timKiemTheoGiaTangDan(keyword);
        } else if ("desc".equals(sort)) {
            ketQua = sanPhamService.timKiemTheoGiaGiamDan(keyword);
        } else {
            ketQua = sanPhamService.timKiemSanPham(keyword);
        }

        model.addAttribute("ketQua", ketQua);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);

        return "clientTemplate/timkiem";
    }

    @GetMapping("/dangxuat")
    public String logout(HttpSession session) {
        session.invalidate(); // Xóa toàn bộ session
        return "redirect:/"; // hoặc "redirect:/" nếu bạn muốn về trang chủ
    }

    @Controller
    @RequestMapping("/thongtincanhan")
    public class ThongTinCaNhanController {

        @Autowired
        private NguoiDungRepository nguoiDungRepo;

        @Autowired
        private KhachHangRepository khachHangRepo;

        @Autowired
        private NguoiDungService nguoiDungService;

        @GetMapping
        public String thongTinCaNhan(HttpSession session, Model model) {
            NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoiDung");
            if (nguoiDung == null)
                return "redirect:/dangnhap";

            model.addAttribute("nguoiDung", nguoiDung);
            boolean isKhachHang = khachHangRepo.findByNguoiDung_Id(nguoiDung.getId()).isPresent();
            // Xác định nhân viên trực tiếp từ bảng nhanvien theo nguoiDungID.
            boolean isNhanVien = nhanVienRepo.findByNguoiDung_Id(nguoiDung.getId()).isPresent();
            boolean showClientNavbar = !isNhanVien && isKhachHang;
            System.out.println("isKhachHang: " + isKhachHang);
            System.out.println("isNhanVien: " + isNhanVien);
            System.out.println("Co nguoi dung: " + nguoiDung.getTenNguoiDung());
            model.addAttribute("isKhachHang", isKhachHang);
            model.addAttribute("isNhanVien", isNhanVien);
            model.addAttribute("showClientNavbar", showClientNavbar);
            return "/thongtincanhan";
        }

        @PostMapping("/capnhat")
        public String capNhatThongTin(HttpSession session,
                @RequestParam String tenDangNhap,
                @RequestParam String tenNguoiDung,
                @RequestParam String email,
                @RequestParam String sdt,
                @RequestParam String diaChi,
                @RequestParam String matKhauXacNhan,
                RedirectAttributes redirectAttributes) {
            NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoiDung");
            if (nguoiDung == null)
                return "redirect:/dangnhap";

            try {
                nguoiDungService.capNhatThongTin(nguoiDung.getId(), tenDangNhap, tenNguoiDung, email, sdt, diaChi,
                        matKhauXacNhan);

                // Đồng bộ session với DB sau khi lưu (tránh entity trong session lệch dữ liệu)
                NguoiDung capNhat = nguoiDungRepo.findById(nguoiDung.getId()).orElse(nguoiDung);
                session.setAttribute("nguoiDung", capNhat);

                // Gửi thông báo thành công
                redirectAttributes.addFlashAttribute("thongBaoCapNhat", "Cập nhật thông tin thành công!");
            } catch (IllegalArgumentException e) {
                // Trả lỗi theo từng trường để hiển thị inline sau redirect.
                redirectAttributes.addFlashAttribute("tenDangNhap", tenDangNhap);
                redirectAttributes.addFlashAttribute("tenNguoiDung", tenNguoiDung);
                redirectAttributes.addFlashAttribute("email", email);
                redirectAttributes.addFlashAttribute("sdt", sdt);
                redirectAttributes.addFlashAttribute("diaChi", diaChi);
                String errorMessage = e.getMessage() == null ? "" : e.getMessage();
                if (errorMessage.toLowerCase().contains("tên đăng nhập")) {
                    redirectAttributes.addFlashAttribute("tenDangNhapError", errorMessage);
                } else if (errorMessage.toLowerCase().contains("email")) {
                    redirectAttributes.addFlashAttribute("emailError", errorMessage);
                } else if (errorMessage.toLowerCase().contains("số điện thoại") || errorMessage.toLowerCase().contains("sdt")) {
                    redirectAttributes.addFlashAttribute("sdtError", errorMessage);
                } else if (errorMessage.toLowerCase().contains("họ tên")) {
                    redirectAttributes.addFlashAttribute("tenNguoiDungError", errorMessage);
                } else if (errorMessage.toLowerCase().contains("mật khẩu")) {
                    redirectAttributes.addFlashAttribute("matKhauXacNhanError", errorMessage);
                } else {
                    redirectAttributes.addFlashAttribute("thongBaoLoiCapNhatInline", errorMessage);
                }
            }

            return "redirect:/thongtincanhan";
        }

        @PostMapping("/doimatkhau")
        public String doiMatKhau(HttpSession session,
                @RequestParam String matKhauCu,
                @RequestParam String matKhauMoi,
                @RequestParam String xacNhanMatKhauMoi,
                RedirectAttributes redirectAttributes) {
            NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoiDung");
            if (nguoiDung == null)
                return "redirect:/dangnhap";

            String thongBao = nguoiDungService.doiMatKhau(
                    nguoiDung.getTenDangNhap(), matKhauCu, matKhauMoi, xacNhanMatKhauMoi);

            if ("Đổi mật khẩu thành công.".equals(thongBao)) {
                redirectAttributes.addFlashAttribute("thongBaoDoiMatKhauSuccess", thongBao);
            } else {
                redirectAttributes.addFlashAttribute("matKhauCu", matKhauCu);
                redirectAttributes.addFlashAttribute("matKhauMoi", matKhauMoi);
                redirectAttributes.addFlashAttribute("xacNhanMatKhauMoi", xacNhanMatKhauMoi);
                if (thongBao.toLowerCase().contains("xác nhận")) {
                    redirectAttributes.addFlashAttribute("xacNhanMatKhauMoiError", thongBao);
                } else if (thongBao.toLowerCase().contains("mật khẩu cũ")) {
                    redirectAttributes.addFlashAttribute("matKhauCuError", thongBao);
                } else if (thongBao.toLowerCase().contains("mật khẩu mới")) {
                    redirectAttributes.addFlashAttribute("matKhauMoiError", thongBao);
                } else {
                    redirectAttributes.addFlashAttribute("matKhauMoiError", thongBao);
                }
            }
            return "redirect:/thongtincanhan";
        }
    }

    @Controller
    @RequestMapping("/dangky")
    public class DangKyController {

        @Autowired
        private DangKyService dangKyService;

        @GetMapping
        public String hienFormDangKy() {
            return "dangky"; // trang giao diện Thymeleaf
        }

        @PostMapping
        public String xuLyDangKy(@RequestParam String tenDangNhap,
                @RequestParam String matKhau,
                @RequestParam String nhapLaiMatKhau,
                @RequestParam String email,
                @RequestParam String sdt,
                @RequestParam String diaChi,
                @RequestParam String tenNguoiDung,
                RedirectAttributes redirectAttributes,
                Model model) {
            StringBuilder thongBao = new StringBuilder();
            boolean thanhCong = dangKyService.dangKyTaiKhoan(
                    tenDangNhap, matKhau, nhapLaiMatKhau,
                    email, sdt, diaChi, tenNguoiDung, thongBao);

            if (thanhCong) {
                redirectAttributes.addFlashAttribute("success", thongBao.toString());
                return "redirect:/dangnhap";
            } else {
                redirectAttributes.addFlashAttribute("error", thongBao.toString());
                return "clientTemplate/dangnhap";
            }
        }
    }

}
