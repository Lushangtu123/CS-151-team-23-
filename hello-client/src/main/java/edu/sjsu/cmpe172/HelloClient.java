package edu.sjsu.cmpe172;

import org.springframework.web.client.RestTemplate;
import java.io.FileInputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class HelloClient {

    public static void main(String[] args) {

        // Check if we have the right arguments
        if (args.length == 0) {
            System.out.println("Usage: java -jar helloClient.jar list");
            System.out.println("Usage: java -jar helloClient.jar post <message>");
            return;
        }

        // Load the config file
        Properties config = new Properties();
        String configFile = System.getProperty("user.home") + "/.config/cmpe172hello.properties";
        try {
            config.load(new FileInputStream(configFile));
        } catch (Exception e) {
            System.out.println("Cannot read config file: " + e.getMessage());
            return;
        }

        String baseUrl = config.getProperty("baseUrl");
        String token = config.getProperty("token");
        String author = config.getProperty("author");

        RestTemplate restTemplate = new RestTemplate();

        // Handle list command
        if (args[0].equals("list")) {
            try {
                int page = 0;
                int maxPages = 1000;
                while (page < maxPages) {
                    String url = baseUrl + "/posts?page=" + page;
                    Map response = restTemplate.getForObject(url, Map.class);

                    if (response == null || !response.containsKey("content")) {
                        System.out.println("Invalid response from server.");
                        break;
                    }

                    List<Map> messages = (List<Map>) response.get("content");
                    for (Map message : messages) {
                        if (message == null) continue;

                        Object idObj = message.get("id");
                        Object textObj = message.get("message");
                        Object authorObj = message.get("author");

                        if (idObj == null || textObj == null || authorObj == null) continue;

                        Long id = Long.valueOf(idObj.toString());
                        String text = textObj.toString();
                        String messageAuthor = authorObj.toString();

                        ZoneId zone = ZoneId.systemDefault();
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        var zdt = Instant.ofEpochMilli(id).atZone(zone);
                        System.out.printf("%s %s said %s%n", zdt.format(dtf), messageAuthor, text);
                    }

                    // Safe boolean parsing
                    Object lastObj = response.get("last");
                    if (lastObj != null) {
                        boolean isLast = Boolean.parseBoolean(lastObj.toString());
                        if (isLast) break;
                    } else {
                        break;
                    }
                    page++;
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        // Handle post command
        else if (args[0].equals("post")) {
            if (args.length < 2) {
                System.out.println("Usage: java -jar helloClient.jar post <message>");
                return;
            }

            try {
                // Combine all arguments after "post" into a single message
                StringBuilder messageBuilder = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (i > 1) messageBuilder.append(" ");
                    messageBuilder.append(args[i]);
                }
                String fullMessage = messageBuilder.toString();

                String url = baseUrl + "/posts";
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("author", author);
                requestBody.put("message", fullMessage);
                requestBody.put("token", token);

                Map response = restTemplate.postForObject(url, requestBody, Map.class);

                if (response == null) {
                    System.out.println("No response from server.");
                    return;
                }

                Object idObj = response.get("id");
                Object textObj = response.get("message");
                Object authorObj = response.get("author");

                if (idObj == null || textObj == null || authorObj == null) {
                    System.out.println("Invalid response format from server.");
                    return;
                }

                Long id = Long.valueOf(idObj.toString());
                String text = textObj.toString();
                String responseAuthor = authorObj.toString();

                ZoneId zone = ZoneId.systemDefault();
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                var zdt = Instant.ofEpochMilli(id).atZone(zone);
                System.out.printf("%s %s said %s%n", zdt.format(dtf), responseAuthor, text);

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        // Handle invalid command
        else {
            System.out.println("Usage: java -jar helloClient.jar list");
            System.out.println("Usage: java -jar helloClient.jar post <message>");
        }
    }
}