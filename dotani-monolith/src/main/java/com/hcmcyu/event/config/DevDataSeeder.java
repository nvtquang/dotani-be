package com.hcmcyu.event.config;

import com.hcmcyu.event.entity.Event;
import com.hcmcyu.event.entity.EventParticipation;
import com.hcmcyu.event.entity.EventStatus;
import com.hcmcyu.event.entity.EventType;
import com.hcmcyu.event.entity.ParticipationStatus;
import com.hcmcyu.event.repository.EventParticipationRepository;
import com.hcmcyu.event.repository.EventRepository;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private final EventRepository eventRepository;
    private final EventParticipationRepository participationRepository;

    public DevDataSeeder(
            EventRepository eventRepository,
            EventParticipationRepository participationRepository
    ) {
        this.eventRepository = eventRepository;
        this.participationRepository = participationRepository;
    }

    @Override
    public void run(String... args) {
        if (eventRepository.count() > 0) {
            return;
        }

        Event wardMeeting = seedEvent(
                "Họp BCH Đoàn phường Thượng Cát",
                EventType.MEETING,
                "Hội trường UBND phường",
                "ward-thuong-cat",
                "user-ward-secretary",
                3,
                80
        );
        seedParticipation(wardMeeting, "ward-secretary-member", ParticipationStatus.GOING);
        seedParticipation(wardMeeting, "ward-deputy-member", ParticipationStatus.GOING);

        for (int tdp = 1; tdp <= 5; tdp++) {
            Event activity = seedEvent(
                    "Hoạt động tình nguyện TDP " + tdp,
                    EventType.ACTIVITY,
                    "Nhà văn hóa TDP " + tdp,
                    "tdp-" + tdp,
                    "user-tdp-" + tdp + "-secretary",
                    tdp + 5,
                    30
            );
            Event congress = seedEvent(
                    "Đại hội chi đoàn TDP " + tdp,
                    EventType.CONGRESS,
                    "Nhà văn hóa TDP " + tdp,
                    "tdp-" + tdp,
                    "user-tdp-" + tdp + "-secretary",
                    tdp + 20,
                    50
            );
            seedParticipation(activity, "tdp-" + tdp + "-secretary-member", ParticipationStatus.GOING);
            seedParticipation(activity, "tdp-" + tdp + "-deputy-member", ParticipationStatus.GOING);
            seedParticipation(congress, "tdp-" + tdp + "-secretary-member", ParticipationStatus.GOING);
            for (int member = 1; member <= 5; member++) {
                seedParticipation(activity, "tdp-" + tdp + "-member-" + member, member == 5
                        ? ParticipationStatus.UNDECIDED
                        : ParticipationStatus.GOING);
                seedParticipation(congress, "tdp-" + tdp + "-member-" + member, member == 4
                        ? ParticipationStatus.NOT_GOING
                        : ParticipationStatus.GOING);
            }
        }
    }

    private Event seedEvent(
            String title,
            EventType type,
            String location,
            String organizationId,
            String createdBy,
            int daysFromNow,
            int maxParticipants
    ) {
        LocalDateTime start = LocalDateTime.now().plusDays(daysFromNow).withNano(0);
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(title + " - dữ liệu mẫu phục vụ môi trường phát triển.");
        event.setType(type);
        event.setLocation(location);
        event.setStartTime(start);
        event.setEndTime(start.plusHours(2));
        event.setRegistrationDeadline(start.minusDays(1));
        event.setOrganizationId(organizationId);
        event.setMaxParticipants(maxParticipants);
        event.setStatus(EventStatus.PUBLISHED);
        event.setCreatedBy(createdBy);
        return eventRepository.save(event);
    }

    private void seedParticipation(Event event, String memberId, ParticipationStatus status) {
        EventParticipation participation = new EventParticipation();
        participation.setEvent(event);
        participation.setMemberId(memberId);
        participation.setStatus(status);
        participationRepository.save(participation);
    }
}
