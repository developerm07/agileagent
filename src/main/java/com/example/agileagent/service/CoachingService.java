package com.example.agileagent.service;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CoachingService {

    public String generatePlan(String team, String quarter, List<String> levers) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 Coaching Plan for ").append(team).append(" (").append(quarter).append(")\n\n");

        for (String lever : levers) {
            sb.append("🔹 Focus Area: ").append(lever).append("\n");
            sb.append("  - Objective: Improve ").append(lever).append("\n");
            sb.append("  - Actions:\n");
            sb.append("    • Conduct a workshop on ").append(lever).append("\n");
            sb.append("    • Assign a coach to mentor the team\n");
            sb.append("    • Track progress in retrospectives\n\n");
        }

        sb.append(" Plan will be sent to the Scrum Master and Agile Coach for validation.");
        return sb.toString();
    }
}