package com.latecoder.azure.eventgrid.service;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class AzureTokenService {


    public String getAccessToken(String tenantId, String clientId, String clientSecret) {
        try {
            log.info("Acquiring Azure access token for tenantId={}, clientId={}, clientSecret={}", tenantId, clientId, clientSecret != null ? "****" : null);
            ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                    .tenantId(tenantId)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .build();

            AccessToken token = credential.getToken(
                            new TokenRequestContext()
                                    .addScopes("https://management.azure.com/.default"))
                    .block(Duration.ofSeconds(30));

            if (token != null) {
                log.info("Token acquired, expires at {}", token.getExpiresAt());
                return token.getToken();
            }
        } catch (Exception e) {
            log.error("Failed to acquire Azure access token", e);
        }
        return null;
    }
}
