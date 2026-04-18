package com.example.gearshop.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.gearshop.model.HomeDisplayConfig;
import com.example.gearshop.model.SanPham;
import com.example.gearshop.repository.HomeDisplayConfigRepository;
import com.example.gearshop.repository.SanPhamRepository;

@Service
public class HomeDisplayConfigService {

    private static final String DEFAULT_BANNER_SOURCE = "UPLOAD";
    private static final String DEFAULT_PRODUCT_ORDER = "BEST_SELLING";
    private static final int DEFAULT_PRODUCTS_PER_ROW = 5;
    private static final int DEFAULT_NUMBER_OF_ROWS = 2;
    private static final String DEFAULT_BANNER_URL = "/images/banner/banner3.png";

    @Autowired
    private HomeDisplayConfigRepository homeDisplayConfigRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    public HomeDisplayConfig getOrCreateConfig() {
        return homeDisplayConfigRepository.findTopByOrderByIdAsc()
                .orElseGet(this::createDefaultConfig);
    }

    public HomeDisplayConfig save(HomeDisplayConfig config) {
        return homeDisplayConfigRepository.save(config);
    }

    public String resolveBannerImageUrl(HomeDisplayConfig config) {
        if ("PRODUCT".equalsIgnoreCase(config.getBannerSourceType())
                && config.getBannerProductId() != null) {
            SanPham sanPham = sanPhamRepository.findById(config.getBannerProductId());
            if (sanPham != null && sanPham.getHinhAnh() != null && !sanPham.getHinhAnh().isBlank()) {
                return "/images/product/" + sanPham.getHinhAnh();
            }
        }

        if (config.getUploadedBannerImage() != null && !config.getUploadedBannerImage().isBlank()) {
            return "/images/banner/" + config.getUploadedBannerImage();
        }

        return DEFAULT_BANNER_URL;
    }

    public List<SanPham> sortAndLimitProducts(List<SanPham> sanPhams, HomeDisplayConfig config) {
        List<SanPham> copied = new ArrayList<>(sanPhams);
        copied.sort(buildComparator(config.getProductDisplayOrder()));

        int limit = Math.max(1, config.getProductsPerRow()) * Math.max(1, config.getNumberOfRows());
        return copied.stream().limit(limit).toList();
    }

    public List<SanPham> limitProducts(List<SanPham> sanPhams, HomeDisplayConfig config) {
        int limit = Math.max(1, config.getProductsPerRow()) * Math.max(1, config.getNumberOfRows());
        return sanPhams.stream().limit(limit).toList();
    }

    private Comparator<SanPham> buildComparator(String productDisplayOrder) {
        String order = productDisplayOrder == null ? DEFAULT_PRODUCT_ORDER : productDisplayOrder;

        return switch (order) {
            case "NEWEST" -> Comparator
                    .comparing(SanPham::getNgayThem, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(SanPham::getId, Comparator.nullsLast(Comparator.reverseOrder()));
            case "PRICE_ASC" -> Comparator
                    .comparing(SanPham::getGia, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(SanPham::getId, Comparator.nullsLast(Comparator.naturalOrder()));
            case "PRICE_DESC" -> Comparator
                    .comparing(SanPham::getGia, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(SanPham::getId, Comparator.nullsLast(Comparator.reverseOrder()));
            case "NAME_ASC" -> Comparator
                    .comparing(SanPham::getTenSanPham, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(SanPham::getId, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator
                    .comparing(SanPham::getDaBan, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(SanPham::getNgayThem, Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }

    private HomeDisplayConfig createDefaultConfig() {
        HomeDisplayConfig config = new HomeDisplayConfig();
        config.setBannerSourceType(DEFAULT_BANNER_SOURCE);
        config.setProductDisplayOrder(DEFAULT_PRODUCT_ORDER);
        config.setProductsPerRow(DEFAULT_PRODUCTS_PER_ROW);
        config.setNumberOfRows(DEFAULT_NUMBER_OF_ROWS);
        return homeDisplayConfigRepository.save(config);
    }
}
