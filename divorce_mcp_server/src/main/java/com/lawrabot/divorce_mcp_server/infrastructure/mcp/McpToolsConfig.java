package com.lawrabot.divorce_mcp_server.infrastructure.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolsConfig {

    @Bean
    public ToolCallbackProvider divorceToolsProvider(
            DivorceMcpController divorceMcpController,
            MciMcpController mciMcpController) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(divorceMcpController, mciMcpController)
                .build();
    }
}
