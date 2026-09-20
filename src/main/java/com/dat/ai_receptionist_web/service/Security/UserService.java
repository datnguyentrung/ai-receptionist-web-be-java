package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.dto.Core.UserPersonDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Security.ChangePasswordReq;
import com.dat.ai_receptionist_web.dto.Security.UserDTO;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import com.dat.ai_receptionist_web.mapper.Security.UserMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.service.Core.PersonService;
import com.dat.ai_receptionist_web.service.Core.UserPersonService;
import com.dat.ai_receptionist_web.util.PhoneNumberUtil;
import com.dat.ai_receptionist_web.error.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PersonRepository personRepository;
    private final UserMapper userMapper;
    private final UserPersonService userPersonService;
    private final PersonService personService;
    private final UserPersonRepository userPersonRepository;
    private final PersonMapper personMapper;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<UserDTO.SimpleResponse> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<UserDTO.SimpleResponse> list(Pageable pageable) {
        return list(null, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDTO.SimpleResponse> list(String search, Pageable pageable) {
        String normalizedSearch = normalizeSearch(search);
        var users = normalizedSearch.isBlank()
                ? userRepository.findAll(pageable)
                : userRepository.searchByPhoneNumberOrPersonFullName(
                normalizedSearch,
                normalizePhoneSearch(normalizedSearch),
                pageable
        );
        Set<UUID> userIds = users.getContent().stream()
                .map(User::getUserId)
                .collect(Collectors.toSet());
        Map<UUID, List<PersonDTO.SimpleResponse>> personsByUserId = userIds.isEmpty()
                ? Map.of()
                : userPersonRepository.findAllActiveByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(
                        userPerson -> userPerson.getUser().getUserId(),
                        Collectors.mapping(
                                userPerson -> personMapper.toSimpleResponse(userPerson.getPerson()),
                                Collectors.toList()
                        )
                ));

        return PageResponse.of(users, user -> new UserDTO.SimpleResponse(
                user.getUserId(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.getLastLoginAt(),
                personsByUserId.getOrDefault(user.getUserId(), List.of())
        ));
    }

    private String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase();
    }

    private String normalizePhoneSearch(String search) {
        String phoneSearch = search.replaceAll("[\\s.\\-()]", "");
        if (phoneSearch.startsWith("+84")) {
            return "0" + phoneSearch.substring(3);
        }
        if (phoneSearch.startsWith("84") && phoneSearch.length() > 2) {
            return "0" + phoneSearch.substring(2);
        }
        return phoneSearch.replaceAll("[^0-9]", "");
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về UserDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public UserDTO.Response get(UUID id) {
        return userMapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận UserDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về UserDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public UserDTO.Response create(UserDTO.CreateRequest request) {
        UUID personId;

        if (request.personId() != null) {
            personId = request.personId();

            if (!personRepository.existsById(personId)) {
                throw new ApiException(CoreErrorCode.PERSON_NOT_FOUND);
            }
        } else {
            personId = personService.create(request.person()).personId();
        }

        User user = createLoginUser(request.phoneNumber(), request.passwordHash());

        userPersonService.create(
                new UserPersonDTO.CreateRequest(
                        user.getUserId(),
                        personId,
                        request.relationshipType(),
                        true
                )
        );

        return userMapper.toResponse(user);
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, UserDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về UserDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public UserDTO.Response update(UUID id, UserDTO.UpdateRequest request) {
        User user = find(id);
        userMapper.updateEntity(request, user);
        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void delete(UUID id) {
        User user = find(id);
        user.setStatus(UserStatus.DISABLED);
    }

    /**
     * Tác dụng: Thực hiện logic getUserById của lớp hiện tại.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về User theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return find(id);
    }

    /**
     * Tác dụng: Thực hiện logic getUserByPhoneNumber của lớp hiện tại.
     * Input: Nhận String phoneNumber từ caller hoặc request.
     * Output: Trả về User theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public User getUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(PhoneNumberUtil.normalize(phoneNumber))
                .orElseThrow(() -> new ApiException(
                        SecurityErrorCode.USER_NOT_FOUND));
    }

    /**
     * Tác dụng: Thực hiện logic createLoginUser của lớp hiện tại.
     * Input: Nhận String phoneNumber, String rawPassword từ caller hoặc request.
     * Output: Trả về User theo kết quả xử lý.
     */
    @Transactional
    public User createLoginUser(String phoneNumber, String rawPassword) {
        String normalized = PhoneNumberUtil.normalize(phoneNumber);
        if (userRepository.findByPhoneNumber(normalized).isPresent()) {
            throw new ApiException(
                    SecurityErrorCode.PHONE_NUMBER_ALREADY_EXISTS);
        }
        return createLoginUserWithoutDuplicateCheck(normalized, rawPassword);
    }

    /**
     * Tác dụng: Thực hiện logic createLoginUserWithoutDuplicateCheck của lớp hiện tại.
     * Input: Nhận String phoneNumber, String rawPassword từ caller hoặc request.
     * Output: Trả về User theo kết quả xử lý.
     */
    @Transactional
    public User createLoginUserWithoutDuplicateCheck(String phoneNumber, String rawPassword) {
        String normalized = PhoneNumberUtil.normalize(phoneNumber);
        return userRepository.save(User.builder()
                .phoneNumber(normalized)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .status(UserStatus.ACTIVE)
                .authorizationVersion(0L)
                .build());
    }

    /**
     * Tác dụng: Thực hiện logic updateLastLogin của lớp hiện tại.
     * Input: Nhận UUID userId từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void updateLastLogin(UUID userId) {
        userRepository.updateLastLogin(userId);
    }

    /**
     * Tác dụng: Thực hiện logic changePassword của lớp hiện tại.
     * Input: Nhận UUID userId, ChangePasswordReq request từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordReq request) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new ApiException(
                    SecurityErrorCode.OLD_PASSWORD_INCORRECT);
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ApiException(
                    SecurityErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về User theo kết quả xử lý.
     */
    private User find(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        SecurityErrorCode.USER_NOT_FOUND,
                        "User not found: " + id));
    }
}


