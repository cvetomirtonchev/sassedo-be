-- One-time MySQL migration: owner edit counter and admin rejection reason on both listing types.
--
-- Safe to run more than once:
--   * each column is created only when it is missing;
--   * existing rows keep their current values; new columns default to 0 / NULL.
--
-- Deployment order:
--   1. Stop all application instances.
--   2. Back up the database.
--   3. Run this script.
--   4. Deploy the backend that enforces the owner edit limit and rejection reason.

-- roommate_listings.owner_edit_count
SET @roommate_edit_count_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'roommate_listings'
      AND column_name = 'owner_edit_count'
);

SET @add_roommate_edit_count_sql = IF(
    @roommate_edit_count_exists = 0,
    'ALTER TABLE roommate_listings ADD COLUMN owner_edit_count INT NOT NULL DEFAULT 0',
    'SELECT ''roommate_listings.owner_edit_count already exists'''
);

PREPARE add_roommate_edit_count FROM @add_roommate_edit_count_sql;
EXECUTE add_roommate_edit_count;
DEALLOCATE PREPARE add_roommate_edit_count;

-- roommate_listings.rejection_reason
SET @roommate_rejection_reason_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'roommate_listings'
      AND column_name = 'rejection_reason'
);

SET @add_roommate_rejection_reason_sql = IF(
    @roommate_rejection_reason_exists = 0,
    'ALTER TABLE roommate_listings ADD COLUMN rejection_reason TEXT NULL',
    'SELECT ''roommate_listings.rejection_reason already exists'''
);

PREPARE add_roommate_rejection_reason FROM @add_roommate_rejection_reason_sql;
EXECUTE add_roommate_rejection_reason;
DEALLOCATE PREPARE add_roommate_rejection_reason;

-- rental_listings.owner_edit_count
SET @rental_edit_count_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'rental_listings'
      AND column_name = 'owner_edit_count'
);

SET @add_rental_edit_count_sql = IF(
    @rental_edit_count_exists = 0,
    'ALTER TABLE rental_listings ADD COLUMN owner_edit_count INT NOT NULL DEFAULT 0',
    'SELECT ''rental_listings.owner_edit_count already exists'''
);

PREPARE add_rental_edit_count FROM @add_rental_edit_count_sql;
EXECUTE add_rental_edit_count;
DEALLOCATE PREPARE add_rental_edit_count;

-- rental_listings.rejection_reason
SET @rental_rejection_reason_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'rental_listings'
      AND column_name = 'rejection_reason'
);

SET @add_rental_rejection_reason_sql = IF(
    @rental_rejection_reason_exists = 0,
    'ALTER TABLE rental_listings ADD COLUMN rejection_reason TEXT NULL',
    'SELECT ''rental_listings.rejection_reason already exists'''
);

PREPARE add_rental_rejection_reason FROM @add_rental_rejection_reason_sql;
EXECUTE add_rental_rejection_reason;
DEALLOCATE PREPARE add_rental_rejection_reason;
