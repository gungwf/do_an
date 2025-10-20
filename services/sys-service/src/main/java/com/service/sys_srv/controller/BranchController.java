package com.service.sys_srv.controller;

import com.service.sys_srv.dto.response.BranchSimpleDto;
import com.service.sys_srv.entity.Branch;
import com.service.sys_srv.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<Branch> createBranch(@RequestBody Branch branch) {
        return ResponseEntity.ok(branchService.createBranch(branch));
    }

    @GetMapping
    public ResponseEntity<List<Branch>> getAllBranches() {
        return ResponseEntity.ok(branchService.getAllBranches());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Branch> getBranchById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(branchService.getBranchById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/simple")
    public ResponseEntity<List<BranchSimpleDto>> getAllBranchesSimple() {
        return ResponseEntity.ok(branchService.getAllBranchesSimple());
    }
}