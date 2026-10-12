-- Deleting a member, post or event now sets deleted_at instead of removing the row, so history
-- (messages, participations, attachments, audit) stays consistent. The entities filter on deleted_at IS NULL.
--
-- Members previously "deleted" were only switched to INACTIVE; that status is also set by hand, so
-- existing INACTIVE members are not marked deleted here.
ALTER TABLE members ADD COLUMN deleted_at DATETIME(6) NULL;
ALTER TABLE posts ADD COLUMN deleted_at DATETIME(6) NULL;
ALTER TABLE events ADD COLUMN deleted_at DATETIME(6) NULL;
