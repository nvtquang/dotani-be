package com.hcmcyu.event.service;

import com.hcmcyu.event.dto.EventDashboardSummaryResponse;
import com.hcmcyu.event.dto.EventResponse;
import com.hcmcyu.event.entity.EventStatus;
import com.hcmcyu.event.entity.ParticipationStatus;
import com.hcmcyu.event.mapper.EventMapper;
import com.hcmcyu.event.repository.EventParticipationRepository;
import com.hcmcyu.event.repository.EventRepository;
import com.hcmcyu.event.repository.EventSpecifications;
import com.hcmcyu.event.security.CurrentUser;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventDashboardService {

    private static final int DASHBOARD_EVENT_LIMIT = 5;

    private final EventRepository eventRepository;
    private final EventParticipationRepository participationRepository;
    private final EventMapper eventMapper;

    public EventDashboardService(
            EventRepository eventRepository,
            EventParticipationRepository participationRepository,
            EventMapper eventMapper
    ) {
        this.eventRepository = eventRepository;
        this.participationRepository = participationRepository;
        this.eventMapper = eventMapper;
    }

    @Transactional(readOnly = true)
    public EventDashboardSummaryResponse getSummary(CurrentUser currentUser) {
        Specification<com.hcmcyu.event.entity.Event> upcomingSpec = Specification
                .where(EventSpecifications.withinReadScope(currentUser))
                .and(EventSpecifications.hasStatus(EventStatus.PUBLISHED))
                .and(EventSpecifications.upcoming(true));

        List<EventResponse> upcomingEvents = eventRepository.findAll(
                        upcomingSpec,
                        PageRequest.of(0, DASHBOARD_EVENT_LIMIT, Sort.by("startTime").ascending())
                )
                .map(eventMapper::toResponse)
                .toList();

        String organizationScope = organizationScope(currentUser);
        long registeredParticipantCount = currentUser.isMember() && currentUser.memberId() != null
                ? participationRepository.countByMemberIdAndStatus(currentUser.memberId(), ParticipationStatus.GOING)
                : participationRepository.countByStatusScoped(ParticipationStatus.GOING, organizationScope);

        List<EventResponse> registeredEvents = currentUser.isMember() && currentUser.memberId() != null
                ? participationRepository.findByMemberIdOrderByUpdatedAtDesc(currentUser.memberId())
                        .stream()
                        .map(participation -> eventMapper.toResponse(participation.getEvent()))
                        .limit(DASHBOARD_EVENT_LIMIT)
                        .toList()
                : List.of();

        return new EventDashboardSummaryResponse(
                eventRepository.count(upcomingSpec),
                registeredParticipantCount,
                upcomingEvents,
                registeredEvents
        );
    }

    private String organizationScope(CurrentUser currentUser) {
        if (currentUser.hasWardScope()) {
            return null;
        }
        if (currentUser.tdpId() != null && !currentUser.tdpId().isBlank()) {
            return currentUser.tdpId();
        }
        return currentUser.organizationId();
    }
}
