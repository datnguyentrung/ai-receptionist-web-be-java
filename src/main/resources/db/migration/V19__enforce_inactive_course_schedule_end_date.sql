-- Preserve the historical edit date where possible while ensuring every
-- inactive course schedule has a valid, non-null end date.
UPDATE catalog.course_schedule
SET end_date = GREATEST(updated_at::date, start_date)
WHERE status = 'INACTIVE'
  AND end_date IS NULL;

ALTER TABLE catalog.course_schedule
    ADD CONSTRAINT ck_course_schedule_inactive_end_date
    CHECK (status <> 'INACTIVE' OR end_date IS NOT NULL);
