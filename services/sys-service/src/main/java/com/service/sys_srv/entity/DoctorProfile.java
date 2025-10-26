package com.service.sys_srv.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.service.sys_srv.entity.Enum.Gender;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "doctor_profiles")
public class DoctorProfile {
    @Id
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // Đánh dấu rằng userId cũng là khóa ngoại và là 1 phần của @Id
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private String specialty; // Chuyên khoa

    private String degree; // Bằng cấp
}
