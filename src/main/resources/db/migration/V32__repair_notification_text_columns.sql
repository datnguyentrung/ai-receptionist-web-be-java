DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'notification'
          AND table_name = 'notification'
          AND column_name = 'title'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE notification.notification
            ALTER COLUMN title TYPE VARCHAR(150)
            USING convert_from(title, 'UTF8');
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'notification'
          AND table_name = 'notification'
          AND column_name = 'body'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE notification.notification
            ALTER COLUMN body TYPE VARCHAR(1000)
            USING convert_from(body, 'UTF8');
    END IF;
END $$;
