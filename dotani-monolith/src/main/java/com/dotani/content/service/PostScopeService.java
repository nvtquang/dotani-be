package com.dotani.content.service;

import com.dotani.content.entity.Post;
import com.dotani.content.entity.PostStatus;
import com.dotani.content.entity.PostType;
import com.dotani.member.service.OrganizationDirectory;
import com.dotani.shared.security.CurrentUser;
import com.dotani.shared.security.OrgScopePolicy;
import org.springframework.stereotype.Service;

/** Post rules: members see only published posts in scope; only ward officers manage posts. */
@Service
public class PostScopeService {

    private final OrganizationDirectory organizationDirectory;

    public PostScopeService(OrganizationDirectory organizationDirectory) {
        this.organizationDirectory = organizationDirectory;
    }

    private static final String OUT_OF_SCOPE = "Current user cannot access this post";

    public void requireRead(CurrentUser currentUser, Post post) {
        if (!canRead(currentUser, post)) {
            throw OrgScopePolicy.outOfScope(OUT_OF_SCOPE);
        }
    }

    public void requireManage(CurrentUser currentUser, Post post) {
        requireManageRequest(currentUser, post.getOrganizationId(), post.getType());
    }

    public void requireManageRequest(CurrentUser currentUser, String organizationId, PostType type) {
        if (!OrgScopePolicy.coversWholeWard(currentUser)) {
            throw OrgScopePolicy.outOfScope(OUT_OF_SCOPE);
        }
        organizationDirectory.requireExists(organizationId);
    }

    private boolean canRead(CurrentUser currentUser, Post post) {
        if (!OrgScopePolicy.canSeeContentOf(currentUser, post.getOrganizationId())) {
            return false;
        }
        return !currentUser.isMember() || post.getStatus() == PostStatus.PUBLISHED;
    }
}
