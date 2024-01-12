package com.matejamusa.InvoiceFlow.service;

import com.matejamusa.InvoiceFlow.enumeration.EventType;
import com.matejamusa.InvoiceFlow.model.UserEvent;

import java.util.Collection;

public interface EventService {
    Collection<UserEvent> getEventsByUserId(Long userId);
    void addUserEvent(Long userId, EventType eventType, String device, String ipAddress);
    void addUserEvent(String email, EventType eventType, String device, String ipAddress);
}
