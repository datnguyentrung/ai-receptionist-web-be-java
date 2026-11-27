-- ============================================================================
-- V42: Multi-Tenant Foundation
-- - Tao bang core.tenant va core.tenant_membership
-- - Tao tenant mac dinh (VAN_QUAN) va backfill du lieu hien huu theo quan he cha-con
-- - Chuyen unique keys nghiep vu sang pham vi (tenant_id, ...)
-- - Gan tenant_id cho security.role, role_permission, user_role, auth_session
-- - Bo sung composite PK/UNIQUE (tenant_id, pk) va composite FK (tenant_id, parent_id)
-- - Bo sung composite indexes (tenant_id, ...) theo truy van thuc te
-- ============================================================================

-- 1. Bang Tenant & TenantMembership
CREATE TABLE core.tenant (
    tenant_id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_tenant_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE TABLE core.tenant_membership (
    tenant_membership_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES core.tenant(tenant_id),
    user_id UUID NOT NULL REFERENCES security.users(user_id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_tenant_membership_tenant_user UNIQUE (tenant_id, user_id),
    CONSTRAINT ck_tenant_membership_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED'))
);
CREATE INDEX idx_tenant_membership_user_status ON core.tenant_membership(user_id, status);

-- 2. Tao tenant mac dinh on dinh & backfill membership cho tat ca user hien co
INSERT INTO core.tenant (tenant_id, code, name, status, created_at, updated_at, version)
VALUES (
    '00000000-0000-4000-8000-000000000001'::uuid,
    'VAN_QUAN',
    'Taekwondo Van Quan',
    'ACTIVE',
    NOW(),
    NOW(),
    0
)
ON CONFLICT (tenant_id) DO NOTHING;

INSERT INTO core.tenant_membership (tenant_membership_id, tenant_id, user_id, status, created_at, updated_at, version)
SELECT
    gen_random_uuid(),
    '00000000-0000-4000-8000-000000000001'::uuid,
    u.user_id,
    CASE WHEN u.user_status = 'ACTIVE' THEN 'ACTIVE' ELSE 'SUSPENDED' END,
    NOW(),
    NOW(),
    0
FROM security.users u
ON CONFLICT (tenant_id, user_id) DO NOTHING;

-- 3. Them cot tenant_id cho tat ca bang tenant-owned & infrastructure
ALTER TABLE security.role ADD COLUMN tenant_id UUID;
ALTER TABLE security.role_permission ADD COLUMN tenant_id UUID;
ALTER TABLE security.user_role ADD COLUMN tenant_id UUID;
ALTER TABLE security.auth_session ADD COLUMN tenant_id UUID;

ALTER TABLE core.branch ADD COLUMN tenant_id UUID;
ALTER TABLE core.position ADD COLUMN tenant_id UUID;
ALTER TABLE core.person ADD COLUMN tenant_id UUID;
ALTER TABLE core.user_person ADD COLUMN tenant_id UUID;

ALTER TABLE catalog.class_schedule ADD COLUMN tenant_id UUID;
ALTER TABLE catalog.course ADD COLUMN tenant_id UUID;
ALTER TABLE catalog.course_schedule ADD COLUMN tenant_id UUID;
ALTER TABLE catalog.training_plan ADD COLUMN tenant_id UUID;
ALTER TABLE catalog.training_plan_requirement ADD COLUMN tenant_id UUID;
ALTER TABLE catalog.training_plan_access_rule ADD COLUMN tenant_id UUID;
ALTER TABLE catalog.training_plan_addon ADD COLUMN tenant_id UUID;

ALTER TABLE finance.wallet ADD COLUMN tenant_id UUID;
ALTER TABLE finance.wallet_transaction ADD COLUMN tenant_id UUID;
ALTER TABLE finance.wallet_top_up_request ADD COLUMN tenant_id UUID;
ALTER TABLE finance.course_purchase ADD COLUMN tenant_id UUID;
ALTER TABLE finance.enrollment_addon_purchase ADD COLUMN tenant_id UUID;

ALTER TABLE training.student_enrollment ADD COLUMN tenant_id UUID;
ALTER TABLE training.student_enrollment_schedule ADD COLUMN tenant_id UUID;
ALTER TABLE training.class_session ADD COLUMN tenant_id UUID;
ALTER TABLE training.course_staff_assignment ADD COLUMN tenant_id UUID;
ALTER TABLE training.session_attendance ADD COLUMN tenant_id UUID;
ALTER TABLE training.coach_timesheet ADD COLUMN tenant_id UUID;
ALTER TABLE training.belt_exam ADD COLUMN tenant_id UUID;
ALTER TABLE training.leave_request ADD COLUMN tenant_id UUID;
ALTER TABLE training.student_session_booking ADD COLUMN tenant_id UUID;
ALTER TABLE training.enrollment_schedule_change_request ADD COLUMN tenant_id UUID;
ALTER TABLE training.student_lifecycle_event ADD COLUMN tenant_id UUID;
ALTER TABLE training.attendance_command ADD COLUMN tenant_id UUID;
ALTER TABLE training.face_check_in_scan ADD COLUMN tenant_id UUID;
ALTER TABLE training.face_check_in_event ADD COLUMN tenant_id UUID;

ALTER TABLE crm.lead ADD COLUMN tenant_id UUID;
ALTER TABLE crm.trial_registration ADD COLUMN tenant_id UUID;

ALTER TABLE notification.notification ADD COLUMN tenant_id UUID;
ALTER TABLE notification.notification_recipient ADD COLUMN tenant_id UUID;

ALTER TABLE skill.fitness ADD COLUMN tenant_id UUID;
ALTER TABLE skill.fitness_record ADD COLUMN tenant_id UUID;

ALTER TABLE iot.managed_device ADD COLUMN tenant_id UUID;
ALTER TABLE iot.device_event ADD COLUMN tenant_id UUID;

ALTER TABLE infrastructure.projection_outbox ADD COLUMN tenant_id UUID;
ALTER TABLE infrastructure.projection_scope_state ADD COLUMN tenant_id UUID;

-- 4. Backfill tenant_id theo quan he cha-con (uu tien truy nguon tu bang goc)
DO $$
DECLARE
    default_tenant UUID := '00000000-0000-4000-8000-000000000001'::uuid;
BEGIN
    -- Root tables
    UPDATE security.role SET tenant_id = default_tenant WHERE tenant_id IS NULL;
    UPDATE core.branch SET tenant_id = default_tenant WHERE tenant_id IS NULL;
    UPDATE core.position SET tenant_id = default_tenant WHERE tenant_id IS NULL;
    UPDATE catalog.course SET tenant_id = default_tenant WHERE tenant_id IS NULL;
    UPDATE catalog.training_plan SET tenant_id = default_tenant WHERE tenant_id IS NULL;
    UPDATE notification.notification SET tenant_id = default_tenant WHERE tenant_id IS NULL;
    UPDATE skill.fitness SET tenant_id = default_tenant WHERE tenant_id IS NULL;
    UPDATE iot.managed_device SET tenant_id = default_tenant WHERE tenant_id IS NULL;
    UPDATE crm.lead SET tenant_id = default_tenant WHERE tenant_id IS NULL;
    UPDATE training.attendance_command SET tenant_id = default_tenant WHERE tenant_id IS NULL;
    UPDATE training.face_check_in_scan SET tenant_id = default_tenant WHERE tenant_id IS NULL;
    UPDATE infrastructure.projection_outbox SET tenant_id = default_tenant WHERE tenant_id IS NULL;
    UPDATE infrastructure.projection_scope_state SET tenant_id = default_tenant WHERE tenant_id IS NULL;

    -- Security child tables
    UPDATE security.role_permission rp SET tenant_id = r.tenant_id
    FROM security.role r WHERE rp.role_id = r.role_id AND rp.tenant_id IS NULL;

    UPDATE security.user_role ur SET tenant_id = r.tenant_id
    FROM security.role r WHERE ur.role_id = r.role_id AND ur.tenant_id IS NULL;

    -- Core child tables
    UPDATE core.person p SET tenant_id = COALESCE(pos.tenant_id, default_tenant)
    FROM (SELECT position_id, tenant_id FROM core.position) pos
    WHERE p.position_id = pos.position_id AND p.tenant_id IS NULL;
    UPDATE core.person SET tenant_id = default_tenant WHERE tenant_id IS NULL;

    UPDATE core.user_person up SET tenant_id = p.tenant_id
    FROM core.person p WHERE up.person_id = p.person_id AND up.tenant_id IS NULL;

    UPDATE security.auth_session s SET tenant_id = up.tenant_id
    FROM core.user_person up WHERE s.active_user_person_id = up.user_person_id AND s.tenant_id IS NULL;
    UPDATE security.auth_session SET tenant_id = default_tenant WHERE tenant_id IS NULL;

    -- Catalog child tables
    UPDATE catalog.class_schedule cs SET tenant_id = b.tenant_id
    FROM core.branch b WHERE cs.branch_id = b.branch_id AND cs.tenant_id IS NULL;

    UPDATE catalog.course_schedule cs SET tenant_id = c.tenant_id
    FROM catalog.course c WHERE cs.course_id = c.course_id AND cs.tenant_id IS NULL;

    UPDATE catalog.training_plan_requirement r SET tenant_id = tp.tenant_id
    FROM catalog.training_plan tp WHERE r.training_plan_id = tp.training_plan_id AND r.tenant_id IS NULL;

    UPDATE catalog.training_plan_access_rule ar SET tenant_id = tp.tenant_id
    FROM catalog.training_plan tp WHERE ar.training_plan_id = tp.training_plan_id AND ar.tenant_id IS NULL;

    UPDATE catalog.training_plan_addon a SET tenant_id = tp.tenant_id
    FROM catalog.training_plan tp WHERE a.training_plan_id = tp.training_plan_id AND a.tenant_id IS NULL;

    -- Finance
    UPDATE finance.wallet w SET tenant_id = p.tenant_id
    FROM core.person p WHERE w.person_id = p.person_id AND w.tenant_id IS NULL;

    UPDATE finance.wallet_transaction wt SET tenant_id = w.tenant_id
    FROM finance.wallet w WHERE wt.wallet_id = w.wallet_id AND wt.tenant_id IS NULL;

    UPDATE finance.wallet_top_up_request r SET tenant_id = w.tenant_id
    FROM finance.wallet w WHERE r.wallet_id = w.wallet_id AND r.tenant_id IS NULL;

    UPDATE finance.course_purchase cp SET tenant_id = p.tenant_id
    FROM core.person p WHERE cp.student_person_id = p.person_id AND cp.tenant_id IS NULL;

    -- Training
    UPDATE training.student_enrollment se SET tenant_id = p.tenant_id
    FROM core.person p WHERE se.student_person_id = p.person_id AND se.tenant_id IS NULL;

    UPDATE finance.enrollment_addon_purchase eap SET tenant_id = se.tenant_id
    FROM training.student_enrollment se WHERE eap.student_enrollment_id = se.student_enrollment_id AND eap.tenant_id IS NULL;

    UPDATE training.student_enrollment_schedule ses SET tenant_id = se.tenant_id
    FROM training.student_enrollment se WHERE ses.student_enrollment_id = se.student_enrollment_id AND ses.tenant_id IS NULL;

    UPDATE training.class_session cs SET tenant_id = c.tenant_id
    FROM catalog.course c WHERE cs.course_id = c.course_id AND cs.tenant_id IS NULL;

    UPDATE training.course_staff_assignment csa SET tenant_id = p.tenant_id
    FROM core.person p WHERE csa.staff_person_id = p.person_id AND csa.tenant_id IS NULL;

    UPDATE training.session_attendance sa SET tenant_id = cs.tenant_id
    FROM training.class_session cs WHERE sa.class_session_id = cs.class_session_id AND sa.tenant_id IS NULL;

    UPDATE training.coach_timesheet ct SET tenant_id = cs.tenant_id
    FROM training.class_session cs WHERE ct.class_session_id = cs.class_session_id AND ct.tenant_id IS NULL;

    UPDATE training.belt_exam be SET tenant_id = p.tenant_id
    FROM core.person p WHERE be.person_id = p.person_id AND be.tenant_id IS NULL;

    UPDATE training.leave_request lr SET tenant_id = p.tenant_id
    FROM core.person p WHERE lr.person_id = p.person_id AND lr.tenant_id IS NULL;

    UPDATE training.student_session_booking ssb SET tenant_id = se.tenant_id
    FROM training.student_enrollment se WHERE ssb.student_enrollment_id = se.student_enrollment_id AND ssb.tenant_id IS NULL;

    UPDATE training.enrollment_schedule_change_request escr SET tenant_id = ses.tenant_id
    FROM training.student_enrollment_schedule ses WHERE escr.student_enrollment_schedule_id = ses.student_enrollment_schedule_id AND escr.tenant_id IS NULL;

    UPDATE training.student_lifecycle_event sle SET tenant_id = p.tenant_id
    FROM core.person p WHERE sle.student_person_id = p.person_id AND sle.tenant_id IS NULL;

    UPDATE training.face_check_in_event fce SET tenant_id = fcs.tenant_id
    FROM training.face_check_in_scan fcs WHERE fce.request_id = fcs.request_id AND fce.tenant_id IS NULL;
    UPDATE training.face_check_in_event SET tenant_id = default_tenant WHERE tenant_id IS NULL;

    -- CRM, Notification, Skill, IoT
    UPDATE crm.trial_registration tr SET tenant_id = l.tenant_id
    FROM crm.lead l WHERE tr.lead_id = l.lead_id AND tr.tenant_id IS NULL;

    UPDATE notification.notification_recipient nr SET tenant_id = n.tenant_id
    FROM notification.notification n WHERE nr.notification_id = n.notification_id AND nr.tenant_id IS NULL;

    UPDATE skill.fitness_record fr SET tenant_id = p.tenant_id
    FROM core.person p WHERE fr.student_person_id = p.person_id AND fr.tenant_id IS NULL;

    UPDATE iot.device_event de SET tenant_id = md.tenant_id
    FROM iot.managed_device md WHERE de.device_id = md.device_id AND de.tenant_id IS NULL;
END $$;

-- 5. Kiem tra toan ven truoc khi dat NOT NULL
DO $$
DECLARE
    tbl TEXT;
    null_cnt BIGINT;
    tables_to_check TEXT[] := ARRAY[
        'security.role', 'security.role_permission', 'security.user_role',
        'core.branch', 'core.position', 'core.person', 'core.user_person',
        'catalog.class_schedule', 'catalog.course', 'catalog.course_schedule',
        'catalog.training_plan', 'catalog.training_plan_requirement',
        'catalog.training_plan_access_rule', 'catalog.training_plan_addon',
        'finance.wallet', 'finance.wallet_transaction', 'finance.wallet_top_up_request',
        'finance.course_purchase', 'finance.enrollment_addon_purchase',
        'training.student_enrollment', 'training.student_enrollment_schedule',
        'training.class_session', 'training.course_staff_assignment',
        'training.session_attendance', 'training.coach_timesheet',
        'training.belt_exam', 'training.leave_request',
        'training.student_session_booking', 'training.enrollment_schedule_change_request',
        'training.student_lifecycle_event', 'training.attendance_command',
        'training.face_check_in_scan', 'training.face_check_in_event',
        'crm.lead', 'crm.trial_registration',
        'notification.notification', 'notification.notification_recipient',
        'skill.fitness', 'skill.fitness_record',
        'iot.managed_device', 'iot.device_event',
        'infrastructure.projection_outbox', 'infrastructure.projection_scope_state'
    ];
BEGIN
    FOREACH tbl IN ARRAY tables_to_check LOOP
        EXECUTE format('SELECT COUNT(*) FROM %s WHERE tenant_id IS NULL', tbl) INTO null_cnt;
        IF null_cnt > 0 THEN
            RAISE EXCEPTION 'Backfill failed: % still has % NULL tenant_id rows', tbl, null_cnt;
        END IF;
    END LOOP;
END $$;

-- 6. Dat NOT NULL cho tenant_id (tru auth_session.tenant_id cho phep NULL khi dang chon tenant)
ALTER TABLE security.role ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE security.role_permission ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE security.user_role ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE security.auth_session
    ADD CONSTRAINT ck_auth_session_active_person_requires_tenant
    CHECK (active_user_person_id IS NULL OR tenant_id IS NOT NULL);

ALTER TABLE core.branch ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE core.position ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE core.person ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE core.user_person ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE catalog.class_schedule ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE catalog.course ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE catalog.course_schedule ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE catalog.training_plan ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE catalog.training_plan_requirement ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE catalog.training_plan_access_rule ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE catalog.training_plan_addon ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE finance.wallet ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE finance.wallet_transaction ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE finance.wallet_top_up_request ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE finance.course_purchase ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE finance.enrollment_addon_purchase ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE training.student_enrollment ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE training.student_enrollment_schedule ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE training.class_session ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE training.course_staff_assignment ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE training.session_attendance ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE training.coach_timesheet ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE training.belt_exam ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE training.leave_request ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE training.student_session_booking ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE training.enrollment_schedule_change_request ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE training.student_lifecycle_event ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE training.attendance_command ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE training.face_check_in_scan ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE training.face_check_in_event ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE crm.lead ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE crm.trial_registration ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE notification.notification ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE notification.notification_recipient ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE skill.fitness ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE skill.fitness_record ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE iot.managed_device ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE iot.device_event ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE infrastructure.projection_outbox ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE infrastructure.projection_scope_state ALTER COLUMN tenant_id SET NOT NULL;

-- 7. Chuyen PK cua security.role, role_permission, user_role & projection_scope_state sang pham vi tenant
ALTER TABLE security.role_permission DROP CONSTRAINT IF EXISTS role_permission_role_id_fkey;
ALTER TABLE security.user_role DROP CONSTRAINT IF EXISTS user_role_role_id_fkey;
ALTER TABLE security.role DROP CONSTRAINT IF EXISTS role_pkey;
ALTER TABLE security.role ADD CONSTRAINT pk_security_role PRIMARY KEY (tenant_id, role_id);

ALTER TABLE security.role_permission DROP CONSTRAINT IF EXISTS role_permission_pkey;
ALTER TABLE security.role_permission ADD CONSTRAINT pk_security_role_permission PRIMARY KEY (tenant_id, role_id, permission_id);
ALTER TABLE security.role_permission ADD CONSTRAINT fk_role_permission_tenant_role
    FOREIGN KEY (tenant_id, role_id) REFERENCES security.role(tenant_id, role_id);

ALTER TABLE security.user_role DROP CONSTRAINT IF EXISTS user_role_pkey;
ALTER TABLE security.user_role ADD CONSTRAINT pk_security_user_role PRIMARY KEY (tenant_id, user_id, role_id);
ALTER TABLE security.user_role ADD CONSTRAINT fk_user_role_tenant_role
    FOREIGN KEY (tenant_id, role_id) REFERENCES security.role(tenant_id, role_id);
ALTER TABLE security.user_role ADD CONSTRAINT fk_user_role_tenant_membership
    FOREIGN KEY (tenant_id, user_id) REFERENCES core.tenant_membership(tenant_id, user_id);

ALTER TABLE infrastructure.projection_scope_state DROP CONSTRAINT IF EXISTS projection_scope_state_pkey;
ALTER TABLE infrastructure.projection_scope_state ADD CONSTRAINT pk_projection_scope_state PRIMARY KEY (tenant_id, scope_key);

-- 8. Drop unique constraints/indexes cu & tao unique keys moi theo pham vi (tenant_id, ...)
DO $$
DECLARE
    r RECORD;
BEGIN
    -- Drop tat ca UNIQUE constraint (khong phai PK) tren cac bang can chuyen scope
    FOR r IN
        SELECT n.nspname AS schema_name, c.relname AS table_name, con.conname AS constraint_name
        FROM pg_constraint con
        JOIN pg_class c ON c.oid = con.conrelid
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE con.contype = 'u'
          AND (n.nspname || '.' || c.relname) IN (
              'core.position', 'core.person', 'core.user_person',
              'catalog.training_plan_requirement', 'catalog.training_plan_access_rule',
              'finance.wallet', 'finance.wallet_transaction', 'finance.wallet_top_up_request',
              'finance.course_purchase', 'finance.enrollment_addon_purchase',
              'training.student_enrollment', 'training.student_enrollment_schedule',
              'training.course_staff_assignment', 'training.session_attendance',
              'training.coach_timesheet', 'training.student_session_booking',
              'training.face_check_in_event', 'iot.managed_device',
              'infrastructure.projection_outbox'
          )
    LOOP
        EXECUTE format('ALTER TABLE %I.%I DROP CONSTRAINT %I', r.schema_name, r.table_name, r.constraint_name);
    END LOOP;
END $$;

DROP INDEX IF EXISTS training.uk_class_session_course_schedule_date_active;
DROP INDEX IF EXISTS notification.uk_notification_recipient_account;
DROP INDEX IF EXISTS notification.uk_notification_recipient_context;
DROP INDEX IF EXISTS iot.uk_device_open_alert;
DROP INDEX IF EXISTS iot.uk_managed_device_provisioning_public_id;
DROP INDEX IF EXISTS iot.uk_managed_device_hardware_id;

ALTER TABLE core.position ADD CONSTRAINT uk_position_tenant_code UNIQUE (tenant_id, code);
ALTER TABLE core.person ADD CONSTRAINT uk_person_tenant_national_code UNIQUE (tenant_id, national_code);
ALTER TABLE core.person ADD CONSTRAINT uk_person_tenant_person_code UNIQUE (tenant_id, person_code);
ALTER TABLE core.user_person ADD CONSTRAINT uk_user_person_tenant_relationship UNIQUE (tenant_id, user_id, person_id, relationship_type);

ALTER TABLE catalog.training_plan_requirement ADD CONSTRAINT uk_training_plan_requirement_tenant UNIQUE (tenant_id, training_plan_id, session_type);
ALTER TABLE catalog.training_plan_access_rule ADD CONSTRAINT uk_training_plan_access_rule_tenant UNIQUE (tenant_id, training_plan_id, target_course_id, session_type);

ALTER TABLE finance.wallet ADD CONSTRAINT uk_wallet_tenant_person UNIQUE (tenant_id, person_id);
ALTER TABLE finance.wallet_transaction ADD CONSTRAINT uk_wallet_tx_tenant_type_reference UNIQUE (tenant_id, type, external_reference);
ALTER TABLE finance.wallet_top_up_request ADD CONSTRAINT uk_wallet_top_up_tenant_client_request UNIQUE (tenant_id, client_request_id);
ALTER TABLE finance.wallet_top_up_request ADD CONSTRAINT uk_wallet_top_up_tenant_proof_sha256 UNIQUE (tenant_id, proof_sha256);
ALTER TABLE finance.wallet_top_up_request ADD CONSTRAINT uk_wallet_top_up_tenant_wallet_tx UNIQUE (tenant_id, wallet_transaction_id);
ALTER TABLE finance.course_purchase ADD CONSTRAINT uk_course_purchase_tenant_debit_tx UNIQUE (tenant_id, debit_transaction_id);
ALTER TABLE finance.enrollment_addon_purchase ADD CONSTRAINT uk_enrollment_addon_purchase_tenant_tx UNIQUE (tenant_id, wallet_transaction_id);

ALTER TABLE training.student_enrollment ADD CONSTRAINT uk_student_enrollment_tenant_purchase UNIQUE (tenant_id, course_purchase_id);
ALTER TABLE training.student_enrollment_schedule ADD CONSTRAINT uk_student_enrollment_schedule_tenant UNIQUE (tenant_id, student_enrollment_id, course_schedule_id, effective_from);
CREATE UNIQUE INDEX uk_class_session_tenant_course_schedule_date_active
    ON training.class_session(tenant_id, course_schedule_id, session_date)
    WHERE status <> 'CANCELLED';
ALTER TABLE training.course_staff_assignment ADD CONSTRAINT uk_course_staff_assignment_tenant_schedule_staff_type UNIQUE (tenant_id, course_schedule_id, staff_person_id, assignment_type);
ALTER TABLE training.session_attendance ADD CONSTRAINT uk_session_attendance_tenant_session_enrollment UNIQUE (tenant_id, class_session_id, student_enrollment_id);
ALTER TABLE training.session_attendance ADD CONSTRAINT uk_session_attendance_tenant_session_staff UNIQUE (tenant_id, class_session_id, course_staff_assignment_id);
ALTER TABLE training.session_attendance ADD CONSTRAINT uk_session_attendance_tenant_session_person UNIQUE (tenant_id, class_session_id, person_id);
ALTER TABLE training.coach_timesheet ADD CONSTRAINT uk_coach_timesheet_tenant_assignment_session UNIQUE (tenant_id, course_staff_assignment_id, class_session_id);
ALTER TABLE training.student_session_booking ADD CONSTRAINT uk_student_session_booking_tenant UNIQUE (tenant_id, student_enrollment_id, class_session_id);
ALTER TABLE training.face_check_in_event ADD CONSTRAINT uk_face_check_in_event_tenant_request_stage UNIQUE (tenant_id, request_id, stage);

CREATE UNIQUE INDEX uk_notification_recipient_tenant_account
    ON notification.notification_recipient(tenant_id, notification_id, recipient_user_id)
    WHERE context_person_id IS NULL;
CREATE UNIQUE INDEX uk_notification_recipient_tenant_context
    ON notification.notification_recipient(tenant_id, notification_id, recipient_user_id, context_person_id)
    WHERE context_person_id IS NOT NULL;

ALTER TABLE iot.managed_device ADD CONSTRAINT uk_managed_device_tenant_code UNIQUE (tenant_id, device_code);
CREATE UNIQUE INDEX uk_managed_device_tenant_provisioning_public_id
    ON iot.managed_device(tenant_id, provisioning_public_id) WHERE provisioning_public_id IS NOT NULL;
CREATE UNIQUE INDEX uk_managed_device_tenant_hardware_id
    ON iot.managed_device(tenant_id, hardware_id) WHERE hardware_id IS NOT NULL;
CREATE UNIQUE INDEX uk_device_open_alert_tenant
    ON iot.device_event(tenant_id, device_id, event_code) WHERE resolved_at IS NULL;

ALTER TABLE infrastructure.projection_outbox ADD CONSTRAINT uk_projection_outbox_tenant_key UNIQUE (tenant_id, projection_key);

-- 9. Tao UNIQUE (tenant_id, pk) tren cac bang cha de ho tro composite FK (tenant_id, parent_id)
ALTER TABLE core.branch ADD CONSTRAINT uk_branch_tenant_id UNIQUE (tenant_id, branch_id);
ALTER TABLE core.position ADD CONSTRAINT uk_position_tenant_id UNIQUE (tenant_id, position_id);
ALTER TABLE core.person ADD CONSTRAINT uk_person_tenant_id UNIQUE (tenant_id, person_id);
ALTER TABLE core.user_person ADD CONSTRAINT uk_user_person_tenant_id UNIQUE (tenant_id, user_person_id);
ALTER TABLE catalog.class_schedule ADD CONSTRAINT uk_class_schedule_tenant_id UNIQUE (tenant_id, schedule_id);
ALTER TABLE catalog.course ADD CONSTRAINT uk_course_tenant_id UNIQUE (tenant_id, course_id);
ALTER TABLE catalog.course_schedule ADD CONSTRAINT uk_course_schedule_tenant_id UNIQUE (tenant_id, course_schedule_id);
ALTER TABLE catalog.training_plan ADD CONSTRAINT uk_training_plan_tenant_id UNIQUE (tenant_id, training_plan_id);
ALTER TABLE catalog.training_plan_requirement ADD CONSTRAINT uk_training_plan_req_tenant_id UNIQUE (tenant_id, training_plan_requirement_id);
ALTER TABLE catalog.training_plan_addon ADD CONSTRAINT uk_training_plan_addon_tenant_id UNIQUE (tenant_id, training_plan_addon_id);
ALTER TABLE finance.wallet ADD CONSTRAINT uk_wallet_tenant_id UNIQUE (tenant_id, wallet_id);
ALTER TABLE finance.wallet_transaction ADD CONSTRAINT uk_wallet_tx_tenant_id UNIQUE (tenant_id, wallet_transaction_id);
ALTER TABLE finance.course_purchase ADD CONSTRAINT uk_course_purchase_tenant_id UNIQUE (tenant_id, course_purchase_id);
ALTER TABLE finance.enrollment_addon_purchase ADD CONSTRAINT uk_enrollment_addon_purchase_tenant_id UNIQUE (tenant_id, enrollment_addon_purchase_id);
ALTER TABLE training.student_enrollment ADD CONSTRAINT uk_student_enrollment_tenant_id UNIQUE (tenant_id, student_enrollment_id);
ALTER TABLE training.student_enrollment_schedule ADD CONSTRAINT uk_student_enrollment_schedule_tenant_id UNIQUE (tenant_id, student_enrollment_schedule_id);
ALTER TABLE training.class_session ADD CONSTRAINT uk_class_session_tenant_id UNIQUE (tenant_id, class_session_id);
ALTER TABLE training.course_staff_assignment ADD CONSTRAINT uk_course_staff_assignment_tenant_id UNIQUE (tenant_id, course_staff_assignment_id);
ALTER TABLE training.leave_request ADD CONSTRAINT uk_leave_request_tenant_id UNIQUE (tenant_id, leave_request_id);
ALTER TABLE crm.lead ADD CONSTRAINT uk_crm_lead_tenant_id UNIQUE (tenant_id, lead_id);
ALTER TABLE notification.notification ADD CONSTRAINT uk_notification_tenant_id UNIQUE (tenant_id, notification_id);
ALTER TABLE skill.fitness ADD CONSTRAINT uk_fitness_tenant_id UNIQUE (tenant_id, fitness_id);
ALTER TABLE iot.managed_device ADD CONSTRAINT uk_managed_device_tenant_id UNIQUE (tenant_id, device_id);

-- 10. Composite FKs (tenant_id, parent_id) chan lien ket cheo tenant o tang DB
ALTER TABLE security.auth_session
    ADD CONSTRAINT fk_auth_session_tenant FOREIGN KEY (tenant_id) REFERENCES core.tenant(tenant_id),
    ADD CONSTRAINT fk_auth_session_tenant_active_user_person FOREIGN KEY (tenant_id, active_user_person_id) REFERENCES core.user_person(tenant_id, user_person_id);

ALTER TABLE core.person ADD CONSTRAINT fk_person_tenant_position FOREIGN KEY (tenant_id, position_id) REFERENCES core.position(tenant_id, position_id);
ALTER TABLE core.user_person
    ADD CONSTRAINT fk_user_person_tenant_membership FOREIGN KEY (tenant_id, user_id) REFERENCES core.tenant_membership(tenant_id, user_id),
    ADD CONSTRAINT fk_user_person_tenant_person FOREIGN KEY (tenant_id, person_id) REFERENCES core.person(tenant_id, person_id);

ALTER TABLE catalog.class_schedule ADD CONSTRAINT fk_class_schedule_tenant_branch FOREIGN KEY (tenant_id, branch_id) REFERENCES core.branch(tenant_id, branch_id);
ALTER TABLE catalog.course_schedule
    ADD CONSTRAINT fk_course_schedule_tenant_course FOREIGN KEY (tenant_id, course_id) REFERENCES catalog.course(tenant_id, course_id),
    ADD CONSTRAINT fk_course_schedule_tenant_schedule FOREIGN KEY (tenant_id, class_schedule_id) REFERENCES catalog.class_schedule(tenant_id, schedule_id);
ALTER TABLE catalog.training_plan_requirement ADD CONSTRAINT fk_tp_req_tenant_plan FOREIGN KEY (tenant_id, training_plan_id) REFERENCES catalog.training_plan(tenant_id, training_plan_id);
ALTER TABLE catalog.training_plan_access_rule
    ADD CONSTRAINT fk_tp_access_tenant_plan FOREIGN KEY (tenant_id, training_plan_id) REFERENCES catalog.training_plan(tenant_id, training_plan_id),
    ADD CONSTRAINT fk_tp_access_tenant_course FOREIGN KEY (tenant_id, target_course_id) REFERENCES catalog.course(tenant_id, course_id);
ALTER TABLE catalog.training_plan_addon ADD CONSTRAINT fk_tp_addon_tenant_plan FOREIGN KEY (tenant_id, training_plan_id) REFERENCES catalog.training_plan(tenant_id, training_plan_id);

ALTER TABLE finance.wallet ADD CONSTRAINT fk_wallet_tenant_person FOREIGN KEY (tenant_id, person_id) REFERENCES core.person(tenant_id, person_id);
ALTER TABLE finance.wallet_transaction ADD CONSTRAINT fk_wallet_tx_tenant_wallet FOREIGN KEY (tenant_id, wallet_id) REFERENCES finance.wallet(tenant_id, wallet_id);
ALTER TABLE finance.wallet_top_up_request
    ADD CONSTRAINT fk_wallet_top_up_tenant_wallet FOREIGN KEY (tenant_id, wallet_id) REFERENCES finance.wallet(tenant_id, wallet_id),
    ADD CONSTRAINT fk_wallet_top_up_tenant_tx FOREIGN KEY (tenant_id, wallet_transaction_id) REFERENCES finance.wallet_transaction(tenant_id, wallet_transaction_id);
ALTER TABLE finance.course_purchase
    ADD CONSTRAINT fk_course_purchase_tenant_person FOREIGN KEY (tenant_id, student_person_id) REFERENCES core.person(tenant_id, person_id),
    ADD CONSTRAINT fk_course_purchase_tenant_plan FOREIGN KEY (tenant_id, training_plan_id) REFERENCES catalog.training_plan(tenant_id, training_plan_id),
    ADD CONSTRAINT fk_course_purchase_tenant_tx FOREIGN KEY (tenant_id, debit_transaction_id) REFERENCES finance.wallet_transaction(tenant_id, wallet_transaction_id);
ALTER TABLE finance.enrollment_addon_purchase
    ADD CONSTRAINT fk_addon_purchase_tenant_enrollment FOREIGN KEY (tenant_id, student_enrollment_id) REFERENCES training.student_enrollment(tenant_id, student_enrollment_id),
    ADD CONSTRAINT fk_addon_purchase_tenant_addon FOREIGN KEY (tenant_id, training_plan_addon_id) REFERENCES catalog.training_plan_addon(tenant_id, training_plan_addon_id),
    ADD CONSTRAINT fk_addon_purchase_tenant_tx FOREIGN KEY (tenant_id, wallet_transaction_id) REFERENCES finance.wallet_transaction(tenant_id, wallet_transaction_id);

ALTER TABLE training.student_enrollment
    ADD CONSTRAINT fk_enrollment_tenant_person FOREIGN KEY (tenant_id, student_person_id) REFERENCES core.person(tenant_id, person_id),
    ADD CONSTRAINT fk_enrollment_tenant_purchase FOREIGN KEY (tenant_id, course_purchase_id) REFERENCES finance.course_purchase(tenant_id, course_purchase_id);
ALTER TABLE training.student_enrollment_schedule
    ADD CONSTRAINT fk_enrollment_schedule_tenant_enrollment FOREIGN KEY (tenant_id, student_enrollment_id) REFERENCES training.student_enrollment(tenant_id, student_enrollment_id),
    ADD CONSTRAINT fk_enrollment_schedule_tenant_cs FOREIGN KEY (tenant_id, course_schedule_id) REFERENCES catalog.course_schedule(tenant_id, course_schedule_id),
    ADD CONSTRAINT fk_enrollment_schedule_tenant_req FOREIGN KEY (tenant_id, training_plan_requirement_id) REFERENCES catalog.training_plan_requirement(tenant_id, training_plan_requirement_id);
ALTER TABLE training.class_session
    ADD CONSTRAINT fk_class_session_tenant_course FOREIGN KEY (tenant_id, course_id) REFERENCES catalog.course(tenant_id, course_id),
    ADD CONSTRAINT fk_class_session_tenant_cs FOREIGN KEY (tenant_id, course_schedule_id) REFERENCES catalog.course_schedule(tenant_id, course_schedule_id);
ALTER TABLE training.course_staff_assignment
    ADD CONSTRAINT fk_csa_tenant_person FOREIGN KEY (tenant_id, staff_person_id) REFERENCES core.person(tenant_id, person_id),
    ADD CONSTRAINT fk_csa_tenant_cs FOREIGN KEY (tenant_id, course_schedule_id) REFERENCES catalog.course_schedule(tenant_id, course_schedule_id);
ALTER TABLE training.session_attendance
    ADD CONSTRAINT fk_sa_tenant_session FOREIGN KEY (tenant_id, class_session_id) REFERENCES training.class_session(tenant_id, class_session_id),
    ADD CONSTRAINT fk_sa_tenant_person FOREIGN KEY (tenant_id, person_id) REFERENCES core.person(tenant_id, person_id),
    ADD CONSTRAINT fk_sa_tenant_enrollment FOREIGN KEY (tenant_id, student_enrollment_id) REFERENCES training.student_enrollment(tenant_id, student_enrollment_id),
    ADD CONSTRAINT fk_sa_tenant_csa FOREIGN KEY (tenant_id, course_staff_assignment_id) REFERENCES training.course_staff_assignment(tenant_id, course_staff_assignment_id);
ALTER TABLE training.coach_timesheet
    ADD CONSTRAINT fk_timesheet_tenant_csa FOREIGN KEY (tenant_id, course_staff_assignment_id) REFERENCES training.course_staff_assignment(tenant_id, course_staff_assignment_id),
    ADD CONSTRAINT fk_timesheet_tenant_session FOREIGN KEY (tenant_id, class_session_id) REFERENCES training.class_session(tenant_id, class_session_id);
ALTER TABLE training.belt_exam ADD CONSTRAINT fk_belt_exam_tenant_person FOREIGN KEY (tenant_id, person_id) REFERENCES core.person(tenant_id, person_id);
ALTER TABLE training.leave_request
    ADD CONSTRAINT fk_leave_req_tenant_person FOREIGN KEY (tenant_id, person_id) REFERENCES core.person(tenant_id, person_id),
    ADD CONSTRAINT fk_leave_req_tenant_leave_session FOREIGN KEY (tenant_id, leave_class_session_id) REFERENCES training.class_session(tenant_id, class_session_id),
    ADD CONSTRAINT fk_leave_req_tenant_makeup_session FOREIGN KEY (tenant_id, makeup_class_session_id) REFERENCES training.class_session(tenant_id, class_session_id);
ALTER TABLE training.student_session_booking
    ADD CONSTRAINT fk_booking_tenant_enrollment FOREIGN KEY (tenant_id, student_enrollment_id) REFERENCES training.student_enrollment(tenant_id, student_enrollment_id),
    ADD CONSTRAINT fk_booking_tenant_session FOREIGN KEY (tenant_id, class_session_id) REFERENCES training.class_session(tenant_id, class_session_id),
    ADD CONSTRAINT fk_booking_tenant_req FOREIGN KEY (tenant_id, training_plan_requirement_id) REFERENCES catalog.training_plan_requirement(tenant_id, training_plan_requirement_id),
    ADD CONSTRAINT fk_booking_tenant_addon FOREIGN KEY (tenant_id, enrollment_addon_purchase_id) REFERENCES finance.enrollment_addon_purchase(tenant_id, enrollment_addon_purchase_id),
    ADD CONSTRAINT fk_booking_tenant_leave FOREIGN KEY (tenant_id, leave_request_id) REFERENCES training.leave_request(tenant_id, leave_request_id);
ALTER TABLE training.enrollment_schedule_change_request
    ADD CONSTRAINT fk_escr_tenant_ses FOREIGN KEY (tenant_id, student_enrollment_schedule_id) REFERENCES training.student_enrollment_schedule(tenant_id, student_enrollment_schedule_id),
    ADD CONSTRAINT fk_escr_tenant_target_cs FOREIGN KEY (tenant_id, target_course_schedule_id) REFERENCES catalog.course_schedule(tenant_id, course_schedule_id);
ALTER TABLE training.student_lifecycle_event
    ADD CONSTRAINT fk_sle_tenant_person FOREIGN KEY (tenant_id, student_person_id) REFERENCES core.person(tenant_id, person_id);

ALTER TABLE crm.lead ADD CONSTRAINT fk_crm_lead_tenant_person FOREIGN KEY (tenant_id, converted_person_id) REFERENCES core.person(tenant_id, person_id);
ALTER TABLE crm.trial_registration
    ADD CONSTRAINT fk_trial_tenant_lead FOREIGN KEY (tenant_id, lead_id) REFERENCES crm.lead(tenant_id, lead_id),
    ADD CONSTRAINT fk_trial_tenant_session FOREIGN KEY (tenant_id, class_session_id) REFERENCES training.class_session(tenant_id, class_session_id),
    ADD CONSTRAINT fk_trial_tenant_coach FOREIGN KEY (tenant_id, coach_person_id) REFERENCES core.person(tenant_id, person_id);

ALTER TABLE notification.notification_recipient
    ADD CONSTRAINT fk_nr_tenant_notification FOREIGN KEY (tenant_id, notification_id) REFERENCES notification.notification(tenant_id, notification_id),
    ADD CONSTRAINT fk_nr_tenant_context_person FOREIGN KEY (tenant_id, context_person_id) REFERENCES core.person(tenant_id, person_id),
    ADD CONSTRAINT fk_nr_tenant_membership FOREIGN KEY (tenant_id, recipient_user_id) REFERENCES core.tenant_membership(tenant_id, user_id);

ALTER TABLE skill.fitness_record
    ADD CONSTRAINT fk_fitness_record_tenant_person FOREIGN KEY (tenant_id, student_person_id) REFERENCES core.person(tenant_id, person_id),
    ADD CONSTRAINT fk_fitness_record_tenant_fitness FOREIGN KEY (tenant_id, fitness_id) REFERENCES skill.fitness(tenant_id, fitness_id),
    ADD CONSTRAINT fk_fitness_record_tenant_coach FOREIGN KEY (tenant_id, recorded_by_coach_id) REFERENCES core.person(tenant_id, person_id);

ALTER TABLE iot.device_event ADD CONSTRAINT fk_device_event_tenant_device FOREIGN KEY (tenant_id, device_id) REFERENCES iot.managed_device(tenant_id, device_id);

-- 11. Composite indexes theo truy van thuc te (khong tao index don tenant_id hang loat)
CREATE INDEX idx_person_tenant_status_name ON core.person(tenant_id, status, full_name);
CREATE INDEX idx_user_person_tenant_user_active ON core.user_person(tenant_id, user_id, active);
CREATE INDEX idx_course_tenant_status ON catalog.course(tenant_id, status);
CREATE INDEX idx_class_session_tenant_date_status ON training.class_session(tenant_id, session_date, status);
CREATE INDEX idx_student_enrollment_tenant_status_period ON training.student_enrollment(tenant_id, status, start_date, end_date);
CREATE INDEX idx_wallet_tx_tenant_status_created ON finance.wallet_transaction(tenant_id, status, created_at DESC);
CREATE INDEX idx_wallet_top_up_tenant_status_created ON finance.wallet_top_up_request(tenant_id, status, created_at DESC);
CREATE INDEX idx_crm_lead_tenant_status_created ON crm.lead(tenant_id, status, created_at DESC);
CREATE INDEX idx_notification_recipient_tenant_user_read ON notification.notification_recipient(tenant_id, recipient_user_id, read, created_at DESC);
CREATE INDEX idx_projection_outbox_tenant_ready ON infrastructure.projection_outbox(tenant_id, status, next_attempt_at);
