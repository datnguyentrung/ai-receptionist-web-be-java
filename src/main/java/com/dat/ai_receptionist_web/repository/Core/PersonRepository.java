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

    @Query("select p.personId from Person p where upper(p.personCode) = upper(:personCode)")
    List<UUID> findPersonIdsByPersonCode(@Param("personCode") String personCode);

    @Query(value = """
        select
            p.person_id as "personId",
            (p.face_embedding <=> cast(:embedding as vector)) as "distance"
        from core.person p
        where p.face_embedding is not null
          and p.status = 'ACTIVE'
        order by p.face_embedding <=> cast(:embedding as vector)
        limit 1
        """, nativeQuery = true)
    List<NearestFaceMatchProjection> findNearestFaceMatch(@Param("embedding") String embedding);

    @Query("""
        select p.personId as personId, p.faceImagePath as faceImagePath
        from Person p
        where p.faceImagePath is not null
    """)
    Page<PersonAvatarProjection> findPersonAvatarProjections(Pageable pageable);

    Page<Person> findByFullNameContainingIgnoreCaseOrPersonCodeContainingIgnoreCase(
            String fullName, String personCode, Pageable pageable);

    Page<Person> findByPosition_PositionId(UUID positionId, Pageable pageable);

    interface NearestFaceMatchProjection {
        UUID getPersonId();
        Double getDistance();
    }

    interface PersonAvatarProjection {
        UUID getPersonId();
        String getFaceImagePath();
    }
}
