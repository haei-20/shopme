package com.example.gearshop.repository;

import java.util.List;
import java.util.Optional;

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
            AND (:tuKhoa IS NULL OR :tuKhoa = '' OR LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                OR LOWER(sp.maSanPham) LIKE LOWER(CONCAT('%', :tuKhoa, '%')))
            ORDER BY d.ngayDanhGia DESC, d.id DESC
            """)
    List<DanhGia> findAllForAdmin(
            @Param("sanPhamId") Integer sanPhamId,
            @Param("tuKhoa") String tuKhoa);
}
