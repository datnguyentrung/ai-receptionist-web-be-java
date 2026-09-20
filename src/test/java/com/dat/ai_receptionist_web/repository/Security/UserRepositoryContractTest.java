package com.dat.ai_receptionist_web.repository.Security;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryContractTest {
    @Test
    void userSearchKeepsPaginationOnUsersAndFiltersActiveLinkedPersons() throws Exception {
        Query query = UserRepository.class
                .getMethod("searchByPhoneNumberOrPersonFullName", String.class, String.class, Pageable.class)
                .getAnnotation(Query.class);

        assertThat(query.value())
                .contains(
                        "lower(u.phoneNumber)",
                        "exists",
                        "from UserPerson up",
                        "up.active = true",
                        "lower(p.fullName)"
                )
                .doesNotContain("join fetch");
        assertThat(query.countQuery())
                .isNotBlank()
                .contains("count(u)")
                .doesNotContain("fetch");
    }
}
