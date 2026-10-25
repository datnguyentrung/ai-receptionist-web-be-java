package com.dat.ai_receptionist_web.service.Finance;

import com.dat.ai_receptionist_web.domain.Finance.*;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Finance.WalletTopUpRequestDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.Finance.*;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.FinanceErrorCode;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.repository.Finance.*;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.service.Core.SupabaseStorageService;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletTopUpRequestService {
    private static final BigDecimal MAX_TOP_UP = new BigDecimal("100000000");

    private final WalletTopUpRequestRepository requestRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CurrentAccessContextResolver accessContextResolver;
    private final WalletAccessPolicy accessPolicy;
    private final SupabaseStorageService storageService;

    @Transactional
    public WalletTopUpRequestDTO.Response createGuardian(WalletTopUpRequestDTO.CreateRequest request) {
        return create(request, WalletTopUpRequestMode.GUARDIAN_REQUEST);
    }

    @Transactional
    public WalletTopUpRequestDTO.Response createDirect(WalletTopUpRequestDTO.CreateRequest request) {
        return create(request, WalletTopUpRequestMode.STAFF_DIRECT);
    }

    private WalletTopUpRequestDTO.Response create(
            WalletTopUpRequestDTO.CreateRequest request,
            WalletTopUpRequestMode mode) {
        AccessContext context = accessContextResolver.current();
        if (mode == WalletTopUpRequestMode.GUARDIAN_REQUEST) {
            accessPolicy.requireGuardianOwnsPerson(context, request.personId());
        } else {
            accessPolicy.requireFinanceStaff(context);
        }
        BigDecimal amount = money(request.amount());
        String note = normalize(request.note());
        // A unique constraint catches duplicates, while this transaction-scoped
        // lock makes concurrent first submissions return the same request instead
        // of exposing a transient constraint violation to the client.
        requestRepository.lockClientRequestId(request.clientRequestId());
        WalletTopUpRequest existing = requestRepository.findByClientRequestId(request.clientRequestId()).orElse(null);
        if (existing != null) {
            requireCanRead(context, existing);
            assertSameRequest(existing, request.personId(), amount, note, mode);
            return toResponse(existing);
        }

        Wallet wallet = walletRepository.findByPerson_PersonId(request.personId())
                .orElseThrow(() -> new ApiException(FinanceErrorCode.WALLET_NOT_FOUND));
        requireActive(wallet);
        User actor = user(context.userId());
        WalletTopUpRequest entity = WalletTopUpRequest.builder()
                .clientRequestId(request.clientRequestId())
                .wallet(wallet)
                .requestedByUser(actor)
                .mode(mode)
                .status(WalletTopUpRequestStatus.DRAFT)
                .amount(amount)
                .note(note)
                .build();
        return toResponse(requestRepository.save(entity));
    }

    @Transactional
    public WalletTopUpRequestDTO.ProofResponse uploadProof(UUID id, MultipartFile file) {
        WalletTopUpRequest request = locked(id);
        AccessContext context = accessContextResolver.current();
        requireCanRead(context, request);
        requireDraft(request);

        var image = storageService.validateTopUpProof(file);
        String hash = sha256(image.bytes());
        if (hash.equals(request.getProofSha256())) {
            return new WalletTopUpRequestDTO.ProofResponse(id, true);
        }
        if (StringUtils.hasText(request.getProofSha256())) {
            throw new ApiException(FinanceErrorCode.TOP_UP_PROOF_ALREADY_ATTACHED);
        }
        if (requestRepository.existsByProofSha256AndTopUpRequestIdNot(hash, id)) {
            throw new ApiException(FinanceErrorCode.TOP_UP_PROOF_DUPLICATE);
        }

        String objectPath = storageService.uploadTopUpProof(id, image);
        try {
            request.setProofObjectPath(objectPath);
            request.setProofSha256(hash);
            requestRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException exception) {
            safelyDeleteProof(objectPath);
            throw new ApiException(FinanceErrorCode.TOP_UP_PROOF_DUPLICATE);
        } catch (RuntimeException exception) {
            safelyDeleteProof(objectPath);
            throw exception;
        }
        log.info("Top-up proof attached: requestId={}, actorUserId={}", id, context.userId());
        return new WalletTopUpRequestDTO.ProofResponse(id, true);
    }

    @Transactional
    public WalletTopUpRequestDTO.Response submit(UUID id, WalletTopUpRequestDTO.SubmitRequest command) {
        WalletTopUpRequest request = locked(id);
        AccessContext context = accessContextResolver.current();
        if (request.getStatus() == WalletTopUpRequestStatus.APPROVED
                || request.getStatus() == WalletTopUpRequestStatus.PENDING) {
            requireCanRead(context, request);
            return toResponse(request);
        }
        requireDraft(request);
        requireProof(request);
        requireActive(request.getWallet());
        request.setSubmittedAt(LocalDateTime.now());

        if (request.getMode() == WalletTopUpRequestMode.GUARDIAN_REQUEST) {
            accessPolicy.requireGuardianOwnsPerson(context, request.getWallet().getPerson().getPersonId());
            request.setStatus(WalletTopUpRequestStatus.PENDING);
            log.info("Top-up request submitted: requestId={}, actorUserId={}", id, context.userId());
            return toResponse(request);
        }

        accessPolicy.requireFinanceStaff(context);
        requireReconciled(command);
        return approveLocked(request, context.userId());
    }

    @Transactional
    public WalletTopUpRequestDTO.Response approve(UUID id, WalletTopUpRequestDTO.SubmitRequest command) {
        WalletTopUpRequest request = locked(id);
        AccessContext context = accessContextResolver.current();
        accessPolicy.requireFinanceStaff(context);
        requireReconciled(command);
        if (request.getStatus() == WalletTopUpRequestStatus.APPROVED) return toResponse(request);
        if (request.getStatus() != WalletTopUpRequestStatus.PENDING) {
            throw new ApiException(FinanceErrorCode.TOP_UP_REQUEST_STATE_CONFLICT);
        }
        requireProof(request);
        return approveLocked(request, context.userId());
    }

    private WalletTopUpRequestDTO.Response approveLocked(WalletTopUpRequest request, UUID reviewerId) {
        Wallet wallet = walletRepository.findByIdForUpdate(request.getWallet().getWalletId())
                .orElseThrow(() -> new ApiException(FinanceErrorCode.WALLET_NOT_FOUND));
        requireActive(wallet);
        String reference = externalReference(request.getClientRequestId());
        WalletTransaction existing = transactionRepository
                .findByTypeAndExternalReference(WalletTransactionType.TOP_UP, reference)
                .orElse(null);
        if (existing != null) {
            if (!Objects.equals(existing.getWallet().getWalletId(), wallet.getWalletId())
                    || existing.getAmount().compareTo(request.getAmount()) != 0) {
                throw new ApiException(FinanceErrorCode.IDEMPOTENCY_CONFLICT);
            }
            request.setWalletTransaction(existing);
            markApproved(request, user(reviewerId));
            return toResponse(request);
        }

        User reviewer = user(reviewerId);
        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(request.getAmount());
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .type(WalletTransactionType.TOP_UP)
                .direction(WalletTransactionDirection.CREDIT)
                .amount(request.getAmount())
                .balanceBefore(before)
                .balanceAfter(after)
                .externalReference(reference)
                .createdByUser(request.getRequestedByUser())
                .reviewedByUser(reviewer)
                .reviewedAt(LocalDateTime.now())
                .note(request.getNote())
                .status(WalletTransactionStatus.APPROVED)
                .build();
        transactionRepository.save(transaction);
        wallet.setBalance(after);
        request.setWalletTransaction(transaction);
        markApproved(request, reviewer);
        log.info("Top-up posted: requestId={}, reviewerUserId={}, walletId={}, amount={}",
                request.getTopUpRequestId(), reviewerId, wallet.getWalletId(), request.getAmount());
        return toResponse(request);
    }

    @Transactional
    public WalletTopUpRequestDTO.Response reject(UUID id, WalletTopUpRequestDTO.RejectRequest command) {
        WalletTopUpRequest request = locked(id);
        AccessContext context = accessContextResolver.current();
        accessPolicy.requireFinanceStaff(context);
        if (request.getStatus() == WalletTopUpRequestStatus.REJECTED) return toResponse(request);
        if (request.getStatus() != WalletTopUpRequestStatus.PENDING) {
            throw new ApiException(FinanceErrorCode.TOP_UP_REQUEST_STATE_CONFLICT);
        }
        request.setStatus(WalletTopUpRequestStatus.REJECTED);
        request.setRejectionReason(command.reason().trim());
        request.setReviewedByUser(user(context.userId()));
        request.setReviewedAt(LocalDateTime.now());
        log.info("Top-up rejected: requestId={}, reviewerUserId={}", id, context.userId());
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public WalletTopUpRequestDTO.Response get(UUID id) {
        WalletTopUpRequest request = requestRepository.findDetailedById(id)
                .orElseThrow(() -> new ApiException(FinanceErrorCode.TOP_UP_REQUEST_NOT_FOUND));
        requireCanRead(accessContextResolver.current(), request);
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public WalletTopUpRequestDTO.Response getByClientRequestId(UUID clientRequestId) {
        WalletTopUpRequest request = requestRepository.findByClientRequestId(clientRequestId)
                .orElseThrow(() -> new ApiException(FinanceErrorCode.TOP_UP_REQUEST_NOT_FOUND));
        requireCanRead(accessContextResolver.current(), request);
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public PageResponse<WalletTopUpRequestDTO.Response> listMine(Pageable pageable) {
        UUID userId = accessContextResolver.current().userId();
        return PageResponse.of(
                requestRepository.findByRequestedByUser_UserIdOrderByCreatedAtDesc(userId, pageable),
                this::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<WalletTopUpRequestDTO.Response> listForReview(
            WalletTopUpRequestStatus status, Pageable pageable) {
        accessPolicy.requireFinanceStaff(accessContextResolver.current());
        WalletTopUpRequestStatus requestedStatus = status == null ? WalletTopUpRequestStatus.PENDING : status;
        return PageResponse.of(requestRepository.findByStatusOrderByCreatedAtAsc(requestedStatus, pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public WalletTopUpRequestDTO.ProofUrlResponse proofUrl(UUID id) {
        WalletTopUpRequest request = requestRepository.findDetailedById(id)
                .orElseThrow(() -> new ApiException(FinanceErrorCode.TOP_UP_REQUEST_NOT_FOUND));
        requireCanRead(accessContextResolver.current(), request);
        requireProof(request);
        return new WalletTopUpRequestDTO.ProofUrlResponse(
                storageService.createTopUpProofSignedUrl(request.getProofObjectPath()),
                storageService.topUpProofSignedUrlTtlSeconds());
    }

    @Transactional
    public void purgeExpiredDraft(UUID id, LocalDateTime cutoff) {
        WalletTopUpRequest request = locked(id);
        if (request.getStatus() != WalletTopUpRequestStatus.DRAFT
                || request.getCreatedAt() == null || !request.getCreatedAt().isBefore(cutoff)) return;
        if (StringUtils.hasText(request.getProofObjectPath())) storageService.deleteTopUpProof(request.getProofObjectPath());
        requestRepository.delete(request);
    }

    private void markApproved(WalletTopUpRequest request, User reviewer) {
        request.setStatus(WalletTopUpRequestStatus.APPROVED);
        request.setReviewedByUser(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        request.setRejectionReason(null);
    }

    private void requireCanRead(AccessContext context, WalletTopUpRequest request) {
        if (!accessPolicy.isFinanceStaff(context)
                && !Objects.equals(request.getRequestedByUser().getUserId(), context.userId())) {
            throw new ApiException(SecurityErrorCode.ACCESS_DENIED);
        }
    }

    private void assertSameRequest(WalletTopUpRequest existing, UUID personId, BigDecimal amount,
                                   String note, WalletTopUpRequestMode mode) {
        if (!Objects.equals(existing.getWallet().getPerson().getPersonId(), personId)
                || existing.getAmount().compareTo(amount) != 0
                || !Objects.equals(existing.getNote(), note)
                || existing.getMode() != mode) {
            throw new ApiException(FinanceErrorCode.IDEMPOTENCY_CONFLICT);
        }
    }

    private WalletTopUpRequest locked(UUID id) {
        return requestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(FinanceErrorCode.TOP_UP_REQUEST_NOT_FOUND));
    }

    private User user(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND));
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0) {
            throw new ApiException(FinanceErrorCode.INVALID_AMOUNT);
        }
        if (amount.compareTo(MAX_TOP_UP) > 0) {
            throw new ApiException(FinanceErrorCode.TOP_UP_AMOUNT_LIMIT_EXCEEDED);
        }
        return amount.setScale(0);
    }

    private void requireActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new ApiException(FinanceErrorCode.WALLET_NOT_ACTIVE);
        }
    }

    private void requireDraft(WalletTopUpRequest request) {
        if (request.getStatus() != WalletTopUpRequestStatus.DRAFT) {
            throw new ApiException(FinanceErrorCode.TOP_UP_REQUEST_STATE_CONFLICT);
        }
    }

    private void requireProof(WalletTopUpRequest request) {
        if (!StringUtils.hasText(request.getProofObjectPath()) || !StringUtils.hasText(request.getProofSha256())) {
            throw new ApiException(FinanceErrorCode.TOP_UP_PROOF_REQUIRED);
        }
    }

    private void requireReconciled(WalletTopUpRequestDTO.SubmitRequest command) {
        if (command == null || !command.reconciliationConfirmed()) {
            throw new ApiException(FinanceErrorCode.TOP_UP_RECONCILIATION_REQUIRED);
        }
    }

    private WalletTopUpRequestDTO.Response toResponse(WalletTopUpRequest request) {
        WalletTransaction transaction = request.getWalletTransaction();
        var person = request.getWallet().getPerson();
        return new WalletTopUpRequestDTO.Response(
                request.getTopUpRequestId(), request.getClientRequestId(), person.getPersonId(),
                person.getPersonCode(), person.getFullName(), request.getMode(), request.getStatus(),
                request.getAmount(), request.getNote(), request.getRejectionReason(),
                StringUtils.hasText(request.getProofObjectPath()),
                transaction == null ? null : transaction.getWalletTransactionId(),
                transaction == null ? null : transaction.getBalanceAfter(),
                request.getSubmittedAt(), request.getReviewedAt(), request.getCreatedAt(), request.getUpdatedAt());
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String externalReference(UUID clientRequestId) {
        return "TOPUP-" + clientRequestId;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void safelyDeleteProof(String objectPath) {
        try {
            storageService.deleteTopUpProof(objectPath);
        } catch (RuntimeException cleanupError) {
            log.error("Could not clean up failed top-up proof upload", cleanupError);
        }
    }
}
