package com.dat.ai_receptionist_web.schema;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MultiTenantSchemaContractTest {
    @Test
    void v42CreatesTenantFoundationCompositeFksAndScopedUniques() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V42__add_multi_tenant_foundation.sql"));

        assertThat(sql)
                .contains("CREATE TABLE core.tenant")
                .contains("CREATE TABLE core.tenant_membership")
                .contains("00000000-0000-4000-8000-000000000001")
                .contains("pk_security_role PRIMARY KEY (tenant_id, role_id)")
                .contains("fk_role_permission_tenant_role")
                .contains("fk_user_role_tenant_role")
                .contains("fk_user_role_tenant_membership")
                .contains("ck_auth_session_active_person_requires_tenant")
                .contains("uk_projection_outbox_tenant_key");
    }

    @Test
    void v43EnablesForceRowLevelSecurityAndFailClosedFunction() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V43__enable_multi_tenant_rls.sql"));

        assertThat(sql)
                .contains("CREATE OR REPLACE FUNCTION security.current_tenant_id()")
                .contains("NULLIF(current_setting('app.tenant_id', true), '')::uuid")
                .contains("ENABLE ROW LEVEL SECURITY")
                .contains("FORCE ROW LEVEL SECURITY")
                .contains("USING (tenant_id = security.current_tenant_id()) WITH CHECK (tenant_id = security.current_tenant_id())");
    }
}
