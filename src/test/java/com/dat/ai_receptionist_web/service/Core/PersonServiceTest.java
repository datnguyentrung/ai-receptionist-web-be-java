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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PersonServiceTest {
    @Test
    void createsZeroBalanceActiveWalletInPersonTransaction() {
        PersonRepository people = mock(PersonRepository.class);
        WalletRepository wallets = mock(WalletRepository.class);
        PersonMapper personMapper = mock(PersonMapper.class);
        when(personMapper.toEntity(any(PersonDTO.CreateRequest.class))).thenAnswer(invocation -> {
            PersonDTO.CreateRequest request = invocation.getArgument(0);
            return Person.builder()
                    .fullName(request.fullName())
                    .gender(request.gender())
                    .birthDate(request.birthDate())
                    .email(request.email())
                    .nationalCode(request.nationalCode())
                    .faceImagePath(request.faceImagePath())
                    .currentBelt(request.currentBelt())
                    .status(request.status())
                    .startDate(request.startDate())
                    .build();
        });
        when(personMapper.toResponse(any(Person.class))).thenReturn(null);
        when(people.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PersonService service = new PersonService(people, wallets, personMapper,
                new PersonCodePolicy(), mock(PositionRepository.class));

        service.create(new PersonDTO.CreateRequest("Nguyen Van A", true, LocalDate.of(2000, 1, 1),
                "a@example.com", "N1", "face.jpg", Belt.C10, PersonStatus.ACTIVE, LocalDate.now(), null));

        ArgumentCaptor<Wallet> wallet = ArgumentCaptor.forClass(Wallet.class);
        verify(wallets).save(wallet.capture());
        assertThat(wallet.getValue().getBalance()).isZero();
        assertThat(wallet.getValue().getStatus()).isEqualTo(WalletStatus.ACTIVE);
        assertThat(wallet.getValue().getPerson().getPersonCode()).isEqualTo("VQ_anv_010100");
    }

    @Test
    void listsPeopleByPositionWhenPositionIdFilterIsProvided() {
        PersonRepository people = mock(PersonRepository.class);
        WalletRepository wallets = mock(WalletRepository.class);
        PersonMapper personMapper = mock(PersonMapper.class);
        UUID positionId = UUID.randomUUID();
        Pageable pageable = Pageable.unpaged();
        Person person = Person.builder().personId(UUID.randomUUID()).fullName("Nguyen Van A").build();
        PersonDTO.SimpleResponse response = new PersonDTO.SimpleResponse(person.getPersonId(), "Nguyen Van A",
                null, null, null, null, null, null);

        when(people.findByPosition_PositionId(positionId, pageable)).thenReturn(new PageImpl<>(List.of(person)));
        when(personMapper.toSimpleResponse(person)).thenReturn(response);

        PersonService service = new PersonService(people, wallets, personMapper,
                new PersonCodePolicy(), mock(PositionRepository.class));

        var result = service.list(positionId, pageable);

        assertThat(result.getContent()).containsExactly(response);
        verify(people).findByPosition_PositionId(positionId, pageable);
        verify(people, never()).findAll(pageable);
    }
}
