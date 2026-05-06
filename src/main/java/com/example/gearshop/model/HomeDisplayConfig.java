package com.example.gearshop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "homeDisplayConfig")
public class HomeDisplayConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Setter(AccessLevel.NONE)
    private String bannerSourceType;

    public void setBannerSourceType(String bannerSourceType) {
        this.bannerSourceType = bannerSourceType;
    }

    private String uploadedBannerImage;

    private Integer bannerProductId;

    /** Nhiều ảnh banner (tên file trong static/images/banner/), cách nhau bởi dấu phẩy — dùng khi bannerSourceType = SLIDER. */
    @Column(length = 3000)
    private String bannerSliderImagesCsv;

    /** Thời gian hiển thị mỗi slide (ms), mặc định 5000. */
    private Integer bannerSliderIntervalMs;

    /** Chiều cao banner trang chủ (px), khoảng 150–400. */
    private Integer bannerHeightPx;

    private String productDisplayOrder;

    private Integer productsPerRow;

    private Integer numberOfRows;

    /** Cấu hình số cột/số hàng riêng cho từng khối sản phẩm. */
    private Integer featuredProductsPerRow;
    private Integer featuredNumberOfRows;
    private String featuredProductDisplayOrder;
    private Integer recommendedProductsPerRow;
    private Integer recommendedNumberOfRows;
    private Integer recentlyViewedProductsPerRow;
    private Integer recentlyViewedNumberOfRows;
    private Integer byCategoryProductsPerRow;
    private Integer byCategoryNumberOfRows;
    @Column(length = 1000)
    private String byCategoryOrderCsv;
    @Column(length = 1000)
    private String byCategoryVisibleCsv;

    /** Bật/tắt các khối trên trang chủ (null = mặc định bật khi tải cấu hình cũ). */
    private Boolean showBanner;
    private Boolean showBannerOverlayText;
    private Boolean showCategoryNav;
    private Boolean showSectionFeatured;
    private Boolean showSectionRecommended;
    private Boolean showSectionRecentlyViewed;
    private Boolean showSectionByCategory;
    private Boolean showFooter;
    private Boolean showChatbot;

    /** Để trống = dùng mặc định trên giao diện. */
    @Column(length = 300)
    private String bannerTitleCustom;
    @Column(length = 1000)
    private String bannerSubtitleCustom;
    @Column(length = 200)
    private String titleSectionFeatured;
    @Column(length = 200)
    private String titleSectionRecommended;
    @Column(length = 200)
    private String titleSectionRecentlyViewed;
    @Column(length = 200)
    private String titleSectionByCategory;

    /** Thứ tự xuất hiện các khối ở trang chủ (số nhỏ hiển thị trước). */
    private Integer orderBanner;
    private Integer orderSectionFeatured;
    private Integer orderSectionRecommended;
    private Integer orderSectionRecentlyViewed;
    private Integer orderSectionByCategory;
}
