-- Version 17: Course owns the training level. Class sessions retain their
-- schedule_level column as a historical snapshot.
ALTER TABLE catalog.course ADD COLUMN level VARCHAR(20);

DO $$
DECLARE
    invalid_course_ids TEXT;
BEGIN
    SELECT string_agg(course_id::text, ', ' ORDER BY course_id::text)
      INTO invalid_course_ids
      FROM (
          SELECT c.course_id
            FROM catalog.course c
            LEFT JOIN catalog.course_schedule cs ON cs.course_id = c.course_id
            LEFT JOIN catalog.class_schedule schedule
              ON schedule.schedule_id = cs.class_schedule_id
           GROUP BY c.course_id
          HAVING COUNT(DISTINCT schedule.level) <> 1
      ) invalid_courses;

    IF invalid_course_ids IS NOT NULL THEN
        RAISE EXCEPTION
            'Cannot migrate schedule level to course. Courses without exactly one distinct level: %',
            invalid_course_ids;
    END IF;
END $$;

UPDATE catalog.course course
   SET level = inferred.level
  FROM (
      SELECT cs.course_id, MIN(schedule.level) AS level
        FROM catalog.course_schedule cs
        JOIN catalog.class_schedule schedule
          ON schedule.schedule_id = cs.class_schedule_id
       GROUP BY cs.course_id
  ) inferred
 WHERE inferred.course_id = course.course_id;

ALTER TABLE catalog.course
    ALTER COLUMN level SET NOT NULL,
    ADD CONSTRAINT ck_course_level
        CHECK (level IN ('BASIC', 'ADVANCED', 'EXPERT'));

COMMENT ON COLUMN catalog.course.level IS
    'Training level shared by every timetable assigned to this course';

ALTER TABLE catalog.class_schedule DROP CONSTRAINT IF EXISTS ck_schedule_level;
COMMENT ON COLUMN catalog.class_schedule.level IS NULL;
ALTER TABLE catalog.class_schedule DROP COLUMN level;
