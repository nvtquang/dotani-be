-- When the "registration closes soon" reminder was sent, so each event is reminded once.
ALTER TABLE events ADD COLUMN deadline_reminded_at DATETIME(6) NULL;
