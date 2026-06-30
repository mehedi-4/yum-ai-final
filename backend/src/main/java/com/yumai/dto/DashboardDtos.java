package com.yumai.dto;

import java.util.List;

/** Response records for the analytics dashboard (FR-04). */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    /** FR-04.1 - real-time KPIs. */
    public record Kpis(long totalOrdersToday, double revenueToday, long activeAlerts,
                       long pendingOrders, double revenueThisMonth, long totalOrders) {
    }

    /** FR-04.3 - orders aggregated per hour of day. */
    public record PeakHour(int hour, long orders) {
    }

    public record TopItem(String name, long quantitySold, double revenue) {
    }

    public record DashboardData(Kpis kpis, List<PeakHour> peakHours, List<TopItem> topItems) {
    }
}
