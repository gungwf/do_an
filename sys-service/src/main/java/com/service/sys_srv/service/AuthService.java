package com.service.sys_srv.service;

import com.service.sys_srv.dto.request.LoginRequest;
import com.service.sys_srv.dto.request.RegisterRequest;
import com.service.sys_srv.dto.request.UpdateProfileRequest;
import com.service.sys_srv.dto.request.UpdateUserRequest;
import com.service.sys_srv.dto.response.UserDto;
import com.service.sys_srv.dto.response.UserSimpleDto;
import com.service.sys_srv.entity.Enum.Gender;
import com.service.sys_srv.entity.Enum.UserRole;
import com.service.sys_srv.entity.PatientProfile;
import com.service.sys_srv.entity.User;
import com.service.sys_srv.exception.AppException;
import com.service.sys_srv.exception.ERROR_CODE;
import com.service.sys_srv.repository.PatientProfileRepository;
import com.service.sys_srv.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PatientProfileRepository patientProfileRepository;

    public PatientProfile getPatientProfileByUserId(UUID userId) {
        return patientProfileRepository.findById(userId)
                .orElseThrow(() -> new AppException(ERROR_CODE.PATIENT_PROFILE_NOT_FOUND));
    }

    public List<User> getAllUser() {
        return userRepository.findAll();
    }

    @Transactional
    public User registerPatient(RegisterRequest request) {
        User savedUser = registerUser(request, UserRole.patient);
        PatientProfile newProfile = new PatientProfile();
        newProfile.setUser(savedUser);
        patientProfileRepository.save(newProfile);
        return savedUser;
    }

    // đăng ký nhân viên/bác sĩ
    public User registerStaff(RegisterRequest request, UserRole role) {
        if (role != UserRole.doctor && role != UserRole.staff) {
            throw new AppException(ERROR_CODE.ILLEGAL_ROLE);
        }
        return registerUser(request, role);
    }

    // Hàm đăng kí
    private User registerUser(RegisterRequest request, UserRole role) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ERROR_CODE.DUPLICATE_EMAIL);
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setBranchId(request.getBranchId());

        return userRepository.save(user);
    }

    public String login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        String roleName = user.getRole().name().toLowerCase();
        var userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(roleName))
        );

        // claim chứa danh sách quyền
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("authorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return jwtService.generateToken(extraClaims, userDetails);

        // 3. Tạo token
//        var userDetails = new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPasswordHash(), new ArrayList<>());
//        return jwtService.generateToken(userDetails);
    }

    public UserDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // 2. Chuyển đổi (map) từ User entity sang UserDto
//        UserDto userDto = new UserDto();
//        userDto.setId(user.getId());
//        userDto.setFullName(user.getFullName());
//        userDto.setEmail(user.getEmail());
//        userDto.setPhoneNumber(user.getPhoneNumber());
//        userDto.setRole(user.getRole());
//        userDto.setActive(user.isActive());

        return convertToDto(user);
    }

    public PatientProfile updatePatientProfile(UUID userId, UpdateProfileRequest request) {
        PatientProfile profile = getPatientProfileByUserId(userId); // Tái sử dụng hàm đã có

        // Cập nhật các trường có trong request
        profile.setDateOfBirth(request.getDateOfBirth());
        if(request.getGender() != null) {
            profile.setGender(Gender.valueOf(request.getGender().toLowerCase()));
        }
        profile.setAddress(request.getAddress());
        profile.setAllergies(request.getAllergies());
        profile.setContraindications(request.getContraindications());
        profile.setMedicalHistory(request.getMedicalHistory());

        return patientProfileRepository.save(profile);
    }

    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        return convertToDto(user);
    }

//    public UserDto getUserById(UUID userId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
//
//        return convertToDto(user);
//    }

    private UserDto convertToDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setFullName(user.getFullName());
        userDto.setEmail(user.getEmail());
        userDto.setPhoneNumber(user.getPhoneNumber());
        userDto.setRole(user.getRole());
        userDto.setActive(user.isActive());
        return userDto;
    }

    public List<UserDto> getDoctors() {
        List<User> doctors = userRepository.findByRole(UserRole.doctor);

        return doctors.stream()
                .map(this::convertToDto)
                .toList();
    }

    public UserDto updateUser(UUID userId, UpdateUserRequest request) {
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ERROR_CODE.USER_NOT_FOUND));

        if (request.getFullName() != null) {
            userToUpdate.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            userToUpdate.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getRole() != null) {
            userToUpdate.setRole(UserRole.valueOf(request.getRole().toLowerCase()));
        }
        if (request.getBranchId() != null) {
            // kiểm tra xem branchId có hợp lệ không
            userToUpdate.setBranchId(request.getBranchId());
        }
        if (request.getIsActive() != null) {
            userToUpdate.setActive(request.getIsActive());
        }

        User updatedUser = userRepository.save(userToUpdate);
        return convertToDto(updatedUser);
    }

    public List<UserSimpleDto> getDoctorsSimple() {
        return userRepository.findByRole(UserRole.doctor)
                .stream()
                .map(user -> new UserSimpleDto(user.getId(), user.getFullName()))
                .collect(Collectors.toList());
    }
}