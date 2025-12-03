package com.service.sys_srv.dto.response;

import com.service.sys_srv.entity.DoctorProfile;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyDoctorProfileResponse {
    private UserDto user;
    private DoctorProfile profile;
}
