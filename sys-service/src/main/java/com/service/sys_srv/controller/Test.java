package com.service.sys_srv.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth") // Tiền tố chung cho các API của service này
public class Test {
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello from Auth & User Service!";
    }
}
