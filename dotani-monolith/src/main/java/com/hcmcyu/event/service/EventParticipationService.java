package com.hcmcyu.event.service;

import com.hcmcyu.event.dto.EventParticipationRequest;
import com.hcmcyu.event.dto.EventParticipationResponse;
import com.hcmcyu.event.dto.EventParticipationSummaryResponse;
import com.hcmcyu.event.entity.Event;
import com.hcmcyu.event.entity.EventParticipation;
import com.hcmcyu.event.entity.ParticipationStatus;
import com.hcmcyu.event.exception.EventServiceException;
import com.hcmcyu.event.mapper.EventParticipationMapper;
import com.hcmcyu.event.repository.EventParticipationRepository;
import com.hcmcyu.event.repository.EventRepository;
import com.hcmcyu.event.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventParticipationService {

    private final EventRepository eventRepository;
    private final EventParticipationRepository participationRepository;
    private final EventScopeService eventScopeService;
    private final EventParticipationMapper participationMapper;

    public EventParticipationService(
            EventRepository eventRepository,
            EventParticipationRepository participationRepository,
            EventScopeService eventScopeService,
            EventParticipationMapper participationMapper
    ) {
        this.eventRepository = eventRepository;
        this.participationRepository = participationRepository;
        this.eventScopeService = eventScopeService;
        this.participationMapper = participationMapper;
    }

    @Transactional
    public EventParticipationResponse updateParticipation(
            String eventId,
            EventParticipationRequest request,
            CurrentUser currentUser
    ) {
        String memberId = requireMemberContext(currentUser);
        Event event = getEventForUpdate(eventId);
        eventScopeService.requireRead(currentUser, event);
        validateRegistrationDeadline(event);

        EventParticipation participation = participationRepository
                .findByEvent_IdAndMemberId(eventId, memberId)
                .orElseGet(() -> createParticipation(event, memberId));

        validateCapacity(event, participation.getStatus(), request.status());
        participation.setStatus(request.status());

        try {
            return participationMapper.toResponse(participationRepository.save(participation));
        } catch (DataIntegrityViolationException exception) {
            throw new EventServiceException(
                    HttpStatus.CONFLICT,
                    "DUPLICATE_PARTICIPATION",
                    "Member already has a participation record for this event"
            );
        }
    }

    @Transactional(readOnly = true)
    public List<EventParticipationResponse> findParticipants(String eventId, CurrentUser currentUser) {
        Event event = getEvent(eventId);
        eventScopeService.requireWrite(currentUser, event);
        return participationRepository.findByEvent_IdOrderByUpdatedAtDesc(eventId)
                .stream()
                .map(participationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventParticipationSummaryResponse summarize(String eventId, CurrentUser currentUser) {
        Event event = getEvent(eventId);
        eventScopeService.requireRead(currentUser, event);
        return new EventParticipationSummaryResponse(
                participationRepository.countByEvent_IdAndStatus(eventId, ParticipationStatus.GOING),
                participationRepository.countByEvent_IdAndStatus(eventId, ParticipationStatus.NOT_GOING),
                participationRepository.countByEvent_IdAndStatus(eventId, ParticipationStatus.UNDECIDED)
        );
    }

    @Transactional(readOnly = true)
    public List<EventParticipationResponse> findCurrentMemberEvents(CurrentUser currentUser) {
        String memberId = requireMemberContext(currentUser);
        return participationRepository.findByMemberIdOrderByUpdatedAtDesc(memberId)
                .stream()
                .filter(participation -> canReadParticipation(currentUser, participation))
                .map(participationMapper::toResponse)
                .toList();
    }

    private EventParticipation createParticipation(Event event, String memberId) {
        EventParticipation participation = new EventParticipation();
        participation.setEvent(event);
        participation.setMemberId(memberId);
        return participation;
    }

    private void validateRegistrationDeadline(Event event) {
        LocalDateTime deadline = event.getRegistrationDeadline();
        if (deadline != null && LocalDateTime.now().isAfter(deadline)) {
            throw new EventServiceException(
                    HttpStatus.CONFLICT,
                    "REGISTRATION_DEADLINE_PASSED",
                    "Registration deadline has passed"
            );
        }
    }

    private void validateCapacity(
            Event event,
            ParticipationStatus currentStatus,
            ParticipationStatus newStatus
    ) {
        if (newStatus != ParticipationStatus.GOING || currentStatus == ParticipationStatus.GOING) {
            return;
        }
        Integer maxParticipants = event.getMaxParticipants();
        if (maxParticipants == null) {
            return;
        }
        long currentGoing = participationRepository.countByEvent_IdAndStatus(event.getId(), ParticipationStatus.GOING);
        if (currentGoing >= maxParticipants) {
            throw new EventServiceException(
                    HttpStatus.CONFLICT,
                    "EVENT_FULL",
                    "Event has reached max participants"
            );
        }
    }

    private boolean canReadParticipation(CurrentUser currentUser, EventParticipation participation) {
        try {
            eventScopeService.requireRead(currentUser, participation.getEvent());
            return true;
        } catch (EventServiceException exception) {
            return false;
        }
    }

    private String requireMemberContext(CurrentUser currentUser) {
        if (currentUser.memberId() == null || currentUser.memberId().isBlank()) {
            throw new EventServiceException(
                    HttpStatus.FORBIDDEN,
                    "MEMBER_CONTEXT_REQUIRED",
                    "Current user is not linked to a member profile"
            );
        }
        return currentUser.memberId();
    }

    private Event getEvent(String eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventServiceException(
                        HttpStatus.NOT_FOUND,
                        "EVENT_NOT_FOUND",
                        "Event not found"
                ));
    }

    private Event getEventForUpdate(String eventId) {
        return eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EventServiceException(
                        HttpStatus.NOT_FOUND,
                        "EVENT_NOT_FOUND",
                        "Event not found"
                ));
    }
}
