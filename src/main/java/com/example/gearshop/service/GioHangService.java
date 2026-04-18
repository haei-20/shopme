package com.example.gearshop.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.gearshop.model.GioHang;
import com.example.gearshop.model.GioHangChiTiet;
import com.example.gearshop.model.SanPham;
import com.example.gearshop.repository.GioHangChiTietRepository;
import com.example.gearshop.repository.GioHangRepository;

@Service
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

    public GioHangChiTiet addOrUpdateCartItem(Integer customerId, SanPham sanPham, int soLuong) {
        if (customerId == null || sanPham == null) {
            throw new IllegalArgumentException("Customer and product are required");
        }

        GioHang gioHang = getOrCreateCartByCustomerId(customerId);
        Optional<GioHangChiTiet> existingItem = gioHangChiTietRepository
                .findByGioHang_IdAndSanPham_Id(gioHang.getId(), sanPham.getId());

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