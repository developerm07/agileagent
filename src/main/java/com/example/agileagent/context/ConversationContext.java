package com.example.agileagent.context;

import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ConversationContext {
    private String team;
    private String quarter;
    private List<String> lowScoringLevers;
    private String lastUserQuery;
    private String lastResponse;
    private Map<String, Object> pendingAction;
    private List<Message> messageHistory = new ArrayList<>();


}
