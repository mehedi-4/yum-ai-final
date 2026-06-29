package com.yumai.repository;

import com.yumai.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {
    Optional<Bill> findByOrderOrderId(Long orderId);

    List<Bill> findAllByOrderByGeneratedAtDesc();

    List<Bill> findByGeneratedAtBetween(LocalDateTime from, LocalDateTime to);
}
