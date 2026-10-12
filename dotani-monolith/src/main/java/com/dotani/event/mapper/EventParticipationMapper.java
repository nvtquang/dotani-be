package com.dotani.event.mapper;

import com.dotani.member.service.MemberDirectoryService;
import java.util.List;
import java.util.Map;
import com.dotani.event.dto.EventParticipationResponse;
import com.dotani.event.entity.Event;
import com.dotani.event.entity.EventParticipation;
import org.springframework.stereotype.Component;

@Component
public class EventParticipationMapper {

    private final MemberDirectoryService memberDirectoryService;

    public EventParticipationMapper(MemberDirectoryService memberDirectoryService) {
        this.memberDirectoryService = memberDirectoryService;
    }

    public EventParticipationResponse toResponse(EventParticipation participation) {
        return toResponses(List.of(participation)).getFirst();
    }

    /** Maps a list with one name lookup for all participants. */
    public List<EventParticipationResponse> toResponses(List<EventParticipation> participations) {
        Map<String, String> names = memberDirectoryService.findDisplayNames(
                participations.stream().map(EventParticipation::getMemberId).toList());
        return participations.stream().map(participation -> toResponse(participation, names)).toList();
    }

    private EventParticipationResponse toResponse(EventParticipation participation, Map<String, String> names) {
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
                names.get(participation.getMemberId()),
                participation.getStatus(),
                participation.getCreatedAt(),
                participation.getUpdatedAt()
        );
    }
}
