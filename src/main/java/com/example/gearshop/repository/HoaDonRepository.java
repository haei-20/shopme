package com.example.gearshop.repository;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.gearshop.model.HoaDon;

import java.util.Optional;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> {
    Optional<HoaDon> findTopByOrderByIdDesc();

    List<HoaDon> findByThongTinNhanHang_KhachHangID(Integer khachHangID);
    Page<HoaDon> findByThongTinNhanHang_KhachHangID(Integer khachHangID, Pageable pageable);
    Page<HoaDon> findByThongTinNhanHang_KhachHangIDAndTrangThaiDonHangIn(Integer khachHangID, List<String> trangThaiList,
            Pageable pageable);

    boolean existsByIdAndThongTinNhanHang_KhachHangID(Integer id, Integer khachHangID);

    @Query("""
            SELECT hd
            FROM HoaDon hd
            JOIN FETCH hd.thongTinNhanHang ttnh
            JOIN KhachHang kh ON ttnh.khachHangID = kh.id
            JOIN kh.nguoiDung nd
            WHERE nd.id = :nguoiDungId
            ORDER BY hd.ngayTao DESC
            """)
    List<HoaDon> findByNguoiDungId(@Param("nguoiDungId") Integer nguoiDungId);

    @Query("""
            SELECT CASE WHEN COUNT(hd) > 0 THEN true ELSE false END
            FROM HoaDon hd
            JOIN hd.thongTinNhanHang ttnh
            JOIN KhachHang kh ON ttnh.khachHangID = kh.id
            JOIN kh.nguoiDung nd
            WHERE hd.id = :hoaDonId AND nd.id = :nguoiDungId
            """)
    boolean existsByIdAndNguoiDungId(@Param("hoaDonId") Integer hoaDonId, @Param("nguoiDungId") Integer nguoiDungId);

    @Query("SELECT hd FROM HoaDon hd " +
            "JOIN ThongTinNhanHang ttnh ON hd.thongTinNhanHang.id = ttnh.id " +
            "JOIN KhachHang kh ON ttnh.khachHangID = kh.id " +
            "JOIN NguoiDung nd ON kh.nguoiDung.id = nd.id " +
            "WHERE nd.tenNguoiDung LIKE %:tenNguoiDung% " +
            "ORDER BY hd.ngayTao DESC")
    List<HoaDon> findByTenNguoiDungContainingIgnoreCase(@Param("tenNguoiDung") String tenNguoiDung);

    @Query("""
            SELECT hd
            FROM HoaDon hd
            JOIN hd.thongTinNhanHang ttnh
            LEFT JOIN KhachHang kh ON ttnh.khachHangID = kh.id
            LEFT JOIN kh.nguoiDung nd
            WHERE (:maHoaDon IS NULL OR LOWER(hd.maHoaDon) LIKE LOWER(CONCAT('%', :maHoaDon, '%')))
              AND (:tenKhachHang IS NULL OR LOWER(nd.tenNguoiDung) LIKE LOWER(CONCAT('%', :tenKhachHang, '%')))
              AND (:trangThai IS NULL OR hd.trangThaiDonHang = :trangThai)
              AND (:tuNgay IS NULL OR hd.ngayTao >= :tuNgay)
              AND (:denNgay IS NULL OR hd.ngayTao <= :denNgay)
            """)
    List<HoaDon> findAllForAdminFilters(
            @Param("maHoaDon") String maHoaDon,
            @Param("tenKhachHang") String tenKhachHang,
            @Param("trangThai") String trangThai,
            @Param("tuNgay") LocalDateTime tuNgay,
            @Param("denNgay") LocalDateTime denNgay);

    // Thống kê doanh thu theo ngày
    @Query("SELECT FUNCTION('DATE', hd.ngayTao), SUM(hd.tongGia) " +
            "FROM HoaDon hd " +
            "GROUP BY FUNCTION('DATE', hd.ngayTao) " +
            "ORDER BY FUNCTION('DATE', hd.ngayTao)")
    List<Object[]> doanhThuTheoNgay();

    // Thống kê doanh thu theo tháng
    @Query("SELECT FUNCTION('MONTH', hd.ngayTao), SUM(hd.tongGia) " +
            "FROM HoaDon hd " +
            "GROUP BY FUNCTION('MONTH', hd.ngayTao) " +
            "ORDER BY FUNCTION('MONTH', hd.ngayTao)")
    List<Object[]> doanhThuTheoThang();
}
