package com.dat.ai_receptionist_web.mapper.Notification;

import com.dat.ai_receptionist_web.domain.Notification.NotificationRecipient;
import com.dat.ai_receptionist_web.dto.Notification.NotificationRecipientDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NotificationRecipientMapper {
    @Mapping(target = "notificationId", source = "notification.notificationId")
    @Mapping(target = "recipientUserId", source = "recipientUser.userId")
    @Mapping(target = "contextPersonId", source = "contextPerson.personId")
    NotificationRecipientDTO.Response toResponse(NotificationRecipient entity);

    @Mapping(target = "notificationId", source = "notification.notificationId")
    @Mapping(target = "contextPersonId", source = "contextPerson.personId")
    @Mapping(target = "title", source = "notification.title")
    @Mapping(target = "body", source = "notification.body")
    @Mapping(target = "notificationType", source = "notification.notificationType")
    @Mapping(target = "referenceType", source = "notification.referenceType")
    @Mapping(target = "referenceId", source = "notification.referenceId")
    @Mapping(target = "payload", source = "notification.payload")
    NotificationRecipientDTO.MineResponse toMineResponse(NotificationRecipient entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "deliveredAt", source = "deliveredAt")
    @Mapping(target = "notificationRecipientStatus", source = "notificationRecipientStatus")
    void updateEntity(NotificationRecipientDTO.UpdateRequest request, @MappingTarget NotificationRecipient entity);
}
