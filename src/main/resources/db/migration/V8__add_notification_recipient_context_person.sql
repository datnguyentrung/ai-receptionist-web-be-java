ALTER TABLE notification.notification_recipient
    ADD COLUMN context_person_id UUID NULL REFERENCES core.person(person_id);

ALTER TABLE notification.notification_recipient
    DROP CONSTRAINT uk_notification_recipient;

CREATE UNIQUE INDEX uk_notification_recipient_account
    ON notification.notification_recipient(notification_id, recipient_user_id)
    WHERE context_person_id IS NULL;

CREATE UNIQUE INDEX uk_notification_recipient_context
    ON notification.notification_recipient(notification_id, recipient_user_id, context_person_id)
    WHERE context_person_id IS NOT NULL;

CREATE INDEX idx_notification_recipient_inbox_context
    ON notification.notification_recipient(
        recipient_user_id,
        context_person_id,
        created_at DESC,
        notification_recipient_id DESC
    );

CREATE INDEX idx_notification_recipient_unread_context
    ON notification.notification_recipient(
        recipient_user_id,
        context_person_id,
        created_at DESC,
        notification_recipient_id DESC
    )
    WHERE read = false;
