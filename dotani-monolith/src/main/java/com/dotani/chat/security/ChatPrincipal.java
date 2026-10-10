package com.dotani.chat.security;

import com.dotani.shared.security.CurrentUser;

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
