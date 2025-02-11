package org.ilghar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ilghar.Secrets;
import org.ilghar.handler.MemcachedHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
public class S3Controller {

    @Autowired
    public MemcachedHandler memcached;

    @PostMapping("/fetch-token")
    public ResponseEntity<String> fetchRefreshToken(@CookieValue(name = "refresh_token", required = false)String refresh_token) {
        if (refresh_token == null || refresh_token.isEmpty()) {
            return ResponseEntity.badRequest().body("Refresh token is missing");
        }
        return ResponseEntity.ok("Refresh token received successfully!");
    }

//    @PostMapping("/getid")
//    public ResponseEntity<String> getID(@RequestBody Map<String, String> payload) throws Exception {
//        String user_id = payload.get("userId");
//        if (user_id == null || user_id.isEmpty()) {
//            return ResponseEntity.badRequest().body("User ID is missing");
//        }
//
//        String id_token = memcached.memcachedGetData(user_id);
//        System.out.println("ID Token: " + id_token);
//
//        String identityResponse = fetchIdentityId(id_token);
//        System.out.println("ID Token: " + identityResponse);
//        if (identityResponse == null || identityResponse.isEmpty()) {
//            System.err.println("Failed to fetch Identity ID.");
//        }
//        return ResponseEntity.ok("User ID received successfully");
//    }

//    private String fetchIdentityId(String id_token) throws Exception {
//
//        String requestBody = String.format(
//                "{\"IdentityPoolId\": \"%s\", \"Logins\": {\"cognito-idp.ca-central-1.amazonaws.com/%s\": \"%s\"}}",
//                Secrets.IDENTITY_POOL_ID,
//                Secrets.USER_POOL_ID,
//                id_token
//        );
//
//        HttpClient client = HttpClient.newHttpClient();
//
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(Secrets.COGNITO_IDENTITY_ENDPOINT))
//                .header("Content-Type", "application/x-amz-json-1.1")
//                .header("X-Amz-Target", "AWSCognitoIdentityService.GetId")
//                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
//                .build();
//
//        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//        if (response.statusCode() == 200) {
//            ObjectMapper objectMapper = new ObjectMapper();
//            Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
//            return (String) responseMap.get("IdentityId");
//        } else {
//            throw new RuntimeException("Failed to retrieve identity ID: " + response.body());
//        }
//    }

}
