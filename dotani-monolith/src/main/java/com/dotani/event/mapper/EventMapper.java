package com.dotani.event.mapper;

import com.dotani.member.service.NameLookup;
import java.util.List;
import org.springframework.data.domain.Page;
import com.dotani.event.dto.EventRequest;
import com.dotani.event.dto.EventResponse;
import com.dotani.event.entity.Event;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    private final NameLookup nameLookup;

    public EventMapper(NameLookup nameLookup) {
        this.nameLookup = nameLookup;
    }


    public EventResponse toResponse(Event event) {
        return toResponse(event, names(List.of(event)));
    }

    /** Maps a page with one name lookup for all of its items. */
    public Page<EventResponse> toResponsePage(Page<Event> page) {
        NameLookup.Names names = names(page.getContent());
        return page.map(event -> toResponse(event, names));
    }

    private NameLookup.Names names(List<Event> items) {
        return nameLookup.of(
                items.stream().map(event -> (String) null).toList(),
                items.stream().map(event -> event.getOrganizationId()).toList()
        );
    }

    private EventResponse toResponse(Event event, NameLookup.Names names) {
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
                names.organization(event.getOrganizationId()),
                event.getMaxParticipants(),
                event.getStatus(),
                event.getCreatedBy(),
                event.getAttachments().stream()
                        .map(attachment -> new com.dotani.event.dto.EventAttachmentResponse(
                                attachment.getId(),
                                attachment.getUrl(),
                                attachment.getKind().name(),
                                attachment.getName(),
                                attachment.getCreatedAt()
                        ))
                        .toList(),
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
