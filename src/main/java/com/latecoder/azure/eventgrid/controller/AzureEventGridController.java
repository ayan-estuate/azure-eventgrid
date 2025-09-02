package com.latecoder.azure.eventgrid.controller;

import com.latecoder.azure.eventgrid.service.AzureEventGridService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/event-grid")
public class AzureEventGridController {
    private final AzureEventGridService eventGridService;

    @PostMapping("/create")
    public ResponseEntity<String> createSubscription(
            @RequestParam String accHolderTenantId,
            @RequestParam String webhookEndpoint
    ) {
        String response = eventGridService.createOrUpdateSubscription(accHolderTenantId, webhookEndpoint);
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteSubscription(
            @RequestParam String accHolderTenantId
    ) {
        String response = eventGridService.deleteSubscription(accHolderTenantId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notify")
    public ResponseEntity<?> handleEvent(@RequestBody List<Map<String, Object>> events) {
        log.info("Received EventGrid event: {}", events);

        Map<String, Object> firstEvent = events.get(0);
        String eventType = (String) firstEvent.get("eventType");

        if ("Microsoft.EventGrid.SubscriptionValidationEvent".equals(eventType)) {
            Map<String, Object> data = (Map<String, Object>) firstEvent.get("data");
            String validationCode = (String) data.get("validationCode");
            log.info("Responding to validation with code={}", validationCode);
            return ResponseEntity.ok(Map.of("validationResponse", validationCode));
        }

        // Handle blob events
        log.info("Received business event type={} data={}", eventType, firstEvent.get("data"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/testing")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Azure Event Grid Controller is working!👍");
    }
}
