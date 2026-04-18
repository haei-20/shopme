package com.example.gearshop.model;

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
}
