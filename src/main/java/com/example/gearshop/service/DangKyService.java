package com.example.gearshop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.gearshop.model.KhachHang;
import com.example.gearshop.model.NguoiDung;
import com.example.gearshop.repository.KhachHangRepository;
import com.example.gearshop.repository.NguoiDungRepository;

@Service
public class DangKyService {

    @Autowired
    private NguoiDungRepository nguoiDungRepo;

    @Autowired
    private KhachHangRepository khachHangRepo;

    public String generateMaNguoiDungMoi() {
        String max = nguoiDungRepo.findMaxMaNguoiDung();
        int next = (max != null && max.startsWith("ND")) ? Integer.parseInt(max.substring(2)) + 1 : 1;
        return String.format("ND%04d", next);
    }

    public String generateMaKhachHangMoi() {
        String max = khachHangRepo.findMaxMaKhachHang();
        int next = (max != null && max.startsWith("KH")) ? Integer.parseInt(max.substring(2)) + 1 : 1;
        return String.format("KH%04d", next);
    }

    public String validateThongTinDangKy(String tenDangNhap, String matKhau, String matKhauNhapLai,
            String email, String sdt, String diaChi, String tenNguoiDung) {
        if (tenDangNhap.isEmpty() || matKhau.isEmpty() || matKhauNhapLai.isEmpty()
                || email.isEmpty() || sdt.isEmpty() || diaChi.isEmpty()) {
            return "Vui lòng điền đầy đủ thông tin.";
        }

        if (!matKhau.equals(matKhauNhapLai)) {
            return "Mật khẩu xác nhận không khớp.";
        }

        if (nguoiDungRepo.existsByTenDangNhap(tenDangNhap)) {
            return "Tên đăng nhập đã được sử dụng.";
        }

        if (nguoiDungRepo.existsByEmail(email)) {
            return "Email đã được sử dụng.";
        }

        return null;
    }

    public void taoTaiKhoan(String tenDangNhap, String matKhau,
            String email, String sdt, String diaChi, String tenNguoiDung) {
        NguoiDung nd = new NguoiDung();
        nd.setMaNguoiDung(generateMaNguoiDungMoi());
        nd.setTenDangNhap(tenDangNhap);
        nd.setMatKhau(matKhau);
        nd.setEmail(email);
        nd.setSdt(sdt);
        nd.setDiaChi(diaChi);
        nd.setTenNguoiDung(tenNguoiDung);

        nguoiDungRepo.save(nd);

        KhachHang kh = new KhachHang();
        kh.setMaKhachHang(generateMaKhachHangMoi());
        kh.setGhiChu("");
        kh.setNguoiDung(nd);

        khachHangRepo.save(kh);
    }

    public boolean dangKyTaiKhoan(String tenDangNhap, String matKhau, String matKhauNhapLai,
            String email, String sdt, String diaChi, String tenNguoiDung, StringBuilder thongBao) {
        String validationError = validateThongTinDangKy(
                tenDangNhap, matKhau, matKhauNhapLai, email, sdt, diaChi, tenNguoiDung);
        if (validationError != null) {
            thongBao.append(validationError);
            return false;
        }

        taoTaiKhoan(tenDangNhap, matKhau, email, sdt, diaChi, tenNguoiDung);

        thongBao.append("Đăng ký thành công! Vui long đăng nhập để tiếp tục.");
        return true;
    }
}
