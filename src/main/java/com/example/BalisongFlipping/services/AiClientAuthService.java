package com.example.BalisongFlipping.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AiClientAuthService {

    private final Map<String, String> clientKeys;

    public AiClientAuthService(
            @Value("${ai.clients.website.key}") String websiteKey,
            @Value("${ai.clients.discord.key}") String discordKey
    ) {
        this.clientKeys = Map.of(
                "website", websiteKey,
                "discord", discordKey
        );
    }

    public boolean isValid(String clientId, String clientKey) {
        if (clientId == null || clientKey == null) return false;
        String expected = clientKeys.get(clientId);
        return expected != null && expected.equals(clientKey);
    }
}
