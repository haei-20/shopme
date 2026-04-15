package com.example.gearshop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gearshop.model.ThongTinShop;

public interface ThongTinShopRepository extends JpaRepository<ThongTinShop, Integer> {
    Optional<ThongTinShop> findTopByOrderByIdAsc();
}
