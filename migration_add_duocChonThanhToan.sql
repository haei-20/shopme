-- Migration: Thêm cột duocChonThanhToan vào bảng gioHangChiTiet
-- Chạy script này nếu bảng gioHangChiTiet đã tồn tại

-- Kiểm tra và thêm cột nếu chưa tồn tại
ALTER TABLE gioHangChiTiet 
ADD COLUMN duocChonThanhToan BOOLEAN DEFAULT FALSE;

-- Nếu cột đã tồn tại nhưng không có default value, chạy lệnh này:
-- ALTER TABLE gioHangChiTiet 
-- MODIFY COLUMN duocChonThanhToan BOOLEAN DEFAULT FALSE;

-- Cập nhật tất cả các bản ghi cũ có NULL thành FALSE (nếu cần)
-- UPDATE gioHangChiTiet SET duocChonThanhToan = FALSE WHERE duocChonThanhToan IS NULL;
