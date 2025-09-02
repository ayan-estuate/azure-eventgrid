package com.latecoder.azure.eventgrid.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/***
 * This class represents the notification that is sent to the event bus when an object is uploaded to the storage
 * bucket.
 * The notification contains the event name, event type, bucket name, object info and the original event payload.
 *
 */
@Data
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadEventNotificationRequest {

    @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "Etc/UTC")
    public Date sentAt = new Date();
    private String tenantId;
    private UploadEventType eventType;
    private String bucketName;
    // will be set by omnistore svc
    private BucketOwner bucketOwner;
    private ObjectInfo objectInfo;
    private String originalEventPayload;
    private boolean newlyUploaded;

}
