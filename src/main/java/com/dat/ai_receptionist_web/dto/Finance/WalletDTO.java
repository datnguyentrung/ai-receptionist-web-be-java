package com.dat.ai_receptionist_web.dto.Finance;

import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import com.dat.ai_receptionist_web.enums.Finance.WalletStatus;
import java.math.BigDecimal;
import java.util.UUID;

public final class WalletDTO {
    private WalletDTO() {
    }

    public record CreateRequest(@NotNull UUID personId, @NotNull BigDecimal balance, @NotNull WalletStatus status) {
    }

    public record UpdateRequest(@NotNull UUID personId, @NotNull BigDecimal balance, @NotNull WalletStatus status) {
    }

    public record Response(UUID walletId, PersonDTO.Response person, BigDecimal balance, WalletStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record SimpleResponse(UUID walletId, PersonDTO.SimpleResponse person, BigDecimal balance, WalletStatus status) {
    }
}
