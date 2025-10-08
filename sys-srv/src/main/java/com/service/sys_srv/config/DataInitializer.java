package com.service.sys_srv.config;

import com.service.sys_srv.entity.Enum.UserRole;
import com.service.sys_srv.entity.User;
import com.service.sys_srv.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin.com";

        // Chỉ tạo nếu tài khoản admin với email này chưa tồn tại
        if (userRepository.findByEmail(adminEmail).isEmpty()) {

            System.out.println("--- Không tìm thấy tài khoản ADMIN, đang tiến hành tạo... ---");

            User admin = new User();
            admin.setFullName("Super Admin");
            admin.setEmail(adminEmail);
            // Sử dụng chính PasswordEncoder của hệ thống để mã hóa mật khẩu
            admin.setPasswordHash(passwordEncoder.encode("sa123456"));
            admin.setRole(UserRole.ADMIN);
            admin.setActive(true);

            userRepository.save(admin);
            System.out.println("--- Đã tạo tài khoản ADMIN mặc định thành công ---");
        }
    }
}