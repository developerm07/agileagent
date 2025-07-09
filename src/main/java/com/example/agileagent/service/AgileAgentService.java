package com.example.agileagent.service;

import com.example.agileagent.context.ContextStore;
import com.example.agileagent.context.ConversationContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AgileAgentService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ContextStore contextStore;
    private final CoachingService coachingService;

    public AgileAgentService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, RestTemplate restTemplate, ObjectMapper objectMapper, ContextStore contextStore, CoachingService coachingService) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.contextStore = contextStore;
        this.coachingService = coachingService;
    }
//    public String handleUserQuery(String userQuery) throws Exception {
//        // Step 1: Search for relevant documents
//        List<Document> docs = vectorStore.similaritySearch(userQuery);
//
//        // Step 2: Separate tool and prompt documents
//        Document toolDoc = docs.stream()
//                .filter(d -> "tool".equals(d.getMetadata().get("type")))
//                .findFirst()
//                .orElse(null);
//
//        Document promptDoc = docs.stream()
//                .filter(d -> "system-prompt".equals(d.getMetadata().get("type")))
//                .findFirst()
//                .orElse(new Document("""
//                        You are an intelligent Agile assistant. Respond helpfully and concisely to user queries about Agile practices, metrics, and team performance.
//                        """));
//
//
//        String systemPrompt = promptDoc.getFormattedContent(MetadataMode.NONE);
//
//
//        // Step 3: If no tool is relevant, just chat with Gemini
//        if (toolDoc == null) {
//            return chatClient.prompt(new Prompt(List.of(
//                    new SystemMessage("Respond to user based on his query"),
//                    new UserMessage(userQuery)
//            ))).call().content();
//        }
//        String toolDc=toolDoc.getFormattedContent(MetadataMode.NONE);
//
//        // Step 4: Parse tool metadata safely
//        JsonNode toolJson;
//        try {
//            toolJson = objectMapper.readTree(toolDc);
//        } catch (Exception e) {
//            System.err.println("❌ Invalid JSON in toolDoc: " + toolDoc.getFormattedContent());
//            throw e;
//        }
//
//        String endpoint = toolJson.get("endpoint").asText();
//        List<String> inputFields = objectMapper.convertValue(
//                toolJson.get("input_schema"), new TypeReference<>() {}
//        );
//
//        // Step 5: Extract tool inputs using Gemini
//        Map<String, String> extractedInputs = extractToolInputsFromQuery(userQuery, inputFields);
//
//        // Step 6: Check for missing inputs
//        List<String> missingFields = new ArrayList<>();
//        for (String field : inputFields) {
//            if (!extractedInputs.containsKey(field) || extractedInputs.get(field).isBlank()) {
//                missingFields.add(field);
//            }
//        }
//
//        if (!missingFields.isEmpty()) {
//            return " I need a bit more info to help you. Could you please provide: " +
//                    String.join(", ", missingFields) + "?";
//        }
//
//        // Step 7: Call the tool endpoint
//        String toolOutput = restTemplate.postForObject("http://localhost:8080" + endpoint, extractedInputs, String.class);
//
//        // Step 8: Build final prompt for Gemini
//        Prompt prompt = new Prompt(List.of(
//                new SystemMessage(systemPrompt),
//                new UserMessage(userQuery + "\n\nTool Output:\n" + toolOutput)
//        ));
//
//        return chatClient.prompt(prompt).call().content();
//    }


    public String handleUserQuery(String userQuery, String sessionId) throws Exception {
        ConversationContext context = contextStore.get(sessionId);

        //  Step 1: Add user message to history
        UserMessage userMessage = new UserMessage(userQuery);
        context.getMessageHistory().add(userMessage);
        context.setLastUserQuery(userQuery);

        //  Step 2: Search vector DB for relevant documents
        List<Document> docs = vectorStore.similaritySearch(userQuery);

        //  Step 3: Load system prompt (from DB or fallback)
        String genericPrompt = """
        You are an intelligent Agile assistant designed to help users understand Agile practices, interpret team metrics, and improve performance.

        Respond clearly and concisely to user queries. When relevant, use available tools or data to provide insights. If the user’s query relates to team performance, generate a coaching plan based on the tool output if tool output is empty , avoid including as Tool output is empty.

        Maintain context across multiple turns. If the user provides vague or partial input, infer intent using conversation history and ask clarifying questions when needed.

        Always be helpful, structured, and user-centric.
        """;

        Document promptDoc = docs.stream()
                .filter(d -> "system-prompt".equals(d.getMetadata().get("type")))
                .findFirst()
                .orElse(new Document(genericPrompt));

        String systemPrompt = promptDoc.getFormattedContent(MetadataMode.NONE);

        // 🛠️ Step 4: Check if a tool is relevant
        Document toolDoc = docs.stream()
                .filter(d -> "tool".equals(d.getMetadata().get("type")))
                .findFirst()
                .orElse(null);

        // Step 5: If no tool, just chat with Gemini using full history
        if (toolDoc == null) {
            List<Message> fullPrompt = new ArrayList<>();
            fullPrompt.add(new SystemMessage(systemPrompt));
            fullPrompt.addAll(context.getMessageHistory());

            Prompt prompt = new Prompt(fullPrompt);
            String response = chatClient.prompt(prompt).call().content();

            context.getMessageHistory().add(new AssistantMessage(response));
            context.setLastResponse(response);
            return response;
        }

        //  Step 6: Parse tool metadata
        String toolDc = toolDoc.getFormattedContent(MetadataMode.NONE);
        JsonNode toolJson = objectMapper.readTree(toolDc);
        String endpoint = toolJson.get("endpoint").asText();
        List<String> inputFields = objectMapper.convertValue(toolJson.get("input_schema"), new TypeReference<>() {});

        //  Step 7: Extract tool inputs from user query
        Map<String, String> extractedInputs = extractToolInputsFromQuery(userQuery, inputFields);
        if (extractedInputs.containsKey("team")) context.setTeam(extractedInputs.get("team"));
        if (extractedInputs.containsKey("quarter")) context.setQuarter(extractedInputs.get("quarter"));

        // Step 8: Fill missing inputs from memory
        for (String field : inputFields) {
            if (!extractedInputs.containsKey(field) || extractedInputs.get(field).isBlank()) {
                String fromMemory = switch (field) {
                    case "team" -> context.getTeam();
                    case "quarter" -> context.getQuarter();
                    default -> null;
                };
                if (fromMemory != null) extractedInputs.put(field, fromMemory);
            }
        }

        //  Step 9: Ask for missing inputs if still incomplete
        List<String> missingFields = inputFields.stream()
                .filter(f -> !extractedInputs.containsKey(f) || extractedInputs.get(f).isBlank())
                .toList();

        if (!missingFields.isEmpty()) {
            String clarification = " I need a bit more info to help you. Could you please provide: " + String.join(", ", missingFields) + "?";
            context.getMessageHistory().add(new AssistantMessage(clarification));
            context.setLastResponse(clarification);
            return clarification;
        }

        // Step 10: Call the tool
        String toolOutput = restTemplate.postForObject("http://localhost:8080" + endpoint, extractedInputs, String.class);

        // Step 11: Add tool output to history and build full prompt
        context.getMessageHistory().add(new UserMessage("Tool Output:\n" + toolOutput));

        List<Message> fullPrompt = new ArrayList<>();
        fullPrompt.add(new SystemMessage(systemPrompt));
        fullPrompt.addAll(context.getMessageHistory());

        Prompt prompt = new Prompt(fullPrompt);
        String assistantResponse = chatClient.prompt(prompt).call().content();

        //  Step 12: Store assistant response and update memory
        context.getMessageHistory().add(new AssistantMessage(assistantResponse));
        context.setLastResponse(assistantResponse);
        context.setPendingAction(extractActionJson(assistantResponse)); // optional

        return assistantResponse;
    }

    private Map<String, String> extractToolInputsFromQuery(String userQuery, List<String> inputFields) {
        String extractionPrompt = String.format("""
                Extract the following fields from the user's query: %s.
                For 'quarter', include both the quarter and the year (e.g., "Q1 2024").
                Return only a plain JSON object with keys matching the field names,do not include fields with null values — no Markdown, no explanation
                User Query: %s
                """, inputFields, userQuery);

        String response = chatClient.prompt(new Prompt(List.of(
                new UserMessage(extractionPrompt)
        ))).call().content();
        String cleaned = stripMarkdownCodeBlock(response);
        try {
            return objectMapper.readValue(response, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse extracted input: " + response, e);
        }
    }
    private String stripMarkdownCodeBlock(String response) {
        String trimmed = response.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastBackticks = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastBackticks > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastBackticks).strip();
            }
        }
        return trimmed;
    }
    public Map<String, Object> extractActionJson(String response) {
        try {
            // Find the first '{' and last '}' to isolate the JSON block
            int start = response.indexOf('{');
            int end = response.lastIndexOf('}') + 1;

            if (start == -1 || end == -1 || end <= start) {
                return Map.of("action", "none"); // fallback
            }

            String jsonPart = response.substring(start, end);

            return objectMapper.readValue(jsonPart, new TypeReference<>() {});
        } catch (Exception e) {
            System.err.println(">>>>>  Failed to extract action JSON: " + e.getMessage());
            return Map.of("action", "none");
        }
    }

   /* public String handleFollowUp(String sessionId) {
        ConversationContext context = contextStore.get(sessionId);

        //  Step 1: Build prompt for Gemini to interpret next action
        String systemPrompt = """
        You are Maddy Agent, an intelligent Agile assistant.

        Based on the full conversation history, determine what the user wants to do next.

        Return a JSON object with:
        - action: one of [generate_coaching_plan, fetch_metrics, clarify, none]
        - team: if known
        - quarter: if known
        - levers: if relevant

        Only return the JSON object. Do not include explanations or Markdown.
        """;

        List<Message> fullPrompt = new ArrayList<>();
        fullPrompt.add(new SystemMessage(systemPrompt));
        fullPrompt.addAll(context.getMessageHistory());

        String json = chatClient.prompt(new Prompt(fullPrompt)).call().content();
        Map<String, Object> action = extractActionJson(json);

        //  Step 2: Route the action
        String actionType = (String) action.get("action");

        return switch (actionType) {
            case "generate_coaching_plan" -> {
                String team = (String) action.get("team");
                String quarter = (String) action.get("quarter");
                List<String> levers = (List<String>) action.get("levers");
                yield coachingService.generatePlan(team, quarter, levers);
            }
            case "fetch_metrics" -> {
                String team = (String) action.get("team");
                String quarter = (String) action.get("quarter");
                yield runToolLogic(Map.of("team", team, "quarter", quarter));
            }
            case "clarify" -> "Can you clarify what you'd like me to do next?";
            default -> "Let me know how I can help.";
        };
    }*/

    /*public String runToolLogic(Map<String, String> inputs) { //TODO: Here I need to dynamically pass endpoint based on inputs
        try {
            // Simulate tool call — you can replace this with a real method call
            String endpoint = "/agile-metrics"; // or dynamically resolve from tool metadata
            return restTemplate.postForObject("http://localhost:8080" + endpoint, inputs, String.class);
        } catch (Exception e) {
            return ">>>>>> Failed to fetch tool output: " + e.getMessage();
        }
    }
*/
}
