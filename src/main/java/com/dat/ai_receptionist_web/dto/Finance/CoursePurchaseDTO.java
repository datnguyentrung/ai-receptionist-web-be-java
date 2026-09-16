package com.dat.ai_receptionist_web.dto.Finance;

import com.dat.ai_receptionist_web.dto.Catalog.CoursePriceDTO;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import jakarta.validation.constraints.*;
import java.util.UUID;

public final class CoursePurchaseDTO {
    private CoursePurchaseDTO() {
    }

    public record CreateRequest(@NotNull UUID studentPersonId, @NotNull UUID coursePriceId, @NotNull UUID debitTransactionId) {
    }

    public record UpdateRequest(@NotNull UUID studentPersonId, @NotNull UUID coursePriceId, @NotNull UUID debitTransactionId) {
    }

    public record Response(UUID coursePurchaseId, PersonDTO.Response studentPerson, CoursePriceDTO.Response coursePrice, UUID debitTransactionId) {
    }

    public record SimpleResponse(UUID coursePurchaseId, PersonDTO.SimpleResponse studentPerson, CoursePriceDTO.SimpleResponse coursePrice, UUID debitTransactionId) {
    }
}
