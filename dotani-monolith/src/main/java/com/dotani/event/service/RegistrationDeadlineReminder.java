package com.dotani.event.service;

import com.dotani.event.entity.Event;
import com.dotani.event.entity.EventStatus;
import com.dotani.event.repository.EventRepository;
import com.dotani.shared.events.DomainEvents.EventDeadlineApproaching;
import java.time.Duration;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every hour, reminds members who have not answered yet that registration closes within 24 hours. */
@Service
public class RegistrationDeadlineReminder {

    static final Duration WINDOW = Duration.ofHours(24);

    private final EventRepository eventRepository;
    private final ApplicationEventPublisher events;

    public RegistrationDeadlineReminder(EventRepository eventRepository, ApplicationEventPublisher events) {
        this.eventRepository = eventRepository;
        this.events = events;
    }

    @Scheduled(cron = "${app.reminders.deadline-cron:0 0 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public int remindUpcomingDeadlines() {
        Instant now = Instant.now();
        var due = eventRepository.findByStatusAndRegistrationDeadlineBetweenAndDeadlineRemindedAtIsNull(
                EventStatus.PUBLISHED, now, now.plus(WINDOW));
        for (Event event : due) {
            event.markDeadlineReminded(now);
            events.publishEvent(new EventDeadlineApproaching(
                    event.getId(), event.getTitle(), event.getOrganizationId(), event.getRegistrationDeadline()));
        }
        return due.size();
    }
}
