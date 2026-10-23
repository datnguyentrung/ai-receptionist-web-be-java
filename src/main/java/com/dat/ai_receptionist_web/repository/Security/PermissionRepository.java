package com.dat.ai_receptionist_web.repository.Security;

import com.dat.ai_receptionist_web.domain.Security.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    Optional<Permission> findByCode(String code);
    List<Permission> findAllByCodeIn(Set<String> codes);

    @Modifying
    @Query(value = """
            INSERT INTO security.permission(code, model, action, description)
            VALUES (:code, :model, :action, :description)
            ON CONFLICT (code) DO UPDATE SET
                model = EXCLUDED.model,
                action = EXCLUDED.action,
                description = EXCLUDED.description
            """, nativeQuery = true)
    void upsert(@Param("code") String code, @Param("model") String model,
                @Param("action") String action, @Param("description") String description);

    @Query("select p.code from Permission p")
    Set<String> findAllCodes();

    long deleteAllByCodeIn(List<String> obsoleteCodes);

    @Modifying
    @Query("""
        DELETE FROM Permission p
        WHERE p.code IN :codes
    """)
    int deleteByCodes(@Param("codes") Set<String> obsoleteCodes);
}
