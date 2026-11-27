package com.dat.ai_receptionist_web.repository.Core;

import com.dat.ai_receptionist_web.domain.Core.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {
    boolean existsByCodeIgnoreCase(String code);

    Optional<Position> findByCodeIgnoreCase(String code);

    @Modifying
    @Query(value = """
            insert into core.position (position_id, tenant_id, code, name, description, active, created_at, updated_at)
            values (:positionId, :tenantId, :code, :name, :description, true, current_timestamp, current_timestamp)
            on conflict (tenant_id, code) do update
            set name = excluded.name,
                description = excluded.description,
                active = true,
                updated_at = current_timestamp
            """, nativeQuery = true)
    void upsertSystemPosition(
            @Param("positionId") UUID positionId,
            @Param("tenantId") UUID tenantId,
            @Param("code") String code,
            @Param("name") String name,
            @Param("description") String description
    );

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
