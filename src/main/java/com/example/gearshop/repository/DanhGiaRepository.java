package com.example.gearshop.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.gearshop.model.DanhGia;

@Repository
public interface DanhGiaRepository extends JpaRepository<DanhGia, Integer> {

    @Query("""
            SELECT d FROM DanhGia d
            JOIN FETCH d.khachHang kh
            JOIN FETCH kh.nguoiDung
            WHERE d.sanPham.id = :sanPhamId
            ORDER BY d.ngayDanhGia DESC
            """)
    List<DanhGia> findBySanPhamIdForDisplay(@Param("sanPhamId") Integer sanPhamId);

    Optional<DanhGia> findByKhachHang_IdAndSanPham_Id(Integer khachHangId, Integer sanPhamId);

    long countBySanPham_Id(Integer sanPhamId);

    @Query("SELECT AVG(d.soSao) FROM DanhGia d WHERE d.sanPham.id = :sanPhamId")
    Double tinhDiemTrungBinh(@Param("sanPhamId") Integer sanPhamId);

    @Query("""
            SELECT d FROM DanhGia d
            JOIN FETCH d.khachHang kh
            JOIN FETCH kh.nguoiDung
            JOIN FETCH d.sanPham sp
            WHERE (:sanPhamId IS NULL OR sp.id = :sanPhamId)
            AND (:khachHang IS NULL OR :khachHang = '' OR LOWER(kh.nguoiDung.tenNguoiDung) LIKE LOWER(CONCAT('%', :khachHang, '%')))
            AND (:soSao IS NULL OR d.soSao = :soSao)
            AND (:tuNgay IS NULL OR d.ngayDanhGia >= :tuNgay)
            AND (:denNgay IS NULL OR d.ngayDanhGia <= :denNgay)
            ORDER BY d.ngayDanhGia DESC, d.id DESC
            """)
    List<DanhGia> findAllForAdmin(
            @Param("sanPhamId") Integer sanPhamId,
            @Param("khachHang") String khachHang,
            @Param("soSao") Integer soSao,
            @Param("tuNgay") LocalDateTime tuNgay,
            @Param("denNgay") LocalDateTime denNgay);

    @Query("""
            SELECT d FROM DanhGia d
            JOIN FETCH d.khachHang kh
            JOIN FETCH kh.nguoiDung
            JOIN FETCH d.sanPham sp
            WHERE (:sanPhamId IS NULL OR sp.id = :sanPhamId)
            AND (:soSao IS NULL OR d.soSao = :soSao)
            AND (:tuNgay IS NULL OR d.ngayDanhGia >= :tuNgay)
            AND (:denNgay IS NULL OR d.ngayDanhGia <= :denNgay)
            ORDER BY d.ngayDanhGia DESC, d.id DESC
            """)
    List<DanhGia> findAllForAdmin(
            @Param("sanPhamId") Integer sanPhamId,
            @Param("soSao") Integer soSao,
            @Param("tuNgay") LocalDateTime tuNgay,
            @Param("denNgay") LocalDateTime denNgay);
}
