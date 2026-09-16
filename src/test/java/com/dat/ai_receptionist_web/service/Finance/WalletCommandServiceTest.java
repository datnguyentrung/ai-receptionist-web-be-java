package com.dat.ai_receptionist_web.service.Finance;

import com.dat.ai_receptionist_web.domain.Catalog.*;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Finance.*;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Finance.WalletCommandDTO;
import com.dat.ai_receptionist_web.enums.Catalog.*;
import com.dat.ai_receptionist_web.enums.Finance.*;
import com.dat.ai_receptionist_web.mapper.Finance.WalletCommandMapper;
import com.dat.ai_receptionist_web.repository.Catalog.*;
import com.dat.ai_receptionist_web.repository.Finance.*;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.service.Core.PersonCodePolicy;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.FinanceErrorCode;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.*;
import org.springframework.data.jpa.repository.Lock;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletCommandServiceTest {
    private WalletRepository wallets;
    private WalletTransactionRepository transactions;
    private CoursePurchaseRepository purchases;
    private CoursePriceRepository prices;
    private CourseRepository courses;
    private StudentEnrollmentRepository enrollments;
    private UserRepository users;
    private WalletCommandService service;

    @BeforeEach
    void setUp() {
        wallets = mock(WalletRepository.class);
        transactions = mock(WalletTransactionRepository.class);
        purchases = mock(CoursePurchaseRepository.class);
        prices = mock(CoursePriceRepository.class);
        courses = mock(CourseRepository.class);
        enrollments = mock(StudentEnrollmentRepository.class);
        users = mock(UserRepository.class);
        service = new WalletCommandService(wallets, transactions, purchases, prices, courses,
                enrollments, users, new PersonCodePolicy(), new WalletCommandMapper() {
                });
    }

    @Test
    void coursePurchaseDebitsOnceAndCreatesPurchaseAndEnrollmentAtomically() {
        UUID personId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID priceId = UUID.randomUUID();
        Person person = Person.builder().personId(personId).personCode("VQ_anv_010100").build();
        Wallet wallet = Wallet.builder().walletId(UUID.randomUUID()).person(person)
                .balance(new BigDecimal("1000000")).status(WalletStatus.ACTIVE).build();
        ClassSchedule schedule = ClassSchedule.builder().scheduleId(UUID.randomUUID()).build();
        Course course = Course.builder().courseId(UUID.randomUUID()).classSchedule(schedule)
                .capacity(20).status(CourseStatus.ACTIVE).build();
        CoursePrice price = CoursePrice.builder().coursePriceId(priceId).course(course)
                .durationMonths(3).sessionCount(24).finalPrice(new BigDecimal("800000"))
                .status(CoursePriceStatus.ACTIVE).build();
        User actor = User.builder().userId(actorId).build();

        when(wallets.findByPersonIdForUpdate(personId)).thenReturn(Optional.of(wallet));
        when(prices.findForPurchase(priceId)).thenReturn(Optional.of(price));
        when(courses.findByIdForUpdate(course.getCourseId())).thenReturn(Optional.of(course));
        when(transactions.findByTypeAndExternalReference(any(), anyString())).thenReturn(Optional.empty());
        when(enrollments.countByCoursePurchase_CoursePrice_Course_CourseId(course.getCourseId())).thenReturn(0L);
        when(users.findById(actorId)).thenReturn(Optional.of(actor));
        when(transactions.save(any())).thenAnswer(invocation -> {
            WalletTransaction value = invocation.getArgument(0);
            value.setWalletTransactionId(UUID.randomUUID());
            return value;
        });
        when(purchases.save(any())).thenAnswer(invocation -> {
            CoursePurchase value = invocation.getArgument(0);
            value.setCoursePurchaseId(UUID.randomUUID());
            return value;
        });
        when(enrollments.save(any())).thenAnswer(invocation -> {
            StudentEnrollment value = invocation.getArgument(0);
            value.setStudentEnrollmentId(UUID.randomUUID());
            return value;
        });

        WalletCommandDTO.TransactionResponse result = service.purchaseCourse(
                new WalletCommandDTO.CoursePurchaseRequest(personId, priceId, "purchase-001", null), actorId);

        assertThat(result.type()).isEqualTo(WalletTransactionType.COURSE_PURCHASE);
        assertThat(result.direction()).isEqualTo(WalletTransactionDirection.DEBIT);
        assertThat(result.status()).isEqualTo(WalletTransactionStatus.APPROVED);
        assertThat(wallet.getBalance()).isEqualByComparingTo("200000");
        verify(transactions, times(1)).save(any());
        verify(purchases, times(1)).save(any());
        verify(enrollments, times(1)).save(any());
    }

    @Test
    void refundCreatesNewFullCreditAndNeverMutatesOriginal() {
        UUID actorId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        Person person = Person.builder().personId(personId).build();
        Wallet wallet = Wallet.builder().walletId(UUID.randomUUID()).balance(new BigDecimal("200000"))
                .person(person).status(WalletStatus.ACTIVE).build();
        WalletTransaction original = WalletTransaction.builder()
                .walletTransactionId(UUID.randomUUID()).wallet(wallet)
                .type(WalletTransactionType.COURSE_PURCHASE)
                .direction(WalletTransactionDirection.DEBIT)
                .status(WalletTransactionStatus.APPROVED)
                .amount(new BigDecimal("800000")).externalReference("purchase-001").build();
        User actor = User.builder().userId(actorId).build();
        CoursePurchase purchase = CoursePurchase.builder().coursePurchaseId(UUID.randomUUID())
                .studentPerson(person).debitTransaction(original).build();
        StudentEnrollment enrollment = StudentEnrollment.builder().studentPerson(person)
                .coursePurchase(purchase).endDate(java.time.LocalDate.now().plusMonths(2))
                .status(com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus.ACTIVE).build();
        when(transactions.findWithWalletById(original.getWalletTransactionId())).thenReturn(Optional.of(original));
        when(wallets.findByIdForUpdate(wallet.getWalletId())).thenReturn(Optional.of(wallet));
        when(transactions.findByTypeAndExternalReference(WalletTransactionType.REFUND,
                original.getWalletTransactionId().toString())).thenReturn(Optional.empty());
        when(users.findById(actorId)).thenReturn(Optional.of(actor));
        when(purchases.findByDebitTransaction_WalletTransactionId(original.getWalletTransactionId()))
                .thenReturn(Optional.of(purchase));
        when(enrollments.findByCoursePurchase_CoursePurchaseId(purchase.getCoursePurchaseId()))
                .thenReturn(Optional.of(enrollment));
        when(transactions.save(any())).thenAnswer(invocation -> {
            WalletTransaction value = invocation.getArgument(0);
            value.setWalletTransactionId(UUID.randomUUID());
            return value;
        });

        WalletCommandDTO.TransactionResponse result = service.refund(
                new WalletCommandDTO.RefundRequest(original.getWalletTransactionId(), "approved"), actorId);

        assertThat(result.type()).isEqualTo(WalletTransactionType.REFUND);
        assertThat(result.direction()).isEqualTo(WalletTransactionDirection.CREDIT);
        assertThat(result.externalReference()).isEqualTo(original.getWalletTransactionId().toString());
        assertThat(result.amount()).isEqualByComparingTo(original.getAmount());
        assertThat(wallet.getBalance()).isEqualByComparingTo("1000000");
        assertThat(original.getType()).isEqualTo(WalletTransactionType.COURSE_PURCHASE);
        assertThat(original.getExternalReference()).isEqualTo("purchase-001");
        assertThat(enrollment.getStatus())
                .isEqualTo(com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus.CANCELLED);
        assertThat(enrollment.getEndDate()).isEqualTo(java.time.LocalDate.now());
    }

    @Test
    void duplicatePurchaseReferenceUsesImmutablePurchaseIdentityBeforeCatalogChecks() {
        UUID personId = UUID.randomUUID();
        UUID originalPriceId = UUID.randomUUID();
        Person person = Person.builder().personId(personId).build();
        Wallet wallet = Wallet.builder().walletId(UUID.randomUUID()).person(person)
                .balance(new BigDecimal("200000")).status(WalletStatus.ACTIVE).build();
        WalletTransaction existing = WalletTransaction.builder().walletTransactionId(UUID.randomUUID())
                .wallet(wallet).type(WalletTransactionType.COURSE_PURCHASE)
                .direction(WalletTransactionDirection.DEBIT).status(WalletTransactionStatus.APPROVED)
                .amount(new BigDecimal("800000")).externalReference("purchase-001").build();
        CoursePrice originalPrice = CoursePrice.builder().coursePriceId(originalPriceId).build();
        CoursePurchase purchase = CoursePurchase.builder().coursePurchaseId(UUID.randomUUID())
                .studentPerson(person).coursePrice(originalPrice).debitTransaction(existing).build();
        StudentEnrollment enrollment = StudentEnrollment.builder()
                .studentEnrollmentId(UUID.randomUUID()).coursePurchase(purchase).build();

        when(wallets.findByPersonIdForUpdate(personId)).thenReturn(Optional.of(wallet));
        when(transactions.findByTypeAndExternalReference(WalletTransactionType.COURSE_PURCHASE,
                "purchase-001")).thenReturn(Optional.of(existing));
        when(purchases.findByDebitTransaction_WalletTransactionId(existing.getWalletTransactionId()))
                .thenReturn(Optional.of(purchase));
        when(enrollments.findByCoursePurchase_CoursePurchaseId(purchase.getCoursePurchaseId()))
                .thenReturn(Optional.of(enrollment));

        WalletCommandDTO.TransactionResponse retry = service.purchaseCourse(
                new WalletCommandDTO.CoursePurchaseRequest(personId, originalPriceId,
                        "purchase-001", null), UUID.randomUUID());

        assertThat(retry.coursePurchaseId()).isEqualTo(purchase.getCoursePurchaseId());
        verifyNoInteractions(prices, courses, users);

        assertThatThrownBy(() -> service.purchaseCourse(
                new WalletCommandDTO.CoursePurchaseRequest(personId, UUID.randomUUID(),
                        "purchase-001", null), UUID.randomUUID()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FinanceErrorCode.IDEMPOTENCY_CONFLICT));
    }

    @Test
    void duplicateTopUpReferenceReturnsOriginalLedgerWithoutCreditingAgain() {
        UUID personId = UUID.randomUUID();
        Wallet wallet = Wallet.builder().walletId(UUID.randomUUID())
                .balance(new BigDecimal("1000000")).status(WalletStatus.ACTIVE).build();
        WalletTransaction existing = WalletTransaction.builder()
                .walletTransactionId(UUID.randomUUID()).wallet(wallet)
                .type(WalletTransactionType.TOP_UP).direction(WalletTransactionDirection.CREDIT)
                .status(WalletTransactionStatus.APPROVED).amount(new BigDecimal("500000"))
                .balanceBefore(new BigDecimal("500000")).balanceAfter(new BigDecimal("1000000"))
                .externalReference("topup-001").build();
        when(wallets.findByPersonIdForUpdate(personId)).thenReturn(Optional.of(wallet));
        when(transactions.findByTypeAndExternalReference(
                WalletTransactionType.TOP_UP, "topup-001")).thenReturn(Optional.of(existing));

        WalletCommandDTO.TransactionResponse result = service.topUp(
                new WalletCommandDTO.TopUpRequest(personId, new BigDecimal("500000"),
                        "topup-001", null), UUID.randomUUID());

        assertThat(result.walletTransactionId()).isEqualTo(existing.getWalletTransactionId());
        assertThat(wallet.getBalance()).isEqualByComparingTo("1000000");
        verify(transactions, never()).save(any());
        verifyNoInteractions(users);
    }

    @Test
    void walletMutationQueriesDeclarePessimisticWriteLocks() throws Exception {
        assertLock("findByPersonIdForUpdate", UUID.class);
        assertLock("findByIdForUpdate", UUID.class);
    }

    private void assertLock(String methodName, Class<?> parameter) throws Exception {
        Method method = WalletRepository.class.getMethod(methodName, parameter);
        assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }
}
