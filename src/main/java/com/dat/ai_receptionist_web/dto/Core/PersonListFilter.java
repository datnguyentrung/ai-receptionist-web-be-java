package com.dat.ai_receptionist_web.dto.Core;

import com.dat.ai_receptionist_web.enums.Core.Belt;
import com.dat.ai_receptionist_web.enums.Core.PersonStatus;

import java.util.UUID;

public record PersonListFilter(
        UUID positionId,
        String search,
        PersonStatus status,
        Belt currentBelt,
        Boolean gender,
        Boolean isStudent
) {
}
