package com.service.medical_record_service.controller;

import com.service.medical_record_service.dto.request.BillLineRequest;
import com.service.medical_record_service.dto.request.CreateBillRequest;
import com.service.medical_record_service.dto.response.StockShortage;
import com.service.medical_record_service.entity.Bill;
import com.service.medical_record_service.entity.BillLine;
import com.service.medical_record_service.entity.Enum.BillType;
import com.service.medical_record_service.repository.BillLineRepository;
import com.service.medical_record_service.repository.BillRepository;
import com.service.medical_record_service.client.client.ProductInventoryClient;
import com.service.medical_record_service.client.dto.DeductStockRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillRepository billRepository;

    private final BillLineRepository billLineRepository;

    private final ProductInventoryClient productInventoryClient;

    @Value("${app.branch.central-warehouse-id}")
    private String centralWarehouseBranchId;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBill(@Valid @RequestBody CreateBillRequest request) {
        Bill bill = new Bill();
        bill.setPatientId(request.patientId());
        bill.setCreatorId(request.creatorId());
        bill.setBranchId(request.branchId());
        bill.setBillType(BillType.valueOf(request.billType()));
        bill.setTotalAmount(request.totalAmount());
        bill = billRepository.save(bill);

        if (request.items() != null && !request.items().isEmpty()) {
            for (BillLineRequest li : request.items()) {
                BillLine bl = new BillLine();
                bl.setBill(bill);
                bl.setProductId(li.productId());
                bl.setQuantity(li.quantity());
                bl.setUnitPrice(li.unitPrice());
                bl.setLineAmount(li.unitPrice().multiply(BigDecimal.valueOf(li.quantity())));
                billLineRepository.save(bl);
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("billId", bill.getId().toString());
        resp.put("status", bill.getStatus().toString());
        resp.put("totalAmount", bill.getTotalAmount());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/prod")
    public ResponseEntity<Map<String, Object>> createBillProducts(@Valid @RequestBody CreateBillRequest request) {
        Bill bill = new Bill();
        bill.setPatientId(request.patientId());
        bill.setCreatorId(request.creatorId());
        bill.setBranchId(request.branchId());
        bill.setBillType(BillType.valueOf(request.billType()));

        // Compute totalAmount server-side from items (fetch unit price from product service)
        BigDecimal total = BigDecimal.ZERO;
        Map<UUID, BigDecimal> priceMap = new HashMap<>();
        if (request.items() != null && !request.items().isEmpty()) {
            for (BillLineRequest li : request.items()) {
                try {
                    var prod = productInventoryClient.getProductById(li.productId());
                    BigDecimal unitPrice = prod.price();
                    priceMap.put(li.productId(), unitPrice);
                    total = total.add(unitPrice.multiply(BigDecimal.valueOf(li.quantity())));
                } catch (Exception ex) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Product not found: " + li.productId()));
                }
            }
        }

        bill.setTotalAmount(total);
        bill = billRepository.save(bill);

        if (request.items() != null && !request.items().isEmpty()) {
            for (BillLineRequest li : request.items()) {
                BigDecimal unitPrice = priceMap.getOrDefault(li.productId(), BigDecimal.ZERO);
                BillLine bl = new BillLine();
                bl.setBill(bill);
                bl.setProductId(li.productId());
                bl.setQuantity(li.quantity());
                bl.setUnitPrice(unitPrice);
                bl.setLineAmount(unitPrice.multiply(BigDecimal.valueOf(li.quantity())));
                billLineRepository.save(bl);
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("billId", bill.getId().toString());
        resp.put("status", bill.getStatus().toString());
        resp.put("totalAmount", bill.getTotalAmount());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/staff-purchase")
    public ResponseEntity<Map<String, Object>> staffPurchase(@RequestHeader(value = "X-Staff-Id", required = false) UUID staffId,
                                                             @RequestHeader(value = "X-Staff-Branch-Id", required = false) UUID staffBranchId,
                                                             @Valid @RequestBody CreateBillRequest request) {
        // Determine branch: prefer staffBranchId from header (extracted from token upstream)
        if (staffBranchId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing staff branch id"));
        }
        // Compute total and pre-check stock using product service (server-side pricing)
        BigDecimal total = BigDecimal.ZERO;
        Map<UUID, BigDecimal> priceMap = new HashMap<>();
        List<StockShortage> shortages = new ArrayList<>();
        if (request.items() != null && !request.items().isEmpty()) {
            for (BillLineRequest li : request.items()) {
                try {
                    var prod = productInventoryClient.getProductById(li.productId());
                    BigDecimal unitPrice = prod.price();
                    priceMap.put(li.productId(), unitPrice);
                    total = total.add(unitPrice.multiply(BigDecimal.valueOf(li.quantity())));

                    var inv = productInventoryClient.getInventoryByBranchAndProduct(staffBranchId, li.productId());
                    int available = (inv == null || inv.quantity() == null) ? 0 : inv.quantity();
                    if (available < li.quantity()) {
                        shortages.add(new StockShortage(li.productId(), li.quantity(), available));
                    }
                } catch (Exception e) {
                    shortages.add(new StockShortage(li.productId(), li.quantity(), 0));
                }
            }
        }

        if (!shortages.isEmpty()) {
            return ResponseEntity.status(409).body(Map.of("shortages", shortages));
        }

        // Build and save bill with computed total
        Bill bill = new Bill();
        bill.setPatientId(request.patientId());
        bill.setCreatorId(staffId);
        bill.setBranchId(staffBranchId);
        bill.setBillType(BillType.valueOf(request.billType()));
        bill.setTotalAmount(total);
        bill = billRepository.save(bill);

        // persist bill lines with unit prices from product service
        if (request.items() != null && !request.items().isEmpty()) {
            for (BillLineRequest li : request.items()) {
                BigDecimal unitPrice = priceMap.getOrDefault(li.productId(), BigDecimal.ZERO);
                BillLine bl = new BillLine();
                bl.setBill(bill);
                bl.setProductId(li.productId());
                bl.setQuantity(li.quantity());
                bl.setUnitPrice(unitPrice);
                bl.setLineAmount(unitPrice.multiply(BigDecimal.valueOf(li.quantity())));
                billLineRepository.save(bl);
            }
        }

        return ResponseEntity.ok(Map.of("billId", bill.getId().toString(), "totalAmount", bill.getTotalAmount()));
    }

    @PostMapping("/online-purchase")
    public ResponseEntity<Map<String, Object>> onlinePurchase(@Valid @RequestBody CreateBillRequest request) {
        UUID centralBranchId = UUID.fromString(centralWarehouseBranchId);

        if (centralBranchId == null) {
            return ResponseEntity.status(500).body(
                Map.of("error", "Central warehouse branch id not configured"));
        }
        // Compute total and pre-check stock at central branch using product service
        BigDecimal total = BigDecimal.ZERO;
        Map<UUID, BigDecimal> priceMap = new HashMap<>();
        List<StockShortage> shortages = new ArrayList<>();
        if (request.items() != null && !request.items().isEmpty()) {
            for (BillLineRequest li : request.items()) {
                try {
                    var prod = productInventoryClient.getProductById(li.productId());
                    BigDecimal unitPrice = prod.price();
                    priceMap.put(li.productId(), unitPrice);
                    total = total.add(unitPrice.multiply(BigDecimal.valueOf(li.quantity())));

                    var inv = productInventoryClient.getInventoryByBranchAndProduct(centralBranchId, li.productId());
                    int available = (inv == null || inv.quantity() == null) ? 0 : inv.quantity();
                    if (available < li.quantity()) {
                        shortages.add(new StockShortage(li.productId(), li.quantity(), available));
                    }
                } catch (Exception e) {
                    shortages.add(new StockShortage(li.productId(), li.quantity(), 0));
                }
            }
        }

        if (!shortages.isEmpty()) {
            return ResponseEntity.status(409).body(Map.of("shortages", shortages));
        }

        // Build and save bill with computed total
        Bill bill = new Bill();
        bill.setPatientId(request.patientId());
        bill.setCreatorId(request.creatorId());
        bill.setBranchId(centralBranchId);
        bill.setBillType(BillType.valueOf(request.billType()));
        bill.setTotalAmount(total);
        bill = billRepository.save(bill);

        // persist bill lines with computed unit prices
        if (request.items() != null && !request.items().isEmpty()) {
            for (BillLineRequest li : request.items()) {
                BigDecimal unitPrice = priceMap.getOrDefault(li.productId(), BigDecimal.ZERO);
                BillLine bl = new BillLine();
                bl.setBill(bill);
                bl.setProductId(li.productId());
                bl.setQuantity(li.quantity());
                bl.setUnitPrice(unitPrice);
                bl.setLineAmount(unitPrice.multiply(BigDecimal.valueOf(li.quantity())));
                billLineRepository.save(bl);
            }
        }

        return ResponseEntity.ok(Map.of("billId", bill.getId().toString(), "totalAmount", bill.getTotalAmount()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBill(@PathVariable("id") UUID id) {
        var opt = billRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Bill bill = opt.get();
        var lines = billLineRepository.findByBillId(bill.getId());
        java.util.List<Map<String,Object>> items = new ArrayList<>();
        for (BillLine l : lines) {
            items.add(Map.of(
                    "productId", l.getProductId(),
                    "quantity", l.getQuantity(),
                    "unitPrice", l.getUnitPrice(),
                    "lineAmount", l.getLineAmount()
            ));
        }
        return ResponseEntity.ok(Map.of(
                "billId", bill.getId().toString(),
                "status", bill.getStatus().toString(),
                "totalAmount", bill.getTotalAmount(),
                "branchId", bill.getBranchId(),
                "items", items
        ));
    }

    @GetMapping("/{id}/check-stock")
    public ResponseEntity<List<StockShortage>> checkStockForBill(@PathVariable("id") UUID id) {
        var opt = billRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Bill bill = opt.get();
        var lines = billLineRepository.findByBillId(bill.getId());
        List<StockShortage> shortages = new ArrayList<>();
        for (BillLine l : lines) {
            try {
                var inv = productInventoryClient.getInventoryByBranchAndProduct(bill.getBranchId(), l.getProductId());
                int available = (inv == null || inv.quantity() == null) ? 0 : inv.quantity();
                if (available < l.getQuantity()) {
                    shortages.add(new StockShortage(l.getProductId(), l.getQuantity(), available));
                }
            } catch (Exception ex) {
                shortages.add(new StockShortage(l.getProductId(), l.getQuantity(), 0));
            }
        }
        return ResponseEntity.ok(shortages);
    }

    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<Map<String, Object>> markBillPaid(@PathVariable("id") UUID id) {
        var billOpt = billRepository.findById(id);
        if (billOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Bill bill = billOpt.get();
        bill.setStatus(com.service.medical_record_service.entity.Enum.BillStatus.PAID);
        bill.setPaidAt(java.time.Instant.now());
        billRepository.save(bill);

        // Deduct stock for bill lines
        var lines = billLineRepository.findByBillId(bill.getId());
        for (BillLine line : lines) {
            try {
                DeductStockRequest req = new DeductStockRequest();
                req.setBranchId(bill.getBranchId());
                req.setProductId(line.getProductId());
                req.setQuantityToDeduct(line.getQuantity());
                productInventoryClient.deductStock(req);
            } catch (Exception ex) {
                // If any deduct fails, mark special status and log
                bill.setStatus(com.service.medical_record_service.entity.Enum.BillStatus.PAID_BUT_DEDUCT_FAILED);
                billRepository.save(bill);
                java.util.Map<String,Object> r = new HashMap<>();
                r.put("status","PARTIAL_FAILURE");
                r.put("message","Paid but deduct failed: " + ex.getMessage());
                return ResponseEntity.status(500).body(r);
            }
        }

        java.util.Map<String,Object> r = new HashMap<>();
        r.put("status","PAID");
        r.put("billId", bill.getId().toString());
        return ResponseEntity.ok(r);
    }
}
