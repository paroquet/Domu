-- V2: Add deleted_at column to families table (soft delete support)
-- NOTE: This migration is NOT idempotent in SQLite.
-- For existing databases that already have this column from Hibernate ddl-auto=update,
-- you must manually add the column before running this migration, OR use Flyway's
-- baseline-on-migrate feature to skip this migration.
--
-- Manual fix (run before starting the app with Flyway enabled):
--   sqlite3 your_database.db "ALTER TABLE families ADD COLUMN deleted_at TEXT;"
--
-- If the column already exists and this migration fails, run:
--   sqlite3 your_database.db "INSERT INTO flyway_schema_history (version, description, type, installed_on)
--    VALUES ('2', 'add deleted_at to families', 'SQL', datetime('now'));"
--   -- Then restart the app.

ALTER TABLE families ADD COLUMN deleted_at TEXT;
