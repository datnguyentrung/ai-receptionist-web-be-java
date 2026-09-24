package com.dat.ai_receptionist_web.repository.Core;

import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;

public interface UserPersonRepository extends JpaRepository<UserPerson, UUID> {
    @Query(value = """
            select up
            from UserPerson up
            join fetch up.user
            join fetch up.person
            """,
            countQuery = """
            select count(up)
            from UserPerson up
            """)
    Page<UserPerson> findAllDetailed(Pageable pageable);

    @EntityGraph(attributePaths = "person")
    List<UserPerson> findAllByUser_UserIdAndActiveTrue(UUID userId);

    @EntityGraph(attributePaths = {"person", "person.position"})
    List<UserPerson> findAllDetailedByUser_UserIdAndActiveTrue(UUID userId);

    @EntityGraph(attributePaths = {"user", "person"})
    @Query("""
            select up
            from UserPerson up
            where up.user.userId in :userIds
              and up.active = true
            """)
    List<UserPerson> findAllActiveByUserIds(@Param("userIds") Set<UUID> userIds);

    @EntityGraph(attributePaths = "person")
    Optional<UserPerson> findByUserPersonIdAndUser_UserIdAndActiveTrue(UUID userPersonId, UUID userId);

    Optional<UserPerson> findByUser_UserIdAndPerson_PersonIdAndRelationshipType(
            UUID userId,
            UUID personId,
            RelationshipType relationshipType
    );

    Optional<UserPerson> findByUser_UserIdAndPerson_PersonIdAndActiveTrue(UUID userId, UUID personId);

    @Query("""
            select up
            from UserPerson up
            where up.user.userId in :userIds
              and up.person.personId in :personIds
              and up.relationshipType = :relationshipType
            """)
    List<UserPerson> findAllByUserIdInAndPersonIdInAndRelationshipType(
            @Param("userIds") Set<UUID> userIds,
            @Param("personIds") Set<UUID> personIds,
            @Param("relationshipType") RelationshipType relationshipType
    );

    @Query("select distinct up.user.userId from UserPerson up where up.person.personId = :personId and up.active = true")
    List<UUID> findActiveUserIdsByPersonId(@Param("personId") UUID personId);

    @EntityGraph(attributePaths = {"user", "person"})
    @Query("""
            select up
            from UserPerson up
            where up.person.personId in :personIds
              and up.active = true
            """)
    List<UserPerson> findActiveByPersonIds(@Param("personIds") Set<UUID> personIds);
}
