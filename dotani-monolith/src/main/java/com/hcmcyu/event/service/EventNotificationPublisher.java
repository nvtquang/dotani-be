package com.hcmcyu.event.service;

import com.hcmcyu.event.entity.Event;

public interface EventNotificationPublisher {

    void publishEventCreated(Event event);
}
