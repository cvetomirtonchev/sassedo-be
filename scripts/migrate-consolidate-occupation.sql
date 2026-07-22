-- One-time MySQL migration for the canonical Occupation model.
--
-- Deployment order:
--   1. Stop all application instances.
--   2. Back up the database.
--   3. Run this script once.
--   4. Deploy the backend and frontend that no longer use the dropped columns.
--
-- The script intentionally is not run by the application. Hibernate's ddl-auto=update does not
-- remove obsolete columns, and the enum conversion must finish before the new backend reads rows.

-- Hibernate created this as a native MySQL ENUM for the old JobStatus values. Widen it first so
-- both legacy rows and new Occupation values are valid while the data is being converted.
ALTER TABLE roommate_listings
    MODIFY COLUMN employment_status ENUM(
        'EMPLOYED',
        'WORKING',
        'UNEMPLOYED',
        'STUDENT',
        'SELF_EMPLOYED',
        'REMOTE_WORKER',
        'RETIRED',
        'OTHER',
        'PREFER_NOT_TO_SAY'
    ) NULL;

START TRANSACTION;

-- The retained profile occupation wins when both legacy fields contain data.
UPDATE users
SET occupation = CASE job_status
    WHEN 'EMPLOYED' THEN 'WORKING'
    WHEN 'SELF_EMPLOYED' THEN 'SELF_EMPLOYED'
    WHEN 'STUDENT' THEN 'STUDENT'
    WHEN 'UNEMPLOYED' THEN 'UNEMPLOYED'
    WHEN 'OTHER' THEN 'OTHER'
    ELSE NULL
END
WHERE occupation IS NULL
  AND job_status IS NOT NULL;

-- The retained listing employment_status wins over the coarser working/student preference.
UPDATE roommate_listings
SET employment_status = CASE employment_status
    WHEN 'EMPLOYED' THEN 'WORKING'
    ELSE employment_status
END
WHERE employment_status IS NOT NULL;

UPDATE roommate_listings
SET employment_status = CASE occupation_preference
    WHEN 'WORKING' THEN 'WORKING'
    WHEN 'STUDENT' THEN 'STUDENT'
    ELSE NULL
END
WHERE employment_status IS NULL
  AND occupation_preference IN ('WORKING', 'STUDENT');

COMMIT;

-- MySQL implicitly commits DDL. Run these only after the data updates complete successfully.
ALTER TABLE users
    DROP COLUMN job_status;

ALTER TABLE roommate_listings
    MODIFY COLUMN employment_status ENUM(
        'WORKING',
        'UNEMPLOYED',
        'STUDENT',
        'SELF_EMPLOYED',
        'REMOTE_WORKER',
        'RETIRED',
        'OTHER',
        'PREFER_NOT_TO_SAY'
    ) NULL,
    DROP COLUMN occupation_preference,
    DROP COLUMN people_in_property;
