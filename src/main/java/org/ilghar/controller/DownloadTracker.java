package org.ilghar.controller;

import java.io.*;

public class DownloadTracker {

    private static final File file = new File("src/main/java/org/ilghar/controller/DownloadCount.txt");

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
    public static void removeUser(String user_id) {
        String directoryPath = "src/main/java/org/ilghar/controller";
        File tempFile = new File(directoryPath, "temp_" + file.getName());

        try (
                BufferedReader reader = new BufferedReader(new FileReader(file));
                BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))
        ) {
            tempFile.deleteOnExit();
            String currentLine;

            while ((currentLine = reader.readLine()) != null) {
                if (!currentLine.contains("id:" + user_id + ",")) {
                    writer.write(currentLine);
                    writer.newLine();
                }
            }

            if (!file.delete()) {
                System.err.println("Error: Could not delete the original file.");
                tempFile.delete();
                return;
            }

            if (!tempFile.renameTo(file)) {
                System.err.println("Error: Could not rename the temp file to the original file.");
                tempFile.delete();
            }

        } catch (IOException e) {
            System.err.println("Error removing User from downloads file: " + e.getMessage());
            tempFile.delete();
        }
    }

    public static int getCurrentCount(String user_id) {
        int current_count = -1;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String currentLine;

            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.startsWith("id:" + user_id + ",")) {
                    String[] parts = currentLine.split(",");
                    for (String part : parts) {
                        if (part.startsWith("current:")) {
                            current_count = Integer.parseInt(part.split(":")[1]);
                            break;
                        }
                    }
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Error parsing the current count: " + e.getMessage());
        }
        return current_count;
    }

    public static int getMaxCountByUserId(String user_id) {
        int max_count = -1;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String currentLine;

            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.startsWith("id:" + user_id + ",")) {
                    String[] parts = currentLine.split(",");
                    for (String part : parts) {
                        if (part.startsWith("max:")) {
                            max_count = Integer.parseInt(part.split(":")[1]);
                            break;
                        }
                    }
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Error parsing the max count: " + e.getMessage());
        }
        return max_count;
    }

    public static void incrementCurrentByUserId(String user_id, int increment) {
        File temp_file = new File(file.getAbsolutePath() + ".tmp");
        boolean user_found = false;

        try (
                BufferedReader reader = new BufferedReader(new FileReader(file));
                BufferedWriter writer = new BufferedWriter(new FileWriter(temp_file))
        ) {
            String current_line;

            while ((current_line = reader.readLine()) != null) {
                if (current_line.startsWith("id:" + user_id + ",")) {
                    int max = getMaxCountByUserId(user_id); // Fetch max count
                    int current = getCurrentCount(user_id); // Fetch current count
                    int new_current = Math.min(current + increment, max);
                    current_line = "id:" + user_id + ",max:" + max + ",current:" + new_current;
                    user_found = true;
                }

                writer.write(current_line);
                writer.newLine();
            }

            if (user_found) {
                if (!file.delete()) {
                    System.err.println("Error: Could not delete the original file.");
                }
                if (!temp_file.renameTo(file)) {
                    System.err.println("Error: Could not rename the temp file.");
                }
            } else {
                temp_file.delete();
                System.out.println("User ID not found: " + user_id);
            }

        } catch (IOException | NumberFormatException e) {
            System.err.println("Error processing the file: " + e.getMessage());
        }
    }

    public static void updateMaxCountByUserId(String user_id, int new_max) {
        File temp_file = new File(file.getAbsolutePath() + ".tmp");
        boolean user_found = false;

        try (
                BufferedReader reader = new BufferedReader(new FileReader(file));
                BufferedWriter writer = new BufferedWriter(new FileWriter(temp_file))
        ) {
            String current_line;

            while ((current_line = reader.readLine()) != null) {
                if (current_line.startsWith("id:" + user_id + ",")) {
                    int current = getCurrentCount(user_id); // Fetch current count

                    current_line = "id:" + user_id + ",max:" + new_max + ",current:" + current;
                    user_found = true;
                }
                writer.write(current_line);
                writer.newLine();
            }

            if (user_found) {
                if (!file.delete()) {
                    System.err.println("Error: Could not delete the original file.");
                }
                if (!temp_file.renameTo(file)) {
                    System.err.println("Error: Could not rename the temporary file.");
                }
            } else {
                temp_file.delete();
                System.out.println("User ID not found: " + user_id);
            }

        } catch (IOException e) {
            System.err.println("Error processing the file: " + e.getMessage());
        }
    }
}
