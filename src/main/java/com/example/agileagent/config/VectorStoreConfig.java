package com.example.agileagent.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


@Configuration
public class VectorStoreConfig {
    @Bean
    public VectorStore vectorStore() {
        return new VectorStore() {

            private final List<Document> docs = new ArrayList<>();

            @Override
            public void add(List<Document> documents) {
                docs.addAll(documents);
            }

            @Override
            public void delete(List<String> idList) {
                docs.removeIf(doc -> idList.contains(doc.getId()));
            }

            @Override
            public void delete(Filter.Expression filterExpression) {
                // No-op: not implemented
            }

            @Override
            public List<Document> similaritySearch(SearchRequest request) {
                // Dummy logic: just return all docs with score = 1.0
                return docs;
            }

            @Override
            public List<Document> similaritySearch(String query) {
                Set<String> stopwords = Set.of(
                        "hi", "hello", "hey", "how", "are", "you", "is", "the", "a", "an", "i", "we", "us", "me", "my", "your", "what", "who", "when", "where", "why", "do", "does"
                );

                String[] tokens = query.toLowerCase().split("\\s+");

                List<String> filteredTokens = Arrays.stream(tokens)
                        .filter(token -> !stopwords.contains(token))
                        .toList();

                if (filteredTokens.isEmpty()) {
                    return List.of(); // No meaningful tokens to match
                }

                return docs.stream()
                        .filter(doc -> {
                            String content = doc.getFormattedContent().toLowerCase();
                            return filteredTokens.stream().anyMatch(content::contains);
                        })
                        .toList();
            }
        };
    }
    }
