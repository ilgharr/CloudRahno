package org.ilghar.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.ilghar.Secrets;

import java.util.Date;

public class JWTHandler {

    private static final String SK = Secrets.JWT_KEY;

    // creates a JWT given the secret key, returns JWT as string
    public static String createJwt(String value, int expirationInSeconds) {
        Algorithm algorithm = Algorithm.HMAC256(SK);

        return JWT.create()
                .withSubject(value)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationInSeconds * 1000L)) // Set expiration
                .sign(algorithm);
    }

    // decodes the JWT and returns the value/payload
    public static String getValueFromJwt(String token) {
        try {
            DecodedJWT decodedJWT = JWT.decode(token);
            return decodedJWT.getSubject();
        } catch (JWTVerificationException e) {
            System.err.println("Invalid JWT: " + e.getMessage());
            return null;
        }
    }

    // boolean for testing a JWT for tampering
    public static boolean isJwtValid(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SK);
            JWT.require(algorithm).build().verify(token); // Verify JWT signature and claims
            return true;
        } catch (JWTVerificationException e) {
            System.err.println("Invalid JWT: " + e.getMessage());
            return false;
        }
    }

    // runs all tests
    public static void runInvalidJwtTests() {
        System.out.println("===================== Running Invalid JWT Tests =====================");
        testTamperedJwt();          // test with a tampered payload
        testExpiredJwt();           // test with an expired token
        testInvalidJwtStructure();  // test with an invalid JWT structure
        testEmptyJwt();             // test with an empty token
        testTamperedSignature();    // test with a tampered signature
        System.out.println("===================== End of Test =====================");

    }

    // split JWT and decode the value/payload
    // replace the payload ""testUser" with "hacker"
    // re-encode the payload
    // re-construct the JWT
    // check validity, should be false
    private static void testTamperedJwt() {
        // create a valid JWT
        String validJwt = createJwt("testUser", 30); // Generate a valid token
        System.out.println("Test: Tampered JWT");
        System.out.println("Valid JWT: " + validJwt);

        // split the JWT into its parts: header, payload, and signature
        String[] jwtParts = validJwt.split("\\.");
        String originalPayload = new String(java.util.Base64.getDecoder().decode(jwtParts[1]));

        // tamper with the payload JSON (e.g., modify "sub": "testUser" -> "hacker")
        String tamperedPayload = originalPayload.replace("testUser", "hacker");

        // re-encode the tampered payload back into Base64
        String encodedTamperedPayload = java.util.Base64.getEncoder().encodeToString(tamperedPayload.getBytes());

        // reconstruct the tampered JWT (header + tampered payload + original signature)
        String tamperedJwt = jwtParts[0] + "." + encodedTamperedPayload + "." + jwtParts[2];

        // validate the tampered JWT (should be invalid)
        System.out.println("Tampered JWT: " + tamperedJwt);
        System.out.println("Is JWT valid? " + isJwtValid(tamperedJwt));
        System.out.println();
    }

    // creates a JWT with TTL of 1 second
    // waits for 2 seconds, which expires the JWT
    // checks validity of JWT, should be false
    private static void testExpiredJwt() {
        String expiredJwt = createJwt("expiredUser", 1);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Test: Expired JWT");
        System.out.println("Expired JWT: " + expiredJwt);
        System.out.println("Is JWT valid? " + isJwtValid(expiredJwt));
        System.out.println();
    }

    // create a string and test if it is valid JWT
    // should be invalid
    private static void testInvalidJwtStructure() {
        String invalidJwt = "this.is.invalid.jwt";
        System.out.println("Test: Invalid JWT Structure");
        System.out.println("Invalid JWT: " + invalidJwt);
        System.out.println("Is JWT valid? " + isJwtValid(invalidJwt));
        System.out.println();
    }

    // create empty string and test if valid JWT
    // should be invalid
    private static void testEmptyJwt() {
        String emptyJwt = "";
        System.out.println("Test: Empty JWT");
        System.out.println("Empty JWT: " + emptyJwt);
        System.out.println("Is JWT valid? " + isJwtValid(emptyJwt));
        System.out.println();
    }

    // JWT is split into its 3 parts
    // replace actual signature with ".tamperedSignature"
    // test for validity, should be false
    private static void testTamperedSignature() {
        String validJwt = createJwt("testUser", 30);
        String[] jwtParts = validJwt.split("\\.");
        String tamperedJwt = jwtParts[0] + "." + jwtParts[1] + ".tamperedSignature";
        System.out.println("Test: Tampered JWT Signature");
        System.out.println("Tampered JWT: " + tamperedJwt);
        System.out.println("Is JWT valid? " + isJwtValid(tamperedJwt));
        System.out.println();
    }
}
