package com.dat.ai_receptionist_web.controller.Finance;

import com.dat.ai_receptionist_web.dto.Finance.WalletCommandDTO;
import com.dat.ai_receptionist_web.service.Finance.WalletCommandService;
import com.dat.ai_receptionist_web.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletCommandController {
    private final WalletCommandService commandService;

    @PostMapping("/training-plan-purchases")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COURSE_PURCHASE_CREATE.getCode())")
    public WalletCommandDTO.TransactionResponse purchaseTrainingPlan(
            @Valid @RequestBody WalletCommandDTO.TrainingPlanPurchaseRequest request) {
        return commandService.purchaseTrainingPlan(request, currentUserId());
    }

    /**
     * Tác dụng: Thực hiện logic refund của lớp hiện tại.
     * Input: Nhận WalletCommandDTO.RefundRequest request từ caller hoặc request.
     * Output: Trả về WalletCommandDTO.TransactionResponse theo kết quả xử lý.
     */
    @PostMapping("/refunds")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).WALLET_REFUND_CREATE.getCode())")
    public WalletCommandDTO.TransactionResponse refund(
            @Valid @RequestBody WalletCommandDTO.RefundRequest request) {
        return commandService.refund(request, currentUserId());
    }

    /**
     * Tác dụng: Lấy thông tin hiện tại từ ngữ cảnh bảo mật của request đang xử lý.
     * Input: Không có tham số đầu vào.
     * Output: Trả về UUID theo kết quả xử lý.
     */
    private UUID currentUserId() {
        return SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Missing authenticated user"));
    }
}


