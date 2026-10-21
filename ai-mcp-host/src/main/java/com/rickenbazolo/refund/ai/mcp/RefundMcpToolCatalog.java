package com.rickenbazolo.refund.ai.mcp;

import java.util.Arrays;
import java.util.List;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class RefundMcpToolCatalog {

  private final SyncMcpToolCallbackProvider provider;

  public RefundMcpToolCatalog(SyncMcpToolCallbackProvider provider) {
    this.provider = provider;
  }

  public List<String> names() {
    return names(provider.getToolCallbacks());
  }

  static List<String> names(ToolCallback[] callbacks) {
    return Arrays.stream(callbacks).map(callback -> callback.getToolDefinition().name()).toList();
  }

  public int count() {
    return provider.getToolCallbacks().length;
  }
}
