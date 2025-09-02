package com.latecoder.azure.eventgrid.model;

/***
 * Enum to represent the upload event type
 * Currently supported event types are OBJECT_CREATED, OBJECT_DELETED , OBJECT_COPIED and OBJECT_RESTORED
 */
public enum UploadEventType {
    OBJECT_CREATED, OBJECT_DELETED, OBJECT_RESTORED, OBJECT_COPIED;
}