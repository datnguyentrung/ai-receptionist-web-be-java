package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Finance.Wallet;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.enums.Core.*;
import com.dat.ai_receptionist_web.enums.Finance.WalletStatus;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Core.PositionRepository;
import com.dat.ai_receptionist_web.repository.Finance.WalletRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PersonServiceTest {
    @Test
    void createsZeroBalanceActiveWalletInPersonTransaction() {
        PersonRepository people = mock(PersonRepository.class);
        WalletRepository wallets = mock(WalletRepository.class);
        when(people.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PersonService service = new PersonService(people, wallets, mock(PersonMapper.class),
                new PersonCodePolicy(), mock(PositionRepository.class));

        service.create(new PersonDTO.CreateRequest("Nguyen Van A", true, LocalDate.of(2000, 1, 1),
                "a@example.com", "N1", "face.jpg", Belt.C10, PersonStatus.ACTIVE, LocalDate.now(), null));

        ArgumentCaptor<Wallet> wallet = ArgumentCaptor.forClass(Wallet.class);
        verify(wallets).save(wallet.capture());
        assertThat(wallet.getValue().getBalance()).isZero();
        assertThat(wallet.getValue().getStatus()).isEqualTo(WalletStatus.ACTIVE);
        assertThat(wallet.getValue().getPerson().getPersonCode()).isEqualTo("VQ_anv_010100");
    }
}
