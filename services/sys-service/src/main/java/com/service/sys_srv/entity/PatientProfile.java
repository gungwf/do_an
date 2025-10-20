package com.service.sys_srv.entity;

import com.service.sys_srv.entity.Enum.Gender;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "patient_profiles")
@Data
public class PatientProfile {
    @Id
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String address;
    private String allergies;
    private String contraindications;
    private String medicalHistory;

    @Column(name = "membership_tier")
    private String membershipTier = "STANDARD";

    @Column
    private Integer points = 0;
}