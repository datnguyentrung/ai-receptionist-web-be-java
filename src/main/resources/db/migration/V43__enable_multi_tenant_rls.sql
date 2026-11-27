-- ============================================================================
-- V43: Row-Level Security (RLS) cho tat ca bang tenant-owned
-- - Fail-closed khi thieu app.tenant_id (security.current_tenant_id() tra ve NULL)
-- - Bat ENABLE + FORCE ROW LEVEL SECURITY
-- - Cau hinh policy USING + WITH CHECK theo tenant_id
-- ============================================================================

CREATE OR REPLACE FUNCTION security.current_tenant_id()
RETURNS UUID
LANGUAGE sql
STABLE
AS $$
    SELECT NULLIF(current_setting('app.tenant_id', true), '')::uuid;
$$;

DO $$
DECLARE
    tbl TEXT;
    tenant_tables TEXT[] := ARRAY[
        'core.tenant_membership',
        'security.role',
        'security.role_permission',
        'security.user_role',
        'security.auth_session',
        'core.branch',
        'core.position',
        'core.person',
        'core.user_person',
        'catalog.class_schedule',
        'catalog.course',
        'catalog.course_schedule',
        'catalog.training_plan',
        'catalog.training_plan_requirement',
        'catalog.training_plan_access_rule',
        'catalog.training_plan_addon',
        'finance.wallet',
        'finance.wallet_transaction',
        'finance.wallet_top_up_request',
        'finance.course_purchase',
        'finance.enrollment_addon_purchase',
        'training.student_enrollment',
        'training.student_enrollment_schedule',
        'training.class_session',
        'training.course_staff_assignment',
        'training.session_attendance',
        'training.coach_timesheet',
        'training.belt_exam',
        'training.leave_request',
        'training.student_session_booking',
        'training.enrollment_schedule_change_request',
        'training.student_lifecycle_event',
        'training.attendance_command',
        'training.face_check_in_scan',
        'training.face_check_in_event',
        'crm.lead',
        'crm.trial_registration',
        'notification.notification',
        'notification.notification_recipient',
        'skill.fitness',
        'skill.fitness_record',
        'iot.managed_device',
        'iot.device_event',
        'infrastructure.projection_outbox',
        'infrastructure.projection_scope_state'
    ];
BEGIN
    FOREACH tbl IN ARRAY tenant_tables LOOP
        EXECUTE format('ALTER TABLE %s ENABLE ROW LEVEL SECURITY', tbl);
        EXECUTE format('ALTER TABLE %s FORCE ROW LEVEL SECURITY', tbl);
        EXECUTE format('DROP POLICY IF EXISTS tenant_isolation_policy ON %s', tbl);
        EXECUTE format(
            'CREATE POLICY tenant_isolation_policy ON %s USING (tenant_id = security.current_tenant_id()) WITH CHECK (tenant_id = security.current_tenant_id())',
            tbl
        );
    END LOOP;
END $$;
