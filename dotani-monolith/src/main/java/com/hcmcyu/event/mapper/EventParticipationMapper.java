package com.hcmcyu.event.mapper;

import com.hcmcyu.event.dto.EventParticipationResponse;
import com.hcmcyu.event.entity.Event;
import com.hcmcyu.event.entity.EventParticipation;
import org.springframework.stereotype.Component;

@Component
public class EventParticipationMapper {

    public EventParticipationResponse toResponse(EventParticipation participation) {
        Event event = participation.getEvent();
        return new EventParticipationResponse(
                participation.getId(),
                event.getId(),
                event.getTitle(),
                event.getType(),
                event.getOrganizationId(),
                event.getStartTime(),
                event.getEndTime(),
                participation.getMemberId(),
                participation.getStatus(),
                participation.getCreatedAt(),
                participation.getUpdatedAt()
        );
    }
}
