package org.ilghar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ilghar.Secrets;
import org.ilghar.handler.MemcachedHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

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

//    @PostMapping("/fetch-token")
//    public ResponseEntity<String> fetchRefreshToken(@CookieValue(name = "refresh_token", required = false)String refresh_token) {
//        if (refresh_token == null || refresh_token.isEmpty()) {
//            return ResponseEntity.badRequest().body("Refresh token is missing");
//        }
//        System.out.println("Refresh token received successfully in backend!!!: " + refresh_token);
//        return ResponseEntity.ok("Refresh token received successfully!");
//    }

    public String fetchRefreshToken(){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:8443/fetch-token", String.class);
        return response.getBody();
    }

}
