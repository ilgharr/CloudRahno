package org.ilghar.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Map;

import org.ilghar.Secrets;
import org.ilghar.controller.DownloadTracker;

@RestController
public class LoginController {

    @GetMapping("/login")
    public ResponseEntity<Void> login(@CookieValue(value = "refresh_token", required = false) String refresh_token) throws IOException {

        // user is redirected to AWS Cognito Login/Signup page
        // responds with AWS login endpoint, client id, redirect uri and scope

        //AWS will handle users that are already logged in

        String cognito_url = Secrets.AUTHORIZATION_ENDPOINT + "?" +
                "client_id=" + Secrets.CLIENT_ID + "&" +
                "response_type=code&" +
                "redirect_uri=" + Secrets.REDIRECT_URI + "&" +
                "scope=" + Secrets.SCOPES;

        return redirectTo(cognito_url);
    }

    @GetMapping("/api/callback")
    public ResponseEntity<String> callback(@RequestParam(name = "code", required = false) String code,
                                           @RequestParam(name = "error", required = false) String error) {
        // AWS Cognito communicates with this endpoint
        // Code is received after successful login
        // code and secrets are exchanged for user token
        // responds frontend with the refresh token
        if (code == null || error != null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Map<String, String> token_response = exchangeCodeForTokens(code);
            if (token_response == null) {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Exchanging code for token failed.");
            }

            String refresh_token = extractRefreshToken(token_response);

//            String user_id = Utility.extractUserId(Utility.getIdToken(refresh_token));
//            Integer n = getNumberOfFiles(user_id);
//            DownloadTracker.addUser(user_id, n);
//            System.out.println("User has " + n + " objects in storage.");

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, generateHttpOnlyCookie("refresh_token", refresh_token,  428400));
            return ResponseEntity.ok().headers(headers).build();

        } catch (Exception e) {
            throw new RuntimeException("Error while processing user login /api/callback.", e);
        }
    }

    // this clears the cookie in the header by setting the expiration to 0
    @GetMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = "refresh_token", required = false) String refresh_token) {

        if(Utility.validateUserSession(refresh_token) != null){
            System.out.println("User attempted access to /logout while already logged out");
            return redirectTo("/");
        }

//        DownloadTracker.removeUser(Utility.extractUserId(Utility.getIdToken(refresh_token)));

        String cognito_logout_url = String.format(
                "%s?client_id=%s&logout_uri=%s",
                Secrets.LOGOUT_ENDPOINT,
                Secrets.CLIENT_ID,
                Secrets.DOMAIN
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, generateHttpOnlyCookie("refresh_token", "",  0));
        headers.setLocation(URI.create(cognito_logout_url));
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

    private static String generateHttpOnlyCookie(String key, String value, int expiration) {
        return String.format("%s=%s; HttpOnly; Path=/; Max-Age=%d; SameSite=Strict",
                key, value, expiration);
    }

    private static ResponseEntity<Void> redirectTo(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Location", url);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    // extract refresh token from the token recieved by aws after exchanging one time use code
    private static String extractRefreshToken(Map<String, String> token_response) {
        try {
            String refresh_token = token_response.get("refresh_token");
            if (refresh_token == null || refresh_token.isEmpty()) {
                throw new IllegalArgumentException("refresh_token is missing or empty");
            }
            return refresh_token;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // make a request to S3Controller at /get-obj-count to get the number of objects the user has
    public Integer getNumberOfFiles(String user_id){
        RestTemplate rest_template = new RestTemplate();
        String endpoint = "http://localhost:8443/max-count?user_id={user_id}";
        String response = "";
        try{
            response = rest_template.getForObject(endpoint, String.class, user_id);
            System.out.println("Response from the server: " + response);
        }catch(Exception e){
            System.err.println("Error occurred while making the GET request: " + e.getMessage());
            return -1;
        }
        return Integer.parseInt(response);
    }

    // code from successful user login is exchanged for AWS Cognito token
    private static Map<String, String> exchangeCodeForTokens(String code) {
        RestTemplate rest_template = new RestTemplate();

        MultiValueMap<String, String> request_body = new LinkedMultiValueMap<>();
        request_body.add("grant_type", "authorization_code");
        request_body.add("client_id", Secrets.CLIENT_ID);
        request_body.add("client_secret", Secrets.CLIENT_SECRET);
        request_body.add("redirect_uri", Secrets.REDIRECT_URI);
        request_body.add("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<String> response = rest_template.postForEntity(
                    Secrets.TOKEN_ENDPOINT, new HttpEntity<>(request_body, headers), String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return new ObjectMapper().readValue(response.getBody(), new TypeReference<>() {});
            } else {
                throw new HttpClientErrorException(response.getStatusCode(), "Token exchange failed.");            }
        } catch (Exception e) {
            throw new RuntimeException("Error while exchanging code for tokens", e);
        }
    }
}
