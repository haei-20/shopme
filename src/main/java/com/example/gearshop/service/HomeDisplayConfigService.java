package com.example.gearshop.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /** Thứ tự khối trên trang chủ (giá trị lưu trong form kéo-thả). */
    public static final List<String> HOME_SECTION_KEYS = List.of(
            "banner", "featured", "recommended", "recentlyViewed", "byCategory");

    private static final Set<String> HOME_SECTION_KEY_SET = Set.copyOf(HOME_SECTION_KEYS);

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

    public List<SanPham> sortAndLimitProducts(List<SanPham> sanPhams, String productDisplayOrder, Integer productsPerRow,
            Integer numberOfRows) {
        List<SanPham> copied = new ArrayList<>(sanPhams);
        copied.sort(buildComparator(productDisplayOrder));
        int limit = Math.max(1, productsPerRow != null ? productsPerRow : DEFAULT_PRODUCTS_PER_ROW)
                * Math.max(1, numberOfRows != null ? numberOfRows : DEFAULT_NUMBER_OF_ROWS);
        return copied.stream().limit(limit).toList();
    }

    public List<SanPham> limitProducts(List<SanPham> sanPhams, Integer productsPerRow, Integer numberOfRows) {
        int limit = Math.max(1, productsPerRow != null ? productsPerRow : DEFAULT_PRODUCTS_PER_ROW)
                * Math.max(1, numberOfRows != null ? numberOfRows : DEFAULT_NUMBER_OF_ROWS);
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
        config.setShowBanner(Boolean.TRUE);
        config.setShowBannerOverlayText(Boolean.TRUE);
        config.setShowCategoryNav(Boolean.TRUE);
        config.setShowSectionFeatured(Boolean.TRUE);
        config.setShowSectionRecommended(Boolean.TRUE);
        config.setShowSectionRecentlyViewed(Boolean.TRUE);
        config.setShowSectionByCategory(Boolean.TRUE);
        config.setShowFooter(Boolean.TRUE);
        config.setShowChatbot(Boolean.TRUE);
        config.setFeaturedProductsPerRow(DEFAULT_PRODUCTS_PER_ROW);
        config.setFeaturedNumberOfRows(DEFAULT_NUMBER_OF_ROWS);
        config.setRecommendedProductsPerRow(DEFAULT_PRODUCTS_PER_ROW);
        config.setRecommendedNumberOfRows(DEFAULT_NUMBER_OF_ROWS);
        config.setRecentlyViewedProductsPerRow(DEFAULT_PRODUCTS_PER_ROW);
        config.setRecentlyViewedNumberOfRows(DEFAULT_NUMBER_OF_ROWS);
        config.setByCategoryProductsPerRow(DEFAULT_PRODUCTS_PER_ROW);
        config.setByCategoryNumberOfRows(DEFAULT_NUMBER_OF_ROWS);
        config.setOrderBanner(1);
        config.setOrderSectionFeatured(2);
        config.setOrderSectionRecommended(3);
        config.setOrderSectionRecentlyViewed(4);
        config.setOrderSectionByCategory(5);
        return homeDisplayConfigRepository.save(config);
    }

    public static String chonTieuDe(String tuyChon, String macDinh) {
        if (tuyChon == null) {
            return macDinh;
        }
        String t = tuyChon.trim();
        return t.isEmpty() ? macDinh : t;
    }

    /**
     * Trả về danh sách key khối (banner, featured, …) đã sắp xếp theo thứ tự hiển thị hiện tại.
     */
    public List<String> sectionKeysSortedByDisplayOrder(HomeDisplayConfig cfg) {
        Map<String, Integer> order = new HashMap<>();
        order.put("banner", valueOrDefault(cfg.getOrderBanner(), 1));
        order.put("featured", valueOrDefault(cfg.getOrderSectionFeatured(), 2));
        order.put("recommended", valueOrDefault(cfg.getOrderSectionRecommended(), 3));
        order.put("recentlyViewed", valueOrDefault(cfg.getOrderSectionRecentlyViewed(), 4));
        order.put("byCategory", valueOrDefault(cfg.getOrderSectionByCategory(), 5));

        return order.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Chuỗi CSV từ form (vd: banner,featured,...) → cập nhật orderBanner, orderSection* trên entity.
     */
    public void applyHomeSectionOrderCsv(HomeDisplayConfig config, String csv) {
        List<String> keys = parseHomeSectionOrderCsv(csv);
        for (int i = 0; i < keys.size(); i++) {
            int ord = i + 1;
            switch (keys.get(i)) {
                case "banner" -> config.setOrderBanner(ord);
                case "featured" -> config.setOrderSectionFeatured(ord);
                case "recommended" -> config.setOrderSectionRecommended(ord);
                case "recentlyViewed" -> config.setOrderSectionRecentlyViewed(ord);
                case "byCategory" -> config.setOrderSectionByCategory(ord);
                default -> {
                }
            }
        }
    }

    private List<String> parseHomeSectionOrderCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return HOME_SECTION_KEYS;
        }
        List<String> keys = Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (keys.size() != HOME_SECTION_KEYS.size() || new HashSet<>(keys).size() != HOME_SECTION_KEYS.size()) {
            return HOME_SECTION_KEYS;
        }
        for (String k : keys) {
            if (!HOME_SECTION_KEY_SET.contains(k)) {
                return HOME_SECTION_KEYS;
            }
        }
        return keys;
    }

    private static int valueOrDefault(Integer value, int defaultValue) {
        return value != null ? value.intValue() : defaultValue;
    }
}
