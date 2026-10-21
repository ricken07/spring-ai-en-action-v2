package com.rickenbazolo.refund.mcpserver;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RefundMcpServerConfig {

  @Bean
  ToolCallbackProvider refundPolicyToolProvider(RefundPolicyTools tools) {
    return MethodToolCallbackProvider.builder().toolObjects(tools).build();
  }
}
