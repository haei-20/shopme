package com.example.gearshop.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.gearshop.model.KhachHang;
import com.example.gearshop.model.NguoiDung;
import com.example.gearshop.model.NhanVien;
import com.example.gearshop.repository.KhachHangRepository;
import com.example.gearshop.repository.NguoiDungRepository;
import com.example.gearshop.repository.NhanVienRepository;

import jakarta.transaction.Transactional;

@Service
public class NguoiDungService {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private NhanVienRepository nhanVienRepository;

    /**
     * Cập nhật hồ sơ. Luôn tìm bản ghi theo {@code nguoiDungId} để tránh lỗi khi đổi {@code tenDangNhap}
     * (lookup theo tên cũ có thể null nếu session/DB lệch hoặc đã đổi tên trước đó).
     */
    public void capNhatThongTin(Integer nguoiDungId, String tenDangNhapMoi, String tenNguoiDungMoi, String emailMoi, String sdtMoi,
            String diaChiMoi, String matKhauXacNhan) {
        NguoiDung nd = nguoiDungRepository.findById(nguoiDungId).orElse(null);
        if (nd == null) {
            throw new IllegalArgumentException("Không tìm thấy người dùng.");
        }
        String tenDangNhapCapNhat = tenDangNhapMoi == null ? "" : tenDangNhapMoi.trim();
        String tenNguoiDung = tenNguoiDungMoi == null ? "" : tenNguoiDungMoi.trim();
        String email = emailMoi == null ? "" : emailMoi.trim();
        String sdt = sdtMoi == null ? "" : sdtMoi.trim();
        String diaChi = diaChiMoi == null ? "" : diaChiMoi.trim();

        if (tenDangNhapCapNhat.isBlank() || tenNguoiDung.isBlank() || email.isBlank() || sdt.isBlank() || diaChi.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin bắt buộc.");
        }
        if (!tenDangNhapCapNhat.matches("^[A-Za-z0-9._-]{4,30}$")) {
            throw new IllegalArgumentException(
                    "Tên đăng nhập chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch ngang (4-30 ký tự).");
        }
        if (tenNguoiDung.length() < 2) {
            throw new IllegalArgumentException("Họ tên cần ít nhất 2 ký tự.");
        }
        if (matKhauXacNhan == null || matKhauXacNhan.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu hiện tại để xác nhận.");
        }
        if (!matKhauXacNhan.equals(nd.getMatKhau())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không đúng.");
        }

        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Email không đúng định dạng (vd: abc@gmail.com).");
        }
        if (!sdt.matches("^(0|\\+84)[0-9]{9}$")) {
            throw new IllegalArgumentException("Số điện thoại phải bắt đầu bằng 0 và có 10 số.");
        }

        Optional<NguoiDung> nguoiDungTheoEmail = nguoiDungRepository.findByEmail(email);
        if (nguoiDungTheoEmail.isPresent() && !nguoiDungTheoEmail.get().getId().equals(nd.getId())) {
            throw new IllegalArgumentException("Email đã được sử dụng bởi tài khoản khác.");
        }
        if (nguoiDungRepository.existsByTenDangNhap(tenDangNhapCapNhat)
                && !tenDangNhapCapNhat.equals(nd.getTenDangNhap())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }

        nd.setTenDangNhap(tenDangNhapCapNhat);
        nd.setTenNguoiDung(tenNguoiDung);
        nd.setEmail(email);
        nd.setSdt(sdt);
        nd.setDiaChi(diaChi);
        nguoiDungRepository.save(nd);
    }

    public String doiMatKhau(String tenDangNhap, String matKhauCu, String matKhauMoi, String xacNhan) {
        NguoiDung nd = nguoiDungRepository.findByTenDangNhap(tenDangNhap);
        if (nd == null) {
            return "Không tìm thấy người dùng.";
        }

        if (!matKhauCu.equals(nd.getMatKhau())) {
            return "Mật khẩu cũ không đúng.";
        }

        if (!matKhauMoi.equals(xacNhan)) {
            return "Mật khẩu xác nhận không khớp.";
        }
        if (matKhauMoi.equals(matKhauCu)) {
            return "Mật khẩu mới không được trùng mật khẩu cũ.";
        }
        if (!matKhauMoi.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$")) {
            return "Mật khẩu mới phải có cả chữ và số, tối thiểu 6 ký tự.";
        }

        nd.setMatKhau(matKhauMoi);
        nguoiDungRepository.save(nd);
        return "Đổi mật khẩu thành công.";
    }

    public void capNhat(NguoiDung nguoiDung) {
        nguoiDungRepository.save(nguoiDung);
    }

    @Transactional
    public void capNhatQuyenAdmin(Integer id) {
        Optional<NguoiDung> nguoiDungOpt = nguoiDungRepository.findById(id);
        if (nguoiDungOpt.isPresent()) {
            NguoiDung nd = nguoiDungOpt.get();

            // Xóa KhachHang nếu tồn tại
            khachHangRepository.deleteByNguoiDung_Id(nd.getId());

            // Tạo mới NhanVien
            NhanVien nv = new NhanVien();
            nv.setMaNhanVien(String.format("NV%04d", nd.getId()));
            nv.setNguoiDung(nd);
            nv.setGhiChu("Cấp quyền admin từ hệ thống");
            nhanVienRepository.save(nv);
        }
    }

    @Transactional
    public void goQuyenAdmin(Integer id) {
        Optional<NguoiDung> nguoiDungOpt = nguoiDungRepository.findById(id);
        if (nguoiDungOpt.isPresent()) {
            NguoiDung nd = nguoiDungOpt.get();

            nhanVienRepository.deleteByNguoiDung_Id(nd.getId());

            // Có thể gán lại thành khách hàng nếu muốn
            KhachHang kh = new KhachHang();
            kh.setMaKhachHang(String.format("KH%04d", nd.getId()));
            kh.setNguoiDung(nd);
            kh.setGhiChu("Gỡ quyền admin");
            kh.setDoanhThu(0L);
            khachHangRepository.save(kh);
        }
    }
}
