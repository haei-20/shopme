package com.example.gearshop.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gearshop.model.HomeSectionBlock;
import com.example.gearshop.model.SanPham;
import com.example.gearshop.repository.HomeSectionBlockRepository;
import com.example.gearshop.repository.SanPhamRepository;

import jakarta.annotation.PostConstruct;

@Service
public class HomeSectionBlockService {
    public static final String TYPE_BUILT_IN = "BUILT_IN";
    public static final String TYPE_PRODUCT_CUSTOM = "PRODUCT_CUSTOM";
    private static final String REMOVED_SECTION_KEY = "recommended";

    private static final List<String> BUILT_IN_KEYS = List.of(
            "banner", "featured", "recentlyViewed", "byCategory");
    private static final Map<String, String> BUILT_IN_TITLES = Map.of(
            "banner", "Banner",
            "featured", "Khối nổi bật",
            "recentlyViewed", "Khối vừa xem",
            "byCategory", "Khối theo loại");

    @Autowired
    private HomeSectionBlockRepository homeSectionBlockRepository;
    @Autowired
    private SanPhamRepository sanPhamRepository;

    @PostConstruct
    @Transactional
    public void initDefaultBlocksOnStartup() {
        initializeDefaultBlocksIfEmpty();
    }

    @Transactional
    public List<HomeSectionBlock> getOrderedBlocks() {
        initializeDefaultBlocksIfEmpty();
        List<HomeSectionBlock> all = homeSectionBlockRepository.findAllByOrderByDisplayOrderAscIdAsc();
        for (HomeSectionBlock block : all) {
            if (REMOVED_SECTION_KEY.equalsIgnoreCase(block.getBlockKey())) {
                homeSectionBlockRepository.delete(block);
            }
        }
        return homeSectionBlockRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .filter(b -> !REMOVED_SECTION_KEY.equalsIgnoreCase(b.getBlockKey()))
                .toList();
    }

    @Transactional
    public List<String> getOrderedBlockKeys() {
        return getOrderedBlocks().stream().map(HomeSectionBlock::getBlockKey).toList();
    }

    @Transactional
    public List<HomeSectionBlock> getCustomProductBlocksOrdered() {
        return getOrderedBlocks().stream()
                .filter(b -> TYPE_PRODUCT_CUSTOM.equalsIgnoreCase(b.getBlockType()))
                .toList();
    }

    @Transactional
    public Map<String, HomeSectionBlock> getCustomProductBlocksByKey() {
        Map<String, HomeSectionBlock> out = new LinkedHashMap<>();
        for (HomeSectionBlock b : getCustomProductBlocksOrdered()) {
            out.put(b.getBlockKey(), b);
        }
        return out;
    }

    @Transactional
    public void createCustomProductBlock(String title, String productIdsCsv, Integer productsPerRow, Integer numberOfRows) {
        String t = title == null ? "" : title.trim();
        if (t.isEmpty()) {
            return;
        }
        String normalizedCsv = normalizeProductIdsCsv(productIdsCsv);
        if (normalizedCsv == null || normalizedCsv.isBlank()) {
            return;
        }
        int maxOrder = getOrderedBlocks().stream()
                .map(HomeSectionBlock::getDisplayOrder)
                .filter(v -> v != null)
                .max(Integer::compareTo)
                .orElse(0);
        HomeSectionBlock block = new HomeSectionBlock();
        block.setBlockKey("manual_" + LocalDateTime.now().toString().replace(":", "").replace(".", ""));
        block.setBlockType(TYPE_PRODUCT_CUSTOM);
        block.setTitle(t);
        block.setContent(normalizedCsv);
        block.setDisplayOrder(maxOrder + 1);
        block.setBuiltIn(Boolean.FALSE);
        block.setProductsPerRow(normalizePositive(productsPerRow, 4));
        block.setNumberOfRows(normalizePositive(numberOfRows, 2));
        homeSectionBlockRepository.save(block);
    }

    @Transactional
    public Map<String, String> getMissingBuiltInBlocks() {
        List<HomeSectionBlock> all = getOrderedBlocks();
        Set<String> exists = new LinkedHashSet<>();
        for (HomeSectionBlock b : all) {
            exists.add(b.getBlockKey());
        }
        Map<String, String> missing = new LinkedHashMap<>();
        for (String key : BUILT_IN_KEYS) {
            if (!exists.contains(key)) {
                missing.put(key, BUILT_IN_TITLES.getOrDefault(key, key));
            }
        }
        return missing;
    }

    @Transactional
    public void addBuiltInBlockIfMissing(String builtInKey) {
        if (builtInKey == null || builtInKey.isBlank() || !BUILT_IN_KEYS.contains(builtInKey)) {
            return;
        }
        if (homeSectionBlockRepository.findByBlockKey(builtInKey).isPresent()) {
            return;
        }
        int maxOrder = getOrderedBlocks().stream()
                .map(HomeSectionBlock::getDisplayOrder)
                .filter(v -> v != null)
                .max(Integer::compareTo)
                .orElse(0);
        HomeSectionBlock block = new HomeSectionBlock();
        block.setBlockKey(builtInKey);
        block.setBlockType(TYPE_BUILT_IN);
        block.setTitle(BUILT_IN_TITLES.getOrDefault(builtInKey, builtInKey));
        block.setContent(null);
        block.setDisplayOrder(maxOrder + 1);
        block.setBuiltIn(Boolean.TRUE);
        homeSectionBlockRepository.save(block);
    }

