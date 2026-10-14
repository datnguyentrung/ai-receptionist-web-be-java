-- Run against staging/production before deploying V17. The result set must be empty.
-- Back up the database before applying the forward-only migration.
SELECT
    course.course_id,
    course.name,
    COUNT(DISTINCT schedule.level) AS distinct_level_count,
    ARRAY_AGG(DISTINCT schedule.level ORDER BY schedule.level)
        FILTER (WHERE schedule.level IS NOT NULL) AS levels
FROM catalog.course course
LEFT JOIN catalog.course_schedule course_schedule
    ON course_schedule.course_id = course.course_id
LEFT JOIN catalog.class_schedule schedule
    ON schedule.schedule_id = course_schedule.class_schedule_id
GROUP BY course.course_id, course.name
HAVING COUNT(DISTINCT schedule.level) <> 1
ORDER BY course.course_id;
