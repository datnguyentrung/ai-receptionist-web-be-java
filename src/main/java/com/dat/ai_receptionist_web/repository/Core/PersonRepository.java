package com.dat.ai_receptionist_web.repository.Core;

import com.dat.ai_receptionist_web.domain.Core.Person;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface PersonRepository extends JpaRepository<Person, UUID> {
    boolean existsByNationalCode(String nationalCode);
    boolean existsByPersonCode(String personCode);
    Optional<Person> findByPersonCodeIgnoreCase(String personCode);

    @Query("select p from Person p where upper(p.personCode) in :personCodes")
    List<Person> findAllByPersonCodeUpperIn(@Param("personCodes") Set<String> personCodes);

    Page<Person> findByFullNameContainingIgnoreCaseOrPersonCodeContainingIgnoreCase(
            String fullName, String personCode, Pageable pageable);

    Page<Person> findByPosition_PositionId(UUID positionId, Pageable pageable);
}
