package com.hcmcyu.event.service;

import com.hcmcyu.event.entity.Event;
import com.hcmcyu.event.security.CurrentUser;

public interface AuditClient {

    void record(AuditAction action, Event event, CurrentUser currentUser);
}
