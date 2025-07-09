package com.example.agileagent.controller;

import com.example.agileagent.service.AgileAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AgileAgentController {

    private final AgileAgentService agentService;

    /*@GetMapping("/ask")
    public String ask(@RequestParam String query) throws Exception {
        return agentService.handleUserQuery(query);
    }*/

    @PostMapping("/chat")
    public String ask(@RequestBody Map<String, String> input, @RequestHeader("X-Session-ID") String sessionId) throws Exception{
        String userQuery = input.get("query");
        return agentService.handleUserQuery(userQuery, sessionId);
    }



}
