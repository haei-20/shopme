package com.example.gearshop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gearshop.model.HomeSectionBlock;

public interface HomeSectionBlockRepository extends JpaRepository<HomeSectionBlock, Long> {
    List<HomeSectionBlock> findAllByOrderByDisplayOrderAscIdAsc();

    Optional<HomeSectionBlock> findByBlockKey(String blockKey);
}
