package com.dotani.emulation.entity;

/** Forms of commendation (khen thưởng) and discipline (kỷ luật) a member can receive. */
public enum AwardForm {
    PRAISE(AwardKind.COMMENDATION),
    CERTIFICATE_OF_MERIT(AwardKind.COMMENDATION),
    HONOR_CERTIFICATE(AwardKind.COMMENDATION),
    OTHER_COMMENDATION(AwardKind.COMMENDATION),
    REPRIMAND(AwardKind.DISCIPLINE),
    WARNING(AwardKind.DISCIPLINE),
    EXPULSION(AwardKind.DISCIPLINE);

    private final AwardKind kind;

    AwardForm(AwardKind kind) {
        this.kind = kind;
    }

    public AwardKind kind() {
        return kind;
    }
}
