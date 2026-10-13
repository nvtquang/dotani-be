package com.dotani.event.mapper;

import com.dotani.member.service.NameLookup;
import com.dotani.event.repository.EventParticipationRepository;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import com.dotani.event.dto.EventRequest;
import com.dotani.event.dto.EventResponse;
import com.dotani.event.entity.Event;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    private final NameLookup nameLookup;
    private final EventParticipationRepository participationRepository;

    public EventMapper(NameLookup nameLookup, EventParticipationRepository participationRepository) {
        this.nameLookup = nameLookup;
        this.participationRepository = participationRepository;
    }


    public EventResponse toResponse(Event event) {
        return toResponse(event, names(List.of(event)), participationRepository.goingCounts(List.of(event.getId())));
    }

    /** Maps a page with one name lookup and one GOING count query for all of its items. */
    public Page<EventResponse> toResponsePage(Page<Event> page) {
        List<EventResponse> items = toResponses(page.getContent());
        return new org.springframework.data.domain.PageImpl<>(items, page.getPageable(), page.getTotalElements());
    }

    public List<EventResponse> toResponses(List<Event> events) {
        NameLookup.Names names = names(events);
        Map<String, Long> goingCounts = participationRepository.goingCounts(events.stream().map(Event::getId).toList());
        return events.stream().map(event -> toResponse(event, names, goingCounts)).toList();
    }

    private NameLookup.Names names(List<Event> items) {
        return nameLookup.of(
                items.stream().map(event -> (String) null).toList(),
                items.stream().map(event -> event.getOrganizationId()).toList()
        );
    }

    private EventResponse toResponse(Event event, NameLookup.Names names, Map<String, Long> goingCounts) {
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
                goingCounts.getOrDefault(event.getId(), 0L),
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
