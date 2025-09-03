package com.latecoder.azure.eventgrid.controller;

import com.latecoder.azure.eventgrid.model.BucketOwner;
import com.latecoder.azure.eventgrid.model.ObjectInfo;
import com.latecoder.azure.eventgrid.model.UploadEventNotificationRequest;
import com.latecoder.azure.eventgrid.model.UploadEventType;
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

//    @PostMapping("/notify")
//    public Map<String, String> handleAzureEventGrid(@RequestBody List<Map<String, Object>> events) {
//        for (Map<String, Object> event : events) {
//            try {
//                String eventType = (String) event.get("eventType");
//
//                // 🔹 Handle Subscription Validation handshake
//                if ("Microsoft.EventGrid.SubscriptionValidationEvent".equals(eventType)) {
//                    Map<String, Object> data = (Map<String, Object>) event.get("data");
//                    String validationCode = (String) data.get("validationCode");
//                    log.info("✅ Subscription validation event received, responding with code={}", validationCode);
//
//                    return Map.of("validationResponse", validationCode);
//                }
//
//                // 🔹 Only process blob created events
//                if (!"Microsoft.Storage.BlobCreated".equals(eventType)) continue;
//
//                String subject = (String) event.get("subject");
//                Map<String, Object> data = (Map<String, Object>) event.get("data");
//
//                // Example subject: /blobServices/default/containers/mycontainer/blobs/folder/file.txt
//                String[] parts = subject.split("/blobs/");
//                if (parts.length != 2) continue;
//
//                String containerPath = parts[0];
//                String objectPath = parts[1];
//                String bucketName = containerPath.substring(containerPath.lastIndexOf('/') + 1);
//
//                UploadEventNotificationRequest request = new UploadEventNotificationRequest();
//                request.setBucketName(bucketName);
//
//                ObjectInfo objectInfo = new ObjectInfo();
//                objectInfo.setKey(objectPath);
//                objectInfo.setFileName(objectPath.substring(objectPath.lastIndexOf('/') + 1));
//                objectInfo.setSize(((Number) data.getOrDefault("contentLength", 0L)).longValue());
//                objectInfo.setETag((String) data.getOrDefault("etag", null));
//
//                // Infer file format
//                String fileName = objectInfo.getFileName();
//                if (fileName != null && fileName.contains(".")) {
//                    objectInfo.setFormat(fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase());
//                } else {
//                    objectInfo.setFormat("UNKNOWN");
//                }
//                // 🔹 Print collected data in logs
//                log.info("📦 New Blob Event Received:");
//                log.info("   Bucket Name: {}", request.getBucketName());
//                log.info("   Object Key : {}", objectInfo.getKey());
//                log.info("   File Name  : {}", objectInfo.getFileName());
//                log.info("   Size       : {} bytes", objectInfo.getSize());
//                log.info("   ETag       : {}", objectInfo.getETag());
//                log.info("   Format     : {}", objectInfo.getFormat());
//                log.info("   Source IP  : {}", objectInfo.getSourceIP());
//                if (!objectInfo.getMetadata().isEmpty()) {
//                    log.info("   Metadata   : {}", objectInfo.getMetadata());
//                }
//
//                objectInfo.setSourceIP((String) data.getOrDefault("clientRequestId", null));
//
//                if (data.containsKey("metadata") && data.get("metadata") instanceof Map<?, ?> md) {
//                    md.forEach((k, v) -> objectInfo.getMetadata().put(String.valueOf(k), String.valueOf(v)));
//                }
//
//                request.setObjectInfo(objectInfo);
//
//
//            } catch (Exception e) {
//                log.error("❌ Failed to process Event Grid event: {}", event, e);
//            }
//        }
//        return Map.of(); // always return something to keep Event Grid happy
//    }
@PostMapping("/notify")
public Map<String, String> handleAzureEventGrid(@RequestBody List<Map<String, Object>> events) {
    for (Map<String, Object> event : events) {
        try {
            // Subscription handshake
            if ("Microsoft.EventGrid.SubscriptionValidationEvent".equals(event.get("eventType"))) {
                Map<String, Object> data = (Map<String, Object>) event.get("data");
                return Map.of("validationResponse", (String) data.get("validationCode"));
            }

            UploadEventNotificationRequest request = getUploadEventNotificationRequest(event);

            log.info("📦 Notifying upload event: {}", request);

            // 🔹 forward to downstream service (instead of only logging)
//            restClient.saveUploadNotification(request);

        } catch (Exception e) {
            log.error("❌ Failed to process event: {}", event, e);
        }
    }
    return Map.of();
}
    private UploadEventNotificationRequest getUploadEventNotificationRequest(Map<String, Object> event) {
        UploadEventNotificationRequest request = new UploadEventNotificationRequest();

        String eventType = (String) event.get("eventType");
        Map<String, Object> data = (Map<String, Object>) event.get("data");
        String subject = (String) event.get("subject");

        // Map Event Grid type -> internal type
        if ("Microsoft.Storage.BlobCreated".equals(eventType)) {
            request.setEventType(UploadEventType.OBJECT_CREATED);
            request.setNewlyUploaded(true);
        } else if ("Microsoft.Storage.BlobDeleted".equals(eventType)) {
            request.setEventType(UploadEventType.OBJECT_DELETED);
            request.setNewlyUploaded(false);
        }

        // Extract container & object key
        String[] parts = subject.split("/blobs/");
        String bucketName = parts[0].substring(parts[0].lastIndexOf('/') + 1);
        String objectPath = parts.length > 1 ? parts[1] : null;

        ObjectInfo objectInfo = new ObjectInfo();
        objectInfo.setKey(objectPath);
        objectInfo.setFileName(objectPath != null && objectPath.contains("/")
                ? objectPath.substring(objectPath.lastIndexOf("/") + 1)
                : objectPath);
        objectInfo.setSize(((Number) data.getOrDefault("contentLength", 0L)).longValue());
        objectInfo.setETag((String) data.getOrDefault("etag", null));
        objectInfo.setFormat(objectInfo.getFileName() != null && objectInfo.getFileName().contains(".")
                ? objectInfo.getFileName().substring(objectInfo.getFileName().lastIndexOf('.') + 1).toUpperCase()
                : "UNKNOWN");
        objectInfo.setSourceIP((String) data.getOrDefault("clientRequestId", null));

        request.setBucketName(bucketName);
        request.setBucketOwner(BucketOwner.TENANT); // or NDP
        request.setObjectInfo(objectInfo);

        // keep raw event
        request.setOriginalEventPayload(event.toString());

        return request;
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("App Controller is healthy");
    }
}
