CREATE TABLE IF NOT EXISTS thongTinShop (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tenShop VARCHAR(255) NOT NULL,
    diaChiShop VARCHAR(500) NOT NULL,
    soDienThoaiShop VARCHAR(50) NOT NULL,
    emailShop VARCHAR(255) NOT NULL
);

INSERT INTO thongTinShop (tenShop, diaChiShop, soDienThoaiShop, emailShop)
SELECT 'PGearShop', 'Km10 đường Trần Phú, quận Hà Đông, thành phố Hà Nội', '0999 999 999', 'contact@pgearshop.vn'
WHERE NOT EXISTS (SELECT 1 FROM thongTinShop);
