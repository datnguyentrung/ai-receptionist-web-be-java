package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.config.BootstrapUserProperties;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.enums.Core.Belt;
import com.dat.ai_receptionist_web.enums.Core.PersonStatus;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.util.PhoneNumberUtil;
import com.dat.ai_receptionist_web.util.converter.NameConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(4)
public class SystemUserSynchronizer implements ApplicationRunner {
    private final BootstrapUserProperties bootstrapUserProperties;
    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final UserPersonRepository userPersonRepository;
    private final UserService userService;
    private final UserRoleService userRoleService;

    /**
     * Tác dụng: Chạy tác vụ khởi động hoặc đồng bộ dữ liệu theo ngữ cảnh của lớp.
     * Input: Nhận ApplicationArguments args từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SyncResult result = syncAll(bootstrapUserProperties.getUsers());

        log.info(
                "System user sync completed: total={}, created={}, personsCreated={}, contextsCreated={}, contextsActivated={}, roleAssignments={}",
                bootstrapUserProperties.getUsers().size(),
                result.usersCreated(),
                result.personsCreated(),
                result.contextsCreated(),
                result.contextsActivated(),
                result.roleAssignments()
        );
    }

    SyncResult syncAll(List<BootstrapUserProperties.UserAccount> accounts) {
        List<BootstrapDefinition> definitions = accounts.stream()
                .map(this::normalize)
                .toList();
        if (definitions.isEmpty()) {
            return new SyncResult(0, 0, 0, 0, 0);
        }

        Set<String> phoneNumbers = definitions.stream()
                .map(BootstrapDefinition::phoneNumber)
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> personCodes = definitions.stream()
                .map(BootstrapDefinition::personCode)
                .collect(Collectors.toCollection(TreeSet::new));

        Map<String, User> usersByPhone = userRepository.findAllByPhoneNumberIn(phoneNumbers).stream()
                .collect(Collectors.toMap(User::getPhoneNumber, Function.identity()));
        Map<String, Person> personsByCode = personRepository.findAllByPersonCodeUpperIn(personCodes).stream()
                .collect(Collectors.toMap(person -> person.getPersonCode().toUpperCase(Locale.ROOT), Function.identity()));

        int usersCreated = 0;
        for (BootstrapDefinition definition : definitions) {
            if (!usersByPhone.containsKey(definition.phoneNumber())) {
                User user = userService.createLoginUserWithoutDuplicateCheck(
                        definition.phoneNumber(),
                        definition.password()
                );
                usersByPhone.put(definition.phoneNumber(), user);
                usersCreated++;
            }
        }

        int personsCreated = 0;
        for (BootstrapDefinition definition : definitions) {
            if (!personsByCode.containsKey(definition.personCode())) {
                Person person = createSystemPerson(definition);
                personsByCode.put(definition.personCode(), person);
                personsCreated++;
            }
        }

        ContextSyncResult contextResult = syncUserPersons(definitions, usersByPhone, personsByCode);
        int roleAssignments = syncUserRoles(definitions, usersByPhone);

        return new SyncResult(
                usersCreated,
                personsCreated,
                contextResult.created(),
                contextResult.activated(),
                roleAssignments
        );
    }

    /**
     * Tác dụng: Đồng bộ dữ liệu theo cấu hình hiện tại và chỉ ghi khi có thay đổi.
     * Input: Nhận List<BootstrapDefinition> definitions, Map<String, User> usersByPhone, Map<String, Person> personsByCode từ caller hoặc request.
     * Output: Trả về ContextSyncResult theo kết quả xử lý.
     */
    private ContextSyncResult syncUserPersons(
            List<BootstrapDefinition> definitions,
            Map<String, User> usersByPhone,
            Map<String, Person> personsByCode
    ) {
        Set<UUID> userIds = usersByPhone.values().stream()
                .map(User::getUserId)
                .collect(Collectors.toSet());
        Set<UUID> personIds = personsByCode.values().stream()
                .map(Person::getPersonId)
                .collect(Collectors.toSet());

        Map<UserPersonKey, UserPerson> contextsByKey = userPersonRepository
                .findAllByUserIdInAndPersonIdInAndRelationshipType(userIds, personIds, RelationshipType.MANAGER)
                .stream()
                .collect(Collectors.toMap(
                        userPerson -> new UserPersonKey(
                                userPerson.getUser().getUserId(),
                                userPerson.getPerson().getPersonId()
                        ),
                        Function.identity()
                ));

        int contextsCreated = 0;
        int contextsActivated = 0;
        for (BootstrapDefinition definition : definitions) {
            User user = usersByPhone.get(definition.phoneNumber());
            Person person = personsByCode.get(definition.personCode());
            UserPersonKey key = new UserPersonKey(user.getUserId(), person.getPersonId());
            UserPerson userPerson = contextsByKey.get(key);
            if (userPerson == null) {
                UserPerson created = userPersonRepository.save(UserPerson.builder()
                        .user(user)
                        .person(person)
                        .relationshipType(RelationshipType.MANAGER)
                        .active(true)
                        .build());
                contextsByKey.put(key, created);
                contextsCreated++;
                continue;
            }

            if (!userPerson.isActive()) {
                userPerson.setActive(true);
                contextsActivated++;
            }
        }

        return new ContextSyncResult(contextsCreated, contextsActivated);
    }

