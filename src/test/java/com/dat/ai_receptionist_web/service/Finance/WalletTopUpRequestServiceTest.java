package com.dat.ai_receptionist_web.service.Finance;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Finance.Wallet;
import com.dat.ai_receptionist_web.domain.Finance.WalletTopUpRequest;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Finance.WalletTopUpRequestDTO;
import com.dat.ai_receptionist_web.enums.Finance.*;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import com.dat.ai_receptionist_web.enums.Security.SystemRoleDefinition;
import com.dat.ai_receptionist_web.repository.Finance.*;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.service.Core.SupabaseStorageService;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletTopUpRequestServiceTest {
    private final WalletTopUpRequestRepository requests = mock(WalletTopUpRequestRepository.class);
    private final WalletRepository wallets = mock(WalletRepository.class);
    private final WalletTransactionRepository transactions = mock(WalletTransactionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CurrentAccessContextResolver contexts = mock(CurrentAccessContextResolver.class);
    private final SupabaseStorageService storage = mock(SupabaseStorageService.class);
    private final WalletAccessPolicy accessPolicy = new WalletAccessPolicy();
    private final WalletTopUpRequestService service = new WalletTopUpRequestService(
            requests, wallets, transactions, users, contexts, accessPolicy, storage);

    @Test
    void guardianSubmitMovesToPendingWithoutChangingBalance() {
        UUID requestId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User guardian = User.builder().userId(userId).build();
        Wallet wallet = wallet(personId, "500000");
        WalletTopUpRequest request = request(requestId, wallet, guardian, WalletTopUpRequestMode.GUARDIAN_REQUEST);
        when(requests.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        when(contexts.current()).thenReturn(new AccessContext(
                userId, UUID.randomUUID(), personId, RelationshipType.GUARDIAN, Set.of("GUARDIAN"), Set.of()));

        var response = service.submit(requestId, new WalletTopUpRequestDTO.SubmitRequest(false));

        assertThat(response.status()).isEqualTo(WalletTopUpRequestStatus.PENDING);
        assertThat(wallet.getBalance()).isEqualByComparingTo("500000");
        verify(wallets, never()).findByIdForUpdate(any());
        verify(transactions, never()).save(any());
    }

    @Test
    void financeApprovalCreditsExactlyOnceWhenRetried() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User finance = User.builder().userId(userId).build();
        Wallet wallet = wallet(UUID.randomUUID(), "500000");
        WalletTopUpRequest request = request(requestId, wallet, finance, WalletTopUpRequestMode.GUARDIAN_REQUEST);
        request.setStatus(WalletTopUpRequestStatus.PENDING);
        when(requests.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        when(contexts.current()).thenReturn(new AccessContext(
                userId, null, null, null, Set.of(SystemRoleDefinition.FINANCE_STAFF.getCode()), Set.of()));
        when(wallets.findByIdForUpdate(wallet.getWalletId())).thenReturn(Optional.of(wallet));
        when(users.findById(userId)).thenReturn(Optional.of(finance));
        when(transactions.findByTypeAndExternalReference(any(), any())).thenReturn(Optional.empty());
        when(transactions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.approve(requestId, new WalletTopUpRequestDTO.SubmitRequest(true));
        service.approve(requestId, new WalletTopUpRequestDTO.SubmitRequest(true));

        assertThat(wallet.getBalance()).isEqualByComparingTo("700000");
        assertThat(request.getStatus()).isEqualTo(WalletTopUpRequestStatus.APPROVED);
        verify(transactions, times(1)).save(any());
    }

    private Wallet wallet(UUID personId, String balance) {
        return Wallet.builder()
                .walletId(UUID.randomUUID())
                .person(Person.builder().personId(personId).personCode("VQ_TEST").fullName("Học viên").build())
                .balance(new BigDecimal(balance))
                .status(WalletStatus.ACTIVE)
                .build();
    }

    private WalletTopUpRequest request(UUID requestId, Wallet wallet, User requester, WalletTopUpRequestMode mode) {
        return WalletTopUpRequest.builder()
                .topUpRequestId(requestId)
                .clientRequestId(UUID.randomUUID())
                .wallet(wallet)
                .requestedByUser(requester)
                .mode(mode)
                .status(WalletTopUpRequestStatus.DRAFT)
                .amount(new BigDecimal("200000"))
                .proofObjectPath("private/proof.jpg")
                .proofSha256("a".repeat(64))
                .build();
    }
}
