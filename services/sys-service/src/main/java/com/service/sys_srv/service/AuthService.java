package com.service.sys_srv.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.service.sys_srv.dto.request.DoctorSearchRequest;
import com.service.sys_srv.dto.request.LoginRequest;
import com.service.sys_srv.dto.request.PatientSearchRequest;
import com.service.sys_srv.dto.request.RegisterRequest;
import com.service.sys_srv.dto.request.StaffSearchRequest;
import com.service.sys_srv.dto.request.UpdateDoctorProfileRequest;
import com.service.sys_srv.dto.request.UpdateProfileRequest;
import com.service.sys_srv.dto.request.UpdateUserRequest;
import com.service.sys_srv.dto.response.DoctorDto;
import com.service.sys_srv.dto.response.DoctorSearchResponseDto;
import com.service.sys_srv.dto.response.PatientSearchResponseDto;
import com.service.sys_srv.dto.response.SpecialtySimpleDto;
import com.service.sys_srv.dto.response.StaffSearchResponseDto;
import com.service.sys_srv.dto.response.UserDto;
import com.service.sys_srv.dto.response.UserSimpleDto;
import com.service.sys_srv.entity.DoctorProfile;
import com.service.sys_srv.entity.Enum.Gender;
import com.service.sys_srv.entity.Enum.UserRole;
import com.service.sys_srv.entity.PatientProfile;
import com.service.sys_srv.entity.User;
import com.service.sys_srv.exception.AppException;
import com.service.sys_srv.exception.ERROR_CODE;
import com.service.sys_srv.repository.DoctorProfileRepository;
import com.service.sys_srv.repository.PatientProfileRepository;
import com.service.sys_srv.repository.UserRepository;

