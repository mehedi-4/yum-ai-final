package com.yumai.controller;

import com.yumai.entity.Bill;
import com.yumai.service.OrderService;
import com.yumai.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** FR-02.4 / FR-02.5 / UC-06 - billing history and printable invoices. */
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final OrderService orderService;
    private final PdfService pdfService;

    @GetMapping
    public List<Bill> history() {
        return orderService.billingHistory();
    }

    @GetMapping("/{id}")
    public Bill findById(@PathVariable Long id) {
        return orderService.findBill(id);
    }

    @PatchMapping("/{id}/pay")
    public Bill markPaid(@PathVariable Long id) {
        return orderService.markBillPaid(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        Bill bill = orderService.findBill(id);
        byte[] pdf = pdfService.billPdf(bill);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + id + ".pdf")
                .body(pdf);
    }
}
