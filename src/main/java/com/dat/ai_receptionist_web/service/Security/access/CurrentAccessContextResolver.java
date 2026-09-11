package com.dat.ai_receptionist_web.service.Security.access;

import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentAccessContextResolver {
    private final UserPersonRepository userPersonRepository;

    @Transactional(readOnly = true)
    public AccessContext current() {
        UUID userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new ApiException(SecurityErrorCode.MISSING_AUTHENTICATED_USER));
        UUID activeUserPersonId = SecurityUtil.getCurrentActiveUserPersonId().orElse(null);
        var activeUserPerson = activeUserPersonId == null
                ? null
                : userPersonRepository.findByUserPersonIdAndUser_UserIdAndActiveTrue(activeUserPersonId, userId)
                .orElseThrow(() -> new ApiException(SecurityErrorCode.ACTIVE_CONTEXT_UNAVAILABLE));
        Jwt jwt = SecurityUtil.getJwt()
                .orElseThrow(() -> new ApiException(SecurityErrorCode.MISSING_AUTHENTICATED_USER));

        return new AccessContext(
                userId,
                activeUserPersonId,
                activeUserPerson == null ? null : activeUserPerson.getPerson().getPersonId(),
                activeUserPerson == null ? null : activeUserPerson.getRelationshipType(),
                claimSet(jwt, "roles"),
                claimSet(jwt, "permissions")
        );
    }

    private Set<String> claimSet(Jwt jwt, String claimName) {
        List<String> values = jwt.getClaimAsStringList(claimName);
        return values == null ? Set.of() : new TreeSet<>(values);
    }
}
