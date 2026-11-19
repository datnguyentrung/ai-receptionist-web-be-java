package com.dat.ai_receptionist_web.domain.Security;

import jakarta.persistence.*;
import lombok.*;

/**
 * Vai trò dùng để gom nhóm các quyền được gán cho người dùng.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "role", schema = "security")
public class Role {
    /** Mã duy nhất và cũng là khóa chính của vai trò. */
    @Id
    @Column(name = "role_id", nullable = false, length = 50)
    private String code;

    /** Tên hiển thị của vai trò. */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Mô tả mục đích và phạm vi của vai trò. */
    @Column(name = "description", length = 255)
    private String description;

    /** Phiên bản quyền dùng để làm mới thông tin phân quyền đã lưu. */
    @Column(name = "permission_version", nullable = false)
    @Builder.Default
    private long permissionVersion = 1;
}
