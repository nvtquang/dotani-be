package com.hcmcyu.event.mapper;

import com.hcmcyu.event.dto.EventRequest;
import com.hcmcyu.event.dto.EventResponse;
import com.hcmcyu.event.entity.Event;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getType(),
                event.getLocation(),
                event.getStartTime(),
                event.getEndTime(),
                event.getRegistrationDeadline(),
                event.getOrganizationId(),
                event.getMaxParticipants(),
                event.getStatus(),
                event.getCreatedBy(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    public void apply(Event event, EventRequest request) {
        event.setTitle(request.title().trim());
        event.setDescription(blankToNull(request.description()));
        event.setType(request.type());
        event.setLocation(blankToNull(request.location()));
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setRegistrationDeadline(request.registrationDeadline());
        event.setOrganizationId(request.organizationId().trim());
        event.setMaxParticipants(request.maxParticipants());
        event.setStatus(request.status());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
