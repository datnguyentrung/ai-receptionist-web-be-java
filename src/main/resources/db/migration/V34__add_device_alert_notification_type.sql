alter table notification.notification drop constraint if exists ck_notification_type;
alter table notification.notification add constraint ck_notification_type check (notification_type in (
 'SYSTEM','ATTENDANCE','TUITION','CLASS_SCHEDULE','COACH_TIMESHEET','ANNOUNCEMENT','CLASS_SESSION_REPORT','DEVICE_ALERT'
));
