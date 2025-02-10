package org.ilghar.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ilghar.Secrets;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class LoginHelper {

    public static Map<String, String> exchangeCodeForTokens(String code) {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("client_id", Secrets.CLIENT_ID);
        requestBody.add("client_secret", Secrets.CLIENT_SECRET);
        requestBody.add("redirect_uri", Secrets.REDIRECT_URI);
        requestBody.add("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    Secrets.TOKEN_ENDPOINT, new HttpEntity<>(requestBody, headers), String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return new ObjectMapper().readValue(response.getBody(), new TypeReference<>() {});
            } else {
                throw new HttpClientErrorException(response.getStatusCode(), "Token exchange failed.");            }
        } catch (Exception e) {
            throw new RuntimeException("Error while exchanging code for tokens", e);
        }
    }

    public static String extractUserId(Map<String, String> tokenResponse) throws JsonProcessingException {
        try{
            String id_token = extractIdToken(tokenResponse);

            String[] tokenParts = id_token.split("\\.");
            if (tokenParts.length != 3) {
                throw new IllegalArgumentException("Invalid id_token format.");
            }

            // Base64.getDecoder(): returns a Base64.Decoder instance
            // decode(): decodes the base 64 encoded String, returns as byte[]
            // new String(): converts bytes to String
            String payload = new String(Base64.getDecoder().decode(tokenParts[1]));

            int sub_start = payload.indexOf("\"sub\":\"") + 7;
            int sub_end = payload.indexOf("\"", sub_start);

            // checks if "sub" field is missing
            if (sub_start < 7 || sub_end == -1) {
                throw new IllegalArgumentException("sub claim not found in id_token");
            }

            return payload.substring(sub_start, sub_end);
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static String extractIdToken(Map<String, String> tokenResponse) {
        try {
            String idToken = tokenResponse.get("id_token");
            if (idToken == null || idToken.isEmpty()) {
                throw new IllegalArgumentException("id_token is missing or empty");
            }
            return idToken;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String extractRefreshToken(Map<String, String> tokenResponse) {
        try {
            String refreshToken = tokenResponse.get("refresh_token");
            if (refreshToken == null || refreshToken.isEmpty()) {
                throw new IllegalArgumentException("refresh_token is missing or empty");
            }
            return refreshToken;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    // ---------------------------UNUSED------------------------------------

    public static String extractAccessToken(Map<String, String> tokenResponse) {
        if (tokenResponse == null || tokenResponse.isEmpty()) {
            throw new IllegalArgumentException("tokenResponse map is empty or null");
        }

        String accessToken = tokenResponse.get("access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("access_token is missing or empty");
        }

        return accessToken;
    }


    public static void parseAndPrintFirstJwt(Map<String, String> tokenResponse) {
        try {
            // Look for the first valid JWT token in the map
            String jwt = null;
            for (String value : tokenResponse.values()) {
                if (isJwt(value)) {
                    jwt = value;
                    break;
                }
            }

            if (jwt == null) {
                throw new IllegalArgumentException("No valid JWT found in the token response.");
            }

            // Split the JWT into its parts
            String[] parts = jwt.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format.");
            }

            // Decode the JWT parts
            String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String signature = parts[2];

            // Print the JWT contents
            System.out.println("----- HEADER -----");
            prettyPrintJson(header);

            System.out.println("\n----- PAYLOAD -----");
            prettyPrintJson(payload);

            System.out.println("\n----- SIGNATURE -----");
            System.out.println(signature);

        } catch (Exception e) {
            System.err.println("Error decoding JWT: " + e.getMessage());
        }
    }

    private static boolean isJwt(String token) {
        return token != null && token.split("\\.").length == 3;
    }

    private static void prettyPrintJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Object jsonObject = objectMapper.readValue(json, Object.class);
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
            System.out.println(prettyJson);
        } catch (Exception e) {
            System.out.println("Raw JSON: " + json); // Fallback for malformed JSON
        }
    }

}

