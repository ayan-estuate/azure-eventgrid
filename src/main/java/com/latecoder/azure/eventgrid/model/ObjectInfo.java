package com.latecoder.azure.eventgrid.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/***
 * information about the uploaded object
 */
@Data
public class ObjectInfo {
    private String key;
    private long size;
    private String eTag;
    private String fileName;
    private String format;
    private String sourceIP;
    private Map<String,String> metadata = new HashMap<>();
}
