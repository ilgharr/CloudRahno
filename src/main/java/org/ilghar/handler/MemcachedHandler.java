package org.ilghar.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;

@Component
public class MemcachedHandler {

    @Value("${memcached.host}")
    private String memcachedHost;

    @Value("${memcached.port}")
    private int memcachedPort;

    private MemcachedClient memcachedClient;

    public void memcachedConnect(){
        try {
            this.memcachedClient = new MemcachedClient(
                    new InetSocketAddress(memcachedHost, memcachedPort)
            );
        } catch (IOException e) {
            System.err.println("Error connecting to Memcached: " + e.getMessage());
            throw new RuntimeException("Failed to connect to Memcached.", e);
        }
    }

    public boolean memcachedAddData(String key, String value, int expiration) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Adding KEY to memcached cannot be null or empty.");
        }

        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Adding VALUE to memcached cannot be null or empty.");
        }

        if (this.memcachedClient == null) {
            throw new IllegalStateException("Memcached client is not connected!");
        }

        if (expiration < 250) {
            throw new IllegalArgumentException("Expiration time must be no less than 250 milliseconds.");
        }

        if (this.memcachedClient.get(key) != null) {
            return false;
        }

        this.memcachedClient.set(key, expiration, value);
        return true;
    }

    public String memcachedGetData(String key) {
        if (this.memcachedClient == null) {
            throw new IllegalStateException("Memcached client is not connected!");
        }
        return (String) this.memcachedClient.get(key);
    }

    public void memcachedDelete(String key) {
        if (this.memcachedClient == null) {
            throw new IllegalStateException("Memcached client is not connected!");
        }
        this.memcachedClient.delete(key);
    }

    // UNUSED ------------------------

    public String memcachedGetIdToken(String key) {
        try {
            return new ObjectMapper()
                    .readTree(memcachedGetData(key))
                    .get("id_token")
                    .asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract id_token", e);
        }
    }

    public String memcachedGetAccessToken(String key) {
        try {
            return new ObjectMapper()
                    .readTree(memcachedGetData(key))
                    .get("access_token")
                    .asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract id_token", e);
        }
    }

    public void memcachedShutdown() {
        if (this.memcachedClient != null) {
            this.memcachedClient.shutdown();
            System.out.println("Memcached connection shut down successfully.");
        } else {
            System.out.println("Memcached client is not initialized or already shut down.");
        }
    }
}