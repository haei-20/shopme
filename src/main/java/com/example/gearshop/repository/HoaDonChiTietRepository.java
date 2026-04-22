package com.example.gearshop.repository;

import com.example.gearshop.model.HoaDonChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, Integer> {
    Optional<HoaDonChiTiet> findTopByOrderByIdDesc();

    List<HoaDonChiTiet> findByHoaDonID(Integer hoaDonID);

    boolean existsBySanPhamID(Integer sanPhamID);

    /** Khách đã có đơn ở trạng thái đã giao chứa sản phẩm (đủ điều kiện đánh giá). */
    @Query("""
            SELECT CASE WHEN COUNT(ct) > 0 THEN true ELSE false END
            FROM HoaDonChiTiet ct, HoaDon hd
            WHERE ct.hoaDonID = hd.id
            AND hd.thongTinNhanHang.khachHangID = :khachHangId
            AND ct.sanPhamID = :sanPhamId
            AND hd.trangThaiDonHang = :trangThaiDaGiao
            """)
    boolean existsDaNhanHangVoiSanPham(
            @Param("khachHangId") Integer khachHangId,
            @Param("sanPhamId") Integer sanPhamId,
            @Param("trangThaiDaGiao") String trangThaiDaGiao);

    @Query("SELECT COALESCE(sp.loaiSanPham.tenLoaiSanPham, 'Khác'), SUM(hdct.thanhTien) " +
            "FROM HoaDonChiTiet hdct " +
            "JOIN SanPham sp ON hdct.sanPhamID = sp.id " +
            "GROUP BY sp.loaiSanPham.id, COALESCE(sp.loaiSanPham.tenLoaiSanPham, 'Khác')")
    List<Object[]> doanhThuTheoLoaiSanPham();

    /** Cơ cấu doanh thu theo loại SP trong khoảng thời gian (ngày tạo hóa đơn). */
    @Query("SELECT COALESCE(sp.loaiSanPham.tenLoaiSanPham, 'Khác'), SUM(hdct.thanhTien) " +
            "FROM HoaDonChiTiet hdct " +
            "JOIN hdct.hoaDon hd " +
            "JOIN SanPham sp ON sp.id = hdct.sanPhamID " +
            "WHERE hd.ngayTao >= :tu AND hd.ngayTao < :den " +
            "GROUP BY sp.loaiSanPham.id, COALESCE(sp.loaiSanPham.tenLoaiSanPham, 'Khác')")
    List<Object[]> doanhThuTheoLoaiSanPhamTrongKhoang(
            @Param("tu") LocalDateTime tu,
            @Param("den") LocalDateTime den);
}
