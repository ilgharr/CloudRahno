package org.ilghar.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ilghar.Secrets;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@RestController
public class Utility {

    public static ResponseEntity<?> validateUserSession(String refresh_token) {

        // If the refresh token is null, redirect to the home page
        if (refresh_token == null) {
            System.out.println("User attempted access while logged out");
            HttpHeaders headers = new HttpHeaders();
            headers.set("Location", "/"); // Use the string directly
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }

        String user_id = Utility.extractUserId(Utility.getIdToken(refresh_token));

        // if user_id is null, it means the refresh token was not validated by aws
        // redirect to /logout to expire the cookie that stores the refresh token
        if (user_id == null) {
            System.out.println("Invalid or expired refresh token. Redirecting to /logout.");
            HttpHeaders headers = new HttpHeaders();
            headers.set("Location", "/logout"); // Use the string directly
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }

        // on successful validation, null is returned
        return null;
    }

    // if a refresh_token exists in the cookie that means user is logged in
    @GetMapping("/check-session")
    public ResponseEntity<Map<String, String>> checkSession(@CookieValue(value = "refresh_token", required = false) String refresh_token){
        ResponseEntity<?> sessionResponse = validateUserSession(refresh_token);
        if (sessionResponse != null) {
            return ResponseEntity.ok(Map.of("isLoggedIn", "false"));
        }
        return ResponseEntity.ok(Map.of("isLoggedIn", "true"));
    }

    // if aws does not validate the refresh token, null is returned
    public static Map<String, String> getIdToken(String refresh_token) {
        System.out.println("Requesting ID token with refresh token...");
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "refresh_token");
        requestBody.add("client_id", Secrets.CLIENT_ID);
        requestBody.add("client_secret", Secrets.CLIENT_SECRET);
        requestBody.add("refresh_token", refresh_token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    Secrets.TOKEN_ENDPOINT, new HttpEntity<>(requestBody, headers), String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                System.out.println("ID token successfully fetched.");
                return new ObjectMapper().readValue(response.getBody(), new TypeReference<Map<String, String>>() {});
            } else {
                System.out.println("AWS token exchange failed with status: " + response.getStatusCode());
                return null;
            }
        } catch (HttpClientErrorException e) {
            System.out.println("HTTP Client Error: " + e.getStatusCode());
            System.out.println("Error Body: " + e.getResponseBodyAsString());
            throw new RuntimeException("HTTP error occurred: " + e.getMessage(), e); // Rethrow for appropriate handling
        } catch (Exception e) {
            throw new RuntimeException("Error while exchanging code for tokens", e);
        }
    }

    // if given token is null, it means aws did not validate te token and so null is returned again
    public static String extractUserId(Map<String, String> token) {
        try {

            // because the getIdToken returns null if the refresh token is not validated by aws, we need to return null here also
            if (token == null) {
                return null;
            }

            if (!token.containsKey("id_token")) {
                throw new IllegalArgumentException("id_token is missing.");
            }

            String id_token = token.get("id_token");

            String[] token_parts = id_token.split("\\.");
            if (token_parts.length != 3) {
                throw new IllegalArgumentException("Invalid id_token format.");
            }

            String payload = new String(Base64.getDecoder().decode(token_parts[1]));

            int sub_start = payload.indexOf("\"sub\":\"") + 7;
            int sub_end = payload.indexOf("\"", sub_start);

            if (sub_start < 7 || sub_end == -1) {
                throw new IllegalArgumentException("sub claim not found in id_token");
            }

            return payload.substring(sub_start, sub_end);
        } catch (Exception e) {
            System.err.println("Error parsing id_token: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
