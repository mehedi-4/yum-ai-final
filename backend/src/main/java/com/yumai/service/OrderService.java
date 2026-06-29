package com.yumai.service;

import com.yumai.dto.OrderDtos.OrderItemRequest;
import com.yumai.dto.OrderDtos.OrderRequest;
import com.yumai.entity.*;
import com.yumai.exception.BadRequestException;
import com.yumai.exception.NotFoundException;
import com.yumai.repository.BillRepository;
import com.yumai.repository.MenuItemRepository;
import com.yumai.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Order lifecycle and billing (FR-02). */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final BillRepository billRepository;

    /** ER-03 - applicable tax rate, configurable per deployment. */
    @Value("${yumai.billing.tax-rate}")
    private double taxRate;

    public List<Order> findAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }

    /** FR-02.1 - create an order with one or more menu items. */
    @Transactional
    public Order create(OrderRequest request, User createdBy) {
        Order order = new Order();
        order.setTableNumber(request.tableNumber());
        order.setCreatedBy(createdBy);
        applyItems(order, request);
        return orderRepository.save(order);
    }

    /** FR-02.2 - update only while the order is still pending. */
    @Transactional
    public Order update(Long id, OrderRequest request) {
        Order order = findById(id);
        requirePending(order, "updated");
        order.setTableNumber(request.tableNumber());
        applyItems(order, request);
        return orderRepository.save(order);
    }

    /** FR-02.2 - cancel only while pending. */
    @Transactional
    public Order cancel(Long id) {
        Order order = findById(id);
        requirePending(order, "cancelled");
        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    /**
     * Complete an order: deduct ingredients from inventory (FR-02.6)
     * and generate its bill (FR-02.4).
     */
    @Transactional
    public Order complete(Long id) {
        Order order = findById(id);
        requirePending(order, "completed");
        for (OrderItem orderItem : order.getItems()) {
            for (MenuIngredient ingredient : orderItem.getMenuItem().getIngredients()) {
                InventoryItem inv = ingredient.getInventoryItem();
                double needed = ingredient.getQuantityNeeded() * orderItem.getQuantity();
                if (inv.getQuantity() < needed) {
                    throw new BadRequestException("Insufficient stock of " + inv.getName()
                            + " to complete this order (" + needed + " " + inv.getUnit() + " needed)");
                }
                inv.setQuantity(inv.getQuantity() - needed);
            }
        }
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
        billRepository.save(new Bill(order));
        return order;
    }

    /** FR-02.5 - complete billing history (Manager/Admin). */
    public List<Bill> billingHistory() {
        return billRepository.findAllByOrderByGeneratedAtDesc();
    }

    public Bill findBill(Long billId) {
        return billRepository.findById(billId)
                .orElseThrow(() -> new NotFoundException("Bill not found: " + billId));
    }

    @Transactional
    public Bill markBillPaid(Long billId) {
        Bill bill = findBill(billId);
        bill.setPaymentStatus(PaymentStatus.PAID);
        return billRepository.save(bill);
    }

    /** FR-02.3 - totals with tax and discount applied automatically. */
    private void applyItems(Order order, OrderRequest request) {
        order.getItems().clear();
        for (OrderItemRequest itemRequest : request.items()) {
            MenuItem menuItem = menuItemRepository.findById(itemRequest.menuItemId())
                    .orElseThrow(() -> new NotFoundException("Menu item not found: " + itemRequest.menuItemId()));
            if (!menuItem.getIsAvailable()) {
                throw new BadRequestException(menuItem.getName() + " is currently unavailable");
            }
            order.getItems().add(new OrderItem(order, menuItem, itemRequest.quantity()));
        }
        double subtotal = order.subtotal();
        double discountPercent = request.discountPercent() != null ? request.discountPercent() : 0.0;
        double discount = round2(subtotal * discountPercent / 100.0);
        double tax = round2((subtotal - discount) * taxRate);
        order.setDiscountAmount(discount);
        order.setTaxAmount(tax);
        order.setTotalAmount(round2(subtotal - discount + tax));
    }

    private void requirePending(Order order, String action) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Only pending orders can be " + action
                    + " (current status: " + order.getStatus() + ")");
        }
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
