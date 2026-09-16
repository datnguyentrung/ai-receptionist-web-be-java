package com.dat.ai_receptionist_web.service.Finance;

import com.dat.ai_receptionist_web.domain.Finance.WalletTransaction;
import com.dat.ai_receptionist_web.dto.Finance.WalletTransactionDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.FinanceErrorCode;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.mapper.Finance.WalletTransactionMapper;
import com.dat.ai_receptionist_web.repository.Finance.WalletTransactionRepository;
import com.dat.ai_receptionist_web.repository.Finance.WalletRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.enums.Finance.WalletTransactionStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletTransactionService {
    private final WalletTransactionRepository repository;
    private final WalletTransactionMapper mapper;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<WalletTransactionDTO.SimpleResponse> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<WalletTransactionDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toSimpleResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về WalletTransactionDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public WalletTransactionDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận WalletTransactionDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về WalletTransactionDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public WalletTransactionDTO.Response create(WalletTransactionDTO.CreateRequest request) {
        WalletTransaction entity = new WalletTransaction();
        entity.setWallet(walletRepository.findById(request.walletId()).orElseThrow(() -> new ApiException(FinanceErrorCode.WALLET_NOT_FOUND)));
        entity.setCreatedByUser(userRepository.findById(request.createdByUserId()).orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND)));
        entity.setType(request.type());
        entity.setDirection(request.direction());
        entity.setAmount(request.amount());
        entity.setBalanceBefore(request.balanceBefore());
        entity.setBalanceAfter(request.balanceAfter());
        entity.setExternalReference(request.externalReference());
        entity.setReviewedAt(request.reviewedAt());
        entity.setNote(request.note());
        entity.setStatus(WalletTransactionStatus.PENDING);
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, WalletTransactionDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về WalletTransactionDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public WalletTransactionDTO.Response update(UUID id, WalletTransactionDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setWallet(walletRepository.findById(request.walletId()).orElseThrow(() -> new ApiException(FinanceErrorCode.WALLET_NOT_FOUND)));
        entity.setCreatedByUser(userRepository.findById(request.createdByUserId()).orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND)));

        if (request.reviewedByUserId() != null && (request.status() != null && request.status() != WalletTransactionStatus.PENDING)) {
            entity.setReviewedByUser(userRepository.findById(request.reviewedByUserId()).orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND)));
        }
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        entity.setStatus(WalletTransactionStatus.REJECTED);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về WalletTransaction theo kết quả xử lý.
     */
    private WalletTransaction find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(FinanceErrorCode.WALLET_TRANSACTION_NOT_FOUND));
    }
}


