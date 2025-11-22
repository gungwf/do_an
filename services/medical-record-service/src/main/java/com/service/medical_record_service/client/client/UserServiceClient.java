package com.service.medical_record_service.client.client;

import com.service.medical_record_service.client.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;

@FeignClient(name = "sys-srv")
public interface UserServiceClient {
    @GetMapping("/users/by-email")
    UserDto getUserByEmail(@RequestParam("email") String email);

    @GetMapping("/users/search-by-name")
    List<java.util.UUID> searchUserIdsByNameAndRole(@RequestParam("name") String name,
                                                   @RequestParam("role") String role);

    @GetMapping("/users/basic-info")
    Map<java.util.UUID, String> getPatientNames(@RequestParam("ids") List<java.util.UUID> ids);
}