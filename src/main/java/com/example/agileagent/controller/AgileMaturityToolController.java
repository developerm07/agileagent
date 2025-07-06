package com.example.agileagent.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/***
 * Basically this is not a controller it is a service.. in real time we will mark this as service.
 */
@RestController
@RequestMapping("/tools/agile-maturity")
@RequiredArgsConstructor
public class AgileMaturityToolController {
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @PostMapping
    public List<Map<String, String>> getAssessment(@RequestBody Map<String, String> input) throws Exception {
        Resource resource = resourceLoader.getResource("classpath:mock-data/mock-agile-data.json");
        List<Map<String, String>> records = objectMapper.readValue(
                resource.getInputStream(), new TypeReference<>() {}
        );
        return records.stream()
                .filter(r -> input.get("team") == null ||
                        normalize(r.get("Team")).equalsIgnoreCase(normalize(input.get("team"))))
                .filter(r -> input.get("quarter") == null ||
                        normalize(r.get("Quarter")).equalsIgnoreCase(normalize(input.get("quarter"))))
                .collect(Collectors.toList());
    }

    private String normalize(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "").trim();
    }


}
