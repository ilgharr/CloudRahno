package org.ilghar.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Controller
public class ReactController {

    @GetMapping("/{path:[^\\.]*}")
    public String serveReactApp() {
        return "forward:/index.html";
    }

//    private static final List<String> ALLOWED_ENDPOINTS = Arrays.asList(
//            "/",
//            "/login",
//            "/logout",
//            "/api/callback",
//            "/home",
//            "/max-count",
//            "/upload",
//            "/check-session"
//    );
//
//    @RequestMapping(value = "/**")
//    public ResponseEntity<Void> handleFallback(HttpServletRequest request) {
//        String requestedUri = request.getRequestURI();
//
//        // Check if the requested URI is in the list of allowed endpoints
//        if (!isAllowedEndpoint(requestedUri)) {
//            System.out.println("Redirecting unused endpoint: " + requestedUri + " to /home");
//
//            // Redirect to /home for unused endpoints
//            HttpHeaders headers = new HttpHeaders();
//            headers.setLocation(URI.create("/home"));
//            return new ResponseEntity<>(headers, HttpStatus.FOUND);
//        }
//
//        // If the endpoint is in the list, return a 404 (if no handler exists)
//        return ResponseEntity.notFound().build();
//    }
//
//    // Utility method to check if the endpoint is allowed
//    private boolean isAllowedEndpoint(String uri) {
//        return ALLOWED_ENDPOINTS.contains(uri);
//    }
}
