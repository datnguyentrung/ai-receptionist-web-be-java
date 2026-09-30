INSERT INTO core.position (position_id, code, name, description, active, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000090000',
    'STUDENT',
    'Học viên',
    'Student business position',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    active = EXCLUDED.active,
    updated_at = CURRENT_TIMESTAMP;
