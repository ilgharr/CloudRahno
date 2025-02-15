package org.ilghar.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

import org.ilghar.Secrets;
import org.ilghar.handler.MemcachedHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.*;
import java.util.*;

@RestController
public class LoginController {

    @Autowired
    public MemcachedHandler memcached;

    @GetMapping("/login")
    public ResponseEntity<Void> login(@CookieValue(value = "refresh_token", required = false) String refresh_token
    ) throws IOException {
        // user is redirected to AWS Cognito Login/Signup page
        // responds with AWS login endpoint, client id, redirect uri and scope

        if(refresh_token != null){
            HttpHeaders headers = new HttpHeaders();
            headers.set("Location", "/home");
            System.out.println("User attempted access to /login while already logged in");
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }

        String cognitoUrl = Secrets.AUTHORIZATION_ENDPOINT + "?" +
                "client_id=" + Secrets.CLIENT_ID + "&" +
                "response_type=code&" +
                "redirect_uri=" + Secrets.REDIRECT_URI + "&" +
                "scope=" + Secrets.SCOPES;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Location", cognitoUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/api/callback")
    public ResponseEntity<String> callback(@RequestParam(name = "code", required = false) String code,
                                                        @RequestParam(name = "error", required = false) String error) {
        // AWS Cognito communicates with this endpoint
        // Code is received after successful login
        // code and secrets are exchanged for user token
        // responds frontend with the token
        if (code == null || error != null) {
            return ResponseEntity.badRequest().build();
        }

        String user_id = "";
        String id_token = "";
        String refresh_token = "";

        try {
            Map<String, String> tokenResponse = exchangeCodeForTokens(code);
            if (tokenResponse == null) {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Exchanging code for token failed.");
            }

            user_id = extractUserId(tokenResponse);
            id_token = extractIdToken(tokenResponse);
            refresh_token = extractRefreshToken(tokenResponse);

            if(user_id != null && id_token != null){
                memcached.memcachedAddData(user_id, id_token, 300);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, generateHttpOnlyCookie("user_id", user_id,  428400));
            headers.add(HttpHeaders.SET_COOKIE, generateHttpOnlyCookie("refresh_token", refresh_token,  428400));

            return ResponseEntity.ok().headers(headers).build();

        } catch (Exception e) {
            throw new RuntimeException("Error while processing user login /api/callback.", e);
        }
    }

    // if a refresh_token exists in the cookie that means user is logged in
    @GetMapping("/check-session")
    public ResponseEntity<Map<String, String>> checkSession(@CookieValue(value = "refresh_token", required = false) String refresh_token){
        return ResponseEntity.ok(Map.of("isLoggedIn", refresh_token != null ? "true" : "false"));
    }

    // the response does not contain a body
    // indicated in <void> return type
    // this clears the cookie in the header by setting the expiration to 0
    @GetMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = "refresh_token", required = false) String refresh_token,
                                       @CookieValue(value = "user_id", required = false) String user_id) {

        if(refresh_token == null){
            HttpHeaders headers = new HttpHeaders();
            headers.set("Location", "/");
            System.out.println("User attempted access to /logout while already logged out");
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }

        System.out.println("User logged out successfully.");
        memcached.memcachedDelete(user_id);

        // responds with AWS Cognito logout endpoint, client id and redirect uri
        String cognitoLogoutUrl = String.format(
                "%s?client_id=%s&logout_uri=%s",
                Secrets.LOGOUT_ENDPOINT,
                Secrets.CLIENT_ID,
                Secrets.DOMAIN
        );

        System.out.println("Logging out from Cognito with URL: " + cognitoLogoutUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, generateHttpOnlyCookie("user_id", "",  0));
        headers.add(HttpHeaders.SET_COOKIE, generateHttpOnlyCookie("refresh_token", "",  0));
        headers.setLocation(URI.create(cognitoLogoutUrl));
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

    // code from successful user login is exchanged for AWS Cognito token
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

    // MUST ADD SECURE FIELD WHEN DEPLOYING
    // THIS ONLY USES HTTP AND IS NOT SECURE
    private String generateHttpOnlyCookie(String key, String value, int maxAgeInSeconds) {
        return String.format("%s=%s; HttpOnly; Path=/; Max-Age=%d; SameSite=Strict",
                key, value, maxAgeInSeconds);
    }

    // extract user id (SUB) from the token recieved by aws after exchanging one time use code
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

    // extract id token from the token recieved by aws after exchanging one time use code
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

    // extract refresh token from the token recieved by aws after exchanging one time use code
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
}