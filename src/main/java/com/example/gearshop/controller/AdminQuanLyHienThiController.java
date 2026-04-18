package com.example.gearshop.controller;

import java.util.List;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

import com.example.gearshop.model.HomeDisplayConfig;
import com.example.gearshop.service.HomeDisplayConfigService;
import com.example.gearshop.service.SanPhamService;

@Controller
@RequestMapping("/admin/quanlyhienthi")
public class AdminQuanLyHienThiController {

    @Autowired
    private HomeDisplayConfigService homeDisplayConfigService;

    @Autowired
    private SanPhamService sanPhamService;

    @GetMapping
    public String hienThiTrangQuanLy(Model model) {
        HomeDisplayConfig config = homeDisplayConfigService.getOrCreateConfig();
        model.addAttribute("displayConfig", config);
        model.addAttribute("bannerImageUrl", homeDisplayConfigService.resolveBannerImageUrl(config));
        model.addAttribute("dsSanPham", sanPhamService.getAllSanPham());
        List<String> sectionOrder = homeDisplayConfigService.sectionKeysSortedByDisplayOrder(config);
        model.addAttribute("homeSectionKeysOrder", sectionOrder);
        model.addAttribute("homeSectionOrderCsv", String.join(",", sectionOrder));
        return "adminTemplate/quanlyhienthi";
    }

    @PostMapping
    public String capNhatHienThi(
            @RequestParam String bannerSourceType,
            @RequestParam(required = false) Integer bannerProductId,
            @RequestParam(name = "bannerImageFile", required = false) MultipartFile bannerImageFile,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        HomeDisplayConfig config = homeDisplayConfigService.getOrCreateConfig();

        config.setBannerSourceType(bannerSourceType);
        if (config.getProductDisplayOrder() == null || config.getProductDisplayOrder().isBlank()) {
            config.setProductDisplayOrder("BEST_SELLING");
        }
        if (config.getProductsPerRow() == null || config.getProductsPerRow() < 1) {
            config.setProductsPerRow(5);
        }
        if (config.getNumberOfRows() == null || config.getNumberOfRows() < 1) {
            config.setNumberOfRows(2);
        }
        config.setFeaturedProductsPerRow(parseOrder(request.getParameter("featuredProductsPerRow"), config.getProductsPerRow()));
        config.setFeaturedNumberOfRows(parseOrder(request.getParameter("featuredNumberOfRows"), config.getNumberOfRows()));
        config.setRecommendedProductsPerRow(parseOrder(request.getParameter("recommendedProductsPerRow"), config.getProductsPerRow()));
        config.setRecommendedNumberOfRows(parseOrder(request.getParameter("recommendedNumberOfRows"), config.getNumberOfRows()));
        config.setRecentlyViewedProductsPerRow(parseOrder(request.getParameter("recentlyViewedProductsPerRow"), config.getProductsPerRow()));
        config.setRecentlyViewedNumberOfRows(parseOrder(request.getParameter("recentlyViewedNumberOfRows"), config.getNumberOfRows()));
        config.setByCategoryProductsPerRow(parseOrder(request.getParameter("byCategoryProductsPerRow"), config.getProductsPerRow()));
        config.setByCategoryNumberOfRows(parseOrder(request.getParameter("byCategoryNumberOfRows"), config.getNumberOfRows()));

        config.setShowBanner(boolParam(request, "showBanner"));
        config.setShowBannerOverlayText(boolParam(request, "showBannerOverlayText"));
        config.setShowCategoryNav(boolParam(request, "showCategoryNav"));
        config.setShowSectionFeatured(boolParam(request, "showSectionFeatured"));
        config.setShowSectionRecommended(boolParam(request, "showSectionRecommended"));
        config.setShowSectionRecentlyViewed(boolParam(request, "showSectionRecentlyViewed"));
        config.setShowSectionByCategory(boolParam(request, "showSectionByCategory"));
        config.setShowFooter(boolParam(request, "showFooter"));
        config.setShowChatbot(boolParam(request, "showChatbot"));

        config.setBannerTitleCustom(trimToNull(request.getParameter("bannerTitleCustom")));
        config.setBannerSubtitleCustom(trimToNull(request.getParameter("bannerSubtitleCustom")));
        config.setTitleSectionFeatured(trimToNull(request.getParameter("titleSectionFeatured")));
        config.setTitleSectionRecommended(trimToNull(request.getParameter("titleSectionRecommended")));
        config.setTitleSectionRecentlyViewed(trimToNull(request.getParameter("titleSectionRecentlyViewed")));
        config.setTitleSectionByCategory(trimToNull(request.getParameter("titleSectionByCategory")));
        homeDisplayConfigService.applyHomeSectionOrderCsv(config, request.getParameter("homeSectionOrder"));

        if ("PRODUCT".equalsIgnoreCase(bannerSourceType)) {
            if (Boolean.TRUE.equals(config.getShowBanner()) && bannerProductId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn sản phẩm làm banner.");
                return "redirect:/admin/quanlyhienthi";
            }
            config.setBannerProductId(bannerProductId);
        } else {
            if (bannerImageFile != null && !bannerImageFile.isEmpty()) {
                try {
                    String originalFileName = bannerImageFile.getOriginalFilename();
                    String originalName = (originalFileName == null || originalFileName.isBlank())
                            ? "banner.jpg"
                            : originalFileName.replaceAll("\\s+", "_");
                    String savedFileName = LocalDateTime.now().toString().replace(":", "-") + "-" + originalName;

                    String uploadDir = new ClassPathResource("static/images/banner/").getFile().getAbsolutePath();
                    Path uploadPath = Paths.get(uploadDir);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }

                    Files.copy(
                            bannerImageFile.getInputStream(),
                            uploadPath.resolve(savedFileName),
                            StandardCopyOption.REPLACE_EXISTING);
                    config.setUploadedBannerImage(savedFileName);
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Không thể lưu ảnh banner: " + e.getMessage());
                    return "redirect:/admin/quanlyhienthi";
                }
            } else if (Boolean.TRUE.equals(config.getShowBanner())
                    && (config.getUploadedBannerImage() == null || config.getUploadedBannerImage().isBlank())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng tải lên ảnh banner.");
                return "redirect:/admin/quanlyhienthi";
            }
        }

        homeDisplayConfigService.save(config);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật cấu hình hiển thị trang chủ.");
        return "redirect:/admin/quanlyhienthi";
    }

    private static boolean boolParam(HttpServletRequest req, String name) {
        return "true".equalsIgnoreCase(req.getParameter(name));
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static int parseOrder(String raw, int macDinh) {
        if (raw == null || raw.isBlank()) {
            return macDinh;
        }
        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ex) {
            return macDinh;
        }
    }
}
