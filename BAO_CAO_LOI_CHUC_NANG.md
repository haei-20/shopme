# Bao cao loi chuc nang (kiem tra nhanh)

Ngay kiem tra: 2026-04-15
Pham vi: giao dien client/admin, route controller, mot so endpoint runtime

## 1. Loi nghiem trong (xac nhan)

### 1.1 Quen mat khau bi 404
- Nut/link: `templates/clientTemplate/dangnhap.html`
- Duong dan goi: `/quen-mat-khau`
- Van de: khong co endpoint backend xu ly
- Hien tuong: bam "Quen mat khau?" bi 404

### 1.2 Chon nguoi nhan khong cap nhat thongTinNhanHangID
- Trang: `templates/clientTemplate/xemhoadon.html`
- Van de: hidden input `thongTinNhanHangID` duoc gui khi luu hoa don, nhung ham `selectReceiver()` khong cap nhat lai gia tri nay theo nguoi vua chon
- Anh huong: co the luu hoa don sai nguoi nhan hoac loi khi submit

### 1.3 Nut "Chon" cua nguoi nhan vua them moi khong hoat dong dung
- Trang: `templates/clientTemplate/xemhoadon.html`
- Van de: dong moi duoc append khong gan `data-id`, trong khi `selectReceiver()` can `data-id` de goi `/receivers/{id}`
- Anh huong: co the goi `/receivers/undefined` hoac khong thay du lieu dung

### 1.4 Admin them san pham loai Case co nguy co loi switch fall-through
- Backend: `controller/AdminThemSanPhamController.java`
- Van de: `case 8` (Case) khong co `break`, roi xuong `case 9` (Man hinh)
- Anh huong: de phat sinh loi parse du lieu khi them san pham loai Case

### 1.5 GET /dangky bi 500
- Backend: `controller/HomeController.java` co `@GetMapping("/dangky")` tra view `dangky`
- Van de: khong tim thay template `dangky.html`
- Anh huong: truy cap truc tiep `/dangky` bi loi 500

## 2. Van de trung binh

### 2.1 Checkbox "Ghi nho dang nhap" chua co xu ly backend
- Trang: `templates/clientTemplate/dangnhap.html`
- Backend dang nhap hien tai khong co logic remember-me

### 2.2 Nhieu route redirect ve /error nhung chua thay custom error handler
- Nhieu controller san pham chi tiet dang dung `redirect:/error`
- Can xac dinh trang error hien tai co duoc xu ly dung muc mong muon khong

## 3. Goi y uu tien sua

1. Them endpoint quen mat khau hoac an link tam thoi
2. Sua `xemhoadon.html` de dong bo `thongTinNhanHangID` khi chon nguoi nhan
3. Sua append row nguoi nhan moi de co `data-id`
4. Them `break` cho `case 8` trong `AdminThemSanPhamController`
5. Tao template `dangky.html` hoac doi GET `/dangky` ve trang dang nhap/modal

## 4. Cach test nhanh sau khi sua

1. Vao `/dangnhap` va bam "Quen mat khau?"
2. Them 2 thong tin nhan hang, chon qua lai tung nguoi, bam "Luu hoa don"
3. Thu them san pham loai Case tren trang admin
4. Truy cap truc tiep `/dangky`
