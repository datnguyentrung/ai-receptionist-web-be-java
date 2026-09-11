package com.dat.ai_receptionist_web.schema;

import com.dat.ai_receptionist_web.enums.Catalog.*;
import com.dat.ai_receptionist_web.enums.Core.*;
import com.dat.ai_receptionist_web.enums.Finance.*;
import com.dat.ai_receptionist_web.enums.Training.*;
import com.dat.ai_receptionist_web.enums.Security.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class EnumSchemaContractTest {
    @Test
    void persistedEnumsMatchClassFiveSchemaExactly() {
        assertValues(PermissionAction.class, "CREATE", "UPDATE", "READ", "DELETE", "APPROVE");
        assertValues(UserStatus.class, "PENDING", "ACTIVE", "LOCKED", "DISABLED", "BANNED");
        assertValues(Belt.class, "C10", "C9", "C8", "C7", "C6", "C5", "C4", "C3", "C2", "C1",
                "D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9", "D10");
        assertValues(Weekday.class, "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY");
        assertValues(BranchStatus.class, "OPERATING", "CLOSED", "MAINTENANCE");
        assertValues(ScheduleStatus.class, "ACTIVE", "INACTIVE");
        assertValues(CourseStatus.class, "OPEN", "ACTIVE", "CLOSED", "CANCELLED");
        assertValues(RelationshipType.class, "OWNER", "GUARDIAN", "MANAGER");
        assertValues(PersonStatus.class, "ACTIVE", "INACTIVE", "SUSPENDED");
        assertValues(AssignmentType.class, "PRIMARY_COACH", "ASSISTANT_COACH", "TEACHING_ASSISTANT", "MANAGER");
        assertValues(CourseStaffAssignmentStatus.class, "PENDING", "ACTIVE", "SUSPENDED", "ENDED", "CANCELLED");
        assertValues(StudentEnrollmentStatus.class, "PENDING_START", "ACTIVE", "COMPLETED", "EXPIRED", "CANCELLED");
        assertValues(WalletTransactionStatus.class, "PENDING", "PROCESSING", "APPROVED", "REJECTED");
        assertValues(ScheduleLevel.class, "BASIC", "ADVANCED", "EXPERT");
        assertValues(NotificationRecipientStatus.class, "PENDING", "SENT", "ARCHIVED", "FAILED");
        assertValues(SessionStatus.class, "SCHEDULED", "ACTIVE", "COMPLETED", "TERMINATED", "CANCELLED", "POSTPONED");
        assertValues(AttendanceStatus.class, "PRESENT", "ABSENT", "EXCUSED", "MAKEUP", "LATE");
        assertValues(WalletStatus.class, "ACTIVE", "FROZEN", "CLOSED");
        assertValues(CoursePriceStatus.class, "ACTIVE", "INACTIVE");
        assertValues(WalletTransactionType.class, "TOP_UP", "COURSE_PURCHASE", "SUPPLY_PURCHASE", "REFUND", "MANUAL_ADJUSTMENT");
        assertValues(EvaluationStatus.class, "PENDING", "GOOD", "AVERAGE", "WEAK");
        assertValues(WalletTransactionDirection.class, "CREDIT", "DEBIT");
        assertValues(NotificationType.class, "SYSTEM", "ATTENDANCE", "TUITION", "CLASS_SCHEDULE",
                "COACH_TIMESHEET", "ANNOUNCEMENT", "CLASS_SESSION_REPORT");
        assertValues(ScheduleLocation.class, "INDOOR", "OUTDOOR", "ONLINE");
    }

    @Test
    void weekdayCodesMatchClassFiveSchema() {
        assertThat(Arrays.stream(Weekday.values()).map(Weekday::getCode))
                .containsExactly(1, 2, 3, 4, 5, 6, 7);
    }

    private void assertValues(Class<? extends Enum<?>> type, String... expected) {
        assertThat(Arrays.stream(type.getEnumConstants()).map(Enum::name))
                .containsExactly(expected);
    }
}
