package com.dotani.event;

import com.dotani.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dotani.event.entity.Event;
import com.dotani.event.entity.EventParticipation;
import com.dotani.event.entity.EventStatus;
import com.dotani.event.entity.EventType;
import com.dotani.event.entity.ParticipationStatus;
import com.dotani.event.repository.EventParticipationRepository;
import com.dotani.event.repository.EventRepository;
import com.dotani.shared.time.AppTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class EventIntegrationTest extends IntegrationTest {

    private static final String WARD_ID = "ward-thuong-cat";
    private static final String TDP_1_ID = "tdp-1";
    private static final String TDP_2_ID = "tdp-2";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventParticipationRepository participationRepository;

    private Event wardEvent;
    private Event tdp1Event;
    private Event tdp2Event;

    @BeforeEach
    void setUp() {
        data.legacyIds();
        participationRepository.deleteAll();
        eventRepository.deleteAll();
        wardEvent = saveEvent("Hop BCH phuong", EventType.MEETING, WARD_ID, EventStatus.PUBLISHED, 1);
        tdp1Event = saveEvent("Hoat dong TDP 1", EventType.ACTIVITY, TDP_1_ID, EventStatus.PUBLISHED, 2);
        tdp2Event = saveEvent("Dai hoi TDP 2", EventType.CONGRESS, TDP_2_ID, EventStatus.DRAFT, 3);
    }

    @Test
    void wardSecretaryCanCrudEventsAcrossWard() throws Exception {
        MvcResult result = mockMvc.perform(withWardSecretary(post("/api/events"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(eventPayload("Su kien toan phuong", EventType.EVENT, TDP_2_ID, EventStatus.PUBLISHED, 5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Su kien toan phuong"))
                .andExpect(jsonPath("$.organizationId").value(TDP_2_ID))
                .andReturn();

        String eventId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(withWardSecretary(get("/api/events/{id}", eventId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId));

        mockMvc.perform(withWardSecretary(put("/api/events/{id}", eventId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(eventPayload("Cap nhat su kien", EventType.TASK, TDP_1_ID, EventStatus.PUBLISHED, 6))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Cap nhat su kien"))
                .andExpect(jsonPath("$.organizationId").value(TDP_1_ID));

        mockMvc.perform(withWardSecretary(delete("/api/events/{id}", eventId)))
                .andExpect(status().isNoContent());

        assertThat(eventRepository.findById(eventId)).isEmpty();
    }

    @Test
    void tdpSecretaryCanManageOwnTdpEventsButNotWardEvents() throws Exception {
        String createdId = objectMapper.readTree(mockMvc.perform(withTdpSecretary(post("/api/events"), TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(eventPayload("Sinh hoat TDP 1", EventType.MEETING, TDP_1_ID, EventStatus.PUBLISHED, 7))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(withTdpSecretary(put("/api/events/{id}", tdp1Event.getId()), TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(eventPayload("Sinh hoat TDP 1 updated", EventType.MEETING, TDP_1_ID, EventStatus.PUBLISHED, 8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Sinh hoat TDP 1 updated"));

        mockMvc.perform(withTdpSecretary(delete("/api/events/{id}", createdId), TDP_1_ID))
                .andExpect(status().is2xxSuccessful());

        // a ward-level event is out of a TDP secretary's reach
        mockMvc.perform(withTdpSecretary(post("/api/events"), TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(eventPayload("Dai hoi phuong", EventType.CONGRESS, WARD_ID, EventStatus.PUBLISHED, 7))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));
        mockMvc.perform(withTdpSecretary(put("/api/events/{id}", wardEvent.getId()), TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(eventPayload("Sua su kien phuong", EventType.MEETING, WARD_ID, EventStatus.PUBLISHED, 7))))
                .andExpect(status().isForbidden());
    }

    @Test
    void tdpSecretaryCannotUpdateOrDeleteEventFromAnotherTdp() throws Exception {
        mockMvc.perform(withTdpSecretary(put("/api/events/{id}", tdp2Event.getId()), TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(eventPayload("Blocked", EventType.CONGRESS, TDP_2_ID, EventStatus.PUBLISHED, 9))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withTdpSecretary(delete("/api/events/{id}", tdp2Event.getId()), TDP_1_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        Event unchanged = eventRepository.findById(tdp2Event.getId()).orElseThrow();
        assertThat(unchanged.getTitle()).isEqualTo("Dai hoi TDP 2");
    }

    @Test
    void tdpSecretaryCannotCreateOrMoveEventIntoAnotherTdp() throws Exception {
        mockMvc.perform(withTdpSecretary(post("/api/events"), TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(eventPayload("Cross create", EventType.EVENT, TDP_2_ID, EventStatus.PUBLISHED, 10))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withTdpSecretary(put("/api/events/{id}", tdp1Event.getId()), TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(eventPayload("Cross move", EventType.ACTIVITY, TDP_2_ID, EventStatus.PUBLISHED, 11))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        assertThat(eventRepository.findById(tdp1Event.getId()).orElseThrow().getOrganizationId())
                .isEqualTo(TDP_1_ID);
    }

    @Test
    void memberCanOnlySeeWardAndOwnTdpEvents() throws Exception {
        mockMvc.perform(withMember(get("/api/events"), "member-1", TDP_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(wardEvent.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(tdp1Event.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(tdp2Event.getId())).doesNotExist());

        mockMvc.perform(withMember(get("/api/events/{id}", tdp2Event.getId()), "member-1", TDP_1_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withMember(post("/api/events"), "member-1", TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(eventPayload("Member blocked", EventType.EVENT, TDP_1_ID, EventStatus.PUBLISHED, 12))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));
    }

    @Test
    void supportsFiltersAndPagination() throws Exception {
        mockMvc.perform(withWardSecretary(get("/api/events")
                        .param("type", "CONGRESS")
                        .param("organization", TDP_2_ID)
                        .param("status", "DRAFT")
                        .param("date", OffsetDateTime.now(AppTime.ZONE).plusDays(3).toLocalDate().toString())
                        .param("upcoming", "true")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "startTime,asc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(tdp2Event.getId()));
    }

    @Test
    void validatesStartTimeBeforeEndTime() throws Exception {
        Map<String, Object> payload = eventPayload("Invalid time", EventType.EVENT, TDP_1_ID, EventStatus.PUBLISHED, 13);
        payload.put("endTime", payload.get("startTime"));

        mockMvc.perform(withWardSecretary(post("/api/events"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_EVENT_TIME_RANGE"));
    }

    @Test
    void validatesRegistrationDeadlineBeforeStartTime() throws Exception {
        Map<String, Object> payload = eventPayload("Invalid deadline", EventType.EVENT, TDP_1_ID, EventStatus.PUBLISHED, 14);
        payload.put("registrationDeadline", OffsetDateTime.now(AppTime.ZONE).plusDays(20).toInstant().toString());

        mockMvc.perform(withWardSecretary(post("/api/events"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REGISTRATION_DEADLINE"));
    }

    @Test
    void memberCanVoteAndVoteAgainUpdatesExistingRecord() throws Exception {
        mockMvc.perform(withMember(put("/api/events/{eventId}/participation", tdp1Event.getId()), "member-1", TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "GOING"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(tdp1Event.getId()))
                .andExpect(jsonPath("$.memberId").value("member-1"))
                .andExpect(jsonPath("$.status").value("GOING"));

        mockMvc.perform(withMember(put("/api/events/{eventId}/participation", tdp1Event.getId()), "member-1", TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "NOT_GOING"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_GOING"));

        assertThat(participationRepository.findAll()).hasSize(1);
        assertThat(participationRepository.findByEvent_IdAndMemberId(tdp1Event.getId(), "member-1").orElseThrow().getStatus())
                .isEqualTo(ParticipationStatus.NOT_GOING);
    }

    @Test
    void memberCannotVoteEventOutsideReadableScope() throws Exception {
        mockMvc.perform(withMember(put("/api/events/{eventId}/participation", tdp2Event.getId()), "member-1", TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "GOING"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));
    }

    @Test
    void memberCannotVoteAfterRegistrationDeadline() throws Exception {
        tdp1Event.setRegistrationDeadline(OffsetDateTime.now(AppTime.ZONE).minusMinutes(1).toInstant());
        eventRepository.save(tdp1Event);

        mockMvc.perform(withMember(put("/api/events/{eventId}/participation", tdp1Event.getId()), "member-1", TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "GOING"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REGISTRATION_DEADLINE_PASSED"));
    }

    @Test
    void maxParticipantsPreventsExtraGoingVotes() throws Exception {
        tdp1Event.setMaxParticipants(1);
        eventRepository.save(tdp1Event);

        mockMvc.perform(withMember(put("/api/events/{eventId}/participation", tdp1Event.getId()), "member-1", TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "GOING"))))
                .andExpect(status().isOk());

        mockMvc.perform(withMember(put("/api/events/{eventId}/participation", tdp1Event.getId()), "member-2", TDP_1_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "GOING"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_FULL"));

        assertThat(participationRepository.countByEvent_IdAndStatus(tdp1Event.getId(), ParticipationStatus.GOING))
                .isEqualTo(1);
    }

    @Test
    void officersCanViewParticipantListOfEventsTheyManage() throws Exception {
        saveParticipation(tdp1Event, "member-1", ParticipationStatus.GOING);
        saveParticipation(tdp1Event, "member-2", ParticipationStatus.UNDECIDED);

        mockMvc.perform(withWardSecretary(get("/api/events/{eventId}/participants", tdp1Event.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[?(@.memberId == 'member-1')]").exists())
                .andExpect(jsonPath("$.content[?(@.memberId == 'member-2')]").exists());

        // a TDP secretary manages, and so sees participants of, events of their own TDP only
        mockMvc.perform(withTdpSecretary(get("/api/events/{eventId}/participants", tdp1Event.getId()), TDP_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
        mockMvc.perform(withTdpSecretary(get("/api/events/{eventId}/participants", tdp2Event.getId()), TDP_1_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withMember(get("/api/events/{eventId}/participants", tdp1Event.getId()), "member-1", TDP_1_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));
    }

    @Test
    void participationSummaryReturnsCountsForReadableEvent() throws Exception {
        saveParticipation(tdp1Event, "member-1", ParticipationStatus.GOING);
        saveParticipation(tdp1Event, "member-2", ParticipationStatus.NOT_GOING);
        saveParticipation(tdp1Event, "member-3", ParticipationStatus.UNDECIDED);
        saveParticipation(tdp1Event, "member-4", ParticipationStatus.GOING);

        mockMvc.perform(withMember(get("/api/events/{eventId}/participation-summary", tdp1Event.getId()), "member-1", TDP_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.going").value(2))
                .andExpect(jsonPath("$.notGoing").value(1))
                .andExpect(jsonPath("$.undecided").value(1));
    }

    @Test
    void currentMemberCanViewOwnParticipatedEvents() throws Exception {
        saveParticipation(wardEvent, "member-1", ParticipationStatus.GOING);
        saveParticipation(tdp1Event, "member-1", ParticipationStatus.UNDECIDED);
        saveParticipation(tdp2Event, "member-1", ParticipationStatus.NOT_GOING);
        saveParticipation(tdp1Event, "member-2", ParticipationStatus.GOING);

        mockMvc.perform(withMember(get("/api/events/me"), "member-1", TDP_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[?(@.eventId == '%s')]".formatted(wardEvent.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.eventId == '%s')]".formatted(tdp1Event.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.eventId == '%s')]".formatted(tdp2Event.getId())).doesNotExist());
    }

    private Event saveEvent(
            String title,
            EventType type,
            String organizationId,
            EventStatus status,
            int daysFromNow
    ) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(title + " description");
        event.setType(type);
        event.setLocation("Thuong Cat");
        event.setStartTime(OffsetDateTime.now(AppTime.ZONE).plusDays(daysFromNow).withNano(0).toInstant());
        event.setEndTime(OffsetDateTime.now(AppTime.ZONE).plusDays(daysFromNow).plusHours(2).withNano(0).toInstant());
        event.setRegistrationDeadline(OffsetDateTime.now(AppTime.ZONE).plusDays(daysFromNow).minusDays(1).withNano(0).toInstant());
        event.setOrganizationId(organizationId);
        event.setMaxParticipants(50);
        event.setStatus(status);
        event.setCreatedBy("seed-user");
        return eventRepository.save(event);
    }

    private EventParticipation saveParticipation(
            Event event,
            String memberId,
            ParticipationStatus status
    ) {
        EventParticipation participation = new EventParticipation();
        participation.setEvent(event);
        participation.setMemberId(memberId);
        participation.setStatus(status);
        return participationRepository.save(participation);
    }

    private Map<String, Object> eventPayload(
            String title,
            EventType type,
            String organizationId,
            EventStatus status,
            int daysFromNow
    ) {
        OffsetDateTime start = OffsetDateTime.now(AppTime.ZONE).plusDays(daysFromNow).withNano(0);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("description", title + " description");
        payload.put("type", type.name());
        payload.put("location", "Nha van hoa");
        payload.put("startTime", start.toInstant().toString());
        payload.put("endTime", start.plusHours(2).toInstant().toString());
        payload.put("registrationDeadline", start.minusDays(1).toInstant().toString());
        payload.put("organizationId", organizationId);
        payload.put("maxParticipants", 100);
        payload.put("status", status.name());
        return payload;
    }

    private MockHttpServletRequestBuilder withWardSecretary(MockHttpServletRequestBuilder request) {
        return request
                .with(auth.identity("ward-secretary-user", "WARD_SECRETARY", WARD_ID, null, null));
    }

    private MockHttpServletRequestBuilder withTdpSecretary(MockHttpServletRequestBuilder request, String tdpId) {
        return request
                .with(auth.identity("tdp-secretary-user", "TDP_SECRETARY", WARD_ID, tdpId, null));
    }

    private MockHttpServletRequestBuilder withMember(
            MockHttpServletRequestBuilder request,
            String memberId,
            String tdpId
    ) {
        return request
                .with(auth.identity("member-user", "MEMBER", WARD_ID, tdpId, memberId));
    }

}
