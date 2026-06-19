package com.smartbasket.tools;

import com.smartbasket.substitution.SubstitutionService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/** MCP tools for learned substitutions (Phase 3). */
@Component
public class SubstitutionTools {

    private final SubstitutionService substitutions;

    public SubstitutionTools(SubstitutionService substitutions) {
        this.substitutions = substitutions;
    }

    @Tool(description = "Record that a fallback product may replace a preferred product when it is "
            + "out of stock. Learned from the user's explicit choice; appended at the end of the "
            + "fallback chain.")
    public String learn_substitution(
            @ToolParam(description = "User id") String userId,
            @ToolParam(description = "Preferred product, exact full name") String preferred,
            @ToolParam(description = "Acceptable fallback product, exact full name") String fallback) {
        substitutions.learn(userId, preferred, fallback);
        return "Learned: " + fallback + " can substitute " + preferred;
    }

    @Tool(description = "Return the ordered fallback chain (best first) for a preferred product.")
    public List<String> get_substitutions(
            @ToolParam(description = "User id") String userId,
            @ToolParam(description = "Preferred product, exact full name") String preferred) {
        return substitutions.chain(userId, preferred);
    }
}