import jakarta.persistence.criteria.Join;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;


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
        User savedUser = userRepository.save(user);
        if (role == UserRole.doctor) {
            DoctorProfile newDoctorProfile = new DoctorProfile();
            newDoctorProfile.setUser(savedUser);
            doctorProfileRepository.save(newDoctorProfile);
        }

        return savedUser;
    }

    public DoctorProfile getDoctorProfile(UUID userId) {
        return doctorProfileRepository.findById(userId)
                .orElseThrow(() -> new AppException(ERROR_CODE.PROFILE_NOT_FOUND)); // Có thể tạo mã lỗi riêng
    }

    @Transactional
    public DoctorProfile updateDoctorProfile(UUID userId, UpdateDoctorProfileRequest request) {
        DoctorProfile profile = getDoctorProfile(userId);

        // Cập nhật các trường
        if (request.getSpecialty() != null) profile.setSpecialty(request.getSpecialty());
        if (request.getDegree() != null) profile.setDegree(request.getDegree());


        return doctorProfileRepository.save(profile);
    }

    public List<SpecialtySimpleDto> getUniqueSpecialties() {
        List<String> specialtyNames = doctorProfileRepository.findDistinctSpecialties();

        return specialtyNames.stream()
                .map(name -> new SpecialtySimpleDto(name, name))
                .collect(Collectors.toList());
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

        return convertToDto(user);
    }

    public PatientProfile updatePatientProfile(UUID userId, UpdateProfileRequest request) {
        PatientProfile profile = getPatientProfileByUserId(userId); // Tái sử dụng hàm đã có

        // Cập nhật các trường có trong request
        profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) {
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

    private DoctorDto convertToDoctorDto(User user) {
        DoctorDto userDto = new DoctorDto();
        userDto.setId(user.getId());
        userDto.setFullName(user.getFullName());
        userDto.setEmail(user.getEmail());
        userDto.setPhoneNumber(user.getPhoneNumber());
        userDto.setRole(user.getRole());
        userDto.setActive(user.isActive());
        userDto.setBranchId(user.getBranchId());
        userDto.setSpecialty(user.getDoctorProfile().getSpecialty());
        userDto.setDegree(user.getDoctorProfile().getDegree());
        return userDto;
    }

    public List<DoctorDto> getDoctors() {
        List<User> doctors = userRepository.findByRole(UserRole.doctor);

        return doctors.stream()
                .map(this::convertToDoctorDto)
                .toList();
    }

    public Page<StaffSearchResponseDto> searchStaffs(StaffSearchRequest request) {

        // 1. Tạo đối tượng Phân trang (Pageable)
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        // 2. Tạo đối tượng Lọc (Specification) TRỰC TIẾP TRONG SERVICE

        // Điều kiện cơ bản: Luôn luôn lọc các vai trò là 'staff' hoặc 'admin'
        Specification<User> spec = (root, query, cb) ->
                root.get("role").in(UserRole.staff, UserRole.admin);

        // Lọc theo tên (fullName) nếu có
        if (StringUtils.hasText(request.getFullName())) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("fullName")), "%" + request.getFullName().toLowerCase() + "%")
            );
        }

        // Lọc theo email nếu có
        if (StringUtils.hasText(request.getEmail())) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")), "%" + request.getEmail().toLowerCase() + "%")
            );
        }

        // Lọc theo số điện thoại nếu có
        if (StringUtils.hasText(request.getPhoneNumber())) {
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("phoneNumber"), "%" + request.getPhoneNumber() + "%")
            );
        }

        // Lọc theo chi nhánh (branchId) nếu có
        if (request.getBranchId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("branchId"), request.getBranchId())
            );
        }

        // Lọc theo vai trò cụ thể (staff hoặc admin) nếu được chỉ định
        if (request.getRole() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("role"), request.getRole())
            );
        }

        if (request.getActive() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("isActive"), request.getActive())
            );
        }
        // --- Kết thúc logic Specification ---

        // 3. Gọi Repository (UserRepository phải extends JpaSpecificationExecutor)
        Page<User> userPage = userRepository.findAll(spec, pageable);

        // 4. Chuyển đổi (Map) Page<User> sang Page<StaffSearchResponseDto>
        return userPage.map(this::convertToStaffSearchResponseDto);
    }

    /**
     * Hàm helper để chuyển đổi User sang DTO Response cho Staff
     */
    private StaffSearchResponseDto convertToStaffSearchResponseDto(User user) {
        StaffSearchResponseDto dto = new StaffSearchResponseDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setBranchId(user.getBranchId());
        dto.setActive(user.isActive());
        dto.setRole(user.getRole());
        return dto;
    }

    public Page<DoctorSearchResponseDto> searchDoctors(DoctorSearchRequest request) {

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        // --- CẬP NHẬT LOGIC SPECIFICATION ---
        Specification<User> spec = (root, query, cb) ->
                cb.equal(root.get("role"), UserRole.doctor);

        if (StringUtils.hasText(request.getFullName())) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("fullName")), "%" + request.getFullName().toLowerCase() + "%")
            );
        }

        // Lọc thêm theo email (mới)
        if (StringUtils.hasText(request.getEmail())) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")), "%" + request.getEmail().toLowerCase() + "%")
            );
        }

        // Lọc thêm theo SĐT (mới)
        if (StringUtils.hasText(request.getPhoneNumber())) {
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("phoneNumber"), "%" + request.getPhoneNumber() + "%")
            );
        }

        if (request.getBranchId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("branchId"), request.getBranchId())
            );
        }

        if (StringUtils.hasText(request.getSpecialty())) {
            spec = spec.and((root, query, cb) -> {
                Join<User, DoctorProfile> profileJoin = root.join("doctorProfile");
                return cb.like(cb.lower(profileJoin.get("specialty")), "%" + request.getSpecialty().toLowerCase() + "%");
            });
        }

        if (request.getActive() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("isActive"), request.getActive())
            );
        }

        // --- Kết thúc logic Specification ---

        Page<User> userPage = userRepository.findAll(spec, pageable);

        // Map sang DTO mới
        return userPage.map(this::convertToDoctorSearchResponseDto);
    }

    /**
     * Sửa lại Hàm helper: Chuyển đổi User sang DTO Response mới
     */
    private DoctorSearchResponseDto convertToDoctorSearchResponseDto(User user) {
        DoctorSearchResponseDto dto = new DoctorSearchResponseDto();

        // Các trường từ User (giống Staff)
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setBranchId(user.getBranchId());
        dto.setActive(user.isActive());
        dto.setRole(user.getRole());

        // Các trường từ DoctorProfile
        DoctorProfile profile = user.getDoctorProfile();
        if (profile != null) {
            dto.setSpecialty(profile.getSpecialty());
            dto.setDegree(profile.getDegree());
        }

        return dto;
    }

    public Page<PatientSearchResponseDto> searchPatients(PatientSearchRequest request) {

        // 1. Tạo đối tượng Phân trang (Pageable)
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        Specification<User> spec = (root, query, cb) ->
                cb.equal(root.get("role"), UserRole.patient);

        // Lọc theo tên (fullName) nếu có
        if (StringUtils.hasText(request.getFullName())) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("fullName")), "%" + request.getFullName().toLowerCase() + "%")
            );
        }

        // Lọc theo email nếu có
        if (StringUtils.hasText(request.getEmail())) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")), "%" + request.getEmail().toLowerCase() + "%")
            );
        }

        // Lọc theo số điện thoại nếu có
        if (StringUtils.hasText(request.getPhoneNumber())) {
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("phoneNumber"), "%" + request.getPhoneNumber() + "%")
            );
        }

        // Lọc theo hạng thành viên (yêu cầu JOIN)
        if (StringUtils.hasText(request.getMembershipTier())) {
            spec = spec.and((root, query, cb) -> {
                Join<User, PatientProfile> profileJoin = root.join("patientProfile");
                return cb.equal(profileJoin.get("membershipTier"), request.getMembershipTier());
            });
        }

        if (request.getActive() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("isActive"), request.getActive())
            );
        }

        Page<User> userPage = userRepository.findAll(spec, pageable);

        return userPage.map(this::convertToPatientSearchResponseDto);
    }

    private PatientSearchResponseDto convertToPatientSearchResponseDto(User user) {
        PatientSearchResponseDto dto = new PatientSearchResponseDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setActive(user.isActive());

        PatientProfile profile = user.getPatientProfile();
        if (profile != null) {
            dto.setMembershipTier(profile.getMembershipTier());
            dto.setPoints(profile.getPoints());
        } else {
            dto.setMembershipTier("STANDARD");
            dto.setPoints(0);
        }
        return dto;
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

    @Transactional
    public PatientProfile addPointsToPatient(UUID userId, int pointsToAdd) {
        PatientProfile profile = getPatientProfileByUserId(userId);
        profile.setPoints(profile.getPoints() + pointsToAdd);

        if (profile.getPoints() >= 5000 && "SILVER".equals(profile.getMembershipTier())) {
            profile.setMembershipTier("GOLD");
        } else if (profile.getPoints() >= 1000 && "STANDARD".equals(profile.getMembershipTier())) {
            profile.setMembershipTier("SILVER");
        }

        return patientProfileRepository.save(profile);
    }

    @Transactional
    public UserDto toggleUserActiveStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ERROR_CODE.USER_NOT_FOUND));

        boolean currentStatus = user.isActive();
        user.setActive(!currentStatus);

        User updatedUser = userRepository.save(user);


        return convertToDto(updatedUser);
    }

    public List<UUID> searchUserIdsByNameAndRole(String name, String roleStr) {
        if (!StringUtils.hasText(name) || !StringUtils.hasText(roleStr)) return List.of();

        try {
            // Convert role string to lowercase to match database values (patient, doctor, etc.)
            String normalizedRole = roleStr.toLowerCase();
            UserRole role = UserRole.valueOf(normalizedRole);
            List<User> users = userRepository.findByRoleAndFullNameIgnoreCaseContaining(role, name);
            return users.stream().map(User::getId).collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            // If role doesn't match, return empty list
            return List.of();
        }
    }

    public Map<UUID, String> getPatientNames(List<UUID> ids) {
        return userRepository.findByIdInAndRole(ids, UserRole.patient)
            .stream()
            .collect(Collectors.toMap(
                User::getId,
                User::getFullName
            ));
    }
}