package com.dat.ai_receptionist_web.service.Finance;

import com.dat.ai_receptionist_web.domain.Catalog.*;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Core.Position;
import com.dat.ai_receptionist_web.domain.Finance.*;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Finance.WalletCommandDTO;
import com.dat.ai_receptionist_web.enums.Catalog.*;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import com.dat.ai_receptionist_web.enums.Core.Weekday;
import com.dat.ai_receptionist_web.enums.Finance.*;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.mapper.Finance.WalletCommandMapper;
import com.dat.ai_receptionist_web.repository.Catalog.*;
import com.dat.ai_receptionist_web.repository.Finance.*;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.service.Core.PersonCodePolicy;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletCommandServiceTest {
    @Test
    void trainingPlanPurchaseCreatesOnlyTheReplacementPurchaseShape() {
        WalletRepository wallets = mock(WalletRepository.class);
        WalletTransactionRepository transactions = mock(WalletTransactionRepository.class);
        CoursePurchaseRepository purchases = mock(CoursePurchaseRepository.class);
        TrainingPlanRepository plans = mock(TrainingPlanRepository.class);
        TrainingPlanRequirementRepository requirements = mock(TrainingPlanRequirementRepository.class);
        TrainingPlanAccessRuleRepository accessRules = mock(TrainingPlanAccessRuleRepository.class);
        CourseScheduleRepository schedules = mock(CourseScheduleRepository.class);
        StudentEnrollmentRepository enrollments = mock(StudentEnrollmentRepository.class);
        CurrentAccessContextResolver access = mock(CurrentAccessContextResolver.class);
        UserRepository users = mock(UserRepository.class);
        WalletCommandService service = new WalletCommandService(wallets, transactions, purchases, plans,
                requirements, accessRules, schedules, enrollments, access, users,
                new PersonCodePolicy(), new WalletCommandMapper() {});

        UUID studentId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID requirementId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        LocalDate start = LocalDate.now();
        Person student = Person.builder().personId(studentId).personCode("VQ_anv_010100")
                .position(Position.builder().code("STUDENT").active(true).build()).build();
        Wallet wallet = Wallet.builder().walletId(UUID.randomUUID()).person(student)
                .balance(new BigDecimal("1000000")).status(WalletStatus.ACTIVE).build();
        TrainingPlan plan = TrainingPlan.builder().trainingPlanId(planId).durationMonths(3)
                .finalPrice(new BigDecimal("800000")).status(TrainingPlanStatus.ACTIVE).build();
        Course course = Course.builder().courseId(courseId).status(CourseStatus.ACTIVE).build();
        TrainingPlanRequirement requirement = TrainingPlanRequirement.builder()
                .trainingPlanRequirementId(requirementId).trainingPlan(plan)
                .sessionType(SessionType.STANDARD).sessionsPerWeek(1).build();
        CourseSchedule schedule = CourseSchedule.builder().courseScheduleId(scheduleId).course(course)
                .sessionType(SessionType.STANDARD).status(ScheduleStatus.ACTIVE)
                .startDate(start.minusDays(1)).endDate(start.plusMonths(4)).build();

        when(access.current()).thenReturn(new AccessContext(actorId, null, studentId, null, Set.of(), Set.of()));
        when(wallets.findByPersonIdForUpdate(studentId)).thenReturn(Optional.of(wallet));
        when(transactions.findByTypeAndExternalReference(WalletTransactionType.TRAINING_PLAN_PURCHASE, "plan-001"))
                .thenReturn(Optional.empty());
        when(plans.findWithLockByTrainingPlanIdAndStatus(planId, TrainingPlanStatus.ACTIVE)).thenReturn(Optional.of(plan));
        when(requirements.findByTrainingPlan_TrainingPlanId(planId)).thenReturn(List.of(requirement));
        when(schedules.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(accessRules.existsByTrainingPlan_TrainingPlanIdAndTargetCourse_CourseIdAndSessionType(
                planId, courseId, SessionType.STANDARD)).thenReturn(true);
        when(users.findById(actorId)).thenReturn(Optional.of(User.builder().userId(actorId).build()));
        when(transactions.save(any())).thenAnswer(call -> {
            WalletTransaction transaction = call.getArgument(0);
            transaction.setWalletTransactionId(UUID.randomUUID());
            return transaction;
        });
        when(purchases.save(any())).thenAnswer(call -> {
            CoursePurchase purchase = call.getArgument(0);
            purchase.setCoursePurchaseId(UUID.randomUUID());
            return purchase;
        });
        when(enrollments.save(any())).thenAnswer(call -> {
            StudentEnrollment enrollment = call.getArgument(0);
            enrollment.setStudentEnrollmentId(UUID.randomUUID());
            return enrollment;
        });

        WalletCommandDTO.TransactionResponse result = service.purchaseTrainingPlan(
                new WalletCommandDTO.TrainingPlanPurchaseRequest(studentId, planId, start,
                        List.of(new WalletCommandDTO.RecurringScheduleSelection(requirementId, scheduleId)), "plan-001", null),
                actorId);

        assertThat(result.type()).isEqualTo(WalletTransactionType.TRAINING_PLAN_PURCHASE);
        assertThat(wallet.getBalance()).isEqualByComparingTo("200000");
        verify(purchases).save(argThat(purchase -> purchase.getTrainingPlan() == plan));
    }

    @Test
    void trainingPlanPurchaseReturnsSpecificErrorForOverlappingSchedules() {
        WalletRepository wallets = mock(WalletRepository.class);
        WalletTransactionRepository transactions = mock(WalletTransactionRepository.class);
        CoursePurchaseRepository purchases = mock(CoursePurchaseRepository.class);
        TrainingPlanRepository plans = mock(TrainingPlanRepository.class);
        TrainingPlanRequirementRepository requirements = mock(TrainingPlanRequirementRepository.class);
        TrainingPlanAccessRuleRepository accessRules = mock(TrainingPlanAccessRuleRepository.class);
        CourseScheduleRepository schedules = mock(CourseScheduleRepository.class);
        StudentEnrollmentRepository enrollments = mock(StudentEnrollmentRepository.class);
        CurrentAccessContextResolver access = mock(CurrentAccessContextResolver.class);
        UserRepository users = mock(UserRepository.class);
        WalletCommandService service = new WalletCommandService(wallets, transactions, purchases, plans,
                requirements, accessRules, schedules, enrollments, access, users,
                new PersonCodePolicy(), new WalletCommandMapper() {});

        UUID studentId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID standardRequirementId = UUID.randomUUID();
        UUID requiredRequirementId = UUID.randomUUID();
        UUID standardScheduleId = UUID.randomUUID();
        UUID requiredScheduleId = UUID.randomUUID();
        LocalDate start = LocalDate.now();
        Person student = Person.builder().personId(studentId).personCode("VQ_anv_010100")
                .position(Position.builder().code("STUDENT").active(true).build()).build();
        Wallet wallet = Wallet.builder().walletId(UUID.randomUUID()).person(student)
                .balance(new BigDecimal("5000000")).status(WalletStatus.ACTIVE).build();
        TrainingPlan plan = TrainingPlan.builder().trainingPlanId(planId).durationMonths(3)
                .finalPrice(new BigDecimal("3000000")).status(TrainingPlanStatus.ACTIVE).build();
        Course course = Course.builder().courseId(courseId).status(CourseStatus.ACTIVE).build();
        TrainingPlanRequirement standardRequirement = TrainingPlanRequirement.builder()
                .trainingPlanRequirementId(standardRequirementId).trainingPlan(plan)
                .sessionType(SessionType.STANDARD).sessionsPerWeek(1).build();
        TrainingPlanRequirement requiredRequirement = TrainingPlanRequirement.builder()
                .trainingPlanRequirementId(requiredRequirementId).trainingPlan(plan)
                .sessionType(SessionType.REQUIRED).sessionsPerWeek(1).build();
        ClassSchedule standardClassSchedule = ClassSchedule.builder().scheduleId(UUID.randomUUID())
                .weekday(Weekday.SATURDAY).startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 30)).status(ScheduleStatus.ACTIVE).build();
        ClassSchedule requiredClassSchedule = ClassSchedule.builder().scheduleId(UUID.randomUUID())
                .weekday(Weekday.SATURDAY).startTime(LocalTime.of(18, 30))
                .endTime(LocalTime.of(20, 0)).status(ScheduleStatus.ACTIVE).build();
        CourseSchedule standardSchedule = CourseSchedule.builder().courseScheduleId(standardScheduleId)
                .course(course).classSchedule(standardClassSchedule).sessionType(SessionType.STANDARD)
                .status(ScheduleStatus.ACTIVE).startDate(start.minusDays(1)).endDate(start.plusMonths(4)).build();
        CourseSchedule requiredSchedule = CourseSchedule.builder().courseScheduleId(requiredScheduleId)
                .course(course).classSchedule(requiredClassSchedule).sessionType(SessionType.REQUIRED)
                .status(ScheduleStatus.ACTIVE).startDate(start.minusDays(1)).endDate(start.plusMonths(4)).build();

        when(access.current()).thenReturn(new AccessContext(actorId, null, studentId, null, Set.of(), Set.of()));
        when(wallets.findByPersonIdForUpdate(studentId)).thenReturn(Optional.of(wallet));
        when(transactions.findByTypeAndExternalReference(WalletTransactionType.TRAINING_PLAN_PURCHASE, "overlap-001"))
                .thenReturn(Optional.empty());
        when(plans.findWithLockByTrainingPlanIdAndStatus(planId, TrainingPlanStatus.ACTIVE)).thenReturn(Optional.of(plan));
        when(requirements.findByTrainingPlan_TrainingPlanId(planId))
                .thenReturn(List.of(standardRequirement, requiredRequirement));
        when(schedules.findById(standardScheduleId)).thenReturn(Optional.of(standardSchedule));
        when(schedules.findById(requiredScheduleId)).thenReturn(Optional.of(requiredSchedule));
        when(accessRules.existsByTrainingPlan_TrainingPlanIdAndTargetCourse_CourseIdAndSessionType(
                eq(planId), eq(courseId), any(SessionType.class))).thenReturn(true);

        var request = new WalletCommandDTO.TrainingPlanPurchaseRequest(studentId, planId, start,
                List.of(
                        new WalletCommandDTO.RecurringScheduleSelection(standardRequirementId, standardScheduleId),
                        new WalletCommandDTO.RecurringScheduleSelection(requiredRequirementId, requiredScheduleId)),
                "overlap-001", null);

        assertThatThrownBy(() -> service.purchaseTrainingPlan(request, actorId))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(CatalogErrorCode.TRAINING_PLAN_SCHEDULE_CONFLICT));
        verify(transactions, never()).save(any());
    }
}
