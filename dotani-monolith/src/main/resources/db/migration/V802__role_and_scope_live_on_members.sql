-- Role and organization scope were stored twice: on user_accounts (for the token) and on members.
-- members is now the only source; tokens derive role, ward and TDP from the linked member.
--
-- No data is copied: members.member_role and members.organization_id already hold the
-- authoritative values (the member module was the one that changed roles and moved members).
ALTER TABLE user_accounts
    DROP COLUMN role,
    DROP COLUMN organization_id,
    DROP COLUMN tdp_id;
