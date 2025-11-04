package com.service.sys_srv.service;

import com.service.sys_srv.dto.request.DoctorSearchRequest;
import com.service.sys_srv.entity.DoctorProfile;
import com.service.sys_srv.entity.Enum.UserRole;
import com.service.sys_srv.entity.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class DoctorSpecification {

    public static Specification<User> filterDoctors(DoctorSearchRequest request) {

        Specification<User> spec = (root, query, cb) ->
                cb.equal(root.get("role"), UserRole.doctor);

        if (StringUtils.hasText(request.getFullName())) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("fullName")), "%" + request.getFullName().toLowerCase() + "%")
            );
        }

        if (request.getBranchId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("branchId"), request.getBranchId())
            );
        }

        if (StringUtils.hasText(request.getSpecialty())) {
            spec = spec.and((root, query, cb) -> {
                Join<User, DoctorProfile> profileJoin = root.join("doctorProfile");
                return cb.like(cb.lower(profileJoin.get("specialty")), "%" + request.getSpecialty().toLowerCase() + "%");
            });
        }

        return spec;
    }
}