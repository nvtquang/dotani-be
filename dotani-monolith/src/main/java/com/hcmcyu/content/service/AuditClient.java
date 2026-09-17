package com.hcmcyu.content.service;

import com.hcmcyu.content.entity.Post;
import com.hcmcyu.content.security.CurrentUser;

public interface AuditClient {

    void record(AuditAction action, Post post, CurrentUser currentUser);
}
