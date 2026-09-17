package com.hcmcyu.event.controller;

import com.hcmcyu.event.dto.EventRequest;
import com.hcmcyu.event.dto.EventParticipationRequest;
import com.hcmcyu.event.dto.EventParticipationResponse;
import com.hcmcyu.event.dto.EventParticipationSummaryResponse;
import com.hcmcyu.event.dto.EventResponse;
import com.hcmcyu.event.entity.EventStatus;
import com.hcmcyu.event.entity.EventType;
import com.hcmcyu.event.security.CurrentUser;
import com.hcmcyu.event.service.EventParticipationService;
import com.hcmcyu.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Event, meeting, congress, task, activity, and participation APIs.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT Bearer token"),
        @ApiResponse(responseCode = "403", description = "Role or organization scope denied"),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "409", description = "Registration deadline, duplicate, or capacity conflict")
})
public class EventController {

    private final EventService eventService;
    private final EventParticipationService participationService;

    public EventController(EventService eventService, EventParticipationService participationService) {
        this.eventService = eventService;
        this.participationService = participationService;
    }

    @GetMapping
    @Operation(summary = "List events", description = "Supports type, organization, status, date, upcoming, page, and size filters. Visibility is scoped by the current user's role and organization.")
    public Page<EventResponse> findAll(
            @RequestParam(required = false, name = "type") EventType type,
            @RequestParam(required = false, name = "organization") String organizationId,
            @RequestParam(required = false, name = "status") EventStatus status,
            @RequestParam(required = false, name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, name = "upcoming") Boolean upcoming,
            Pageable pageable,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return eventService.findAll(type, organizationId, status, date, upcoming, pageable, currentUser);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by id", description = "Returns an event visible to the current user. Organization scope prevents IDOR/BOLA across TDPs.")
    public EventResponse findById(
            @PathVariable("id") String id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return eventService.findById(id, currentUser);
    }

    @GetMapping("/me")
    @Operation(summary = "List my event participations", description = "MEMBER and officers can see their own participation records.")
    public List<EventParticipationResponse> findMyEvents(@AuthenticationPrincipal CurrentUser currentUser) {
        return participationService.findCurrentMemberEvents(currentUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create event", description = "WARD officers can manage ward-wide events. TDP officers can manage only events in their own TDP.")
    public EventResponse create(
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return eventService.create(request, currentUser);
    }

    @PutMapping("/{eventId}/participation")
    @Operation(summary = "Vote event participation", description = "Current member votes only for self. Re-voting updates the existing record. Registration deadline and maxParticipants are enforced.")
    public EventParticipationResponse updateParticipation(
            @PathVariable("eventId") String eventId,
            @Valid @RequestBody EventParticipationRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return participationService.updateParticipation(eventId, request, currentUser);
    }

    @GetMapping("/{eventId}/participants")
    @Operation(summary = "List event participants", description = "Only officers who can manage the event can view participant list.")
    public List<EventParticipationResponse> findParticipants(
            @PathVariable("eventId") String eventId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return participationService.findParticipants(eventId, currentUser);
    }

    @GetMapping("/{eventId}/participation-summary")
    @Operation(summary = "Get participation summary", description = "Returns going, notGoing, and undecided counts for a visible event.")
    public EventParticipationSummaryResponse participationSummary(
            @PathVariable("eventId") String eventId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return participationService.summarize(eventId, currentUser);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update event", description = "Enforces role and organization scope. TDP officers cannot update another TDP's event.")
    public EventResponse update(
            @PathVariable("id") String id,
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return eventService.update(id, request, currentUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete event", description = "Enforces role and organization scope. TDP officers cannot delete another TDP's event.")
    public void delete(
            @PathVariable("id") String id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        eventService.delete(id, currentUser);
    }
}
