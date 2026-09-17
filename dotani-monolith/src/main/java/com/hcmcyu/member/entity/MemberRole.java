package com.hcmcyu.member.entity;

public enum MemberRole {
    WARD_SECRETARY,
    WARD_DEPUTY_SECRETARY,
    TDP_SECRETARY,
    TDP_DEPUTY_SECRETARY,
    MEMBER;

    public boolean isWardOfficer() {
        return this == WARD_SECRETARY || this == WARD_DEPUTY_SECRETARY;
    }
}

