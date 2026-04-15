package com.example.gearshop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.gearshop.model.Voucher;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    Optional<Voucher> findByMaVoucher(String maVoucher);

    boolean existsByMaVoucher(String maVoucher);

    List<Voucher> findAllByOrderByThoiHanDesc(); // Mới nhất

    List<Voucher> findAllByOrderByThoiHanAsc(); // Cũ nhất

    List<Voucher> findAllByOrderByTenVoucherAsc();

    List<Voucher> findAllByOrderByTenVoucherDesc();

    List<Voucher> findByTenVoucherContainingIgnoreCase(String keyword);

    Voucher findTopByOrderByIdDesc();
}
