package org.ilghar.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.ilghar.controller.S3Controller;

public class DownloadTracker {

    private static final File file = new File("downloads.txt");

    // when user is initially written in the text file, the current will always be 0, as this is the entry point
    public static void addUser(String user_id, Integer max){
        // BufferedWriter: multiple characters are stored in memory (buffer) and written in bulk, significantly speeding up performance.
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))){
            writer.write("id:" + user_id + ",max:" + max + ",current:" + 0);
            writer.newLine();
        }catch(IOException e){
            System.err.println("Error writing User into downloads file: " + e.getMessage());
        }
    }

    // user is removed from text file at /logout endpoint
    public static void removeUser(String user_id){

    }
}
