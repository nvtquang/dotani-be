package com.hcmcyu.chat.security;

import java.security.Principal;

public class ChatPrincipal implements Principal {

    private final CurrentUser currentUser;

    public ChatPrincipal(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public String getName() {
        return currentUser.memberId();
    }

    public CurrentUser currentUser() {
        return currentUser;
    }
}
