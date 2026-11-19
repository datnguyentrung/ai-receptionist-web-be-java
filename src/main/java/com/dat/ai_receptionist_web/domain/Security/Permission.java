package com.dat.ai_receptionist_web.domain.Security;

import com.dat.ai_receptionist_web.enums.Security.PermissionAction;
import jakarta.persistence.*;
import lombok.*;

/**
 * Quyền thao tác trên một đối tượng nghiệp vụ của hệ thống.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "permission", schema = "security", uniqueConstraints =
        @UniqueConstraint(name = "uk_permission_code", columnNames = "code"))
public class Permission {
    /** Mã định danh duy nhất của quyền. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Integer permissionId;

    /** Mã duy nhất của quyền. */
    @Column(name = "code", nullable = false, length = 100)
    private String code;

    /** Tên đối tượng hoặc nhóm nghiệp vụ mà quyền tác động đến. */
    @Column(name = "model", nullable = false, length = 50)
    private String model;

    /** Hành động được phép thực hiện trên đối tượng. */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private PermissionAction action;

    /** Mô tả chi tiết ý nghĩa của quyền. */
    @Column(name = "description", nullable = false, length = 255)
    private String description;
}
