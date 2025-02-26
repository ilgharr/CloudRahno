package org.ilghar.controller;

import org.ilghar.Secrets;
import org.ilghar.handler.MemcachedHandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.ilghar.controller.LoginController.extractIdToken;

@RestController
public class S3Controller {

    @Autowired
    public MemcachedHandler memcached;

    final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") List<MultipartFile> files,
                                                   @CookieValue(value = "refresh_token", required = false) String refresh_token,
                                                   @CookieValue(value = "user_id", required = false) String user_id) {

        // get id_token from cache or refresh token
        String id_token = memcached.memcachedGetData(user_id);
        if (id_token == null) {
            System.out.println("ID token not found in cache. Attempting to refresh using refresh token.");
            try {
                id_token = extractIdToken(getIdToken(refresh_token));
                memcached.memcachedAddData(user_id, id_token, 295);
                System.out.println("ID token successfully retrieved and cached.");
            } catch (Exception e) {
                System.err.println("Failed to retrieve or cache ID token: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Failed to retrieve ID token.");
            }
        }

        if (files == null || files.isEmpty()) {
            System.out.println("No files were uploaded.");
            return ResponseEntity.badRequest().body("No files were uploaded.");
        } else {
            System.out.println(files.size() + " files received for upload.");
        }

        StringBuilder result_message = new StringBuilder();

        try {
            for (MultipartFile file : files) {
                if (!file.isEmpty() && file.getSize() <= MAX_FILE_SIZE) {

                    String file_name = file.getOriginalFilename();
                    System.out.println("Processing file: " + file_name);

                    Map<String, String> presigned_data = generatePresignedUploadUrl(user_id, file_name);
                    String presigned_url = presigned_data.get("presignedUrl");
                    System.out.println("Pre-signed URL generated: " + presigned_url);

                    boolean upload_success = sendFileToS3(file, presigned_url);

                    if (upload_success) {
                        System.out.println("File uploaded successfully to S3: " + file_name);
                        result_message.append("File uploaded successfully to S3: ").append(file_name).append("\n");
                    } else {
                        System.out.println("Failed to upload file to S3: " + file_name);
                        result_message.append("Failed to upload file to S3: ").append(file_name).append("\n");
                    }
                } else {
                    System.out.println("Skipped empty or oversized file: " + file.getOriginalFilename());
                    result_message.append("Skipped empty or oversized file: ").append(file.getOriginalFilename()).append("\n");
                }
            }
            System.out.println("File upload process completed.");
            return ResponseEntity.ok(result_message.toString());

        } catch (Exception e) {
            System.err.println("An error occurred during file upload: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file upload.");
        }
    }

    public static Map<String, String> getIdToken(String refresh_token) {
        System.out.println("Requesting ID token with refresh token...");
        RestTemplate rest_template = new RestTemplate();

        MultiValueMap<String, String> request_body = new LinkedMultiValueMap<>();
        request_body.add("grant_type", "refresh_token");
        request_body.add("client_id", Secrets.CLIENT_ID);
        request_body.add("client_secret", Secrets.CLIENT_SECRET);
        request_body.add("refresh_token", refresh_token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<String> response = rest_template.postForEntity(
                    Secrets.TOKEN_ENDPOINT, new HttpEntity<>(request_body, headers), String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                System.out.println("ID token successfully fetched.");
                return new ObjectMapper().readValue(response.getBody(), new TypeReference<>() {});
            } else {
                throw new HttpClientErrorException(response.getStatusCode(), "Token exchange failed.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while exchanging code for tokens", e);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------------------------------------
    //------------------------- AI GENERATED, TO BE STUDIED ----------------------------------------------------------------------
    private boolean sendFileToS3(MultipartFile file, String presignedUrl) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(30))  // Increased timeout
                    .build();
            System.out.println("Sending file to S3...");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(presignedUrl))
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .header("Content-Type", "application/octet-stream")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response from S3: " + response.body() + ", Status Code: " + response.statusCode());

            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Error while uploading file to S3: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Map<String, String> generatePresignedDownloadUrl(String userId, String filename) {
        try {
            System.out.println("Generating pre-signed URL for file: " + filename);

            // Configuration and credentials
            final String bucketName = Secrets.BUCKET_NAME;
            final String region = Secrets.BUCKET_REGION;
            final String service = "s3";
            final String accessKey = Secrets.ACCESS_KEY;
            final String secretKey = Secrets.SECRET_KEY;

            String objectKey = userId + "/" + filename;

            // AWS algorithm and date formatting
            String algorithm = "AWS4-HMAC-SHA256";
            String amzDate = getAmzDate();
            String dateStamp = getDateStamp();
            System.out.println("amzDate (request date in UTC): " + amzDate);

            String credentialScope = dateStamp + "/" + region + "/" + service + "/aws4_request";

            String canonicalUri = "/" + objectKey;
            String canonicalQueryString = buildCanonicalQueryString(algorithm, accessKey, credentialScope, amzDate);

            String canonicalHeaders = "host:" + bucketName + ".s3." + region + ".amazonaws.com\n";
            String signedHeaders = "host";
            String payloadHash = "UNSIGNED-PAYLOAD";

            String canonicalRequest = buildCanonicalRequest(
                    "PUT",
                    canonicalUri,
                    canonicalQueryString,
                    canonicalHeaders,
                    signedHeaders,
                    payloadHash
            );

            // String to Sign
            String stringToSign = buildStringToSign(algorithm, amzDate, credentialScope, canonicalRequest);

            // Signature
            byte[] signingKey = getSignatureKey(secretKey, dateStamp, region, service);
            String signature = hmacSHA256Hex(stringToSign, signingKey);

            // Generate presigned URL
            String presignedUrl = "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + objectKey +
                    "?" + canonicalQueryString + "&X-Amz-Signature=" + signature;

            // Response map
            Map<String, String> response = new HashMap<>();
            response.put("presignedUrl", presignedUrl);
            response.put("key", objectKey);

            System.out.println("Generated pre-signed URL: " + presignedUrl);
            return response;

        } catch (Exception e) {
            throw new RuntimeException("Error generating pre-signed URL", e);
        }
    }

    public Map<String, String> generatePresignedUploadUrl(String userId, String filename) {
        try {
            System.out.println("Generating pre-signed URL for file: " + filename);

            // Configuration and credentials
            final String bucketName = Secrets.BUCKET_NAME;
            final String region = Secrets.BUCKET_REGION;
            final String service = "s3";
            final String accessKey = Secrets.ACCESS_KEY;
            final String secretKey = Secrets.SECRET_KEY;

            String objectKey = userId + "/" + filename;

            // AWS algorithm and date formatting
            String algorithm = "AWS4-HMAC-SHA256";
            String amzDate = getAmzDate();
            String dateStamp = getDateStamp();
            System.out.println("amzDate (request date in UTC): " + amzDate);

            String credentialScope = dateStamp + "/" + region + "/" + service + "/aws4_request";

            String canonicalUri = "/" + objectKey;
            String canonicalQueryString = buildCanonicalQueryString(algorithm, accessKey, credentialScope, amzDate);

            String canonicalHeaders = "host:" + bucketName + ".s3." + region + ".amazonaws.com\n";
            String signedHeaders = "host";
            String payloadHash = "UNSIGNED-PAYLOAD";

            String canonicalRequest = buildCanonicalRequest(
                    "PUT",
                    canonicalUri,
                    canonicalQueryString,
                    canonicalHeaders,
                    signedHeaders,
                    payloadHash
            );

            // String to Sign
            String stringToSign = buildStringToSign(algorithm, amzDate, credentialScope, canonicalRequest);

            // Signature
            byte[] signingKey = getSignatureKey(secretKey, dateStamp, region, service);
            String signature = hmacSHA256Hex(stringToSign, signingKey);

            // Generate presigned URL
            String presignedUrl = "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + objectKey +
                    "?" + canonicalQueryString + "&X-Amz-Signature=" + signature;

            // Response map
            Map<String, String> response = new HashMap<>();
            response.put("presignedUrl", presignedUrl);
            response.put("key", objectKey);

            System.out.println("Generated pre-signed URL: " + presignedUrl);
            return response;

        } catch (Exception e) {
            throw new RuntimeException("Error generating pre-signed URL", e);
        }
    }

    private String getAmzDate() {
        SimpleDateFormat utcFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return utcFormat.format(new Date());
    }

    private String getDateStamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(new Date());
    }

    private String buildCanonicalQueryString(String algorithm, String accessKey, String credentialScope, String amzDate) throws Exception {
        return "X-Amz-Algorithm=" + algorithm +
                "&X-Amz-Credential=" + URLEncoder.encode(accessKey + "/" + credentialScope, StandardCharsets.UTF_8) +
                "&X-Amz-Date=" + amzDate +
                "&X-Amz-Expires=900" +  // URL valid for 15 minutes (900 seconds)
                "&X-Amz-SignedHeaders=host";
    }

    private String buildCanonicalRequest(String httpMethod, String canonicalUri, String canonicalQueryString,
                                         String canonicalHeaders, String signedHeaders, String payloadHash) {
        return httpMethod + "\n" +
                canonicalUri + "\n" +
                canonicalQueryString + "\n" +
                canonicalHeaders + "\n" +
                signedHeaders + "\n" +
                payloadHash;
    }

    private String buildStringToSign(String algorithm, String amzDate, String credentialScope, String canonicalRequest) throws Exception {
        return algorithm + "\n" +
                amzDate + "\n" +
                credentialScope + "\n" +
                sha256Hex(canonicalRequest);
    }

    private static String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashedBytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    private static byte[] hmacSHA256(String data, byte[] key) throws Exception {
        String algorithm = "HmacSHA256";
        Mac hmac = Mac.getInstance(algorithm);
        SecretKeySpec secretKey = new SecretKeySpec(key, algorithm);
        hmac.init(secretKey);
        return hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSHA256Hex(String data, byte[] key) throws Exception {
        byte[] hmac = hmacSHA256(data, key);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hmac) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    private static byte[] getSignatureKey(String key, String datestamp, String regionName, String serviceName) throws Exception {
        byte[] kDate = hmacSHA256(datestamp, ("AWS4" + key).getBytes(StandardCharsets.UTF_8));
        byte[] kRegion = hmacSHA256(regionName, kDate);
        byte[] kService = hmacSHA256(serviceName, kRegion);
        return hmacSHA256("aws4_request", kService);
    }

}