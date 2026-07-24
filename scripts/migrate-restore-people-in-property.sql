-- One-time MySQL migration that restores the roommate occupant-count column.
--
-- Safe to run more than once:
--   * the column is created only when it is missing;
--   * only the invalid legacy value 0 is changed to 1.
--
-- Deployment order:
--   1. Stop all application instances.
--   2. Back up the database.
--   3. Run this script.
--   4. Deploy the backend and frontend that use people_in_property.
--
-- If migrate-consolidate-occupation.sql already dropped the column, its old values cannot be
-- recovered by this script. It recreates the nullable column; restore a pre-drop backup if those
-- values must be recovered.

SET @people_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'roommate_listings'
      AND column_name = 'people_in_property'
);

SET @restore_people_column_sql = IF(
    @people_column_exists = 0,
    'ALTER TABLE roommate_listings ADD COLUMN people_in_property INT NULL',
    'SELECT ''people_in_property already exists'''
);

PREPARE restore_people_column_statement FROM @restore_people_column_sql;
EXECUTE restore_people_column_statement;
DEALLOCATE PREPARE restore_people_column_statement;

START TRANSACTION;

-- A total occupant count of 0 is impossible. Under the legacy Bulgarian form it meant that only
-- the listing owner lived in the property, so the canonical total is 1. Nullable has_property rows
-- predate the mode field and are treated as "has property" by the application.
UPDATE roommate_listings
SET people_in_property = 1
WHERE COALESCE(has_property, TRUE) = TRUE
  AND people_in_property = 0;

COMMIT;
