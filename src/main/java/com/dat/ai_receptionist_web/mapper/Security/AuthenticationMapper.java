package com.dat.ai_receptionist_web.mapper.Security;

import com.dat.ai_receptionist_web.dto.Security.LoginRes;
import com.dat.ai_receptionist_web.service.Security.AuthorizationSnapshot;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuthenticationMapper {
    default LoginRes toResponse(String token, String device, AuthorizationSnapshot snapshot,
                                LoginRes.TenantRes activeTenant,
                                List<LoginRes.TenantRes> availableTenants,
                                LoginRes.UserContextRes active,
                                List<LoginRes.UserContextRes> contexts) {
        LoginRes result = new LoginRes();
        result.setAccessToken(token);
        result.setIdDevice(device);
        result.setUser(new LoginRes.UserLogin(snapshot.userId(), snapshot.phoneNumber(),
                snapshot.userStatus(), snapshot.roleCodes(), snapshot.permissionCodes()));
        result.setActiveTenant(activeTenant);
        result.setAvailableTenants(availableTenants == null ? List.of() : availableTenants);
        result.setRequiresTenantSelection(activeTenant == null);
        result.setActiveContext(active);
        result.setAvailableContexts(contexts == null ? List.of() : contexts);
        result.setRequiresContextSelection(activeTenant != null && active == null);
        return result;
    }

    default LoginRes.MobileResponse toMobileResponse(LoginRes response, String refreshToken) {
        return new LoginRes.MobileResponse(
                response.getAccessToken(),
                refreshToken,
                response.getUser(),
                response.getActiveTenant(),
                response.getAvailableTenants(),
                response.isRequiresTenantSelection(),
                response.getActiveContext(),
                response.getAvailableContexts(),
                response.isRequiresContextSelection()
        );
    }
}
