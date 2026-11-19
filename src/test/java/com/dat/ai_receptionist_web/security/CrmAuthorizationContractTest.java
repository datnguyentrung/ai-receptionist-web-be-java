package com.dat.ai_receptionist_web.security;

import com.dat.ai_receptionist_web.controller.Crm.LeadController;
import com.dat.ai_receptionist_web.controller.Crm.TrialRegistrationController;
import com.dat.ai_receptionist_web.dto.Crm.LeadDTO;
import com.dat.ai_receptionist_web.dto.Crm.TrialRegistrationDTO;
import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CrmAuthorizationContractTest {
    @Test
    void leadCrudRequiresLeadPermissions() throws Exception {
        assertPermission(LeadController.class, "list", PermissionDefinition.LEAD_READ,
                Pageable.class);
        assertPermission(LeadController.class, "get", PermissionDefinition.LEAD_READ,
                UUID.class);
        assertPermission(LeadController.class, "create", PermissionDefinition.LEAD_CREATE,
                LeadDTO.CreateRequest.class);
        assertPermission(LeadController.class, "update", PermissionDefinition.LEAD_UPDATE,
                UUID.class, LeadDTO.UpdateRequest.class);
        assertPermission(LeadController.class, "delete", PermissionDefinition.LEAD_DELETE,
                UUID.class);
    }

    @Test
    void trialRegistrationCrudRequiresTrialRegistrationPermissions() throws Exception {
        assertPermission(TrialRegistrationController.class, "list",
                PermissionDefinition.TRIAL_REGISTRATION_READ, Pageable.class);
        assertPermission(TrialRegistrationController.class, "get",
                PermissionDefinition.TRIAL_REGISTRATION_READ, UUID.class);
        assertPermission(TrialRegistrationController.class, "create",
                PermissionDefinition.TRIAL_REGISTRATION_CREATE,
                TrialRegistrationDTO.CreateRequest.class);
        assertPermission(TrialRegistrationController.class, "update",
                PermissionDefinition.TRIAL_REGISTRATION_UPDATE,
                UUID.class, TrialRegistrationDTO.UpdateRequest.class);
        assertPermission(TrialRegistrationController.class, "delete",
                PermissionDefinition.TRIAL_REGISTRATION_DELETE, UUID.class);
    }

    private void assertPermission(
            Class<?> controller,
            String methodName,
            PermissionDefinition permission,
            Class<?>... parameterTypes
    ) throws Exception {
        PreAuthorize annotation = controller.getMethod(methodName, parameterTypes)
                .getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains(permission.name(), ".getCode()");
    }
}
