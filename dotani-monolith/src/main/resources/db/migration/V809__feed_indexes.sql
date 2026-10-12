-- Feeds filter by organization and status and sort by time.
CREATE INDEX idx_posts_org_status_created ON posts (organization_id, status, created_at);
CREATE INDEX idx_events_org_status_created ON events (organization_id, status, created_at);
-- event lists are sorted by start time by default
CREATE INDEX idx_events_org_status_start ON events (organization_id, status, start_time);