    @Transactional
    public void removeBlocks(List<String> blockKeys) {
        if (blockKeys == null || blockKeys.isEmpty()) {
            return;
        }
        Set<String> set = new LinkedHashSet<>();
        for (String k : blockKeys) {
            if (k != null && !k.isBlank()) {
                set.add(k.trim());
            }
        }
        for (String key : set) {
            homeSectionBlockRepository.findByBlockKey(key).ifPresent(homeSectionBlockRepository::delete);
        }
        normalizeDisplayOrder();
    }

    @Transactional
    public void updateCustomProductBlock(String blockKey, String title, String productIdsCsv, Integer productsPerRow, Integer numberOfRows) {
        if (blockKey == null || blockKey.isBlank()) {
            return;
        }
        homeSectionBlockRepository.findByBlockKey(blockKey.trim()).ifPresent(block -> {
            if (!TYPE_PRODUCT_CUSTOM.equalsIgnoreCase(block.getBlockType())) {
                return;
            }
            String t = title == null ? "" : title.trim();
            if (!t.isEmpty()) {
                block.setTitle(t);
            }
            String normalizedCsv = normalizeProductIdsCsv(productIdsCsv);
            block.setContent(normalizedCsv);
            block.setProductsPerRow(normalizePositive(productsPerRow, 4));
            block.setNumberOfRows(normalizePositive(numberOfRows, 2));
            homeSectionBlockRepository.save(block);
        });
    }

    @Transactional(readOnly = true)
    public Map<String, List<SanPham>> resolveCustomProductBlocks(Map<String, HomeSectionBlock> blockByKey) {
        Map<String, List<SanPham>> out = new LinkedHashMap<>();
        if (blockByKey == null || blockByKey.isEmpty()) {
            return out;
        }
        for (Map.Entry<String, HomeSectionBlock> entry : blockByKey.entrySet()) {
            List<Long> ids = parseProductIds(entry.getValue().getContent());
            if (ids.isEmpty()) {
                out.put(entry.getKey(), List.of());
                continue;
            }
            List<SanPham> products = new ArrayList<>(sanPhamRepository.findAllById(ids));
            products.sort(Comparator.comparingInt(sp -> {
                Long pid = sp == null || sp.getId() == null ? null : sp.getId().longValue();
                return pid == null ? Integer.MAX_VALUE : ids.indexOf(pid);
            }));
            int perRow = normalizePositive(entry.getValue().getProductsPerRow(), 4);
            int rows = normalizePositive(entry.getValue().getNumberOfRows(), 2);
            int limit = Math.max(1, perRow * rows);
            if (products.size() > limit) {
                products = products.subList(0, limit);
            }
            out.put(entry.getKey(), products);
        }
        return out;
    }

    public String normalizeProductIdsCsv(String csv) {
        List<Long> ids = parseProductIds(csv);
        if (ids.isEmpty()) {
            return null;
        }
        return String.join(",", ids.stream().map(String::valueOf).toList());
    }

    public List<Long> parseProductIds(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String raw : csv.split(",")) {
            if (raw == null) {
                continue;
            }
            String t = raw.trim();
            if (t.isEmpty()) {
                continue;
            }
            try {
                long id = Long.parseLong(t);
                if (id > 0 && !ids.contains(id)) {
                    ids.add(id);
                }
            } catch (NumberFormatException ignored) {
                // skip invalid id
            }
        }
        return ids;
    }

    public int normalizePositive(Integer value, int fallback) {
        return value != null && value > 0 ? value.intValue() : fallback;
    }

    @Transactional
    public void applyOrderCsv(String csv) {
        List<HomeSectionBlock> blocks = getOrderedBlocks();
        Map<String, HomeSectionBlock> byKey = new LinkedHashMap<>();
        for (HomeSectionBlock b : blocks) {
            byKey.put(b.getBlockKey(), b);
        }
        List<String> targetOrder = new ArrayList<>();
        if (csv != null && !csv.isBlank()) {
            for (String raw : csv.split(",")) {
                String k = raw == null ? "" : raw.trim();
                if (!k.isEmpty() && byKey.containsKey(k) && !targetOrder.contains(k)) {
                    targetOrder.add(k);
                }
            }
        }
        for (String key : byKey.keySet()) {
            if (!targetOrder.contains(key)) {
                targetOrder.add(key);
            }
        }
        for (int i = 0; i < targetOrder.size(); i++) {
            HomeSectionBlock block = byKey.get(targetOrder.get(i));
            block.setDisplayOrder(i + 1);
            homeSectionBlockRepository.save(block);
        }
    }

    @Transactional
    public void initializeDefaultBlocksIfEmpty() {
        List<HomeSectionBlock> all = homeSectionBlockRepository.findAllByOrderByDisplayOrderAscIdAsc();
        if (!all.isEmpty()) {
            normalizeDisplayOrder();
            return;
        }
        int maxOrder = 0;
        for (String key : BUILT_IN_KEYS) {
            HomeSectionBlock block = new HomeSectionBlock();
            block.setBlockKey(key);
            block.setBlockType(TYPE_BUILT_IN);
            block.setTitle(BUILT_IN_TITLES.getOrDefault(key, key));
            block.setContent(null);
            block.setDisplayOrder(++maxOrder);
            block.setBuiltIn(Boolean.TRUE);
            homeSectionBlockRepository.save(block);
        }
        normalizeDisplayOrder();
    }

    @Transactional
    public void normalizeDisplayOrder() {
        List<HomeSectionBlock> all = homeSectionBlockRepository.findAllByOrderByDisplayOrderAscIdAsc();
        for (int i = 0; i < all.size(); i++) {
            HomeSectionBlock b = all.get(i);
            int ord = i + 1;
            if (b.getDisplayOrder() == null || b.getDisplayOrder() != ord) {
                b.setDisplayOrder(ord);
                homeSectionBlockRepository.save(b);
            }
        }
    }
}
