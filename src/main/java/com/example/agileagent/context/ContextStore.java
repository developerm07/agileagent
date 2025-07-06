package com.example.agileagent.context;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class ContextStore {

    private final Map<String, ConversationContext> store = new ConcurrentHashMap<>();

    public ConversationContext get(String sessionId) {
        return store.computeIfAbsent(sessionId, id -> new ConversationContext());
    }

    public void update(String sessionId, Consumer<ConversationContext> updater) {
        updater.accept(get(sessionId));
    }

    public void clear(String sessionId) {
        store.remove(sessionId);
    }


}
