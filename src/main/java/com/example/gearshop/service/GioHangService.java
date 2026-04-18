package com.example.gearshop.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gearshop.model.GioHang;
import com.example.gearshop.model.GioHangChiTiet;
import com.example.gearshop.model.SanPham;
import com.example.gearshop.repository.GioHangChiTietRepository;
import com.example.gearshop.repository.GioHangRepository;

@Service
@Transactional
public class GioHangService {

    @Autowired
    private GioHangRepository gioHangRepository;

    @Autowired
    private GioHangChiTietRepository gioHangChiTietRepository;

    public GioHang getCartByCustomerId(Integer customerId) {
        if (customerId == null) {
            return null;
        }
        return gioHangRepository.findByMaGioHang(buildCartCode(customerId));
    }

    public GioHang getOrCreateCartByCustomerId(Integer customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID must not be null");
        }

        GioHang existingCart = getCartByCustomerId(customerId);
        if (existingCart != null) {
            return existingCart;
        }

        GioHang gioHang = new GioHang();
        gioHang.setMaGioHang(buildCartCode(customerId));
        return gioHangRepository.save(gioHang);
    }

    public List<GioHangChiTiet> getCartDetailsByCartId(Integer cartId) {
        return gioHangChiTietRepository.findByGioHang_Id(cartId);
    }

    public List<GioHangChiTiet> getCartDetailsByCustomerId(Integer customerId) {
        GioHang gioHang = getCartByCustomerId(customerId);
        if (gioHang == null) {
            return new ArrayList<>();
        }
        return getCartDetailsByCartId(gioHang.getId());
    }

    /**
     * Payload giỏ hàng đồng bộ với session / API (một nguồn duy nhất cho add-to-cart, merge, /api/cart).
     */
    public List<Map<String, Object>> buildSessionCartPayload(Integer customerId) {
        if (customerId == null) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (GioHangChiTiet chiTiet : getCartDetailsByCustomerId(customerId)) {
            result.add(mapChiTietToSessionItem(chiTiet));
        }
        return result;
    }

    private Map<String, Object> mapChiTietToSessionItem(GioHangChiTiet chiTiet) {
        Map<String, Object> item = new HashMap<>();
        item.put("sanPhamID", chiTiet.getSanPham().getId());
        item.put("name", chiTiet.getSanPham().getTenSanPham());
        item.put("price", chiTiet.getDonGia());
        item.put("image", "/images/product/" + chiTiet.getSanPham().getHinhAnh());
        item.put("tonKho", chiTiet.getSanPham().getTonKho());
        item.put("quantity", chiTiet.getSoLuong());
        item.put("duocChonThanhToan", Boolean.TRUE.equals(chiTiet.getDuocChonThanhToan()));
        return item;
    }

    /**
     * Các dòng được tích chọn thanh toán — dùng cho trang đặt hàng và lưu hóa đơn (nguồn DB).
     */
    public List<Map<String, Object>> buildSelectedLinesForOrder(Integer customerId) {
        if (customerId == null) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (GioHangChiTiet chiTiet : getCartDetailsByCustomerId(customerId)) {
            if (!Boolean.TRUE.equals(chiTiet.getDuocChonThanhToan())) {
                continue;
            }
            Map<String, Object> item = mapChiTietToSessionItem(chiTiet);
            BigDecimal dg = chiTiet.getDonGia();
            double priceNumeric = dg == null ? 0 : dg.doubleValue();
            item.put("priceNumeric", priceNumeric);
            result.add(item);
        }
        return result;
    }

    /**
     * Đồng bộ cờ chọn trên DB: chỉ các {@code sanPhamId} trong danh sách là được chọn thanh toán.
     */
    public void syncDuocChonThanhToan(Integer customerId, List<Integer> selectedSanPhamIds) {
        GioHang gioHang = getCartByCustomerId(customerId);
        if (gioHang == null) {
            return;
        }
        Set<Integer> selected = new HashSet<>();
        if (selectedSanPhamIds != null) {
            for (Integer id : selectedSanPhamIds) {
                if (id != null) {
                    selected.add(id);
                }
            }
        }
        for (GioHangChiTiet chiTiet : getCartDetailsByCartId(gioHang.getId())) {
            boolean isOn = selected.contains(chiTiet.getSanPham().getId());
            chiTiet.setDuocChonThanhToan(isOn);
            gioHangChiTietRepository.save(chiTiet);
        }
    }

    public GioHangChiTiet addCartItem(GioHangChiTiet cartItem) {
        return gioHangChiTietRepository.save(cartItem);
    }

    public void removeCartItem(Integer cartItemId) {
        gioHangChiTietRepository.deleteById(cartItemId);
    }

    public void clearCart(Integer cartId) {
        List<GioHangChiTiet> cartItems = gioHangChiTietRepository.findByGioHang_Id(cartId);
        gioHangChiTietRepository.deleteAll(cartItems);
    }

    /**
     * Đảm bảo tổng số lượng (đã có trong giỏ + số thêm) không vượt {@link SanPham#getTonKho()}.
     */
    public void assertTongSoLuongKhongVuotTonKho(SanPham sanPham, int tongSoLuongYeuCau) {
        if (sanPham == null) {
            throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
        }
        if (tongSoLuongYeuCau < 1) {
            throw new IllegalArgumentException("Số lượng phải ít nhất là 1.");
        }
        int ton = sanPham.getTonKho() == null ? 0 : sanPham.getTonKho();
        if (tongSoLuongYeuCau > ton) {
            throw new IllegalArgumentException(String.format(
                    "Sản phẩm \"%s\" chỉ còn %d trong kho (bạn đang yêu cầu %d).",
                    sanPham.getTenSanPham() != null ? sanPham.getTenSanPham() : "này",
                    ton,
                    tongSoLuongYeuCau));
        }
    }

    public GioHangChiTiet addOrUpdateCartItem(Integer customerId, SanPham sanPham, int soLuong) {
        if (customerId == null || sanPham == null) {
            throw new IllegalArgumentException("Customer and product are required");
        }

        GioHang gioHang = getOrCreateCartByCustomerId(customerId);
        Optional<GioHangChiTiet> existingItem = gioHangChiTietRepository
                .findByGioHang_IdAndSanPham_Id(gioHang.getId(), sanPham.getId());

        int daCoTrongGio = existingItem.map(GioHangChiTiet::getSoLuong).orElse(0);
        assertTongSoLuongKhongVuotTonKho(sanPham, daCoTrongGio + soLuong);

        if (existingItem.isPresent()) {
            GioHangChiTiet chiTiet = existingItem.get();
            chiTiet.setSoLuong(chiTiet.getSoLuong() + soLuong);
            chiTiet.setDonGia(resolveDonGia(sanPham));
            return gioHangChiTietRepository.save(chiTiet);
        }

        GioHangChiTiet cartItem = new GioHangChiTiet();
        cartItem.setMaGioHangChiTiet(buildCartDetailCode(gioHang.getId(), sanPham.getId()));
        cartItem.setSoLuong(soLuong);
        cartItem.setDonGia(resolveDonGia(sanPham));
        cartItem.setDuocChonThanhToan(false);
        cartItem.setGioHang(gioHang);
        cartItem.setSanPham(sanPham);
        return gioHangChiTietRepository.save(cartItem);
    }

    public GioHangChiTiet updateCartItemQuantity(Integer customerId, Integer sanPhamId, int soLuong) {
        GioHang gioHang = getCartByCustomerId(customerId);
        if (gioHang == null) {
            throw new IllegalArgumentException("Giỏ hàng không tồn tại");
        }

        GioHangChiTiet chiTiet = gioHangChiTietRepository
                .findByGioHang_IdAndSanPham_Id(gioHang.getId(), sanPhamId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại trong giỏ hàng"));

        chiTiet.setSoLuong(soLuong);
        chiTiet.setDonGia(resolveDonGia(chiTiet.getSanPham()));
        return gioHangChiTietRepository.save(chiTiet);
    }

    public void removeCartItem(Integer customerId, Integer sanPhamId) {
        GioHang gioHang = getCartByCustomerId(customerId);
        if (gioHang != null) {
            gioHangChiTietRepository.deleteByGioHang_IdAndSanPham_Id(gioHang.getId(), sanPhamId);
        }
    }

    public void removeSelectedCartItems(Integer customerId, List<Integer> sanPhamIds) {
        if (sanPhamIds == null || sanPhamIds.isEmpty()) {
            return;
        }

        for (Integer sanPhamId : sanPhamIds) {
            removeCartItem(customerId, sanPhamId);
        }
    }

    public void clearCartByCustomerId(Integer customerId) {
        GioHang gioHang = getCartByCustomerId(customerId);
        if (gioHang != null) {
            clearCart(gioHang.getId());
        }
    }

    private String buildCartCode(Integer customerId) {
        return "GH-KH-" + customerId;
    }

    private String buildCartDetailCode(Integer gioHangId, Integer sanPhamId) {
        return "GHCT-" + gioHangId + "-" + sanPhamId;
    }

    private BigDecimal resolveDonGia(SanPham sanPham) {
        return sanPham.getGia() == null ? BigDecimal.ZERO : sanPham.getGia();
    }
}