package com.hcmcyu.content.service;

import com.hcmcyu.content.entity.Post;
import com.hcmcyu.content.entity.PostStatus;
import com.hcmcyu.content.entity.PostType;
import com.hcmcyu.content.exception.ContentServiceException;
import com.hcmcyu.content.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PostScopeService {

    public void requireRead(CurrentUser currentUser, Post post) {
        if (!canRead(currentUser, post)) {
            throw outOfScope();
        }
    }

    public void requireManage(CurrentUser currentUser, Post post) {
        if (!canManage(currentUser, post.getOrganizationId(), post.getType())) {
            throw outOfScope();
        }
    }

    public void requireManageRequest(CurrentUser currentUser, String organizationId, PostType type) {
        if (!canManage(currentUser, organizationId, type)) {
            throw outOfScope();
        }
    }

    private boolean canRead(CurrentUser currentUser, Post post) {
        if (currentUser.hasWardScope()) {
            return true;
        }
        boolean inScope = post.getOrganizationId().equals(currentUser.organizationId())
                || post.getOrganizationId().equals(currentUser.tdpId());
        if (!inScope) {
            return false;
        }
        if (currentUser.isMember()) {
            return post.getStatus() == PostStatus.PUBLISHED;
        }
        return true;
    }

    private boolean canManage(CurrentUser currentUser, String organizationId, PostType type) {
        return currentUser.hasWardScope();
    }

    private ContentServiceException outOfScope() {
        return new ContentServiceException(
                HttpStatus.FORBIDDEN,
                "OUT_OF_SCOPE",
                "Current user cannot access this post"
        );
    }
}