    /**
     * Tác dụng: Đồng bộ dữ liệu theo cấu hình hiện tại và chỉ ghi khi có thay đổi.
     * Input: Nhận List<BootstrapDefinition> definitions, Map<String, User> usersByPhone từ caller hoặc request.
     * Output: Trả về giá trị int biểu thị kết quả tính toán hoặc số lượng.
     */
    private int syncUserRoles(
            List<BootstrapDefinition> definitions,
            Map<String, User> usersByPhone
    ) {
        Map<UUID, Set<String>> desiredRoleCodesByUser = new LinkedHashMap<>();
        for (BootstrapDefinition definition : definitions) {
            UUID userId = usersByPhone.get(definition.phoneNumber()).getUserId();
            desiredRoleCodesByUser
                    .computeIfAbsent(userId, ignored -> new TreeSet<>())
                    .add(definition.roleCode());
        }
        return userRoleService.assignRolesIfMissing(desiredRoleCodesByUser);
    }

    /**
     * Tác dụng: Thực hiện logic createSystemPerson của lớp hiện tại.
     * Input: Nhận BootstrapDefinition definition từ caller hoặc request.
     * Output: Trả về Person theo kết quả xử lý.
     */
    private Person createSystemPerson(BootstrapDefinition definition) {
        return personRepository.save(Person.builder()
                .fullName(NameConverter.formatVietnameseName(definition.fullName()))
                .gender(true)
                .birthDate(LocalDate.of(1970, 1, 1))
                .email(definition.email())
                .personCode(definition.personCode())
                .currentBelt(Belt.C10)
                .status(PersonStatus.ACTIVE)
                .startDate(LocalDate.now())
                .build());
    }

    /**
     * Tác dụng: Chuẩn hóa dữ liệu đầu vào về định dạng thống nhất để so sánh và lưu trữ.
     * Input: Nhận BootstrapUserProperties.UserAccount account từ caller hoặc request.
     * Output: Trả về BootstrapDefinition theo kết quả xử lý.
     */
    private BootstrapDefinition normalize(BootstrapUserProperties.UserAccount account) {
        validate(account);
        String personCode = account.getPersonCode().trim().toUpperCase(Locale.ROOT);
        if (!personCode.startsWith("VQ_")) {
            throw new IllegalArgumentException("Bootstrap user personCode must start with VQ_");
        }
        return new BootstrapDefinition(
                account.getRoleCode().trim().toUpperCase(Locale.ROOT),
                PhoneNumberUtil.normalize(account.getPhoneNumber()),
                account.getPassword(),
                personCode,
                account.getFullName(),
                account.getEmail()
        );
    }

    /**
     * Tác dụng: Kiểm tra tính hợp lệ của dữ liệu đầu vào trước khi xử lý tiếp.
     * Input: Nhận BootstrapUserProperties.UserAccount account từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    private void validate(BootstrapUserProperties.UserAccount account) {
        if (isBlank(account.getRoleCode())) {
            throw new IllegalArgumentException("Bootstrap user roleCode must not be blank");
        }
        if (isBlank(account.getPhoneNumber())) {
            throw new IllegalArgumentException("Bootstrap user phoneNumber must not be blank");
        }
        if (isBlank(account.getPassword())) {
            throw new IllegalArgumentException("Bootstrap user password must not be blank");
        }
        if (isBlank(account.getPersonCode())) {
            throw new IllegalArgumentException("Bootstrap user personCode must not be blank");
        }
        if (isBlank(account.getFullName())) {
            throw new IllegalArgumentException("Bootstrap user fullName must not be blank");
        }
    }

    /**
     * Tác dụng: Thực hiện logic isBlank của lớp hiện tại.
     * Input: Nhận String value từ caller hoặc request.
     * Output: Trả về true/false thể hiện kết quả kiểm tra hoặc xử lý.
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    record BootstrapDefinition(
            String roleCode,
            String phoneNumber,
            String password,
            String personCode,
            String fullName,
            String email
    ) {
    }

    record SyncResult(
            int usersCreated,
            int personsCreated,
            int contextsCreated,
            int contextsActivated,
            int roleAssignments
    ) {
    }

    private record ContextSyncResult(int created, int activated) {
    }

    private record UserPersonKey(UUID userId, UUID personId) {
    }

}


