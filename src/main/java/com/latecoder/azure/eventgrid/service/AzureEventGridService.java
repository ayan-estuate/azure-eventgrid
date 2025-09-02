package com.latecoder.azure.eventgrid.service;

import com.latecoder.azure.eventgrid.model.TenantAzureSecrets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static com.latecoder.azure.eventgrid.util.SecretMasker.tail;

@Slf4j
@Service
@RequiredArgsConstructor
public class AzureEventGridService {
    @Value("${stack.env}")
    private String stackEnv;
    @Value("${stack.name}")
    private String stackName;
    @Value("${azure.aws.secrets.region}")
    private String regionName;
    private final AzureTokenService tokenService;
    private final TenantSecretService tenantSecretService;
    public String createOrUpdateSubscription(String accHolderTenantId, String webhookEndpoint) {
        TenantAzureSecrets tenantAzureSecrets = tenantSecretService.resolveAzureTenantSecrets(accHolderTenantId, stackEnv, stackName, regionName);
        String response = null;
        try {
            String token = tokenService.getAccessToken(tenantAzureSecrets.tenantId(), tenantAzureSecrets.clientId(), tenantAzureSecrets.azureClientSecret());
            if(!token.isEmpty()){
                log.info("fetching Token: {}", tail(token,6));
            }

            String eventSubName = String.format("demo-%s-%s", tenantAzureSecrets.storageAccount(), tenantAzureSecrets.containerName());
            String url = String.format(
                    "https://management.azure.com/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Storage/storageAccounts/%s/providers/Microsoft.EventGrid/eventSubscriptions/%s?api-version=2022-06-15",
                    tenantAzureSecrets.subscriptionId(), tenantAzureSecrets.resourceGroup(), tenantAzureSecrets.storageAccount(), eventSubName);

            Map<String, Object> body = Map.of(
                    "properties", Map.of(
                            "destination", Map.of(
                                    "endpointType", "WebHook",
                                    "properties", Map.of("endpointUrl", webhookEndpoint)
                            ),
                            "filter", Map.of(
                                    "subjectBeginsWith", "/blobServices/default/containers/" + tenantAzureSecrets.containerName() + "/blobs/",
                                    "includedEventTypes", List.of("Microsoft.Storage.BlobCreated", "Microsoft.Storage.BlobDeleted")
                            ),
                            "eventDeliverySchema", "EventGridSchema"
                    )
            );
            log.info("body: {}", body);
            response = WebClient.create()
                    .put()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Create/Update Event Grid Response: {}", response);

        } catch (Exception e) {
            log.error("Failed to create/update Event Grid subscription", e);
        }
        return response;
    }

    public String deleteSubscription(String accHolderTenantId) {
        TenantAzureSecrets tenantAzureSecrets = tenantSecretService.resolveAzureTenantSecrets(accHolderTenantId, stackEnv, stackName, regionName);
        String response = null;
        try {
            String token = tokenService.getAccessToken(tenantAzureSecrets.tenantId(), tenantAzureSecrets.clientId(), tenantAzureSecrets.azureClientSecret());
            log.info("fetching Token: {}", tail(token,6));
            String eventSubName = String.format("demo-%s-%s", tenantAzureSecrets.storageAccount(), tenantAzureSecrets.containerName());
            String url = String.format(
                    "https://management.azure.com/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Storage/storageAccounts/%s/providers/Microsoft.EventGrid/eventSubscriptions/%s?api-version=2022-06-15",
                    tenantAzureSecrets.subscriptionId(), tenantAzureSecrets.resourceGroup(), tenantAzureSecrets.storageAccount(), eventSubName);
            log.info("url: {}", url);
            response = WebClient.create()
                    .delete()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Delete Event Grid Response: {}", response);

        } catch (Exception e) {
            log.error("Failed to delete Event Grid subscription", e);
        }
        return response;
    }
}
