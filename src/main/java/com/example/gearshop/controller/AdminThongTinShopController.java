package com.example.gearshop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.gearshop.model.ThongTinShop;
import com.example.gearshop.service.ThongTinShopService;

@Controller
@RequestMapping("/admin/thongtinshop")
public class AdminThongTinShopController {

    @Autowired
    private ThongTinShopService thongTinShopService;

    @GetMapping
    public String hienThiTrangThongTinShop(Model model) {
        model.addAttribute("thongTinShop", thongTinShopService.getOrCreateThongTinShop());
        return "adminTemplate/thongtinshop";
    }

    @PostMapping
    public String capNhatThongTinShop(
            @RequestParam String tenShop,
            @RequestParam String diaChiShop,
            @RequestParam String soDienThoaiShop,
            @RequestParam String emailShop,
            RedirectAttributes redirectAttributes) {

        ThongTinShop updated = thongTinShopService.capNhatThongTinShop(
                tenShop.trim(),
                diaChiShop.trim(),
                soDienThoaiShop.trim(),
                emailShop.trim());

        redirectAttributes.addFlashAttribute("successMessage",
                "Đã cập nhật thông tin shop: " + updated.getTenShop());
        return "redirect:/admin/thongtinshop";
    }
}
