package com.latecoder.azure.eventgrid.util;
// Utility class to mask secrets in logs
public final class SecretMasker {
    private SecretMasker() {}
    public static String tail(String value, int n) {
        if (value == null) return "null";
        return value.length() <= n ? "*".repeat(Math.max(0, value.length())) : "****" + value.substring(value.length() - n);
    }
}

