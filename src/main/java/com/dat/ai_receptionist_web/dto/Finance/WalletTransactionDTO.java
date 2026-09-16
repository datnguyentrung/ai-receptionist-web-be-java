package com.dat.ai_receptionist_web.dto.Finance;

import com.dat.ai_receptionist_web.dto.Security.UserDTO;
import jakarta.validation.constraints.*;
import com.dat.ai_receptionist_web.enums.Finance.WalletTransactionDirection;
import com.dat.ai_receptionist_web.enums.Finance.WalletTransactionStatus;
import com.dat.ai_receptionist_web.enums.Finance.WalletTransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class WalletTransactionDTO {
    private WalletTransactionDTO() {
    }

    public record CreateRequest(
            @NotNull UUID walletId,                                 // ID của ví liên quan đến giao dịch
            @NotNull UUID createdByUserId,                          // ID của người dùng tạo giao dịch
            UUID reviewedByUserId,                         // ID của người dùng rà soát giao dịch
            @NotNull WalletTransactionType type,                    // Loại giao dịch (ví dụ: nạp tiền, rút tiền, mua khóa học)
            @NotNull WalletTransactionDirection direction,          // Hướng giao dịch (ví dụ: vào ví, ra khỏi ví)
            @NotNull BigDecimal amount,                             // Số tiền liên quan đến giao dịch
            @NotNull BigDecimal balanceBefore,                      // Số dư trước khi giao dịch được thực hiện
            @NotNull BigDecimal balanceAfter,                       // Số dư sau khi giao dịch được thực hiện
            @NotNull String externalReference,                      // Tham chiếu bên ngoài liên quan đến giao dịch (ví dụ: mã giao dịch từ hệ thống thanh toán)
            @NotNull LocalDateTime reviewedAt,                      // Thời gian rà soát giao dịch
            @NotNull String note                                    // Ghi chú hoặc mô tả liên quan đến giao dịch
    ) {
    }

    public record UpdateRequest(
            @NotNull UUID walletId,                                 // ID của ví liên quan đến giao dịch
            @NotNull UUID createdByUserId,                          // ID của người dùng tạo giao dịch
            UUID reviewedByUserId,                         // ID của người dùng rà soát giao dịch
            @NotNull WalletTransactionType type,                    // Loại giao dịch (ví dụ: nạp tiền, rút tiền, mua khóa học)
            @NotNull WalletTransactionDirection direction,          // Hướng giao dịch (ví dụ: vào ví, ra khỏi ví)
            @NotNull BigDecimal amount,                             // Số tiền liên quan đến giao dịch
            @NotNull BigDecimal balanceBefore,                      // Số dư trước khi giao dịch được thực hiện
            @NotNull BigDecimal balanceAfter,                       // Số dư sau khi giao dịch được thực hiện
            @NotNull String externalReference,                      // Tham chiếu bên ngoài liên quan đến giao dịch (ví dụ: mã giao dịch từ hệ thống thanh toán)
            @NotNull LocalDateTime reviewedAt,                      // Thời gian rà soát giao dịch
            @NotNull String note,                                   // Ghi chú hoặc mô tả liên quan đến giao dịch
            @NotNull WalletTransactionStatus status                 // Trạng thái của giao dịch (ví dụ: đang chờ, đã phê duyệt, đã từ chối)
    ) {
    }

    public record Response(
            UUID walletTransactionId,
            WalletDTO.Response wallet,
            UserDTO.SimpleResponse createdByUser,
            UserDTO.SimpleResponse reviewedByUser,
            WalletTransactionType type,
            WalletTransactionDirection direction,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String externalReference,
            LocalDateTime reviewedAt,
            String note,
            WalletTransactionStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record SimpleResponse(
            UUID walletTransactionId,
            WalletDTO.SimpleResponse wallet,
            UserDTO.SimpleResponse createdByUser,
            UserDTO.SimpleResponse reviewedByUser,
            WalletTransactionType type,
            WalletTransactionDirection direction,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String externalReference,
            LocalDateTime reviewedAt,
            WalletTransactionStatus status,
            LocalDateTime createdAt) {
    }
}
