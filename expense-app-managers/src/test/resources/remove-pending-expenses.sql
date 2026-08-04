BEGIN TRANSACTION;
UPDATE approvals
SET status = 'approved'
WHERE status = 'pending';
COMMIT;