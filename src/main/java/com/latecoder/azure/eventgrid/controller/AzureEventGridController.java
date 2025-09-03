package com.latecoder.azure.eventgrid.controller;

import com.latecoder.azure.eventgrid.model.ObjectInfo;
import com.latecoder.azure.eventgrid.model.UploadEventNotificationRequest;
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
    public Map<String, String> handleAzureEventGrid(@RequestBody List<Map<String, Object>> events) {
        for (Map<String, Object> event : events) {
            try {
                String eventType = (String) event.get("eventType");

                // 🔹 Handle Subscription Validation handshake
                if ("Microsoft.EventGrid.SubscriptionValidationEvent".equals(eventType)) {
                    Map<String, Object> data = (Map<String, Object>) event.get("data");
                    String validationCode = (String) data.get("validationCode");
                    log.info("✅ Subscription validation event received, responding with code={}", validationCode);

                    return Map.of("validationResponse", validationCode);
                }

                // 🔹 Only process blob created events
                if (!"Microsoft.Storage.BlobCreated".equals(eventType)) continue;

                String subject = (String) event.get("subject");
                Map<String, Object> data = (Map<String, Object>) event.get("data");

                // Example subject: /blobServices/default/containers/mycontainer/blobs/folder/file.txt
                String[] parts = subject.split("/blobs/");
                if (parts.length != 2) continue;

                String containerPath = parts[0];
                String objectPath = parts[1];
                String bucketName = containerPath.substring(containerPath.lastIndexOf('/') + 1);

                UploadEventNotificationRequest request = new UploadEventNotificationRequest();
                request.setBucketName(bucketName);

                ObjectInfo objectInfo = new ObjectInfo();
                objectInfo.setKey(objectPath);
                objectInfo.setFileName(objectPath.substring(objectPath.lastIndexOf('/') + 1));
                objectInfo.setSize(((Number) data.getOrDefault("contentLength", 0L)).longValue());
                objectInfo.setETag((String) data.getOrDefault("etag", null));

                // Infer file format
                String fileName = objectInfo.getFileName();
                if (fileName != null && fileName.contains(".")) {
                    objectInfo.setFormat(fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase());
                } else {
                    objectInfo.setFormat("UNKNOWN");
                }

                objectInfo.setSourceIP((String) data.getOrDefault("clientRequestId", null));

                if (data.containsKey("metadata") && data.get("metadata") instanceof Map<?, ?> md) {
                    md.forEach((k, v) -> objectInfo.getMetadata().put(String.valueOf(k), String.valueOf(v)));
                }

                request.setObjectInfo(objectInfo);


            } catch (Exception e) {
                log.error("❌ Failed to process Event Grid event: {}", event, e);
            }
        }
        return Map.of(); // always return something to keep Event Grid happy
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("App Controller is healthy");
    }
}
