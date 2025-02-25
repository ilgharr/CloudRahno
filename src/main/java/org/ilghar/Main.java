package org.ilghar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import org.ilghar.handler.MemcachedHandler;

@SpringBootApplication
public class Main {

    public static MemcachedHandler memcached;

    @Autowired
    public void setMemcached(MemcachedHandler memcached) {
        Main.memcached = memcached;
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main.class, args);

        try {
            memcached.memcachedConnect();
            System.out.println("Connected to memcached successfully!");
        } catch (Exception e) {
            System.err.println("Error connecting to Memcached: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}