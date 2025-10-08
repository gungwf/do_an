package com.service.sys_srv.service;

import com.service.sys_srv.dto.response.BranchSimpleDto;
import com.service.sys_srv.entity.Branch;
import com.service.sys_srv.exception.AppException;
import com.service.sys_srv.exception.ERROR_CODE;
import com.service.sys_srv.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    public Branch createBranch(Branch branch) {
        return branchRepository.save(branch);
    }

    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public Branch getBranchById(UUID id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new AppException(ERROR_CODE.BRANCH_NOT_FOUND));
    }

    public List<BranchSimpleDto> getAllBranchesSimple() {
        return branchRepository.findAll()
                .stream()
                .map(branch -> new BranchSimpleDto(branch.getId(), branch.getBranchName()))
                .collect(Collectors.toList());
    }
}