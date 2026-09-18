package com.hcmcyu.event.service;

import com.hcmcyu.event.dto.EventRequest;
import com.hcmcyu.event.dto.EventResponse;
import com.hcmcyu.event.entity.Event;
import com.hcmcyu.event.entity.EventAttachment;
import com.hcmcyu.event.entity.EventStatus;
import com.hcmcyu.event.entity.EventType;
import com.hcmcyu.event.exception.EventServiceException;
import com.hcmcyu.event.mapper.EventMapper;
import com.hcmcyu.event.repository.EventAttachmentRepository;
import com.hcmcyu.event.repository.EventRepository;
import com.hcmcyu.event.repository.EventSpecifications;
import com.hcmcyu.event.security.CurrentUser;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventScopeService eventScopeService;
    private final AuditClient auditClient;
    private final EventNotificationPublisher eventNotificationPublisher;
    private final EventAttachmentRepository eventAttachmentRepository;
    private final EventStorageService eventStorageService;

    public EventService(
            EventRepository eventRepository,
            EventAttachmentRepository eventAttachmentRepository,
            EventMapper eventMapper,
            EventScopeService eventScopeService,
            AuditClient auditClient,
            EventNotificationPublisher eventNotificationPublisher,
            EventStorageService eventStorageService
    ) {
        this.eventRepository = eventRepository;
        this.eventAttachmentRepository = eventAttachmentRepository;
        this.eventMapper = eventMapper;
        this.eventScopeService = eventScopeService;
        this.auditClient = auditClient;
        this.eventNotificationPublisher = eventNotificationPublisher;
        this.eventStorageService = eventStorageService;
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
        if (saved.getStatus() == EventStatus.PUBLISHED) {
            eventNotificationPublisher.publishEventCreated(saved);
        }
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
        List<String> attachmentUrls = event.getAttachments().stream().map(EventAttachment::getFileUrl).toList();
        auditClient.record(AuditAction.DELETE_EVENT, event, currentUser);
        eventRepository.delete(event);
        attachmentUrls.forEach(eventStorageService::delete);
    }

    @Transactional
    public EventResponse uploadAttachments(String id, List<MultipartFile> files, CurrentUser currentUser) {
        Event event = getEvent(id);
        eventScopeService.requireWrite(currentUser, event);
        if (files == null || files.isEmpty()) {
            throw new EventServiceException(
                    HttpStatus.BAD_REQUEST,
                    "EVENT_ATTACHMENT_REQUIRED",
                    "At least one attachment is required"
            );
        }

        List<String> storedUrls = new java.util.ArrayList<>();
        try {
            for (MultipartFile file : files) {
                String url = eventStorageService.storeEventAttachment(file);
                storedUrls.add(url);
                EventAttachment attachment = new EventAttachment();
                attachment.setFileUrl(url);
                attachment.setAttachmentKind(eventStorageService.attachmentKind(file));
                attachment.setFileName(file.getOriginalFilename());
                event.addAttachment(attachment);
            }
            return eventMapper.toResponse(eventRepository.saveAndFlush(event));
        } catch (RuntimeException exception) {
            storedUrls.forEach(eventStorageService::delete);
            throw exception;
        }
    }

    @Transactional
    public void deleteAttachment(String eventId, String attachmentId, CurrentUser currentUser) {
        Event event = getEvent(eventId);
        eventScopeService.requireWrite(currentUser, event);
        EventAttachment attachment = eventAttachmentRepository.findByIdAndEvent_Id(attachmentId, eventId)
                .orElseThrow(() -> new EventServiceException(
                        HttpStatus.NOT_FOUND,
                        "EVENT_ATTACHMENT_NOT_FOUND",
                        "Event attachment not found"
                ));
        String url = attachment.getFileUrl();
        event.removeAttachment(attachment);
        eventStorageService.delete(url);
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
