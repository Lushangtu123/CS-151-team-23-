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

        // Get configuration properties and validate
        String baseUrl = config.getProperty("baseUrl");
        String token = config.getProperty("token");
        String author = config.getProperty("author");

        // Trim whitespace if properties exist
        if (baseUrl != null) baseUrl = baseUrl.trim();
        if (token != null) token = token.trim();
        if (author != null) author = author.trim();

        // Validate required properties
        if (baseUrl == null || baseUrl.isEmpty() ||
                token == null || token.isEmpty() ||
                author == null || author.isEmpty()) {
            System.out.println("Missing required configuration properties (baseUrl, token, author)");
            return;
        }

        RestTemplate restTemplate = new RestTemplate();

        // Handle list command
        if (args[0].equals("list")) {
            try {
                int page = 0;
                int maxPages = 1000;
                while (page < maxPages) {
                    String url = baseUrl + "/posts?page=" + page;
                    Map response = restTemplate.getForObject(url, Map.class);

                    if (response == null) {
                        System.out.println("No response received from server");
                        break;
                    }

                    List<Map> messages = (List<Map>) response.get("content");
                    if (messages != null) {
                        for (Map message : messages) {
                            if (message.get("id") != null && message.get("message") != null && message.get("author") != null) {
                                Long id = Long.valueOf(message.get("id").toString());
                                String text = (String) message.get("message");
                                String messageAuthor = (String) message.get("author");

                                String formattedMessage = formatMessage(id, messageAuthor, text);
                                System.out.println(formattedMessage);
                            }
                        }
                    }

                    Boolean isLast = (Boolean) response.get("last");
                    if (isLast == null || isLast) {
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
                String url = baseUrl + "/posts";
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("author", author);
                requestBody.put("message", args[1]);
                requestBody.put("token", token);

                Map response = restTemplate.postForObject(url, requestBody, Map.class);

                if (response != null && response.get("id") != null &&
                        response.get("message") != null && response.get("author") != null) {

                    Long id = Long.valueOf(response.get("id").toString());
                    String text = (String) response.get("message");
                    String responseAuthor = (String) response.get("author");

                    String formattedMessage = formatMessage(id, responseAuthor, text);
                    System.out.println(formattedMessage);
                } else {
                    System.out.println("Invalid response received from server");
                }

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

    /**
     * Helper method to format message output consistently
     */
    private static String formatMessage(Long id, String author, String message) {
        ZoneId zone = ZoneId.systemDefault();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var zdt = Instant.ofEpochMilli(id).atZone(zone);
        return String.format("%s %s said %s", zdt.format(dtf), author, message);
    }
}