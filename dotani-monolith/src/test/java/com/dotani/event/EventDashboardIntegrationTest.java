package com.dotani.event;

import com.dotani.support.IntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.event.entity.Event;
import com.dotani.event.entity.EventParticipation;
import com.dotani.event.entity.EventStatus;
import com.dotani.event.entity.EventType;
import com.dotani.event.entity.ParticipationStatus;
import com.dotani.event.repository.EventParticipationRepository;
import com.dotani.event.repository.EventRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class EventDashboardIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventParticipationRepository participationRepository;

    private Event tdp1UpcomingEvent;
    private Event tdp2UpcomingEvent;

    @BeforeEach
    void setUp() {
        participationRepository.deleteAll();
        eventRepository.deleteAll();

        tdp1UpcomingEvent = saveEvent("Hop TDP 1", "tdp-1", LocalDateTime.now().plusDays(2), EventStatus.PUBLISHED);
        tdp2UpcomingEvent = saveEvent("Hop TDP 2", "tdp-2", LocalDateTime.now().plusDays(3), EventStatus.PUBLISHED);
        saveEvent("Su kien cu", "tdp-1", LocalDateTime.now().minusDays(1), EventStatus.PUBLISHED);
        saveEvent("Ban nhap", "tdp-1", LocalDateTime.now().plusDays(4), EventStatus.DRAFT);

        saveParticipation(tdp1UpcomingEvent, "member-1", ParticipationStatus.GOING);
        saveParticipation(tdp1UpcomingEvent, "member-2", ParticipationStatus.NOT_GOING);
        saveParticipation(tdp2UpcomingEvent, "member-3", ParticipationStatus.GOING);
    }

    @Test
    void wardSecretarySeesWardEventDashboardCounts() throws Exception {
        mockMvc.perform(withWardSecretary(get("/api/dashboard/event-summary")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcomingEventCount").value(2))
                .andExpect(jsonPath("$.registeredParticipantCount").value(2))
                .andExpect(jsonPath("$.upcomingEvents.length()").value(2));
    }

    @Test
    void tdpSecretaryOnlySeesOwnTdpEventDashboardCounts() throws Exception {
        mockMvc.perform(withTdpSecretary(get("/api/dashboard/event-summary"), "tdp-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcomingEventCount").value(1))
                .andExpect(jsonPath("$.registeredParticipantCount").value(1))
                .andExpect(jsonPath("$.upcomingEvents[0].organizationId").value("tdp-1"));
    }

    @Test
    void memberSeesUpcomingAndRegisteredEvents() throws Exception {
        mockMvc.perform(withMember(get("/api/dashboard/event-summary"), "member-1", "tdp-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcomingEventCount").value(1))
                .andExpect(jsonPath("$.registeredParticipantCount").value(1))
                .andExpect(jsonPath("$.registeredEvents.length()").value(1))
                .andExpect(jsonPath("$.registeredEvents[0].id").value(tdp1UpcomingEvent.getId()));
    }

    private Event saveEvent(String title, String organizationId, LocalDateTime startTime, EventStatus status) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription("Noi dung");
        event.setType(EventType.MEETING);
        event.setLocation("Hoi truong");
        event.setStartTime(startTime);
        event.setEndTime(startTime.plusHours(2));
        event.setRegistrationDeadline(startTime.minusHours(1));
        event.setOrganizationId(organizationId);
        event.setStatus(status);
        event.setCreatedBy("officer-1");
        return eventRepository.save(event);
    }

    private void saveParticipation(Event event, String memberId, ParticipationStatus status) {
        EventParticipation participation = new EventParticipation();
        participation.setEvent(event);
        participation.setMemberId(memberId);
        participation.setStatus(status);
        participationRepository.save(participation);
    }

    private MockHttpServletRequestBuilder withWardSecretary(MockHttpServletRequestBuilder request) {
        return request
                .with(auth.identity("ward-secretary-user", "WARD_SECRETARY", "ward-thuong-cat", null, null));
    }

    private MockHttpServletRequestBuilder withTdpSecretary(
            MockHttpServletRequestBuilder request,
            String tdpId
    ) {
        return request
                .with(auth.identity("tdp-secretary-user", "TDP_SECRETARY", "ward-thuong-cat", tdpId, null));
    }

    private MockHttpServletRequestBuilder withMember(
            MockHttpServletRequestBuilder request,
            String memberId,
            String tdpId
    ) {
        return request
                .with(auth.identity(memberId + "-user", "MEMBER", "ward-thuong-cat", tdpId, memberId));
    }
}
