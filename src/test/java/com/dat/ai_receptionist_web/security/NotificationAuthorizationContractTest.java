package com.dat.ai_receptionist_web.security;

import com.dat.ai_receptionist_web.controller.Notification.NotificationRecipientController;
import com.dat.ai_receptionist_web.dto.Notification.NotificationRecipientDTO;
import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import com.dat.ai_receptionist_web.enums.Training.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationAuthorizationContractTest {

    @Test
    void appFacingNotificationRecipientEndpointsRequireAuthentication() throws Exception {
        assertAuthenticated(NotificationRecipientController.class, "listMine",
                Pageable.class, Boolean.class, NotificationType.class, String.class);
        assertAuthenticated(NotificationRecipientController.class, "getMine", UUID.class);
        assertAuthenticated(NotificationRecipientController.class, "unreadCountMine");
        assertAuthenticated(NotificationRecipientController.class, "markRead", UUID.class);
        assertAuthenticated(NotificationRecipientController.class, "markAllRead");
    }

    @Test
    void managementNotificationRecipientCrudRequiresRecipientPermissions() throws Exception {
        assertPermission(NotificationRecipientController.class, "list",
                PermissionDefinition.NOTIFICATION_RECIPIENT_READ, Pageable.class);
        assertPermission(NotificationRecipientController.class, "get",
                PermissionDefinition.NOTIFICATION_RECIPIENT_READ, UUID.class);
        assertPermission(NotificationRecipientController.class, "create",
                PermissionDefinition.NOTIFICATION_RECIPIENT_CREATE,
                NotificationRecipientDTO.CreateRequest.class);
        assertPermission(NotificationRecipientController.class, "update",
                PermissionDefinition.NOTIFICATION_RECIPIENT_UPDATE,
                UUID.class, NotificationRecipientDTO.UpdateRequest.class);
        assertPermission(NotificationRecipientController.class, "delete",
                PermissionDefinition.NOTIFICATION_RECIPIENT_DELETE, UUID.class);
    }

    private void assertAuthenticated(Class<?> controller, String methodName, Class<?>... parameterTypes)
            throws Exception {
        PreAuthorize annotation = controller.getMethod(methodName, parameterTypes).getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("isAuthenticated()");
    }

    private void assertPermission(
            Class<?> controller,
            String methodName,
            PermissionDefinition permission,
            Class<?>... parameterTypes
    ) throws Exception {
        PreAuthorize annotation = controller.getMethod(methodName, parameterTypes).getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains(permission.name(), ".getCode()");
    }
}
