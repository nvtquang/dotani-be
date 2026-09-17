package com.hcmcyu.event.dto;

public record EventParticipationSummaryResponse(
        long going,
        long notGoing,
        long undecided
) {
}
