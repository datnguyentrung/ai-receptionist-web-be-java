package com.dat.ai_receptionist_web.mapper.Security;

import com.dat.ai_receptionist_web.dto.Security.LoginRes;
import com.dat.ai_receptionist_web.service.Security.AuthorizationSnapshot;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuthenticationMapper {
    default LoginRes toResponse(String token, String device, AuthorizationSnapshot snapshot,
                                LoginRes.UserContextRes active,
                                List<LoginRes.UserContextRes> contexts) {
        LoginRes result = new LoginRes();
        result.setAccessToken(token);
        result.setIdDevice(device);
        result.setUser(new LoginRes.UserLogin(snapshot.userId(), snapshot.phoneNumber(),
                snapshot.userStatus(), snapshot.roleCodes(), snapshot.permissionCodes()));
        result.setActiveContext(active);
        result.setAvailableContexts(contexts);
        result.setRequiresContextSelection(active == null);
        return result;
    }

    default LoginRes.MobileResponse toMobileResponse(LoginRes response, String refreshToken) {
        return new LoginRes.MobileResponse(response.getAccessToken(), refreshToken, response.getUser(),
                response.getActiveContext(), response.getAvailableContexts(),
                response.isRequiresContextSelection());
    }
}
