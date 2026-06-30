package com.yumai.controller;

import com.yumai.dto.DashboardDtos.DashboardData;
import com.yumai.dto.DashboardDtos.Kpis;
import com.yumai.dto.DashboardDtos.PeakHour;
import com.yumai.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** FR-04 / UC-10 - dashboard KPIs and analytics (all authenticated roles). */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardData dashboard() {
        return dashboardService.dashboard();
    }

    @GetMapping("/kpis")
    public Kpis kpis() {
        return dashboardService.kpis();
    }

    @GetMapping("/peak-hours")
    public List<PeakHour> peakHours() {
        return dashboardService.peakHours();
    }
}
