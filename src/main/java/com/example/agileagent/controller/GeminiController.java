package com.example.agileagent.controller;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
@RequestMapping("/chat")
public class GeminiController {

    private final ChatClient chatClient;

    public GeminiController(ChatClient.Builder  chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }


   /* @GetMapping
    public String chat(@RequestParam(defaultValue = "Explain Generative AI") String prompt) {
        return chatClient.prompt(prompt).call().content();
    }*/

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam(defaultValue = "Explain Generative AI") String prompt) {
        return chatClient
                .prompt(prompt) // ✅ start prompt chain
                .stream().content();

    }
    /*@GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> simulatedStream(@RequestParam String prompt) {
        String fullResponse = chatClient.prompt(prompt).call().content();
        return Flux.fromArray(fullResponse.split(" "))
                .delayElements(Duration.ofMillis(100)); // simulate token delay
    }*/
}
