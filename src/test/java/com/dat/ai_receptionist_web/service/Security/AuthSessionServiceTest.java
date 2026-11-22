package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.AuthSession;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.dto.Security.LoginRes;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import com.dat.ai_receptionist_web.mapper.Core.UserPersonMapper;
import com.dat.ai_receptionist_web.mapper.Security.AuthSessionMapper;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Security.AuthSessionRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.service.Core.SupabaseStorageService;
import com.dat.ai_receptionist_web.util.RefreshTokenUtil;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthSessionServiceTest {
    private AuthSessionRepository sessions;
    private UserPersonRepository userPersons;
    private UserPersonMapper userPersonMapper;
    private SupabaseStorageService supabaseStorageService;
    private AuthSessionService service;

    @BeforeEach
    void setUp() {
        sessions = mock(AuthSessionRepository.class);
        userPersons = mock(UserPersonRepository.class);
        userPersonMapper = mock(UserPersonMapper.class);
        supabaseStorageService = mock(SupabaseStorageService.class);
        service = new AuthSessionService(sessions, mock(UserRepository.class), userPersons,
                mock(AuthSessionMapper.class), userPersonMapper, supabaseStorageService);
        ReflectionTestUtils.setField(service, "refreshTokenValidity", 3600L);
    }

    @Test
    void refreshRotationReplacesHashSoOldTokenCannotMatchAgain() {
        String current = "current-refresh-token";
        String next = "next-refresh-token";
        AuthSession session = AuthSession.builder().authSessionId(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusMinutes(10)).revoked(false).build();
        when(sessions.findByRefreshTokenHashForUpdate(RefreshTokenUtil.sha256(current)))
                .thenReturn(Optional.of(session));

        service.rotate(current, next);

        assertThat(session.getRefreshTokenHash()).isEqualTo(RefreshTokenUtil.sha256(next));
        assertThat(session.getRefreshTokenHash()).isNotEqualTo(RefreshTokenUtil.sha256(current));
        verify(sessions).findByRefreshTokenHashForUpdate(RefreshTokenUtil.sha256(current));
    }

    @Test
    void revokeMarksThePersistedSessionImmediately() {
        UUID sessionId = UUID.randomUUID();
        AuthSession session = AuthSession.builder().authSessionId(sessionId).revoked(false).build();
        when(sessions.findById(sessionId)).thenReturn(Optional.of(session));

        service.revoke(sessionId);

        assertThat(session.isRevoked()).isTrue();
        assertThat(session.getRevokedAt()).isNotNull();
    }

    @Test
    void switchContextRejectsUserPersonNotOwnedByCurrentUser() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User user = User.builder().userId(userId).build();
        AuthSession session = AuthSession.builder().authSessionId(sessionId).user(user).revoked(false).build();
        when(sessions.findById(sessionId)).thenReturn(Optional.of(session));
        when(userPersons.findByUserPersonIdAndUser_UserIdAndActiveTrue(targetId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.switchContext(userId, sessionId, targetId))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(SecurityErrorCode.PERSON_CONTEXT_NOT_OWNED));

        assertThat(session.getActiveUserPerson()).isNull();
    }

    @Test
    void contextsExposePublicFaceImageUrl() {
        UUID userId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        String objectPath = "persons/" + personId + "/avatar.jpg";
        String publicUrl = "https://storage.example/face-images/" + objectPath;
        Person person = Person.builder().personId(personId).faceImagePath(objectPath).build();
        UserPerson userPerson = UserPerson.builder()
                .userPersonId(UUID.randomUUID())
                .person(person)
                .relationshipType(RelationshipType.OWNER)
                .active(true)
                .build();
        LoginRes.UserContextRes expected = new LoginRes.UserContextRes(
                userPerson.getUserPersonId(), personId, RelationshipType.OWNER,
                null, null, null, publicUrl);

        when(userPersons.findAllByUser_UserIdAndActiveTrue(userId)).thenReturn(List.of(userPerson));
        when(supabaseStorageService.getPublicUrl(objectPath)).thenReturn(publicUrl);
        when(userPersonMapper.toLoginContext(userPerson, publicUrl)).thenReturn(expected);

        assertThat(service.contexts(userId)).containsExactly(expected);
        verify(supabaseStorageService).getPublicUrl(objectPath);
    }
}
