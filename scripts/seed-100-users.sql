-- Seeds 100 fully-populated, enabled users into the sassedodb database.
--
-- Each user gets:
--   * a unique email: seeduser1@sassedo.test ... seeduser100@sassedo.test
--   * a shared BCrypt password hash for the plaintext password "Password123!"
--     (Spring Security's BCryptPasswordEncoder accepts the $2y$ variant)
--   * a full profile (name, phone, city, age, sex, job status, smoker, pets, description)
--   * enabled = 1, blocked = 0, all consent flags accepted
--   * the ROLE_USER role (looked up by name, not a hard-coded id)
--   * two languages (ENGLISH + a rotated second language)
--
-- Re-running is safe: the NOT EXISTS email guard skips already-seeded users.
--
-- Prerequisite: the Spring app must have started at least once so the
-- users / user_roles / user_languages / roles tables exist and ROLE_USER is seeded.
--
-- Run with:
--   mysql -uroot -p'Feri@2000' -h127.0.0.1 sassedodb < sassedo-be/scripts/seed-100-users.sql

DELIMITER $$

DROP PROCEDURE IF EXISTS seed_users$$

CREATE PROCEDURE seed_users()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE roleId BIGINT;
    DECLARE newUserId BIGINT;
    DECLARE vEmail VARCHAR(50);
    DECLARE vFirstName VARCHAR(60);
    DECLARE vLastName VARCHAR(60);
    DECLARE vName VARCHAR(60);
    DECLARE vPhone VARCHAR(30);
    DECLARE vCity VARCHAR(100);
    DECLARE vAge INT;
    DECLARE vSex VARCHAR(10);
    DECLARE vJobStatus VARCHAR(20);
    DECLARE vSmoker BOOLEAN;
    DECLARE vHasPets BOOLEAN;
    DECLARE vShortDescription VARCHAR(1000);
    DECLARE vSecondLanguage VARCHAR(20);
    DECLARE vPassword VARCHAR(120) DEFAULT '$2y$10$LkXpDdeRlZHyNYfc3PeXh.FE4pfsbrAbDJRTZfeAjC5wbjXEivbxK';

    SELECT id INTO roleId FROM roles WHERE name = 'ROLE_USER' LIMIT 1;

    IF roleId IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'ROLE_USER not found. Start the Spring app once so RoleService seeds the roles.';
    END IF;

    WHILE i <= 100 DO
        SET vEmail = CONCAT('seeduser', i, '@sassedo.test');

        IF NOT EXISTS (SELECT 1 FROM users WHERE email = vEmail) THEN
            SET vFirstName = ELT(1 + MOD(i, 20),
                'Alexander', 'Maria', 'Nikolay', 'Elena', 'Georgi',
                'Ivana', 'Petar', 'Sofia', 'Dimitar', 'Viktoria',
                'Stefan', 'Gabriela', 'Martin', 'Teodora', 'Kristian',
                'Yana', 'Boris', 'Desislava', 'Todor', 'Ralitsa');

            SET vLastName = ELT(1 + MOD(i, 15),
                'Ivanov', 'Petrov', 'Georgiev', 'Dimitrov', 'Kolev',
                'Stoyanov', 'Todorov', 'Nikolov', 'Angelov', 'Marinov',
                'Vasilev', 'Iliev', 'Popov', 'Hristov', 'Atanasov');

            SET vName = CONCAT(vFirstName, ' ', vLastName);
            SET vPhone = CONCAT('+3598', LPAD(i, 8, '0'));

            SET vCity = ELT(1 + MOD(i, 10),
                'Sofia', 'Plovdiv', 'Varna', 'Burgas', 'Ruse',
                'Stara Zagora', 'Pleven', 'Sliven', 'Dobrich', 'Shumen');

            SET vAge = 18 + MOD(i, 50);
            SET vSex = ELT(1 + MOD(i, 3), 'MALE', 'FEMALE', 'OTHER');
            SET vJobStatus = ELT(1 + MOD(i, 5),
                'EMPLOYED', 'SELF_EMPLOYED', 'STUDENT', 'UNEMPLOYED', 'OTHER');
            SET vSmoker = (MOD(i, 2) = 0);
            SET vHasPets = (MOD(i, 3) = 0);
            SET vShortDescription = CONCAT(
                'Hi, I am ', vFirstName, ' from ', vCity,
                '. This is a seeded test profile number ', i, '.');
            SET vSecondLanguage = ELT(1 + MOD(i, 5),
                'BULGARIAN', 'GERMAN', 'SPANISH', 'FRENCH', 'ITALIAN');

            INSERT INTO users (
                email, password, name, first_name, last_name, phone, city, age,
                sex, job_status, smoker, has_pets, short_description, enabled, blocked,
                is_terms_and_conditions_accepted, is_gdpr_accepted,
                terms_and_conditions_accepted_at, gdpr_accepted_at,
                is_marketing_consent_accepted, marketing_consent_accepted_at
            ) VALUES (
                vEmail, vPassword, vName, vFirstName, vLastName, vPhone, vCity, vAge,
                vSex, vJobStatus, vSmoker, vHasPets, vShortDescription, 1, 0,
                1, 1,
                NOW(), NOW(),
                1, NOW()
            );

            SET newUserId = LAST_INSERT_ID();

            INSERT INTO user_roles (user_id, role_id) VALUES (newUserId, roleId);

            INSERT INTO user_languages (user_id, language) VALUES (newUserId, 'ENGLISH');
            INSERT INTO user_languages (user_id, language) VALUES (newUserId, vSecondLanguage);
        END IF;

        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;

CALL seed_users();

DROP PROCEDURE seed_users;
