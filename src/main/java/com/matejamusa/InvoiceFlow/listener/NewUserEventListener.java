package com.matejamusa.InvoiceFlow.listener;

import com.matejamusa.InvoiceFlow.event.NewUserEvent;
import com.matejamusa.InvoiceFlow.service.EventService;
import com.matejamusa.InvoiceFlow.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewUserEventListener {
    private final EventService eventService;
    private final HttpServletRequest request;

    @EventListener
    public void onNewUserEvent(NewUserEvent event) {
        eventService.addUserEvent(event.getEmail(), event.getType(), RequestUtils.getDevice(request), RequestUtils.getIpAddress(request));
    }
}
