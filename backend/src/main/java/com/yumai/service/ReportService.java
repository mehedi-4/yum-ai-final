package com.yumai.service;

import com.yumai.entity.*;
import com.yumai.entity.Report.ReportFormat;
import com.yumai.entity.Report.ReportType;
import com.yumai.exception.BadRequestException;
import com.yumai.repository.InventoryItemRepository;
import com.yumai.repository.OrderRepository;
import com.yumai.repository.ReportRepository;
import com.yumai.repository.WasteLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** PDF/CSV report generation with date/category/item filters (FR-06). */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final WasteLogRepository wasteLogRepository;
    private final ReportRepository reportRepository;
    private final PdfService pdfService;

    public record ReportFile(byte[] bytes, String filename, String contentType) {
    }

    public List<Report> history() {
        return reportRepository.findAllByOrderByGeneratedAtDesc();
    }

    /** FR-06.1/06.2/06.3 - generate a filtered report and record its metadata. */
    @Transactional
    public ReportFile generate(ReportType type, ReportFormat format, LocalDate from, LocalDate to,
                               String category, Long menuItemId, User generatedBy) {
        if (from.isAfter(to)) {
            throw new BadRequestException("Start date must not be after end date");
        }
        List<String> headers;
        List<List<String>> rows;
        String title;
        switch (type) {
            case SALES -> {
                title = "Sales Report";
                headers = List.of("Menu Item", "Category", "Quantity Sold", "Revenue");
                rows = salesRows(from, to, category, menuItemId);
            }
            case INVENTORY -> {
                title = "Inventory Report";
                headers = List.of("Item", "Category", "Quantity", "Unit", "Low-Stock Threshold", "Status", "Wasted in Period");
                rows = inventoryRows(from, to, category);
            }
            case PROFIT_LOSS -> {
                title = "Profit/Loss Report";
                headers = List.of("Menu Item", "Category", "Quantity Sold", "Revenue", "Cost", "Profit");
                rows = profitLossRows(from, to, category, menuItemId);
            }
            default -> throw new BadRequestException("Unknown report type");
        }
        reportRepository.save(new Report(type, format, from, to, generatedBy));

        String subtitle = "Period: " + from + " to " + to
                + (category != null && !category.isBlank() ? "  |  Category: " + category : "");
        String base = type.name().toLowerCase() + "-report_" + from + "_" + to;
        if (format == ReportFormat.CSV) {
            return new ReportFile(csv(headers, rows), base + ".csv", "text/csv");
        }
        return new ReportFile(pdfService.tablePdf(title, subtitle, headers, rows), base + ".pdf", "application/pdf");
    }

    private List<Order> completedOrders(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        return orderRepository.findByStatusAndCreatedAtBetween(OrderStatus.COMPLETED, start, end);
    }

    private List<List<String>> salesRows(LocalDate from, LocalDate to, String category, Long menuItemId) {
        Map<String, double[]> agg = aggregateItems(from, to, category, menuItemId);
        List<List<String>> rows = new ArrayList<>();
        double totalQty = 0;
        double totalRevenue = 0;
        for (Map.Entry<String, double[]> e : sortedByRevenue(agg)) {
            String[] key = e.getKey().split("\\|", 2);
            rows.add(List.of(key[0], key[1], num(e.getValue()[0]), money(e.getValue()[1])));
            totalQty += e.getValue()[0];
            totalRevenue += e.getValue()[1];
        }
        rows.add(List.of("TOTAL", "", num(totalQty), money(totalRevenue)));
        return rows;
    }

    private List<List<String>> profitLossRows(LocalDate from, LocalDate to, String category, Long menuItemId) {
        // key -> [qty, revenue, cost]
        Map<String, double[]> agg = new LinkedHashMap<>();
        for (Order order : completedOrders(from, to)) {
            for (OrderItem item : order.getItems()) {
                MenuItem mi = item.getMenuItem();
                if (skip(mi, category, menuItemId)) {
                    continue;
                }
                double[] acc = agg.computeIfAbsent(mi.getName() + "|" + mi.getCategory(), k -> new double[3]);
                acc[0] += item.getQuantity();
                acc[1] += item.getSubtotal();
                acc[2] += mi.getCostPrice() * item.getQuantity();
            }
        }
        List<List<String>> rows = new ArrayList<>();
        double[] totals = new double[3];
        for (Map.Entry<String, double[]> e : sortedByRevenue(agg)) {
            String[] key = e.getKey().split("\\|", 2);
            double[] v = e.getValue();
            rows.add(List.of(key[0], key[1], num(v[0]), money(v[1]), money(v[2]), money(v[1] - v[2])));
            totals[0] += v[0];
            totals[1] += v[1];
            totals[2] += v[2];
        }
        rows.add(List.of("TOTAL", "", num(totals[0]), money(totals[1]), money(totals[2]),
                money(totals[1] - totals[2])));
        return rows;
    }

    private List<List<String>> inventoryRows(LocalDate from, LocalDate to, String category) {
        Map<Long, Double> wasted = new HashMap<>();
        for (WasteLog log : wasteLogRepository.findByLoggedAtBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay())) {
            wasted.merge(log.getInventoryItem().getItemId(), log.getQuantity(), Double::sum);
        }
        List<List<String>> rows = new ArrayList<>();
        for (InventoryItem item : inventoryItemRepository.findAll()) {
            if (category != null && !category.isBlank() && !item.getCategory().equalsIgnoreCase(category)) {
                continue;
            }
            rows.add(List.of(item.getName(), item.getCategory(), num(item.getQuantity()), item.getUnit(),
                    num(item.getLowStockThreshold()), item.isLowStock() ? "LOW STOCK" : "OK",
                    num(wasted.getOrDefault(item.getItemId(), 0.0))));
        }
        return rows;
    }

    /** key "name|category" -> [qty, revenue] for completed orders in range. */
    private Map<String, double[]> aggregateItems(LocalDate from, LocalDate to, String category, Long menuItemId) {
        Map<String, double[]> agg = new LinkedHashMap<>();
        for (Order order : completedOrders(from, to)) {
            for (OrderItem item : order.getItems()) {
                MenuItem mi = item.getMenuItem();
                if (skip(mi, category, menuItemId)) {
                    continue;
                }
                double[] acc = agg.computeIfAbsent(mi.getName() + "|" + mi.getCategory(), k -> new double[2]);
                acc[0] += item.getQuantity();
                acc[1] += item.getSubtotal();
            }
        }
        return agg;
    }

    private boolean skip(MenuItem item, String category, Long menuItemId) {
        if (menuItemId != null && !item.getMenuItemId().equals(menuItemId)) {
            return true;
        }
        return category != null && !category.isBlank() && !item.getCategory().equalsIgnoreCase(category);
    }

    private List<Map.Entry<String, double[]>> sortedByRevenue(Map<String, double[]> agg) {
        return agg.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[1], a.getValue()[1]))
                .toList();
    }

    private byte[] csv(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", headers.stream().map(this::escape).toList())).append("\r\n");
        for (List<String> row : rows) {
            sb.append(String.join(",", row.stream().map(this::escape).toList())).append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    private static String money(double value) {
        return String.format("%.2f", value);
    }

    private static String num(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.format("%.2f", value);
    }
}
