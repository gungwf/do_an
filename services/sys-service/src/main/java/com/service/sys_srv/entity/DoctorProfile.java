package com.service.sys_srv.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "doctor_profiles")
public class DoctorProfile {
    @Id
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    private String specialty;

    private String degree;
}
