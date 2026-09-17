package com.hcmcyu.event.service;

import com.hcmcyu.event.entity.Event;
import com.hcmcyu.event.exception.EventServiceException;
import com.hcmcyu.event.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EventScopeService {

    public void requireRead(CurrentUser currentUser, Event event) {
        if (!canRead(currentUser, event)) {
            throw outOfScope();
        }
    }

    public void requireWrite(CurrentUser currentUser, Event event) {
        if (!canWrite(currentUser, event.getOrganizationId())) {
            throw outOfScope();
        }
    }

    public void requireCreateInOrganization(CurrentUser currentUser, String organizationId) {
        if (!canWrite(currentUser, organizationId)) {
            throw outOfScope();
        }
    }

    public boolean canWrite(CurrentUser currentUser, String organizationId) {
        return currentUser.hasWardScope();
    }

    private boolean canRead(CurrentUser currentUser, Event event) {
        if (currentUser.hasWardScope()) {
            return true;
        }
        String organizationId = event.getOrganizationId();
        return organizationId.equals(currentUser.organizationId()) || organizationId.equals(currentUser.tdpId());
    }

    private EventServiceException outOfScope() {
        return new EventServiceException(
                HttpStatus.FORBIDDEN,
                "OUT_OF_SCOPE",
                "Current user cannot access this event"
        );
    }
}
