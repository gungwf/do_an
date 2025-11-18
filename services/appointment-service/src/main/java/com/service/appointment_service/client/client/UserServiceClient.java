package com.service.appointment_service.client.client;

import com.service.appointment_service.client.dto.*;
import com.service.appointment_service.dto.response.BranchDto;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@FeignClient(name = "sys-srv")
public interface UserServiceClient {
    @GetMapping("/users/{id}")
    UserDto getUserById(@PathVariable("id") UUID id);

    @GetMapping("/branches/{id}")
    BranchDto getBranchById(@PathVariable("id") UUID id);

    @GetMapping("/patient-profiles/user/{userId}")
    PatientProfileDto getPatientProfile(@PathVariable("userId") UUID userId);

    @GetMapping("/users/by-email")
    UserDto getUserByEmail(@RequestParam("email") String email);

    @PatchMapping("/patient-profiles/{userId}/add-points")
    void addPointsToPatient(@PathVariable("userId") UUID userId, @RequestBody AddPointsRequest request);

    @GetMapping("/users/search-by-name")
    List<UUID> searchUsersByNameAndRole(
            @RequestParam("name") String name,
            @RequestParam("role") String role
    );

    @GetMapping("users/basic-info")
    Map<UUID, String> getPatientNames(@RequestParam("ids") List<UUID> ids);
}