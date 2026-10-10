package com.dotani.event.service;

import com.dotani.event.entity.Event;

public interface EventNotificationPublisher {

    void publishEventCreated(Event event);
}
