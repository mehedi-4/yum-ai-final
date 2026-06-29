package com.yumai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** SRS 5.1.6 - invoice generated for each completed order (FR-02.4). */
@Entity
@Table(name = "bills")
@Getter
@Setter
@NoArgsConstructor
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long billId;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", unique = true)
    private Order order;

    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private Double taxAmount;

    @Column(nullable = false)
    private Double discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    public Bill(Order order) {
        this.order = order;
        this.totalAmount = order.getTotalAmount();
        this.taxAmount = order.getTaxAmount();
        this.discountAmount = order.getDiscountAmount();
    }
}
