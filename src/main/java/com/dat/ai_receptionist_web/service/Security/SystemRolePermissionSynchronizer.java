package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import com.dat.ai_receptionist_web.enums.Security.SystemRoleDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(3)
public class SystemRolePermissionSynchronizer implements ApplicationRunner {
    private static final Set<String> GUARDIAN_PERMISSIONS = Set.of(
            PermissionDefinition.STUDENT_ENROLLMENT_READ.getCode(),
            PermissionDefinition.SESSION_ATTENDANCE_READ.getCode(),
            PermissionDefinition.CLASS_SESSION_READ.getCode(),
            PermissionDefinition.CLASS_SCHEDULE_READ.getCode(),
            PermissionDefinition.COURSE_READ.getCode(),
            PermissionDefinition.COURSE_PRICE_READ.getCode(),
            PermissionDefinition.BELT_EXAM_READ.getCode(),
            PermissionDefinition.FITNESS_READ.getCode(),
            PermissionDefinition.FITNESS_RECORD_READ.getCode(),
            PermissionDefinition.WALLET_READ.getCode(),
            PermissionDefinition.WALLET_TRANSACTION_READ.getCode(),
            PermissionDefinition.COURSE_PURCHASE_READ.getCode(),
            PermissionDefinition.COURSE_PURCHASE_CREATE.getCode(),
            PermissionDefinition.WALLET_TOP_UP_CREATE.getCode()
    );

    private static final Set<String> SYSTEM_ADMIN_EXCLUDED_PERMISSIONS = Set.of(
            PermissionDefinition.WALLET_READ.getCode(),
            PermissionDefinition.WALLET_CREATE.getCode(),
            PermissionDefinition.WALLET_UPDATE.getCode(),
            PermissionDefinition.WALLET_DELETE.getCode(),
            PermissionDefinition.WALLET_TRANSACTION_READ.getCode(),
            PermissionDefinition.WALLET_TRANSACTION_CREATE.getCode(),
            PermissionDefinition.WALLET_TRANSACTION_UPDATE.getCode(),
            PermissionDefinition.WALLET_TRANSACTION_DELETE.getCode(),
            PermissionDefinition.WALLET_TOP_UP_CREATE.getCode(),
            PermissionDefinition.WALLET_REFUND_CREATE.getCode()
    );

    private final RolePermissionService rolePermissionService;

    /**
     * Tác dụng: Chạy tác vụ khởi động hoặc đồng bộ dữ liệu theo ngữ cảnh của lớp.
     * Input: Nhận ApplicationArguments args từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, Set<String>> desiredByRole = Arrays.stream(SystemRoleDefinition.values())
                .collect(Collectors.toMap(
                        SystemRoleDefinition::getCode,
                        this::getPermissionsForRole,
                        (left, right) -> {
                            throw new IllegalStateException("Duplicate system role");
                        },
                        TreeMap::new
                ));

        RolePermissionService.BulkSyncResult result =
                rolePermissionService.replaceAll(desiredByRole);

        log.info(
                "System role-permission sync completed: roles={}, added={}, removed={}",
                SystemRoleDefinition.values().length,
                result.addedCount(),
                result.removedCount()
        );
    }

    Set<String> getPermissionsForRole(SystemRoleDefinition role) {
        if (role == SystemRoleDefinition.GUARDIAN) {
            return Collections.unmodifiableSet(new TreeSet<>(GUARDIAN_PERMISSIONS));
        }

        Set<String> permissionCodes = allPermissionCodes();
        if (role == SystemRoleDefinition.SYSTEM_ADMIN) {
            permissionCodes.removeAll(getSystemAdminExcludedPermissions());
        }
        return Collections.unmodifiableSet(permissionCodes);
    }

    Set<String> getSystemAdminExcludedPermissions() {
        return SYSTEM_ADMIN_EXCLUDED_PERMISSIONS;
    }

    /**
     * Tác dụng: Thực hiện logic allPermissionCodes của lớp hiện tại.
     * Input: Không có tham số đầu vào.
     * Output: Trả về Set<String> theo kết quả xử lý.
     */
    private Set<String> allPermissionCodes() {
        return Arrays.stream(PermissionDefinition.values())
                .map(PermissionDefinition::getCode)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}


