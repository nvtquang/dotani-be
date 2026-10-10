package com.dotani.event.dto;

public record EventParticipationSummaryResponse(
        long going,
        long notGoing,
        long undecided
) {
}
