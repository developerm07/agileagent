package com.example.agileagent.config;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VectorDbLoader {

    private final VectorStore vectorStore;
    private final ResourceLoader resourceLoader;

    @PostConstruct
    public void loadVectorData() throws Exception {
        Resource promptResource = resourceLoader.getResource("classpath:prompts/system-prompt.json");
        String promptContent = new String(promptResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);


        Document promptDoc = new Document(promptContent);
        promptDoc.getMetadata().put("type", "system-prompt");
        promptDoc.getMetadata().put("tool", "agile-maturity");

        System.out.println("✅ Loaded tool content:\n" + promptDoc.getFormattedContent(MetadataMode.NONE)); // ✅ log here


        Resource toolResource = resourceLoader.getResource("classpath:tools/agile-maturity.json");
        String content = new String(toolResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        Document toolDoc = new Document(content);
        toolDoc.getMetadata().put("type", "tool");
        toolDoc.getMetadata().put("name", "agile-maturity");

        System.out.println("✅ Loaded tool content:\n" + toolDoc.getFormattedContent(MetadataMode.NONE));
        vectorStore.add(List.of(promptDoc,toolDoc));
    }

}
