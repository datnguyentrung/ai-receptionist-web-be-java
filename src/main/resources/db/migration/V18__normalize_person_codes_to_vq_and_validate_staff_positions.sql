-- Normalize all person codes to the single VQ_ namespace before the runtime
-- policy starts deriving student/staff eligibility from business positions.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM core.person legacy
        JOIN core.person target
          ON target.person_code = 'VQ_' || substring(legacy.person_code FROM 5)
         AND target.person_id <> legacy.person_id
        WHERE legacy.person_code LIKE 'VQT\_%'
    ) THEN
        RAISE EXCEPTION 'Cannot normalize VQT_ person codes because a VQ_ target code already exists';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM core.position
        WHERE upper(code) = 'ASSISTANT_1'
          AND active = TRUE
    ) THEN
        RAISE EXCEPTION 'Active ASSISTANT_1 position is required to normalize legacy course staff';
    END IF;
END $$;

-- Legacy course staff without an active recognized staff position default to ASSISTANT_1.
UPDATE core.person person
SET position_id = assistant.position_id
FROM core.position assistant
WHERE upper(assistant.code) = 'ASSISTANT_1'
  AND assistant.active = TRUE
  AND EXISTS (
      SELECT 1
      FROM training.course_staff_assignment assignment
      WHERE assignment.staff_person_id = person.person_id
  )
  AND NOT EXISTS (
      SELECT 1
      FROM core.position current_position
      WHERE current_position.position_id = person.position_id
        AND current_position.active = TRUE
        AND upper(current_position.code) IN (
            'COACH_JUNIOR', 'COACH_MIDDLE', 'COACH_SENIOR',
            'ASSISTANT_1', 'ASSISTANT_2', 'ASSISTANT_3', 'MANAGER_1'
        )
  );

UPDATE core.person
SET person_code = 'VQ_' || substring(person_code FROM 5)
WHERE person_code LIKE 'VQT\_%';

ALTER TABLE core.person DROP CONSTRAINT IF EXISTS ck_person_code_prefix;
ALTER TABLE core.person
    ADD CONSTRAINT ck_person_code_prefix CHECK (person_code LIKE 'VQ\_%');
