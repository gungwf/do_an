package com.service.medical_record_service.client.client;

import com.service.medical_record_service.client.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "sys-srv")
public interface UserServiceClient {
    @GetMapping("/users/by-email")
    UserDto getUserByEmail(@RequestParam("email") String email);
}