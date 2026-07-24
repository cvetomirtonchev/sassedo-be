-- One-time MySQL migration: queued promotion renewals (predecessor link on promotions).
--
-- Safe to run more than once:
--   * column and index are created only when missing;
--   * existing rows keep NULL predecessor_promotion_id.
--
-- Deployment order:
--   1. Stop all application instances.
--   2. Back up the database.
--   3. Run this script.
--   4. Deploy the backend that supports renewFromPromotionId purchases.

-- promotions.predecessor_promotion_id
SET @predecessor_col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'promotions'
      AND column_name = 'predecessor_promotion_id'
);

SET @add_predecessor_col_sql = IF(
    @predecessor_col_exists = 0,
    'ALTER TABLE promotions ADD COLUMN predecessor_promotion_id BIGINT NULL',
    'SELECT ''promotions.predecessor_promotion_id already exists'''
);

PREPARE add_predecessor_col FROM @add_predecessor_col_sql;
EXECUTE add_predecessor_col;
DEALLOCATE PREPARE add_predecessor_col;

-- idx_promo_predecessor
SET @predecessor_idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'promotions'
      AND index_name = 'idx_promo_predecessor'
);

SET @add_predecessor_idx_sql = IF(
    @predecessor_idx_exists = 0,
    'CREATE INDEX idx_promo_predecessor ON promotions (predecessor_promotion_id)',
    'SELECT ''idx_promo_predecessor already exists'''
);

PREPARE add_predecessor_idx FROM @add_predecessor_idx_sql;
EXECUTE add_predecessor_idx;
DEALLOCATE PREPARE add_predecessor_idx;
