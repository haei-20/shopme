package com.example.gearshop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gearshop.model.HomeDisplayConfig;

public interface HomeDisplayConfigRepository extends JpaRepository<HomeDisplayConfig, Integer> {
    Optional<HomeDisplayConfig> findTopByOrderByIdAsc();
}
