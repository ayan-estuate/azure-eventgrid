package com.latecoder.azure.eventgrid.model;

public record TenantAzureSecrets(
        String accHolderTenantId,
        String subscriptionId,
        String resourceGroup,
        String storageAccount,
        String containerName,
        String clientId,
        String tenantId,
        String azureClientSecret
) {}
