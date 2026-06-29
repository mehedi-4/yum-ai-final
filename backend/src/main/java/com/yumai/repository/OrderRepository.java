package com.yumai.repository;

import com.yumai.entity.Order;
import com.yumai.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<Order> findByStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime from, LocalDateTime to);

    long countByStatus(OrderStatus status);

    List<Order> findAllByOrderByCreatedAtDesc();
}
