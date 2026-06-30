package com.yumai.service;

import com.yumai.dto.AiDtos.ChatResponse;
import com.yumai.dto.DashboardDtos.PeakHour;
import com.yumai.dto.DashboardDtos.TopItem;
import com.yumai.entity.InventoryItem;
import com.yumai.entity.WasteLog;
import com.yumai.repository.InventoryItemRepository;
import com.yumai.repository.WasteLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI chat assistant (FR-05.4/05.5, UC-14).
 * Prompts are grounded in live business data and sent to Gemini; when the API
 * is unavailable a deterministic statistical engine produces the reply instead,
 * so the chat endpoint remains functional offline (DEVIATIONS.md D5).
 */
@Service
@RequiredArgsConstructor
public class AiService {

    private final GeminiClient geminiClient;
    private final InventoryItemRepository inventoryItemRepository;
    private final WasteLogRepository wasteLogRepository;
    private final DashboardService dashboardService;

    /** FR-05.4/05.5 - natural-language Q&A grounded in business data. */
    public ChatResponse chat(String message) {
        String prompt = """
                You are YumAI, the AI assistant of a restaurant management system. \
                Answer the user's question concisely using the business data below. \
                If the question is unrelated to the restaurant, politely steer back.

                Business data:
                %s

                Question: %s""".formatted(businessContext(), message);
        return geminiClient.generate(prompt)
                .map(reply -> new ChatResponse(reply, true))
                .orElseGet(() -> new ChatResponse(fallbackChat(message), false));
    }

    /** Compact data summary injected into every prompt. */
    private String businessContext() {
        var kpis = dashboardService.kpis();
        List<TopItem> top = dashboardService.topItems();
        List<PeakHour> peaks = dashboardService.peakHours();
        List<InventoryItem> lowStock = inventoryItemRepository.findLowStock();
        Map<String, Double> wasteByItem = wasteLogRepository
                .findByLoggedAtBetween(LocalDateTime.now().minusDays(30), LocalDateTime.now())
                .stream()
                .collect(Collectors.groupingBy(w -> w.getInventoryItem().getName(),
                        Collectors.summingDouble(WasteLog::getQuantity)));

        StringBuilder sb = new StringBuilder();
        sb.append("- Orders today: ").append(kpis.totalOrdersToday())
                .append(", revenue today: ").append(kpis.revenueToday())
                .append(", revenue this month: ").append(kpis.revenueThisMonth())
                .append(", total orders all time: ").append(kpis.totalOrders()).append('\n');
        sb.append("- Top sellers (30d): ").append(top.isEmpty() ? "none yet"
                : top.stream().map(t -> t.name() + " x" + t.quantitySold()).collect(Collectors.joining(", ")))
                .append('\n');
        sb.append("- Peak hours (30d): ").append(peaks.isEmpty() ? "no data"
                : peaks.stream().sorted(Comparator.comparingLong(PeakHour::orders).reversed()).limit(3)
                        .map(p -> p.hour() + ":00 (" + p.orders() + " orders)")
                        .collect(Collectors.joining(", ")))
                .append('\n');
        sb.append("- Low-stock items: ").append(lowStock.isEmpty() ? "none"
                : lowStock.stream().map(i -> i.getName() + " (" + i.getQuantity() + " " + i.getUnit() + ")")
                        .collect(Collectors.joining(", ")))
                .append('\n');
        sb.append("- Waste last 30 days: ").append(wasteByItem.isEmpty() ? "none recorded"
                : wasteByItem.entrySet().stream().map(e -> e.getKey() + " " + e.getValue())
                        .collect(Collectors.joining(", ")));
        return sb.toString();
    }

    /** Keyword-routed offline chat answers (FR-05.5). */
    private String fallbackChat(String message) {
        String q = message.toLowerCase();
        if (q.contains("stock") || q.contains("inventory") || q.contains("ingredient")) {
            List<InventoryItem> low = inventoryItemRepository.findLowStock();
            return low.isEmpty()
                    ? "Inventory looks healthy - no items are below their low-stock threshold."
                    : "These items are low on stock: " + low.stream()
                            .map(i -> i.getName() + " (" + i.getQuantity() + " " + i.getUnit() + ")")
                            .collect(Collectors.joining(", ")) + ". Consider reordering soon.";
        }
        if (q.contains("waste")) {
            var waste = wasteLogRepository.findByLoggedAtBetween(LocalDateTime.now().minusDays(30), LocalDateTime.now());
            return waste.isEmpty() ? "No waste has been logged in the last 30 days."
                    : "In the last 30 days " + waste.size() + " waste entries were logged. "
                            + "Top wasted: " + waste.stream()
                                    .collect(Collectors.groupingBy(w -> w.getInventoryItem().getName(),
                                            Collectors.summingDouble(WasteLog::getQuantity)))
                                    .entrySet().stream()
                                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(3)
                                    .map(e -> e.getKey() + " (" + e.getValue() + ")")
                                    .collect(Collectors.joining(", ")) + ".";
        }
        if (q.contains("sale") || q.contains("revenue") || q.contains("profit") || q.contains("today")) {
            var kpis = dashboardService.kpis();
            return "Today: " + kpis.totalOrdersToday() + " orders, revenue " + kpis.revenueToday()
                    + ". This month's revenue is " + kpis.revenueThisMonth() + ".";
        }
        if (q.contains("best") || q.contains("top") || q.contains("popular") || q.contains("recommend")) {
            List<TopItem> top = dashboardService.topItems();
            return top.isEmpty() ? "No sales recorded yet, so there are no best sellers to report."
                    : "Top sellers (last 30 days): " + top.stream()
                            .map(t -> t.name() + " - " + t.quantitySold() + " sold")
                            .collect(Collectors.joining(", ")) + ".";
        }
        return "I can answer questions about sales, inventory status, waste and recommendations. "
                + "(Note: the Gemini API key is not configured, so I'm using built-in analytics only.)";
    }
}
