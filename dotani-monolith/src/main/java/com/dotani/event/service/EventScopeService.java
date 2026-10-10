package com.dotani.event.service;

import com.dotani.event.entity.Event;
import com.dotani.shared.security.CurrentUser;
import com.dotani.shared.security.OrgScopePolicy;
import org.springframework.stereotype.Service;

/** Event rules: anyone in scope can read; only ward officers create and manage events. */
@Service
public class EventScopeService {

    private static final String OUT_OF_SCOPE = "Current user cannot access this event";

    public void requireRead(CurrentUser currentUser, Event event) {
        if (!OrgScopePolicy.canSeeContentOf(currentUser, event.getOrganizationId())) {
            throw OrgScopePolicy.outOfScope(OUT_OF_SCOPE);
        }
    }

    public void requireWrite(CurrentUser currentUser, Event event) {
        requireCreateInOrganization(currentUser, event.getOrganizationId());
    }

    public void requireCreateInOrganization(CurrentUser currentUser, String organizationId) {
        if (!canWrite(currentUser, organizationId)) {
            throw OrgScopePolicy.outOfScope(OUT_OF_SCOPE);
        }
    }

    public boolean canWrite(CurrentUser currentUser, String organizationId) {
        return OrgScopePolicy.coversWholeWard(currentUser);
    }
}
