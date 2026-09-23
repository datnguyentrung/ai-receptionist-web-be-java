package com.dat.ai_receptionist_web.repository.Core;

import com.dat.ai_receptionist_web.domain.Core.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {
    boolean existsByCodeIgnoreCase(String code);

    Optional<Position> findByCodeIgnoreCase(String code);

    @Query("""
            select p.position.positionId as positionId, count(p) as personCount
            from Person p
            where p.position.positionId in :positionIds
            group by p.position.positionId
            """)
    List<PersonCountByPosition> countPersonsByPositionIds(@Param("positionIds") Collection<UUID> positionIds);

    @Query("""
            select count(p)
            from Person p
            where p.position.positionId = :positionId
            """)
    long countPersonsByPositionId(@Param("positionId") UUID positionId);

    interface PersonCountByPosition {
        UUID getPositionId();

        long getPersonCount();
    }
}
