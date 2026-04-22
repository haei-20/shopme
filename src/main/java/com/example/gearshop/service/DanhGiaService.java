package com.example.gearshop.service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gearshop.model.DanhGia;
import com.example.gearshop.model.KhachHang;
import com.example.gearshop.model.SanPham;
import com.example.gearshop.model.TrangThaiHoaDonHang;
import com.example.gearshop.repository.DanhGiaRepository;
import com.example.gearshop.repository.HoaDonChiTietRepository;

@Service
public class DanhGiaService {
    private static final Pattern DANGEROUS_SCHEME_PATTERN = Pattern.compile("(?i)\\b(?:javascript|data|vbscript)\\s*:");

    @Autowired
    private DanhGiaRepository danhGiaRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private SanPhamService sanPhamService;

    public List<DanhGia> layDanhSachHienThi(Integer sanPhamId) {
        return danhGiaRepository.findBySanPhamIdForDisplay(sanPhamId);
    }

    public Optional<Double> tinhDiemTrungBinh(Integer sanPhamId) {
        Double v = danhGiaRepository.tinhDiemTrungBinh(sanPhamId);
        return Optional.ofNullable(v);
    }

    public long demSoDanhGia(Integer sanPhamId) {
        return danhGiaRepository.countBySanPham_Id(sanPhamId);
    }

    /** Khách đã mua và đơn ở trạng thái "Đã giao" (có thể gửi hoặc sửa đánh giá). */
    public boolean duocPhepDanhGia(Integer khachHangId, Integer sanPhamId) {
        return hoaDonChiTietRepository.existsDaNhanHangVoiSanPham(
                khachHangId, sanPhamId, TrangThaiHoaDonHang.DA_GIAO);
    }

    public boolean daCoDanhGia(Integer khachHangId, Integer sanPhamId) {
        return danhGiaRepository.findByKhachHang_IdAndSanPham_Id(khachHangId, sanPhamId).isPresent();
    }

    public Optional<DanhGia> timCuaKhachHang(Integer khachHangId, Integer sanPhamId) {
        return danhGiaRepository.findByKhachHang_IdAndSanPham_Id(khachHangId, sanPhamId);
    }

    @Transactional
    public void luuHoacCapNhat(KhachHang khachHang, Integer sanPhamId, int soSao, String noiDung) {
        if (khachHang == null || sanPhamId == null) {
            throw new IllegalArgumentException("Thiếu thông tin khách hoặc sản phẩm.");
        }
        if (!duocPhepDanhGia(khachHang.getId(), sanPhamId)) {
            throw new IllegalStateException("Bạn chỉ có thể đánh giá sau khi đơn hàng đã được giao.");
        }
        if (soSao < 1 || soSao > 5) {
            throw new IllegalArgumentException("Số sao từ 1 đến 5.");
        }
        String nd = noiDung != null ? noiDung.trim() : "";
        if (nd.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung đánh giá.");
        }
        if (nd.length() > 2000) {
            throw new IllegalArgumentException("Nội dung không quá 2000 ký tự.");
        }
        if (DANGEROUS_SCHEME_PATTERN.matcher(nd).find()) {
            throw new IllegalArgumentException("Nội dung chứa liên kết không an toàn.");
        }

        SanPham sanPham = sanPhamService.getSanPhamById(sanPhamId);
        if (sanPham == null) {
            throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
        }

        Optional<DanhGia> existing = danhGiaRepository.findByKhachHang_IdAndSanPham_Id(khachHang.getId(), sanPhamId);
        DanhGia dg = existing.orElseGet(DanhGia::new);
        dg.setSoSao(soSao);
        dg.setNoiDung(nd);
        dg.setKhachHang(khachHang);
        dg.setSanPham(sanPham);
        dg.setNgayDanhGia(LocalDateTime.now());
        danhGiaRepository.save(dg);
    }

    @Transactional(readOnly = true)
    public List<DanhGia> adminLayDanhSach(Integer sanPhamIdLoc, String khachHang, Integer soSao, LocalDate tuNgay,
            LocalDate denNgay) {
        String kh = khachHang != null ? khachHang.trim() : "";
        if (kh.isEmpty()) {
            kh = null;
        }
        LocalDateTime from = tuNgay != null ? tuNgay.atStartOfDay() : null;
        LocalDateTime to = denNgay != null ? denNgay.atTime(LocalTime.MAX) : null;
        return danhGiaRepository.findAllForAdmin(sanPhamIdLoc, kh, soSao, from, to);
    }

    @Transactional
    public void adminXoa(Integer id) {
        if (id == null || !danhGiaRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy đánh giá.");
        }
        danhGiaRepository.deleteById(id);
    }

    @Transactional
    public void adminPhanHoi(Integer id, String phanHoi) {
        DanhGia dg = danhGiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá."));
        String text = phanHoi != null ? phanHoi.trim() : "";
        if (text.length() > 2000) {
            throw new IllegalArgumentException("Phản hồi không quá 2000 ký tự.");
        }
        dg.setPhanHoi(text.isEmpty() ? null : text);
        danhGiaRepository.save(dg);
    }
}
