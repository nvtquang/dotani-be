package com.dotani.event.service;

import com.dotani.shared.attachment.Attachment;
import com.dotani.shared.events.DomainEvents.EventCancelled;
import com.dotani.shared.events.DomainEvents.EventChanged;
import com.dotani.shared.events.DomainEvents.EventPublished;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import com.dotani.shared.attachment.AttachmentKind;
import com.dotani.shared.attachment.AttachmentOwnerType;
import com.dotani.shared.attachment.AttachmentRepository;
import com.dotani.audit.entity.AuditAction;
import com.dotani.audit.entity.ResourceType;
import com.dotani.audit.service.AuditDiff;
import com.dotani.audit.service.AuditLogService;

import com.dotani.event.dto.EventRequest;
import com.dotani.event.dto.EventResponse;
import com.dotani.event.entity.Event;
import com.dotani.event.entity.EventStatus;
import com.dotani.event.entity.EventType;
import com.dotani.shared.exception.ApiException;
import com.dotani.event.mapper.EventMapper;
import com.dotani.event.repository.EventRepository;
import com.dotani.event.repository.EventSpecifications;
import com.dotani.shared.security.CurrentUser;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EventService {

    // changes that matter to people who answered the event
    private static final Set<String> NOTIFIED_FIELDS = Set.of("title", "startTime", "endTime", "location", "registrationDeadline");

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventScopeService eventScopeService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher events;
    private final AttachmentRepository attachmentRepository;
    private final EventStorageService eventStorageService;

    public EventService(
            EventRepository eventRepository,
            AttachmentRepository attachmentRepository,
            EventMapper eventMapper,
            EventScopeService eventScopeService,
            AuditLogService auditLogService,
            ApplicationEventPublisher events,
            EventStorageService eventStorageService
    ) {
        this.eventRepository = eventRepository;
        this.attachmentRepository = attachmentRepository;
        this.eventMapper = eventMapper;
        this.eventScopeService = eventScopeService;
        this.auditLogService = auditLogService;
        this.events = events;
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

        return eventMapper.toResponsePage(eventRepository.findAll(specification, pageable));
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
        auditLogService.record(currentUser, AuditAction.CREATE_EVENT, ResourceType.EVENT, saved.getId(),
                saved.getOrganizationId(), AuditDiff.created(auditSnapshot(saved)));
        if (saved.getStatus() == EventStatus.PUBLISHED) {
            events.publishEvent(new EventPublished(saved.getId(), saved.getTitle(), saved.getDescription(), saved.getOrganizationId()));
        }
        return eventMapper.toResponse(saved);
    }

    @Transactional
    public EventResponse update(String id, EventRequest request, CurrentUser currentUser) {
        validateTimeRange(request);
        Event event = getEvent(id);
        eventScopeService.requireWrite(currentUser, event);
        eventScopeService.requireCreateInOrganization(currentUser, request.organizationId());

        Map<String, Object> before = auditSnapshot(event);
        EventStatus oldStatus = event.getStatus();
        eventMapper.apply(event, request);
        Map<String, Object> changes = AuditDiff.changes(before, auditSnapshot(event));
        auditLogService.record(currentUser, AuditAction.UPDATE_EVENT, ResourceType.EVENT, event.getId(),
                event.getOrganizationId(), changes);
        publishUpdateEvents(event, oldStatus, changes);
        return eventMapper.toResponse(event);
    }

    @Transactional
    public void delete(String id, CurrentUser currentUser) {
        Event event = getEvent(id);
        eventScopeService.requireWrite(currentUser, event);
        auditLogService.record(currentUser, AuditAction.DELETE_EVENT, ResourceType.EVENT, event.getId(),
                event.getOrganizationId(), AuditDiff.deleted(auditSnapshot(event)));
        // soft delete: the event, its attachments and participations stay in the database
        event.markDeleted();
        if (event.getStatus() == EventStatus.PUBLISHED) {
            events.publishEvent(new EventCancelled(event.getId(), event.getTitle(), event.getOrganizationId(), true));
        }
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
                event.addAttachment(Attachment.of(
                        AttachmentOwnerType.EVENT,
                        event.getId(),
                        AttachmentKind.valueOf(eventStorageService.attachmentKind(file)),
                        url,
                        file.getOriginalFilename(),
                        file.getContentType(),
                        file.getSize(),
                        currentUser.memberId()
                ));
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
        Attachment attachment = attachmentRepository.findByIdAndOwnerTypeAndOwnerId(attachmentId, AttachmentOwnerType.EVENT, eventId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "EVENT_ATTACHMENT_NOT_FOUND",
                        "Event attachment not found"
                ));
        String url = attachment.getUrl();
        event.removeAttachment(attachment);
        eventStorageService.delete(url);
    }

    // Members hear about an event when it becomes visible, when what they answered changes, or when it is cancelled.
    private void publishUpdateEvents(Event event, EventStatus oldStatus, Map<String, Object> changes) {
        EventStatus newStatus = event.getStatus();
        if (oldStatus != EventStatus.PUBLISHED && newStatus == EventStatus.PUBLISHED) {
            events.publishEvent(new EventPublished(event.getId(), event.getTitle(), event.getDescription(), event.getOrganizationId()));
        } else if (oldStatus == EventStatus.PUBLISHED && newStatus == EventStatus.CANCELLED) {
            events.publishEvent(new EventCancelled(event.getId(), event.getTitle(), event.getOrganizationId(), false));
        } else if (newStatus == EventStatus.PUBLISHED && changes.keySet().stream().anyMatch(NOTIFIED_FIELDS::contains)) {
            events.publishEvent(new EventChanged(event.getId(), event.getTitle(), event.getOrganizationId()));
        }
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

    private Map<String, Object> auditSnapshot(Event event) {
        return AuditDiff.snapshot(
                "title", event.getTitle(),
                "type", event.getType(),
                "status", event.getStatus(),
                "organizationId", event.getOrganizationId(),
                "startTime", event.getStartTime(),
                "endTime", event.getEndTime(),
                "registrationDeadline", event.getRegistrationDeadline(),
                "maxParticipants", event.getMaxParticipants()
        );
    }
}
