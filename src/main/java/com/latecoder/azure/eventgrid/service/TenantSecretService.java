package com.latecoder.azure.eventgrid.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.latecoder.azure.eventgrid.model.TenantAzureSecrets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSecretService {

    private final ObjectMapper mapper;

    public TenantAzureSecrets resolveAzureTenantSecrets(String tenantId, String stackEnv, String stackName, String regionName) {
        String secretName = String.format("upath-%s/%s/azure/%s", stackEnv, stackName, tenantId);
        log.info("Fetching Azure secret for tenant {}: {}", tenantId, secretName);

        Region region = Region.of(regionName);

        try (SecretsManagerClient client = SecretsManagerClient.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            String secretJson = client.getSecretValue(
                    GetSecretValueRequest.builder().secretId(secretName).build()
            ).secretString();

            Map<String, Object> map = mapper.readValue(secretJson, Map.class);
            TenantAzureSecrets secrets = new TenantAzureSecrets(
                    (String) map.get("Acc_holder_tenantId"),
                    (String) map.get("subscriptionId"),
                    (String) map.get("resourceGroup"),
                    (String) map.get("storageAccount"),
                    (String) map.get("containerName"),
                    (String) map.get("clientId"),
                    (String) map.get("tenantId"),
                    (String) map.get("azureClientSecret")
            );

            log.info("✅ Loaded Azure secrets for tenant from secret manager {}", tenantId);
            return secrets;

        } catch (Exception e) {
            log.error("Failed to load Azure secrets for tenant {}", tenantId, e);
            throw new RuntimeException("Azure secrets fetch failed for tenant " + tenantId, e);
        }
    }
}
