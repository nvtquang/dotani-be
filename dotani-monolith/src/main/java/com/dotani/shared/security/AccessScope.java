package com.dotani.shared.security;

/**
 * Role and organization scope of an account, derived from its member profile (the single source of truth).
 *
 * @param organizationId the ward the member belongs to
 * @param tdpId          the member's TDP, or null for ward-level members
 */
public record AccessScope(Role role, String organizationId, String tdpId) {
}
