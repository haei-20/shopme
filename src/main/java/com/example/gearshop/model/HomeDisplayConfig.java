package com.example.gearshop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    private String bannerSourceType;

    private String uploadedBannerImage;

    private Integer bannerProductId;

    private String productDisplayOrder;

    private Integer productsPerRow;

    private Integer numberOfRows;

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
}
