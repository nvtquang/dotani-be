-- Cross-module references were plain strings while the system ran as microservices.
-- Now that everything shares one schema, enforce them with foreign keys.

-- Link rows pointing at a member that no longer exists carry no information: remove them
-- so the constraints below can be created. Content rows (events, posts, messages) are NOT
-- deleted: if one of them is orphaned the ALTER fails and must be fixed by hand.
DELETE x FROM conversation_members x LEFT JOIN members m ON m.id = x.member_id WHERE m.id IS NULL;
DELETE x FROM event_participations x LEFT JOIN members m ON m.id = x.member_id WHERE m.id IS NULL;
DELETE x FROM user_notifications x LEFT JOIN members m ON m.id = x.member_id WHERE m.id IS NULL;
UPDATE user_accounts u LEFT JOIN members m ON m.id = u.member_id
SET u.member_id = NULL
WHERE u.member_id IS NOT NULL AND m.id IS NULL;
UPDATE members m LEFT JOIN user_accounts u ON u.id = m.user_id
SET m.user_id = NULL
WHERE m.user_id IS NOT NULL AND u.id IS NULL;

ALTER TABLE events
    ADD CONSTRAINT fk_events_organization_id FOREIGN KEY (organization_id) REFERENCES organization_units (id);

ALTER TABLE posts
    ADD CONSTRAINT fk_posts_organization_id FOREIGN KEY (organization_id) REFERENCES organization_units (id),
    ADD CONSTRAINT fk_posts_author_id FOREIGN KEY (author_id) REFERENCES members (id);

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_sender_id FOREIGN KEY (sender_id) REFERENCES members (id);

ALTER TABLE conversation_members
    ADD CONSTRAINT fk_conversation_members_member_id FOREIGN KEY (member_id) REFERENCES members (id);

ALTER TABLE event_participations
    ADD CONSTRAINT fk_event_participations_member_id FOREIGN KEY (member_id) REFERENCES members (id);

ALTER TABLE user_notifications
    ADD CONSTRAINT fk_user_notifications_member_id FOREIGN KEY (member_id) REFERENCES members (id);

ALTER TABLE user_accounts
    ADD CONSTRAINT fk_user_accounts_member_id FOREIGN KEY (member_id) REFERENCES members (id);

ALTER TABLE members
    ADD CONSTRAINT fk_members_user_id FOREIGN KEY (user_id) REFERENCES user_accounts (id);
