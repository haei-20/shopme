package com.example.gearshop.controller;

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
        return "adminTemplate/quanlyhienthi";
    }

    @PostMapping
    public String capNhatHienThi(
            @RequestParam String bannerSourceType,
            @RequestParam(required = false) Integer bannerProductId,
            @RequestParam String productDisplayOrder,
            @RequestParam Integer productsPerRow,
            @RequestParam Integer numberOfRows,
            @RequestParam(name = "bannerImageFile", required = false) MultipartFile bannerImageFile,
            RedirectAttributes redirectAttributes) {

        HomeDisplayConfig config = homeDisplayConfigService.getOrCreateConfig();

        config.setBannerSourceType(bannerSourceType);
        config.setProductDisplayOrder(productDisplayOrder);
        config.setProductsPerRow(Math.max(1, productsPerRow));
        config.setNumberOfRows(Math.max(1, numberOfRows));

        if ("PRODUCT".equalsIgnoreCase(bannerSourceType)) {
            if (bannerProductId == null) {
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
            } else if (config.getUploadedBannerImage() == null || config.getUploadedBannerImage().isBlank()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng tải lên ảnh banner.");
                return "redirect:/admin/quanlyhienthi";
            }
        }

        homeDisplayConfigService.save(config);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật cấu hình hiển thị trang chủ.");
        return "redirect:/admin/quanlyhienthi";
    }
}
