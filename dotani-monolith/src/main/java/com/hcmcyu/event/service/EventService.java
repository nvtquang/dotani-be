package com.hcmcyu.event.service;

import com.hcmcyu.event.dto.EventRequest;
import com.hcmcyu.event.dto.EventResponse;
import com.hcmcyu.event.entity.Event;
import com.hcmcyu.event.entity.EventStatus;
import com.hcmcyu.event.entity.EventType;
import com.hcmcyu.event.exception.EventServiceException;
import com.hcmcyu.event.mapper.EventMapper;
import com.hcmcyu.event.repository.EventRepository;
import com.hcmcyu.event.repository.EventSpecifications;
import com.hcmcyu.event.security.CurrentUser;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventScopeService eventScopeService;
    private final AuditClient auditClient;

    public EventService(
            EventRepository eventRepository,
            EventMapper eventMapper,
            EventScopeService eventScopeService,
            AuditClient auditClient
    ) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.eventScopeService = eventScopeService;
        this.auditClient = auditClient;
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> findAll(
            EventType type,
            String organizationId,
            EventStatus status,
            LocalDate date,
            Boolean upcoming,
            Pageable pageable,
            CurrentUser currentUser
    ) {
        Specification<Event> specification = Specification
                .where(EventSpecifications.withinReadScope(currentUser))
                .and(EventSpecifications.hasType(type))
                .and(EventSpecifications.hasOrganization(organizationId))
                .and(EventSpecifications.hasStatus(status))
                .and(EventSpecifications.onDate(date))
                .and(EventSpecifications.upcoming(upcoming));

        return eventRepository.findAll(specification, pageable).map(eventMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse findById(String id, CurrentUser currentUser) {
        Event event = getEvent(id);
        eventScopeService.requireRead(currentUser, event);
        return eventMapper.toResponse(event);
    }

    @Transactional
    public EventResponse create(EventRequest request, CurrentUser currentUser) {
        validateTimeRange(request);
        eventScopeService.requireCreateInOrganization(currentUser, request.organizationId());

        Event event = new Event();
        eventMapper.apply(event, request);
        event.setCreatedBy(currentUser.userId());
        Event saved = eventRepository.save(event);
        auditClient.record(AuditAction.CREATE_EVENT, saved, currentUser);
        return eventMapper.toResponse(saved);
    }

    @Transactional
    public EventResponse update(String id, EventRequest request, CurrentUser currentUser) {
        validateTimeRange(request);
        Event event = getEvent(id);
        eventScopeService.requireWrite(currentUser, event);
        eventScopeService.requireCreateInOrganization(currentUser, request.organizationId());

        eventMapper.apply(event, request);
        auditClient.record(AuditAction.UPDATE_EVENT, event, currentUser);
        return eventMapper.toResponse(event);
    }

    @Transactional
    public void delete(String id, CurrentUser currentUser) {
        Event event = getEvent(id);
        eventScopeService.requireWrite(currentUser, event);
        auditClient.record(AuditAction.DELETE_EVENT, event, currentUser);
        eventRepository.delete(event);
    }

    private Event getEvent(String id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventServiceException(
                        HttpStatus.NOT_FOUND,
                        "EVENT_NOT_FOUND",
                        "Event not found"
                ));
    }

    private void validateTimeRange(EventRequest request) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw new EventServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_EVENT_TIME_RANGE",
                    "startTime must be before endTime"
            );
        }
        if (request.registrationDeadline() != null
                && request.registrationDeadline().isAfter(request.startTime())) {
            throw new EventServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_REGISTRATION_DEADLINE",
                    "registrationDeadline must be before or equal to startTime"
            );
        }
    }
}
