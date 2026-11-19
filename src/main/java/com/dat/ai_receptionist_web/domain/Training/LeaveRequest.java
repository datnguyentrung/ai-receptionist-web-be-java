package com.dat.ai_receptionist_web.domain.Training;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.enums.Training.LeaveRequestStatus;
import com.dat.ai_receptionist_web.enums.Training.RequesterType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Yêu cầu nghỉ học của học viên hoặc nghỉ làm của nhân viên.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "leave_request",
        schema = "training",
        indexes = {
                @Index(name = "idx_leave_request_person_status", columnList = "person_id,status"),
                @Index(name = "idx_leave_request_type_status", columnList = "requester_type,status"),
                @Index(name = "idx_leave_request_leave_date", columnList = "leave_date"),
                @Index(name = "idx_leave_request_leave_session", columnList = "leave_class_session_id"),
                @Index(name = "idx_leave_request_makeup_session", columnList = "makeup_class_session_id"),
                @Index(name = "idx_leave_request_created_by", columnList = "created_by_user_id")
        }
)
@Check(
        name = "chk_leave_request_business_rule",
        constraints = """
                (
                    (
                        requester_type = 'STUDENT'
                        AND leave_class_session_id IS NOT NULL
                        AND makeup_class_session_id IS NOT NULL
                    )
                    OR
                    (
                        requester_type = 'SYSTEM_EMPLOYEE'
                        AND leave_date IS NOT NULL
                    )
                )
                AND
                (
                    leave_class_session_id IS NULL
                    OR makeup_class_session_id IS NULL
                    OR leave_class_session_id <> makeup_class_session_id
                )
                AND
                (
                    status NOT IN ('APPROVED', 'REJECTED')
                    OR (
                        reviewed_by_user_id IS NOT NULL
                        AND reviewed_at IS NOT NULL
                    )
                )
                AND
                (
                    status <> 'PENDING'
                    OR (
                        reviewed_by_user_id IS NULL
                        AND reviewed_at IS NULL
                    )
                )
                """
)
public class LeaveRequest {

    /** Mã định danh duy nhất của yêu cầu nghỉ. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "leave_request_id", nullable = false, updatable = false)
    private UUID leaveRequestId;

    /** Cá nhân xin nghỉ học hoặc nghỉ làm. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "person_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_leave_request_person")
    )
    private Person person;

    /** Loại người gửi yêu cầu, dùng để xác định quy tắc xử lý. */
    @Enumerated(EnumType.STRING)
    @Column(name = "requester_type", nullable = false, length = 30)
    private RequesterType requesterType;

    /** Ngày nghỉ của nhân viên hệ thống. */
    @Column(name = "leave_date")
    private LocalDate leaveDate;

    /** Buổi học mà học viên xin nghỉ. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "leave_class_session_id",
            foreignKey = @ForeignKey(name = "fk_leave_request_leave_session")
    )
    private ClassSession leaveClassSession;

    /** Buổi học bù dành cho học viên. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "makeup_class_session_id",
            foreignKey = @ForeignKey(name = "fk_leave_request_makeup_session")
    )
    private ClassSession makeupClassSession;

    /** Lý do và bối cảnh của yêu cầu nghỉ. */
    @NotBlank
    @Size(max = 1000)
    @Column(name = "leave_context", nullable = false, length = 1000)
    private String leaveContext;

    /** Trạng thái phê duyệt hiện tại của yêu cầu nghỉ. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LeaveRequestStatus status = LeaveRequestStatus.PENDING;

    /** Người dùng tạo yêu cầu nghỉ. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_leave_request_created_by")
    )
    private User createdByUser;

    /** Người dùng phê duyệt hoặc từ chối yêu cầu nghỉ. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reviewed_by_user_id",
            foreignKey = @ForeignKey(name = "fk_leave_request_reviewed_by")
    )
    private User reviewedByUser;

    /** Thời điểm yêu cầu nghỉ được phê duyệt hoặc từ chối. */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** Ghi chú của người duyệt yêu cầu nghỉ. */
    @Size(max = 1000)
    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    /** Thời điểm bản ghi được tạo. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm bản ghi được cập nhật gần nhất. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void validateInvariant() {
        if (requesterType == null) {
            throw new IllegalStateException("requesterType must not be null");
        }
        if (person == null) {
            throw new IllegalStateException("person must not be null");
        }
        if (createdByUser == null) {
            throw new IllegalStateException("createdByUser must not be null");
        }
        if (leaveContext == null || leaveContext.isBlank()) {
            throw new IllegalStateException("leaveContext must not be blank");
        }
        if (status == null) {
            throw new IllegalStateException("status must not be null");
        }

        if (requesterType == RequesterType.STUDENT) {
            if (leaveClassSession == null) {
                throw new IllegalStateException(
                        "leaveClassSession is required when requesterType is STUDENT"
                );
            }
            if (makeupClassSession == null) {
                throw new IllegalStateException(
                        "makeupClassSession is required when requesterType is STUDENT"
                );
            }
        }

        if (requesterType == RequesterType.SYSTEM_EMPLOYEE && leaveDate == null) {
            throw new IllegalStateException(
                    "leaveDate is required when requesterType is SYSTEM_EMPLOYEE"
            );
        }

        if (leaveClassSession != null
                && makeupClassSession != null
                && leaveClassSession.getClassSessionId() != null
                && leaveClassSession.getClassSessionId()
                .equals(makeupClassSession.getClassSessionId())) {
            throw new IllegalStateException(
                    "leaveClassSession and makeupClassSession must be different"
            );
        }

        if (status == LeaveRequestStatus.APPROVED
                || status == LeaveRequestStatus.REJECTED) {
            if (reviewedByUser == null || reviewedAt == null) {
                throw new IllegalStateException(
                        "reviewedByUser and reviewedAt are required when status is APPROVED or REJECTED"
                );
            }
        }

        if (status == LeaveRequestStatus.PENDING
                && (reviewedByUser != null || reviewedAt != null)) {
            throw new IllegalStateException(
                    "reviewedByUser and reviewedAt must be null when status is PENDING"
            );
        }
    }
}
