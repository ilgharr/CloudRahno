package org.ilghar.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

import com.fasterxml.jackson.databind.util.JSONPObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.*;

import org.ilghar.Secrets;

import org.ilghar.handler.MemcachedHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.*;
import java.util.*;

import static org.ilghar.controller.LoginHelper.*;


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

    // MUST ADD SECURE FIELD WHEN DEPLOYING
    // THIS ONLY USES HTTP AND IS NOT SECURE
    private String generateHttpOnlyCookie(String key, String value, int maxAgeInSeconds) {
        return String.format("%s=%s; HttpOnly; Path=/; Max-Age=%d; SameSite=Strict",
                key, value, maxAgeInSeconds);
    }

    @GetMapping("/check-session")
    public ResponseEntity<Map<String, String>> checkSession(@CookieValue(value = "refresh_token", required = false) String refresh_token){
        return ResponseEntity.ok(Map.of("isLoggedIn", refresh_token != null ? "true" : "false"));
    }

    // the response does not contain a body
    // indicated in <void> return type
    @GetMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = "refresh_token", required = false) String refresh_token) {

        if(refresh_token == null){
            HttpHeaders headers = new HttpHeaders();
            headers.set("Location", "/");
            System.out.println("User attempted access to /logout while already logged out");
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }

        // responds with AWS Cognito logout endpoint, client id and redirect uri
        String cognitoLogoutUrl = String.format(
                "%s?client_id=%s&logout_uri=%s",
                Secrets.LOGOUT_ENDPOINT,
                Secrets.CLIENT_ID,
                Secrets.LOGOUT_URI
        );

        System.out.println("Logging out from Cognito with URL: " + cognitoLogoutUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, generateHttpOnlyCookie("user_id", "",  0));
        headers.add(HttpHeaders.SET_COOKIE, generateHttpOnlyCookie("refresh_token", "",  0));
        headers.setLocation(URI.create(cognitoLogoutUrl));
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }
}