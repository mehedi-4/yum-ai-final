package com.yumai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** SRS 5.1.9 - metadata of a generated report (FR-06). */
@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
public class Report {

    public enum ReportType { SALES, INVENTORY, PROFIT_LOSS }

    public enum ReportFormat { PDF, CSV }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportFormat format;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "generated_by")
    private User generatedBy;

    public Report(ReportType type, ReportFormat format, LocalDate startDate, LocalDate endDate, User generatedBy) {
        this.type = type;
        this.format = format;
        this.startDate = startDate;
        this.endDate = endDate;
        this.generatedBy = generatedBy;
    }
}
