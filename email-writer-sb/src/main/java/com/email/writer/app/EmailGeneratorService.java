package com.email.writer.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class EmailGeneratorService {

        private final WebClient webClient;

        @Value("${gemini.api.url}")
        private String geminiApiUrl;
        @Value("${gemini.api.key}")
        private String geminiApikey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = WebClient.builder().build();
    }

    public String generateEmailReply(EmailRequest emailRequest){
            //build the prompt
            String prompt= buildPrompt(emailRequest);
            // craft a request
            Map<String,Object> requestBody =Map.of(
                    "contents",new Object[]{
                            Map.of("parts",new Object[]{
                                    Map.of("text",prompt)
                            })
            }
            );
            //do request and get response
        String response=webClient.post()
        .uri(geminiApiUrl + geminiApikey)
        .header("content-Type","application/json")
                .bodyValue(requestBody)
                .retrieve()
        .bodyToMono(String.class)
        .block();

        //extract return response
        return extractResponseContent(response);
        }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode=mapper.readTree(response);
            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        }
        catch (Exception e){
            return "Error processing request: "+e.getMessage();
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
            StringBuilder prompt= new StringBuilder();
            prompt.append("Generate a professional email reply for the email content please do not generate a subject line and give single response");
            if(emailRequest.getTone() !=null && !emailRequest.getTone().isEmpty()){
                prompt.append("use a").append(emailRequest.getTone()).append("tone.");
            }
            prompt.append("\nOriginal email:\n").append(emailRequest.getEmailContent());
            return prompt.toString();
    }

}
