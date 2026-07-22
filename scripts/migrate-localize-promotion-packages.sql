-- One-time MySQL migration for localized promotion package names.
--
-- Deployment order:
--   1. Stop all application instances.
--   2. Back up the database.
--   3. Run this script once.
--   4. Deploy the backend and frontend that use name_en/name_bg and no pin fields.
--
-- The script intentionally is not run by the application. Hibernate's ddl-auto=update does not
-- rename or remove columns, and both localized names must be populated before they become required.

ALTER TABLE promotion_packages
    ADD COLUMN name_en VARCHAR(120) NULL AFTER id,
    ADD COLUMN name_bg VARCHAR(120) NULL AFTER name_en;

START TRANSACTION;

-- Preserve every existing package. The known defaults receive real Bulgarian labels; custom
-- package names are copied to Bulgarian so an administrator can translate them after deployment.
UPDATE promotion_packages
SET name_en = name,
    name_bg = CASE name
        WHEN 'Promoted 7 days' THEN 'Промотирана за 7 дни'
        WHEN 'Promoted 14 days' THEN 'Промотирана за 14 дни'
        WHEN 'Promoted 30 days' THEN 'Промотирана за 30 дни'
        WHEN 'Featured 7 days' THEN 'Акцентирана за 7 дни'
        WHEN 'Featured 14 days' THEN 'Акцентирана за 14 дни'
        WHEN 'Featured 30 days' THEN 'Акцентирана за 30 дни'
        ELSE name
    END;

COMMIT;

-- MySQL implicitly commits DDL. Run these only after the backfill completes successfully.
ALTER TABLE promotion_packages
    MODIFY COLUMN name_en VARCHAR(120) NOT NULL,
    MODIFY COLUMN name_bg VARCHAR(120) NOT NULL,
    DROP COLUMN name,
    DROP COLUMN pinnable;

ALTER TABLE promotions
    DROP COLUMN pinned;

ALTER TABLE rental_listings
    DROP COLUMN pinned;

ALTER TABLE roommate_listings
    DROP COLUMN pinned;
