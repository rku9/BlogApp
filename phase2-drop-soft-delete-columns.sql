-- Phase 2: Drop soft delete columns from all tables
-- WARNING: This will permanently remove the is_deleted columns and cannot be undone
-- Run this ONLY after Phase 1 code has been deployed and verified

-- Step 1: (Optional) Permanently delete soft-deleted records before dropping column
-- Uncomment these if you want to clean up soft-deleted records first

-- DELETE FROM comment WHERE is_deleted = true;
-- DELETE FROM post WHERE is_deleted = true;
-- DELETE FROM tag WHERE is_deleted = true;
-- DELETE FROM "user" WHERE is_deleted = true;


-- Step 2: Drop the is_deleted columns from all tables

ALTER TABLE post DROP COLUMN is_deleted;

ALTER TABLE comment DROP COLUMN is_deleted;

ALTER TABLE tag DROP COLUMN is_deleted;

ALTER TABLE "user" DROP COLUMN is_deleted;


-- Verification queries (run these to confirm columns are dropped)
-- SELECT column_name FROM information_schema.columns WHERE table_name = 'post';
-- SELECT column_name FROM information_schema.columns WHERE table_name = 'comment';
-- SELECT column_name FROM information_schema.columns WHERE table_name = 'tag';
-- SELECT column_name FROM information_schema.columns WHERE table_name = 'user';
