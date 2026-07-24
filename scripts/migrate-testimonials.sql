-- One-time MySQL migration: testimonials table for homepage user quotes.
--
-- Safe to run more than once: CREATE TABLE IF NOT EXISTS skips when the table already exists.
--
-- Deployment order:
--   1. Stop all application instances.
--   2. Back up the database.
--   3. Run this script.
--   4. Deploy the backend testimonials API.

CREATE TABLE IF NOT EXISTS testimonials (
    id BIGINT NOT NULL AUTO_INCREMENT,
    quote_bg VARCHAR(2000) NOT NULL,
    quote_en VARCHAR(2000) NOT NULL,
    author_bg VARCHAR(200) NOT NULL,
    author_en VARCHAR(200) NOT NULL,
    role_bg VARCHAR(200) NOT NULL,
    role_en VARCHAR(200) NOT NULL,
    enabled BIT(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
