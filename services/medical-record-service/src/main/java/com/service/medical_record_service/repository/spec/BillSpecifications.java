package com.service.medical_record_service.repository.spec;

import com.service.medical_record_service.entity.Bill;
import com.service.medical_record_service.entity.Enum.BillStatus;
import com.service.medical_record_service.entity.Enum.BillType;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public class BillSpecifications {
    public static Specification<Bill> billTypeEquals(BillType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("billType"), type);
    }

    public static Specification<Bill> statusEquals(BillStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Bill> branchEquals(UUID branchId) {
        return (root, query, cb) -> branchId == null ? null : cb.equal(root.get("branchId"), branchId);
    }

    public static Specification<Bill> paidAtBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) return cb.between(root.get("paidAt"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("paidAt"), from);
            return cb.lessThanOrEqualTo(root.get("paidAt"), to);
        };
    }

    public static Specification<Bill> patientIdIn(Collection<UUID> ids) {
        return (root, query, cb) -> {
            if (ids == null || ids.isEmpty()) return cb.disjunction(); // will produce false -> empty result
            return root.get("patientId").in(ids);
        };
    }

    public static Specification<Bill> build(BillType type,
                                            BillStatus status,
                                            UUID branchId,
                                            Instant from,
                                            Instant to,
                                            Collection<UUID> patientIds) {
        Specification<Bill> spec = Specification.where(null);
        spec = spec.and(billTypeEquals(type));
        spec = spec.and(statusEquals(status));
        spec = spec.and(branchEquals(branchId));
        spec = spec.and(paidAtBetween(from, to));
        if (patientIds != null) {
            spec = spec.and(patientIdIn(patientIds));
        }
        return spec;
    }
}
