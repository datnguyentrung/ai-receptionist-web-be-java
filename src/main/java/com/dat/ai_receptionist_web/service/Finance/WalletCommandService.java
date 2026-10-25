package com.dat.ai_receptionist_web.service.Finance;

import com.dat.ai_receptionist_web.domain.Catalog.*;
import com.dat.ai_receptionist_web.domain.Finance.*;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollmentSchedule;
import com.dat.ai_receptionist_web.dto.Finance.WalletCommandDTO;
import com.dat.ai_receptionist_web.enums.Catalog.*;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import com.dat.ai_receptionist_web.enums.Finance.*;
import com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus;
import com.dat.ai_receptionist_web.repository.Catalog.*;
import com.dat.ai_receptionist_web.repository.Finance.*;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.service.Core.PersonCodePolicy;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.ErrorCode;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.error.code.FinanceErrorCode;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.mapper.Finance.WalletCommandMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import java.util.LinkedHashSet;

@Service
@RequiredArgsConstructor
public class WalletCommandService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final CoursePurchaseRepository purchaseRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final TrainingPlanRequirementRepository trainingPlanRequirementRepository;
    private final TrainingPlanAccessRuleRepository trainingPlanAccessRuleRepository;
    private final CourseScheduleRepository courseScheduleRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final CurrentAccessContextResolver accessContextResolver;
    private final UserRepository userRepository;
    private final PersonCodePolicy personCodePolicy;
    private final WalletCommandMapper walletCommandMapper;

    /**
     * Tác dụng: Chuyển đổi dữ liệu sang kiểu kết quả phù hợp cho lớp đang xử lý.
     * Input: Nhận WalletCommandDTO.TopUpRequest request, UUID actorUserId từ caller hoặc request.
     * Output: Trả về WalletCommandDTO.TransactionResponse theo kết quả xử lý.
     */
    @Transactional
    public WalletCommandDTO.TransactionResponse topUp(
            WalletCommandDTO.TopUpRequest request, UUID actorUserId) {
        BigDecimal amount = money(request.amount());
        Wallet wallet = lockWalletByPerson(request.personId());
        WalletTransaction existing = transactionRepository
                .findByTypeAndExternalReference(WalletTransactionType.TOP_UP, request.externalReference())
                .orElse(null);
        if (existing != null) {
            assertSameOperation(existing, wallet, amount);
            return walletCommandMapper.toTransactionResponse(existing, null, null);
        }
        requireActive(wallet);
        User actor = user(actorUserId);
        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(amount);
        WalletTransaction transaction = approvedTransaction(wallet, WalletTransactionType.TOP_UP,
                WalletTransactionDirection.CREDIT, amount, before, after,
                request.externalReference(), actor, request.note());
        wallet.setBalance(after);
        transactionRepository.save(transaction);
        return walletCommandMapper.toTransactionResponse(transaction, null, null);
    }

    @Transactional
    public WalletCommandDTO.TransactionResponse purchaseTrainingPlan(
            WalletCommandDTO.TrainingPlanPurchaseRequest request, UUID actorUserId) {
        requireStudentPurchaseScope(request.studentPersonId());
        Wallet wallet = lockWalletByPerson(request.studentPersonId());
        WalletTransaction duplicate = transactionRepository.findByTypeAndExternalReference(
                WalletTransactionType.TRAINING_PLAN_PURCHASE, request.externalReference()).orElse(null);
        if (duplicate != null) {
            CoursePurchase purchase = purchaseRepository.findByDebitTransaction_WalletTransactionId(duplicate.getWalletTransactionId())
                    .orElseThrow(() -> failure(FinanceErrorCode.LEDGER_INVARIANT_VIOLATION));
            if (!duplicate.getWallet().getWalletId().equals(wallet.getWalletId()) || purchase.getTrainingPlan() == null
                    || !purchase.getTrainingPlan().getTrainingPlanId().equals(request.trainingPlanId())) {
                throw failure(FinanceErrorCode.IDEMPOTENCY_CONFLICT);
            }
            StudentEnrollment enrollment = enrollmentRepository.findByCoursePurchase_CoursePurchaseId(purchase.getCoursePurchaseId())
                    .orElseThrow(() -> failure(FinanceErrorCode.LEDGER_INVARIANT_VIOLATION));
            return walletCommandMapper.toTransactionResponse(duplicate, purchase, enrollment);
        }
        requireActive(wallet);
        personCodePolicy.requireStudent(wallet.getPerson());
        TrainingPlan plan = trainingPlanRepository.findWithLockByTrainingPlanIdAndStatus(
                        request.trainingPlanId(), TrainingPlanStatus.ACTIVE)
                .orElseThrow(() -> failure(CatalogErrorCode.TRAINING_PLAN_NOT_FOUND));
        LocalDate start = request.startDate();
        LocalDate end = start.plusMonths(plan.getDurationMonths());
        LinkedHashSet<StudentEnrollmentSchedule> schedules = resolveTrainingPlanSchedules(
                plan, request.recurringSchedules(), start, end);
        BigDecimal amount = money(plan.getFinalPrice());
        if (wallet.getBalance().compareTo(amount) < 0) throw failure(FinanceErrorCode.INSUFFICIENT_BALANCE);
        User actor = user(actorUserId);
        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.subtract(amount);
        WalletTransaction transaction = transactionRepository.save(approvedTransaction(wallet,
                WalletTransactionType.TRAINING_PLAN_PURCHASE, WalletTransactionDirection.DEBIT, amount,
                before, after, request.externalReference(), actor, request.note()));
        CoursePurchase purchase = purchaseRepository.save(CoursePurchase.builder().studentPerson(wallet.getPerson())
                .trainingPlan(plan).debitTransaction(transaction).build());
        StudentEnrollment enrollment = StudentEnrollment.builder().studentPerson(wallet.getPerson())
                .coursePurchase(purchase).startDate(start).endDate(end)
                .status(start.isAfter(LocalDate.now()) ? StudentEnrollmentStatus.PENDING_START : StudentEnrollmentStatus.ACTIVE)
                .build();
        enrollment.replaceSchedules(schedules);
        enrollment = enrollmentRepository.save(enrollment);
        wallet.setBalance(after);
        return walletCommandMapper.toTransactionResponse(transaction, purchase, enrollment);
    }

    private void requireStudentPurchaseScope(UUID studentPersonId) {
        var context = accessContextResolver.current();
        if (!context.hasUnrestrictedRole() && !studentPersonId.equals(context.activePersonId())) {
            throw failure(SecurityErrorCode.ACCESS_DENIED);
        }
    }

    private LinkedHashSet<StudentEnrollmentSchedule> resolveTrainingPlanSchedules(
            TrainingPlan plan, java.util.List<WalletCommandDTO.RecurringScheduleSelection> selections,
            LocalDate start, LocalDate end) {
        var requirements = trainingPlanRequirementRepository.findByTrainingPlan_TrainingPlanId(
                plan.getTrainingPlanId());
        var byId = requirements.stream().collect(java.util.stream.Collectors.toMap(
                TrainingPlanRequirement::getTrainingPlanRequirementId, requirement -> requirement));
        var selected = new java.util.LinkedHashMap<UUID, java.util.List<UUID>>();
        var selectedScheduleIds = new java.util.HashSet<UUID>();
        for (WalletCommandDTO.RecurringScheduleSelection selection : selections) {
            if (!byId.containsKey(selection.requirementId())) throw failure(CatalogErrorCode.TRAINING_PLAN_REQUIREMENT_NOT_MET);
            selected.computeIfAbsent(selection.requirementId(), ignored -> new java.util.ArrayList<>()).add(selection.courseScheduleId());
        }
        LinkedHashSet<StudentEnrollmentSchedule> result = new LinkedHashSet<>();
        for (TrainingPlanRequirement requirement : requirements) {
            var ids = selected.getOrDefault(requirement.getTrainingPlanRequirementId(), java.util.List.of());
            if (ids.size() != requirement.getSessionsPerWeek() || new LinkedHashSet<>(ids).size() != ids.size()) {
                throw failure(CatalogErrorCode.TRAINING_PLAN_REQUIREMENT_NOT_MET);
            }
            for (UUID id : ids) {
                if (!selectedScheduleIds.add(id)) {
                    throw failure(CatalogErrorCode.TRAINING_PLAN_REQUIREMENT_NOT_MET);
                }
                CourseSchedule schedule = courseScheduleRepository.findById(id)
                        .orElseThrow(() -> failure(CatalogErrorCode.COURSE_SCHEDULE_NOT_FOUND));
                boolean applicable = schedule.getStatus() == ScheduleStatus.ACTIVE
                        && !schedule.getStartDate().isAfter(end)
                        && (schedule.getEndDate() == null || !schedule.getEndDate().isBefore(start))
                        && trainingPlanAccessRuleRepository.existsByTrainingPlan_TrainingPlanIdAndTargetCourse_CourseIdAndSessionType(
                        plan.getTrainingPlanId(), schedule.getCourse().getCourseId(), requirement.getSessionType());
                if (!applicable) throw failure(CatalogErrorCode.TRAINING_PLAN_REQUIREMENT_NOT_MET);
                result.add(StudentEnrollmentSchedule.builder().courseSchedule(schedule).requirement(requirement)
                        .effectiveFrom(start).effectiveTo(end)
                        .status(com.dat.ai_receptionist_web.enums.Training.EnrollmentScheduleStatus.ACTIVE).build());
            }
        }
        requireNoScheduleTimeConflicts(result);
        return result;
    }

    private void requireNoScheduleTimeConflicts(LinkedHashSet<StudentEnrollmentSchedule> schedules) {
        var selected = new java.util.ArrayList<>(schedules);
        for (int left = 0; left < selected.size(); left++) {
            CourseSchedule first = selected.get(left).getCourseSchedule();
            for (int right = left + 1; right < selected.size(); right++) {
                CourseSchedule second = selected.get(right).getCourseSchedule();
                if (first.getClassSchedule().getWeekday() == second.getClassSchedule().getWeekday()
                        && first.getClassSchedule().getStartTime().isBefore(second.getClassSchedule().getEndTime())
                        && second.getClassSchedule().getStartTime().isBefore(first.getClassSchedule().getEndTime())) {
                    throw failure(CatalogErrorCode.TRAINING_PLAN_SCHEDULE_CONFLICT);
                }
            }
        }
    }

    /**
     * Tác dụng: Thực hiện logic refund của lớp hiện tại.
     * Input: Nhận WalletCommandDTO.RefundRequest request, UUID actorUserId từ caller hoặc request.
     * Output: Trả về WalletCommandDTO.TransactionResponse theo kết quả xử lý.
     */
    @Transactional
    public WalletCommandDTO.TransactionResponse refund(
            WalletCommandDTO.RefundRequest request, UUID actorUserId) {
        WalletTransaction original = transactionRepository
                .findWithWalletById(request.originalDebitTransactionId())
                .orElseThrow(() -> failure(FinanceErrorCode.TRANSACTION_NOT_FOUND));
        if (original.getDirection() != WalletTransactionDirection.DEBIT
                || original.getStatus() != WalletTransactionStatus.APPROVED) {
            throw failure(FinanceErrorCode.INVALID_REFUND_TRANSACTION);
        }
        Wallet wallet = walletRepository.findByIdForUpdate(original.getWallet().getWalletId())
                .orElseThrow(() -> failure(FinanceErrorCode.WALLET_NOT_FOUND));
        requireActive(wallet);
        String originalReference = original.getWalletTransactionId().toString();
        WalletTransaction existing = transactionRepository
                .findByTypeAndExternalReference(WalletTransactionType.REFUND, originalReference)
                .orElse(null);
        if (existing != null) {
            return walletCommandMapper.toTransactionResponse(existing, null, null);
        }
        BigDecimal amount = original.getAmount();
        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(amount);
        User actor = user(actorUserId);
        WalletTransaction refund = transactionRepository.save(approvedTransaction(
                wallet, WalletTransactionType.REFUND, WalletTransactionDirection.CREDIT,
                amount, before, after, originalReference, actor, request.note()));
        revokeCourseEntitlement(original);
        wallet.setBalance(after);
        return walletCommandMapper.toTransactionResponse(refund, null, null);
    }

    /**
     * Tác dụng: Thu hồi phiên hoặc quyền truy cập theo điều kiện đầu vào.
     * Input: Nhận WalletTransaction original từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    private void revokeCourseEntitlement(WalletTransaction original) {
        if (original.getType() != WalletTransactionType.TRAINING_PLAN_PURCHASE) {
            return;
        }
        CoursePurchase purchase = purchaseRepository
                .findByDebitTransaction_WalletTransactionId(original.getWalletTransactionId())
                .orElseThrow(() -> failure(FinanceErrorCode.LEDGER_INVARIANT_VIOLATION,
                        "Purchase record is missing"));
        StudentEnrollment enrollment = enrollmentRepository
                .findByCoursePurchase_CoursePurchaseId(purchase.getCoursePurchaseId())
                .orElseThrow(() -> failure(FinanceErrorCode.LEDGER_INVARIANT_VIOLATION,
                        "Enrollment record is missing"));
        enrollment.setStatus(StudentEnrollmentStatus.CANCELLED);
        LocalDate today = LocalDate.now();
        if (enrollment.getEndDate().isAfter(today)) {
            enrollment.setEndDate(today);
        }
    }

    /**
     * Tác dụng: Thực hiện logic approvedTransaction của lớp hiện tại.
     * Input: Nhận Wallet wallet, WalletTransactionType type, WalletTransactionDirection direction, BigDecimal amount, BigDecimal before, BigDecimal after, String reference, User actor, String note từ caller hoặc request.
     * Output: Trả về WalletTransaction theo kết quả xử lý.
     */
    private WalletTransaction approvedTransaction(Wallet wallet, WalletTransactionType type,
                                                   WalletTransactionDirection direction,
                                                   BigDecimal amount, BigDecimal before, BigDecimal after,
                                                   String reference, User actor, String note) {
        LocalDateTime now = LocalDateTime.now();
        return WalletTransaction.builder().wallet(wallet).type(type).direction(direction)
                .amount(amount).balanceBefore(before).balanceAfter(after)
                .externalReference(reference).createdByUser(actor).reviewedByUser(actor)
                .reviewedAt(now).note(note).status(WalletTransactionStatus.APPROVED).build();
    }

    /**
     * Tác dụng: Thực hiện logic lockWalletByPerson của lớp hiện tại.
     * Input: Nhận UUID personId từ caller hoặc request.
     * Output: Trả về Wallet theo kết quả xử lý.
     */
    private Wallet lockWalletByPerson(UUID personId) {
        return walletRepository.findByPersonIdForUpdate(personId)
                .orElseThrow(() -> failure(FinanceErrorCode.WALLET_NOT_FOUND));
    }

    /**
     * Tác dụng: Thực hiện logic requireActive của lớp hiện tại.
     * Input: Nhận Wallet wallet từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    private void requireActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw failure(FinanceErrorCode.WALLET_NOT_ACTIVE);
        }
    }

    /**
     * Tác dụng: Thực hiện logic user của lớp hiện tại.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về User theo kết quả xử lý.
     */
    private User user(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> failure(SecurityErrorCode.USER_NOT_FOUND));
    }

    /**
     * Tác dụng: Thực hiện logic money của lớp hiện tại.
     * Input: Nhận BigDecimal amount từ caller hoặc request.
     * Output: Trả về BigDecimal theo kết quả xử lý.
     */
    private BigDecimal money(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0) {
            throw failure(FinanceErrorCode.INVALID_AMOUNT);
        }
        return amount.setScale(0);
    }

    /**
     * Tác dụng: Thực hiện logic assertSameOperation của lớp hiện tại.
     * Input: Nhận WalletTransaction existing, Wallet wallet, BigDecimal amount từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    private void assertSameOperation(WalletTransaction existing, Wallet wallet, BigDecimal amount) {
        if (!existing.getWallet().getWalletId().equals(wallet.getWalletId())
                || existing.getAmount().compareTo(amount) != 0) {
            throw failure(FinanceErrorCode.IDEMPOTENCY_CONFLICT);
        }
    }

    /**
     * Tác dụng: Thực hiện logic failure của lớp hiện tại.
     * Input: Nhận String code, HttpStatus status, String message từ caller hoặc request.
     * Output: Trả về FinancialException theo kết quả xử lý.
     */
    private ApiException failure(ErrorCode errorCode) {
        return new ApiException(errorCode);
    }

    private ApiException failure(ErrorCode errorCode, String safeDetail) {
        return new ApiException(errorCode, safeDetail);
    }
}


