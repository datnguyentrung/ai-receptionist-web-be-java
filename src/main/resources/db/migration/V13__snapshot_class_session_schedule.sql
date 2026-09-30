-- Preserve the schedule facts used to create a class session. A Course or
-- ClassSchedule may change later, while historical attendance filters must not.

ALTER TABLE training.class_session
    ADD COLUMN schedule_snapshot_id UUID,
    ADD COLUMN schedule_branch_id BIGINT,
    ADD COLUMN schedule_weekday VARCHAR(20),
    ADD COLUMN schedule_level VARCHAR(20),
    ADD COLUMN schedule_location VARCHAR(50);

-- First use the course schedule that was effective for the session when it
-- still agrees with the immutable date/time facts stored on the session.
UPDATE training.class_session cs
SET schedule_snapshot_id = schedule.schedule_id,
    schedule_branch_id = schedule.branch_id,
    schedule_weekday = schedule.weekday,
    schedule_level = schedule.level,
    schedule_location = schedule.location
FROM catalog.course course
JOIN catalog.class_schedule schedule ON TRUE
WHERE cs.course_id = course.course_id
  AND schedule.schedule_id = CASE
      WHEN course.next_schedule_id IS NOT NULL
       AND course.next_schedule_effective_from IS NOT NULL
       AND cs.session_date >= course.next_schedule_effective_from
      THEN course.next_schedule_id
      ELSE course.schedule_id
  END
  AND schedule.weekday = CASE EXTRACT(ISODOW FROM cs.session_date)::INTEGER
      WHEN 1 THEN 'MONDAY'
      WHEN 2 THEN 'TUESDAY'
      WHEN 3 THEN 'WEDNESDAY'
      WHEN 4 THEN 'THURSDAY'
      WHEN 5 THEN 'FRIDAY'
      WHEN 6 THEN 'SATURDAY'
      WHEN 7 THEN 'SUNDAY'
  END
  AND schedule.start_time = cs.start_time
  AND schedule.end_time = cs.end_time;

-- Older course schedule links are not retained. When date/time identifies one
-- schedule unambiguously, use it as the best historical reconstruction.
WITH unique_schedule AS (
    SELECT cs.class_session_id,
           MIN(schedule.schedule_id::TEXT)::UUID AS schedule_id
    FROM training.class_session cs
    JOIN catalog.class_schedule schedule
      ON schedule.weekday = CASE EXTRACT(ISODOW FROM cs.session_date)::INTEGER
          WHEN 1 THEN 'MONDAY'
          WHEN 2 THEN 'TUESDAY'
          WHEN 3 THEN 'WEDNESDAY'
          WHEN 4 THEN 'THURSDAY'
          WHEN 5 THEN 'FRIDAY'
          WHEN 6 THEN 'SATURDAY'
          WHEN 7 THEN 'SUNDAY'
      END
     AND schedule.start_time = cs.start_time
     AND schedule.end_time = cs.end_time
    WHERE cs.schedule_snapshot_id IS NULL
    GROUP BY cs.class_session_id
    HAVING COUNT(*) = 1
)
UPDATE training.class_session cs
SET schedule_snapshot_id = schedule.schedule_id,
    schedule_branch_id = schedule.branch_id,
    schedule_weekday = schedule.weekday,
    schedule_level = schedule.level,
    schedule_location = schedule.location
FROM unique_schedule inferred
JOIN catalog.class_schedule schedule ON schedule.schedule_id = inferred.schedule_id
WHERE cs.class_session_id = inferred.class_session_id;

-- Ambiguous legacy rows fall back to the currently effective course schedule.
UPDATE training.class_session cs
SET schedule_snapshot_id = schedule.schedule_id,
    schedule_branch_id = schedule.branch_id,
    schedule_weekday = schedule.weekday,
    schedule_level = schedule.level,
    schedule_location = schedule.location
FROM catalog.course course
JOIN catalog.class_schedule schedule ON TRUE
WHERE cs.course_id = course.course_id
  AND schedule.schedule_id = CASE
      WHEN course.next_schedule_id IS NOT NULL
       AND course.next_schedule_effective_from IS NOT NULL
       AND cs.session_date >= course.next_schedule_effective_from
      THEN course.next_schedule_id
      ELSE course.schedule_id
  END
  AND cs.schedule_snapshot_id IS NULL;

ALTER TABLE training.class_session
    ALTER COLUMN schedule_snapshot_id SET NOT NULL,
    ALTER COLUMN schedule_branch_id SET NOT NULL,
    ALTER COLUMN schedule_weekday SET NOT NULL,
    ALTER COLUMN schedule_level SET NOT NULL,
    ALTER COLUMN schedule_location SET NOT NULL,
    ADD CONSTRAINT fk_class_session_schedule_snapshot
        FOREIGN KEY (schedule_snapshot_id) REFERENCES catalog.class_schedule(schedule_id),
    ADD CONSTRAINT ck_class_session_schedule_weekday CHECK (schedule_weekday IN (
        'SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'
    )),
    ADD CONSTRAINT ck_class_session_schedule_level CHECK (
        schedule_level IN ('BASIC', 'ADVANCED', 'EXPERT')
    ),
    ADD CONSTRAINT ck_class_session_schedule_location CHECK (
        schedule_location IN ('INDOOR', 'OUTDOOR', 'ONLINE')
    );

CREATE INDEX idx_class_session_schedule_snapshot_date
    ON training.class_session(schedule_snapshot_id, session_date);

COMMENT ON COLUMN training.class_session.schedule_snapshot_id IS
    'Best-known source schedule used when the session was created.';
COMMENT ON COLUMN training.class_session.schedule_branch_id IS
    'Immutable branch identifier snapshot used by historical attendance filters.';
COMMENT ON COLUMN training.class_session.schedule_weekday IS
    'Immutable weekday snapshot used by historical attendance filters.';
COMMENT ON COLUMN training.class_session.schedule_level IS
    'Immutable level snapshot used by historical attendance filters.';
COMMENT ON COLUMN training.class_session.schedule_location IS
    'Immutable location snapshot used by historical attendance filters.';
