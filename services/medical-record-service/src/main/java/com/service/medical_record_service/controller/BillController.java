package com.service.medical_record_service.controller;

import com.service.medical_record_service.dto.request.BillLineRequest;
import com.service.medical_record_service.dto.request.CreateBillRequest;
import com.service.medical_record_service.dto.request.BillSearchRequest;
import com.service.medical_record_service.dto.response.StockShortage;
import com.service.medical_record_service.dto.response.BillSearchItemDto;
import com.service.medical_record_service.entity.Bill;
import com.service.medical_record_service.entity.BillLine;
import com.service.medical_record_service.entity.Enum.BillType;
import com.service.medical_record_service.entity.Enum.BillStatus;
import com.service.medical_record_service.repository.BillLineRepository;
import com.service.medical_record_service.repository.BillRepository;
import com.service.medical_record_service.repository.spec.BillSpecifications;
import com.service.medical_record_service.client.client.UserServiceClient;
import com.service.medical_record_service.client.client.ProductInventoryClient;
import com.service.medical_record_service.client.dto.DeductStockRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;
import org.springframework.lang.NonNull;

@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillRepository billRepository;

    private final BillLineRepository billLineRepository;

    private final ProductInventoryClient productInventoryClient;
    private final UserServiceClient userServiceClient;

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
        bill.setRecipientName(request.recipientName());
        bill.setRecipientPhone(request.recipientPhone());
        bill.setRecipientAddress(request.recipientAddress());
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
        bill.setRecipientName(request.recipientName());
        bill.setRecipientPhone(request.recipientPhone());
        bill.setRecipientAddress(request.recipientAddress());

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

                    var apiInv = productInventoryClient.getInventoryByBranchAndProduct(staffBranchId, li.productId());
                    var inv = apiInv == null ? null : apiInv.result();
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
        bill.setBillType(BillType.DRUG_PAYMENT);
        bill.setTotalAmount(total);
        bill.setRecipientName(request.recipientName());
        bill.setRecipientPhone(request.recipientPhone());
        bill.setRecipientAddress(request.recipientAddress());
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

                    var apiInv = productInventoryClient.getInventoryByBranchAndProduct(centralBranchId, li.productId());
                    var inv = apiInv == null ? null : apiInv.result();
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
        bill.setBillType(BillType.DRUG_PAYMENT);
        bill.setTotalAmount(total);
        bill.setRecipientName(request.recipientName());
        bill.setRecipientPhone(request.recipientPhone());
        bill.setRecipientAddress(request.recipientAddress());
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
    public ResponseEntity<Map<String, Object>> getBill(@PathVariable("id") @NonNull UUID id) {
        Optional<Bill> opt = billRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Bill bill = opt.get();
        var lines = billLineRepository.findByBillId(bill.getId());
        List<Map<String,Object>> items = new ArrayList<>();
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
            "recipientName", bill.getRecipientName(),
            "recipientPhone", bill.getRecipientPhone(),
            "recipientAddress", bill.getRecipientAddress(),
                "items", items
        ));
    }

    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchBills(@RequestBody BillSearchRequest request) {
        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 20 : request.size();
        Instant fromPaidAt = request.fromPaidAt() == null ? null : Instant.ofEpochMilli(request.fromPaidAt());
        Instant toPaidAt = request.toPaidAt() == null ? null : Instant.ofEpochMilli(request.toPaidAt());

        Collection<UUID> patientIdsFilter = null;
        String patientName = request.patientName();
        if (patientName != null && !patientName.isBlank()) {
            try {
                List<UUID> ids = userServiceClient.searchUserIdsByNameAndRole(patientName, "PATIENT");
                if (ids == null || ids.isEmpty()) {
                    return ResponseEntity.ok(emptyPageResponse(page, size));
                }
                patientIdsFilter = ids;
            } catch (Exception ex) {
                return ResponseEntity.ok(emptyPageResponse(page, size));
            }
        }

        BillType bt = null;
        if (request.billType() != null && !request.billType().isBlank()) {
            try { bt = BillType.valueOf(request.billType()); } catch (Exception ignored) {}
        }
        BillStatus st = null;
        if (request.status() != null && !request.status().isBlank()) {
            try { st = BillStatus.valueOf(request.status()); } catch (Exception ignored) {}
        }

        UUID branchId = request.branchId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Bill> spec = BillSpecifications.build(bt, st, branchId, fromPaidAt, toPaidAt, patientIdsFilter);
        Page<Bill> pageResult = billRepository.findAll(spec, pageable);

        List<BillSearchItemDto> content = new ArrayList<>();
        Set<UUID> patientIdsInPage = new HashSet<>();
        for (Bill b : pageResult.getContent()) {
            if (b.getPatientId() != null) patientIdsInPage.add(b.getPatientId());
        }
        Map<UUID,String> nameMap = Collections.emptyMap();
        if (!patientIdsInPage.isEmpty()) {
            try {
                nameMap = userServiceClient.getPatientNames(new ArrayList<>(patientIdsInPage));
            } catch (Exception ignored) {}
        }
        for (Bill b : pageResult.getContent()) {
            content.add(new BillSearchItemDto(
                    b.getId(),
                    b.getBillType(),
                    b.getStatus(),
                    b.getBranchId(),
                    b.getPatientId(),
                    b.getPatientId() == null ? null : nameMap.getOrDefault(b.getPatientId(), null),
                    b.getTotalAmount(),
                    b.getPaidAt(),
                    b.getCreatedAt()
            ));
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("page", pageResult.getNumber());
        resp.put("size", pageResult.getSize());
        resp.put("totalElements", pageResult.getTotalElements());
        resp.put("totalPages", pageResult.getTotalPages());
        resp.put("content", content);
        return ResponseEntity.ok(resp);
    }

    private Map<String,Object> emptyPageResponse(int page, int size) {
        Map<String,Object> m = new HashMap<>();
        m.put("content", Collections.emptyList());
        m.put("page", page);
        m.put("size", size);
        m.put("totalElements", 0);
        m.put("totalPages", 0);
        return m;
    }

    @GetMapping("/{id}/check-stock")
    public ResponseEntity<List<StockShortage>> checkStockForBill(@PathVariable("id") @NonNull UUID id) {
        Optional<Bill> opt = billRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Bill bill = opt.get();
        var lines = billLineRepository.findByBillId(bill.getId());
        List<StockShortage> shortages = new ArrayList<>();
        for (BillLine l : lines) {
            try {
                var apiInv = productInventoryClient.getInventoryByBranchAndProduct(bill.getBranchId(), l.getProductId());
                var inv = apiInv == null ? null : apiInv.result();
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
    public ResponseEntity<Map<String, Object>> markBillPaid(@PathVariable("id") @NonNull UUID id) {
        Optional<Bill> billOpt = billRepository.findById(id);
        if (billOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Bill bill = billOpt.get();
        bill.setStatus(BillStatus.PAID);
        bill.setPaidAt(Instant.now());
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
                bill.setStatus(BillStatus.PAID_BUT_DEDUCT_FAILED);
                billRepository.save(bill);
                Map<String,Object> r = new HashMap<>();
                r.put("status","PARTIAL_FAILURE");
                r.put("message","Paid but deduct failed: " + ex.getMessage());
                return ResponseEntity.status(500).body(r);
            }
        }

        Map<String,Object> r = new HashMap<>();
        r.put("status","PAID");
        r.put("billId", bill.getId().toString());
        return ResponseEntity.ok(r);
    }
}
