package com.dat.ai_receptionist_web.util.error;

import com.dat.ai_receptionist_web.config.AccessTokenStateValidationFilter;
import com.dat.ai_receptionist_web.config.CorrelationIdFilter;
import com.dat.ai_receptionist_web.config.CustomAuthenticationEntryPoint;
import com.dat.ai_receptionist_web.error.ApiErrorResponseFactory;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.SensitiveFieldRule;
import com.dat.ai_receptionist_web.error.ValidationError;
import com.dat.ai_receptionist_web.service.Security.AuthorizationService;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.error.code.NotificationErrorCode;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ApiErrorResponseFactoryTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApiErrorResponseFactory factory = new ApiErrorResponseFactory(objectMapper);

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void apiExceptionProblemDetailContainsContractFields() {
        MDC.put(ApiErrorResponseFactory.CORRELATION_ID_MDC_KEY, "cid-1");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/course-prices");

        ProblemDetail body = factory.response(new ApiException(CatalogErrorCode.COURSE_NOT_FOUND), request).getBody();

        assertThat(body).isNotNull();
        assertThat(body.getType().toString()).isEqualTo("/errors/course-not-found");
        assertThat(body.getTitle()).isEqualTo("Course not found");
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getDetail()).isEqualTo(CatalogErrorCode.COURSE_NOT_FOUND.defaultDetail());
        assertThat(body.getInstance().toString()).isEqualTo("/api/v1/course-prices");
        assertThat(body.getProperties()).containsEntry("code", "COURSE_NOT_FOUND")
                .containsEntry("correlationId", "cid-1");
    }

    @Test
    void apiExceptionSafeDetailSurvivesProblemDetailMapping() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/notifications");

        ProblemDetail denied = factory.response(new ApiException(
                NotificationErrorCode.NOTIFICATION_RECIPIENT_NOT_ELIGIBLE,
                "Notification recipient is denied for this context"), request).getBody();
        ProblemDetail unsupported = factory.response(new ApiException(
                NotificationErrorCode.NOTIFICATION_RECIPIENT_NOT_ELIGIBLE,
                "Notification recipient eligibility is unsupported for this context"), request).getBody();

        assertThat(denied).isNotNull();
        assertThat(unsupported).isNotNull();
        assertThat(denied.getProperties()).containsEntry("code", "NOTIFICATION_RECIPIENT_NOT_ELIGIBLE");
        assertThat(unsupported.getProperties()).containsEntry("code", "NOTIFICATION_RECIPIENT_NOT_ELIGIBLE");
        assertThat(denied.getDetail()).isEqualTo("Notification recipient is denied for this context");
        assertThat(unsupported.getDetail())
                .isEqualTo("Notification recipient eligibility is unsupported for this context");
    }

    @Test
    void unexpectedExceptionResponseDoesNotExposeExceptionMessage() {
        MDC.put(ApiErrorResponseFactory.CORRELATION_ID_MDC_KEY, "cid-2");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");

        ProblemDetail body = factory.unexpectedResponse(request).getBody();

        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(500);
        assertThat(body.getDetail()).isEqualTo("Internal server error");
        assertThat(body.toString()).doesNotContain("database password leaked");
    }

    @Test
    void validationErrorsUseArrayShapeWithoutRejectedValue() {
        MDC.put(ApiErrorResponseFactory.CORRELATION_ID_MDC_KEY, "cid-3");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users");

        ProblemDetail body = factory.validationResponse(List.of(
                new ValidationError("password", "must not be blank"),
                new ValidationError("phoneNumber", "must not be blank")
        ), request).getBody();

        assertThat(body).isNotNull();
        assertThat(body.getProperties()).containsEntry("code", "VALIDATION_ERROR");
        assertThat(body.getProperties().get("errors")).asString()
                .contains("password")
                .contains("must not be blank")
                .doesNotContain("rejectedValue")
                .doesNotContain("secret-password");
        assertThat(SensitiveFieldRule.isSensitive("refreshToken")).isTrue();
        assertThat(SensitiveFieldRule.isSensitive("password")).isTrue();
    }

    @Test
    void authenticationEntryPointWritesTheSharedProblemDetailContract() throws Exception {
        MDC.put(ApiErrorResponseFactory.CORRELATION_ID_MDC_KEY, "cid-4");
        CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint(factory);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/private");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("raw auth failure"));

        Map<String, Object> body = objectMapper.readValue(response.getContentAsString(), Map.class);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body).containsEntry("status", 401)
                .containsEntry("title", SecurityErrorCode.UNAUTHORIZED.title())
                .containsEntry("detail", SecurityErrorCode.UNAUTHORIZED.defaultDetail());
        assertThat(body).containsEntry("code", "UNAUTHORIZED")
                .containsEntry("correlationId", "cid-4");
        assertThat(body.toString()).doesNotContain("raw auth failure");
    }

    @Test
    void staleTokenFilterWritesTheSharedProblemDetailContract() throws IOException, ServletException {
        MDC.put(ApiErrorResponseFactory.CORRELATION_ID_MDC_KEY, "cid-5");
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        AccessTokenStateValidationFilter filter = new AccessTokenStateValidationFilter(authorizationService, factory);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/private");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        doThrow(new AuthorizationService.StaleAccessTokenException("raw stale reason"))
                .when(authorizationService).validateAccessToken(jwt);

        filter.doFilter(request, response, new MockFilterChain());

        Map<String, Object> body = objectMapper.readValue(response.getContentAsString(), Map.class);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body).containsEntry("code", "TOKEN_STALE")
                .containsEntry("detail", SecurityErrorCode.TOKEN_STALE.defaultDetail())
                .containsEntry("correlationId", "cid-5");
        assertThat(body.toString()).doesNotContain("raw stale reason");
    }

    @Test
    void correlationIdFilterUsesIncomingHeaderInMdcAndResponseHeader() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        request.addHeader(ApiErrorResponseFactory.CORRELATION_ID_HEADER, "incoming-cid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        jakarta.servlet.FilterChain chain = (req, res) ->
                assertThat(MDC.get(ApiErrorResponseFactory.CORRELATION_ID_MDC_KEY)).isEqualTo("incoming-cid");

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(ApiErrorResponseFactory.CORRELATION_ID_HEADER)).isEqualTo("incoming-cid");
        assertThat(MDC.get(ApiErrorResponseFactory.CORRELATION_ID_MDC_KEY)).isNull();
    }
}
