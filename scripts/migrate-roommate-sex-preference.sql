-- One-time MySQL migration for the dedicated roommate sex preference model.
--
-- Deployment order:
--   1. Stop all application instances.
--   2. Back up the database.
--   3. Run this script once.
--   4. Deploy the backend and frontend that use NO_PREFERENCE.
--
-- The script intentionally is not run by the application. The old backend cannot deserialize
-- NO_PREFERENCE, while the new backend no longer accepts OTHER for roommate listing preferences.

-- Allow both values while existing rows are converted.
ALTER TABLE roommate_listings
    MODIFY COLUMN preferred_sex ENUM(
        'MALE',
        'FEMALE',
        'OTHER',
        'NO_PREFERENCE'
    ) NULL;

START TRANSACTION;

UPDATE roommate_listings
SET preferred_sex = 'NO_PREFERENCE'
WHERE preferred_sex = 'OTHER';

COMMIT;

-- Remove the legacy listing-only meaning of OTHER after every row has been converted.
ALTER TABLE roommate_listings
    MODIFY COLUMN preferred_sex ENUM(
        'MALE',
        'FEMALE',
        'NO_PREFERENCE'
    ) NULL;
