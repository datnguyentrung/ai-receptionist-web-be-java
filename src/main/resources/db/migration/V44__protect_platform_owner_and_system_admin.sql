-- ============================================================================
-- V44: Bao ve Platform Owner va role SYSTEM_ADMIN
-- - Tao bang security.platform_owner (duy nhat 1 dong tren toan nen tang)
-- - Backfill tu user dang giu SYSTEM_ADMIN trong security.user_role (bat buoc dung 1 user)
-- - Trigger chan gan SYSTEM_ADMIN cho bat ky user nao khac ngoai platform_owner
-- ============================================================================

CREATE TABLE security.platform_owner (
    singleton_key BOOLEAN PRIMARY KEY DEFAULT TRUE,
    user_id UUID NOT NULL REFERENCES security.users(user_id),
    assigned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_platform_owner_singleton CHECK (singleton_key = TRUE)
);

DO $$
DECLARE
    owner_count BIGINT;
    owner_user_id UUID;
BEGIN
    SELECT COUNT(DISTINCT user_id), MIN(user_id::text)::uuid
    INTO owner_count, owner_user_id
    FROM security.user_role
    WHERE role_id = 'SYSTEM_ADMIN';

    IF owner_count <> 1 THEN
        RAISE EXCEPTION 'Expected exactly 1 SYSTEM_ADMIN user for platform_owner backfill, found %', owner_count;
    END IF;

    INSERT INTO security.platform_owner (singleton_key, user_id, assigned_at)
    VALUES (TRUE, owner_user_id, NOW())
    ON CONFLICT (singleton_key) DO NOTHING;
END $$;

CREATE OR REPLACE FUNCTION security.enforce_system_admin_owner_only()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_owner_id UUID;
BEGIN
    IF NEW.role_id = 'SYSTEM_ADMIN' THEN
        SELECT user_id INTO v_owner_id FROM security.platform_owner WHERE singleton_key = TRUE;
        IF v_owner_id IS NULL OR NEW.user_id <> v_owner_id THEN
            RAISE EXCEPTION 'SYSTEM_ADMIN role can only be assigned to the platform owner';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_enforce_system_admin_owner_only ON security.user_role;
CREATE TRIGGER trg_enforce_system_admin_owner_only
    BEFORE INSERT OR UPDATE ON security.user_role
    FOR EACH ROW
    EXECUTE FUNCTION security.enforce_system_admin_owner_only();
