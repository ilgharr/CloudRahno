package org.ilghar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ilghar.Secrets;
import org.ilghar.handler.MemcachedHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class S3Controller {

    @Autowired
    public MemcachedHandler memcached;
    private static final String UPLOAD_DIR = "./uploads";
    final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body("No files were uploaded.");
        }

        StringBuilder resultMessage = new StringBuilder();

        try {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            for (MultipartFile file : files) {
                if (!file.isEmpty() && file.getSize() <= MAX_FILE_SIZE) {
                    Path filePath = Paths.get(UPLOAD_DIR, file.getOriginalFilename());
                    Files.copy(file.getInputStream(), filePath);
                    resultMessage.append("File uploaded successfully: ").append(file.getOriginalFilename()).append("\n");
                } else {
                    resultMessage.append("Skipped empty file: ").append(file.getOriginalFilename()).append("\n");
                }
            }

            return ResponseEntity.ok("File(s) uploaded successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading files.");
        }
    }

    public String fetchRefreshToken(){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:8443/fetch-token", String.class);
        return response.getBody();
    }

}
