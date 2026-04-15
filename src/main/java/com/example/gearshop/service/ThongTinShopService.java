package com.example.gearshop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.gearshop.model.ThongTinShop;
import com.example.gearshop.repository.ThongTinShopRepository;

@Service
public class ThongTinShopService {

    @Autowired
    private ThongTinShopRepository thongTinShopRepository;

    public ThongTinShop getOrCreateThongTinShop() {
        return thongTinShopRepository.findTopByOrderByIdAsc()
                .orElseGet(this::createDefaultThongTinShop);
    }

    public ThongTinShop capNhatThongTinShop(String tenShop, String diaChiShop, String soDienThoaiShop, String emailShop) {
        ThongTinShop thongTinShop = getOrCreateThongTinShop();
        thongTinShop.setTenShop(tenShop);
        thongTinShop.setDiaChiShop(diaChiShop);
        thongTinShop.setSoDienThoaiShop(soDienThoaiShop);
        thongTinShop.setEmailShop(emailShop);
        return thongTinShopRepository.save(thongTinShop);
    }

    private ThongTinShop createDefaultThongTinShop() {
        ThongTinShop defaultInfo = new ThongTinShop();
        defaultInfo.setTenShop("PGearShop");
        defaultInfo.setDiaChiShop("Km10 đường Trần Phú, quận Hà Đông, thành phố Hà Nội");
        defaultInfo.setSoDienThoaiShop("0999 999 999");
        defaultInfo.setEmailShop("contact@pgearshop.vn");
        return thongTinShopRepository.save(defaultInfo);
    }
}
