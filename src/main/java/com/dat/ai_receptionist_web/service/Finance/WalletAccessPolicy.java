package com.dat.ai_receptionist_web.service.Finance;

import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import com.dat.ai_receptionist_web.enums.Security.SystemRoleDefinition;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class WalletAccessPolicy {
    public boolean isFinanceStaff(AccessContext context) {
        return context.hasRole(SystemRoleDefinition.SUPER_ADMIN.getCode())
                || context.hasRole(SystemRoleDefinition.FINANCE_STAFF.getCode());
    }

    public void requireFinanceStaff(AccessContext context) {
        if (!isFinanceStaff(context)) throw new ApiException(SecurityErrorCode.ACCESS_DENIED);
    }

    public void requireGuardianOwnsPerson(AccessContext context, UUID personId) {
        if (context.relationshipType() != RelationshipType.GUARDIAN
                || !Objects.equals(context.activePersonId(), personId)) {
            throw new ApiException(SecurityErrorCode.ACCESS_DENIED);
        }
    }

    public void requireCanReadPersonWallet(AccessContext context, UUID personId) {
        if (!isFinanceStaff(context) && !Objects.equals(context.activePersonId(), personId)) {
            throw new ApiException(SecurityErrorCode.ACCESS_DENIED);
        }
    }
}

