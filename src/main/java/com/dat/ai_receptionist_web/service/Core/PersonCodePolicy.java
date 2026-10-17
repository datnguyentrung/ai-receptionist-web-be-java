package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.enums.Core.PersonKind;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Validates the VQ_ code namespace and derives person kind from business position.
 */
@Component
public class PersonCodePolicy {
    private static final String STUDENT_POSITION_CODE = "STUDENT";
    private static final Set<String> STAFF_POSITION_CODES = Set.of(
            "COACH_JUNIOR", "COACH_MIDDLE", "COACH_SENIOR",
            "ASSISTANT_1", "ASSISTANT_2", "ASSISTANT_3", "MANAGER_1"
    );

    public PersonKind classify(Person person) {
        if (person == null) {
            throw violation();
        }
        validateFormat(person.getPersonCode());
        if (person.getPosition() == null || person.getPosition().getCode() == null) {
            throw violation();
        }
        String positionCode = person.getPosition().getCode().trim().toUpperCase(Locale.ROOT);
        if (STUDENT_POSITION_CODE.equals(positionCode)) {
            return PersonKind.STUDENT;
        }
        if (person.getPosition().isActive() && STAFF_POSITION_CODES.contains(positionCode)) {
            return PersonKind.SYSTEM_EMPLOYEE;
        }
        throw violation();
    }

    public void validateFormat(String personCode) {
        if (personCode == null || personCode.isBlank()) {
            throw violation();
        }
        String code = personCode.trim().toUpperCase(Locale.ROOT);
        if (!code.startsWith("VQ_")) {
            throw violation();
        }
    }

    public void requireStudent(Person person) {
        requireKind(person, PersonKind.STUDENT);
    }

    public void requireSystemEmployee(Person person) {
        requireKind(person, PersonKind.SYSTEM_EMPLOYEE);
    }

    private void requireKind(Person person, PersonKind expected) {
        if (classify(person) != expected) {
            throw violation();
        }
    }

    private ApiException violation() {
        return new ApiException(CoreErrorCode.PERSON_CODE_POLICY_VIOLATION);
    }
}
