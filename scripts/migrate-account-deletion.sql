-- One-time MySQL migration: retain deleted accounts as anonymized tombstones.
--
-- Safe to run more than once: deleted_at is created only when it is missing.
--
-- Deployment order:
--   1. Stop all application instances.
--   2. Back up the database.
--   3. Run this script.
--   4. Deploy the backend account-deletion workflow.

SET @deleted_at_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name = 'deleted_at'
);

SET @add_deleted_at_sql = IF(
    @deleted_at_exists = 0,
    'ALTER TABLE users ADD COLUMN deleted_at DATETIME(6) NULL',
    'SELECT ''users.deleted_at already exists'''
);

PREPARE add_deleted_at FROM @add_deleted_at_sql;
EXECUTE add_deleted_at;
DEALLOCATE PREPARE add_deleted_at;
