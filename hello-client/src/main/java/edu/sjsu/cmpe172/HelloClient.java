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

        // Handle list command - must have exactly 1 argument
        if (args[0].equals("list")) {
            if (args.length != 1) {
                System.out.println("Usage: java -jar helloClient.jar list");
                System.out.println("Usage: java -jar helloClient.jar post <message>");
                return;
            }

            try {
                int page = 0;
                int maxPages = 1000;
                while (page < maxPages) {
                    String url = baseUrl + "/posts?page=" + page;
                    Map response = restTemplate.getForObject(url, Map.class);

                    List<Map> messages = (List<Map>) response.get("content");
                    for (Map message : messages) {
                        Long id = Long.valueOf(message.get("id").toString());
                        String text = (String) message.get("message");
                        String messageAuthor = (String) message.get("author");

                        ZoneId zone = ZoneId.systemDefault();
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        var zdt = Instant.ofEpochMilli(id).atZone(zone);
                        System.out.printf("%s %s said %s%n", zdt.format(dtf), messageAuthor, text);
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

        // Handle post command - must have exactly 2 arguments
        else if (args[0].equals("post")) {
            if (args.length != 2) {
                System.out.println("Usage: java -jar helloClient.jar list");
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

                Long id = Long.valueOf(response.get("id").toString());
                String text = (String) response.get("message");
                String responseAuthor = (String) response.get("author");

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