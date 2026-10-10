package com.dotani.event.service;

import com.dotani.audit.entity.AuditAction;
import com.dotani.audit.entity.ResourceType;
import com.dotani.audit.service.AuditLogService;

import com.dotani.event.dto.EventRequest;
import com.dotani.event.dto.EventResponse;
import com.dotani.event.entity.Event;
import com.dotani.event.entity.EventAttachment;
import com.dotani.event.entity.EventStatus;
import com.dotani.event.entity.EventType;
import com.dotani.shared.exception.ApiException;
import com.dotani.event.mapper.EventMapper;
import com.dotani.event.repository.EventAttachmentRepository;
import com.dotani.event.repository.EventRepository;
import com.dotani.event.repository.EventSpecifications;
import com.dotani.shared.security.CurrentUser;
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
    private final AuditLogService auditLogService;
    private final EventNotificationPublisher eventNotificationPublisher;
    private final EventAttachmentRepository eventAttachmentRepository;
    private final EventStorageService eventStorageService;

    public EventService(
            EventRepository eventRepository,
            EventAttachmentRepository eventAttachmentRepository,
            EventMapper eventMapper,
            EventScopeService eventScopeService,
            AuditLogService auditLogService,
            EventNotificationPublisher eventNotificationPublisher,
            EventStorageService eventStorageService
    ) {
        this.eventRepository = eventRepository;
        this.eventAttachmentRepository = eventAttachmentRepository;
        this.eventMapper = eventMapper;
        this.eventScopeService = eventScopeService;
        this.auditLogService = auditLogService;
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
        auditLogService.record(currentUser, AuditAction.CREATE_EVENT, ResourceType.EVENT, saved.getId(), saved.getOrganizationId());
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
        auditLogService.record(currentUser, AuditAction.UPDATE_EVENT, ResourceType.EVENT, event.getId(), event.getOrganizationId());
        return eventMapper.toResponse(event);
    }

    @Transactional
    public void delete(String id, CurrentUser currentUser) {
        Event event = getEvent(id);
        eventScopeService.requireWrite(currentUser, event);
        List<String> attachmentUrls = event.getAttachments().stream().map(EventAttachment::getFileUrl).toList();
        auditLogService.record(currentUser, AuditAction.DELETE_EVENT, ResourceType.EVENT, event.getId(), event.getOrganizationId());
        eventRepository.delete(event);
        attachmentUrls.forEach(eventStorageService::delete);
    }

    @Transactional
    public EventResponse uploadAttachments(String id, List<MultipartFile> files, CurrentUser currentUser) {
        Event event = getEvent(id);
        eventScopeService.requireWrite(currentUser, event);
        if (files == null || files.isEmpty()) {
            throw new ApiException(
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
                .orElseThrow(() -> new ApiException(
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
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "EVENT_NOT_FOUND",
                        "Event not found"
                ));
    }

    private void validateTimeRange(EventRequest request) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_EVENT_TIME_RANGE",
                    "startTime must be before endTime"
            );
        }
        if (request.registrationDeadline() != null
                && request.registrationDeadline().isAfter(request.startTime())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_REGISTRATION_DEADLINE",
                    "registrationDeadline must be before or equal to startTime"
            );
        }
    }
}
