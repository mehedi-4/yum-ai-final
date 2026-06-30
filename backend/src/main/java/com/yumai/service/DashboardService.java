package com.yumai.service;

import com.yumai.dto.DashboardDtos.*;
import com.yumai.entity.Order;
import com.yumai.entity.OrderItem;
import com.yumai.entity.OrderStatus;
import com.yumai.repository.InventoryItemRepository;
import com.yumai.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Real-time KPIs and peak-hour analytics (FR-04). */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public DashboardData dashboard() {
        return new DashboardData(kpis(), peakHours(), topItems());
    }

    /** FR-04.1 */
    public Kpis kpis() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        List<Order> today = orderRepository.findByCreatedAtBetween(startOfDay, now);
        double revenueToday = today.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .mapToDouble(Order::getTotalAmount).sum();

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        double revenueThisMonth = orderRepository
                .findByStatusAndCreatedAtBetween(OrderStatus.COMPLETED, startOfMonth, now)
                .stream().mapToDouble(Order::getTotalAmount).sum();

        return new Kpis(
                today.size(),
                round2(revenueToday),
                inventoryItemRepository.findLowStock().size(),
                orderRepository.countByStatus(OrderStatus.PENDING),
                round2(revenueThisMonth),
                orderRepository.count());
    }

    /** FR-04.3 - peak-hour ordering patterns over the last 30 days. */
    public List<PeakHour> peakHours() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> orders = orderRepository.findByCreatedAtBetween(now.minusDays(30), now);
        Map<Integer, Long> byHour = new TreeMap<>();
        for (Order order : orders) {
            byHour.merge(order.getCreatedAt().getHour(), 1L, Long::sum);
        }
        return byHour.entrySet().stream()
                .map(e -> new PeakHour(e.getKey(), e.getValue()))
                .toList();
    }

    /** Top 5 selling items over the last 30 days. */
    public List<TopItem> topItems() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> orders = orderRepository
                .findByStatusAndCreatedAtBetween(OrderStatus.COMPLETED, now.minusDays(30), now);
        Map<String, long[]> qty = new HashMap<>();
        Map<String, Double> revenue = new HashMap<>();
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                String name = item.getMenuItem().getName();
                qty.computeIfAbsent(name, k -> new long[1])[0] += item.getQuantity();
                revenue.merge(name, item.getSubtotal(), Double::sum);
            }
        }
        return qty.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .limit(5)
                .map(e -> new TopItem(e.getKey(), e.getValue()[0], round2(revenue.get(e.getKey()))))
                .toList();
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
