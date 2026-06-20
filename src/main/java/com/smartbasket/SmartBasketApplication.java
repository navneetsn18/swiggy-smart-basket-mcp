package com.smartbasket;

import com.smartbasket.tools.BasketTools;
import com.smartbasket.tools.InsightTools;
import com.smartbasket.tools.SubstitutionTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SmartBasketApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartBasketApplication.class, args);
    }

    @Bean
    ToolCallbackProvider basketToolCallbacks(BasketTools basketTools, SubstitutionTools substitutionTools,
                                             InsightTools insightTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(basketTools, substitutionTools, insightTools).build();
    }
}
