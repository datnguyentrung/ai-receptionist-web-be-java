package com.dat.ai_receptionist_web.mapper.Notification;

import com.dat.ai_receptionist_web.domain.Notification.Notification;
import com.dat.ai_receptionist_web.dto.Notification.NotificationDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    @Mapping(target = "notificationId", source = "notificationId")
    @Mapping(target = "recipientCount", ignore = true)
    NotificationDTO.Response toResponse(Notification entity);

    @Mapping(target = "notificationId", source = "notificationId")
    @Mapping(target = "recipientCount", ignore = true)
    NotificationDTO.SimpleResponse toSimpleResponse(Notification entity);

    default NotificationDTO.Response toResponse(java.util.UUID notificationId, int recipientCount) {
        return new NotificationDTO.Response(notificationId, recipientCount);
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "title", source = "title")
    @Mapping(target = "body", source = "body")
    @Mapping(target = "notificationType", source = "notificationType")
    @Mapping(target = "referenceType", source = "referenceType")
    @Mapping(target = "referenceId", source = "referenceId")
    @Mapping(target = "payload", source = "payload")
    void updateEntity(NotificationDTO.UpdateRequest request, @MappingTarget Notification entity);
}
