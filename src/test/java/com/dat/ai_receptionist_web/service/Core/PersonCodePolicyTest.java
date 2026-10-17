package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Core.Position;
import com.dat.ai_receptionist_web.enums.Core.PersonKind;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonCodePolicyTest {
    private final PersonCodePolicy policy = new PersonCodePolicy();

    @Test
    void classifiesStudentAndStaffByPositionWithinVqNamespace() {
        assertThat(policy.classify(person("VQ_anv_010100", "STUDENT", true))).isEqualTo(PersonKind.STUDENT);
        assertThat(policy.classify(person("VQ_coach_001", "COACH_SENIOR", true)))
                .isEqualTo(PersonKind.SYSTEM_EMPLOYEE);
        assertThat(policy.classify(person("VQ_assistant_001", "ASSISTANT_1", true)))
                .isEqualTo(PersonKind.SYSTEM_EMPLOYEE);
    }

    @Test
    void rejectsInvalidOrMissingCodes() {
        assertThatThrownBy(() -> policy.validateFormat(null)).isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode())
                        .isEqualTo(CoreErrorCode.PERSON_CODE_POLICY_VIOLATION));
        assertThatThrownBy(() -> policy.validateFormat("VQT_STAFF_001")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> policy.validateFormat("STAFF-001")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> policy.validateFormat("")).isInstanceOf(ApiException.class);
    }

    @Test
    void requireStudentAndSystemEmployeeEnforceKind() {
        Person student = person("VQ_anv_010100", "STUDENT", true);
        Person employee = person("VQ_coach_001", "COACH_JUNIOR", true);

        policy.requireStudent(student);
        policy.requireSystemEmployee(employee);

        assertThatThrownBy(() -> policy.requireSystemEmployee(student))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> policy.requireStudent(employee))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> policy.requireStudent(Person.builder().personCode(null).build()))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> policy.requireSystemEmployee(person("VQ_staff_001", "STUDENT", true)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> policy.requireSystemEmployee(person("VQ_staff_001", "COACH_JUNIOR", false)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> policy.requireSystemEmployee(Person.builder().personCode("VQ_staff_001").build()))
                .isInstanceOf(ApiException.class);
    }

    private Person person(String personCode, String positionCode, boolean active) {
        return Person.builder()
                .personCode(personCode)
                .position(Position.builder().code(positionCode).active(active).build())
                .build();
    }
}
