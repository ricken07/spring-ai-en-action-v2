package com.rickenbazolo.refund.ai.mcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

class RefundMcpToolCatalogTest {

  @Test
  void shouldReturnDiscoveredToolNamesIncludingPrefixedNames() {
    var first = callback("refund-policies_getRefundPolicy");
    var second = callback("otherServer_lookup");

    assertEquals(
        List.of("refund-policies_getRefundPolicy", "otherServer_lookup"),
        RefundMcpToolCatalog.names(new ToolCallback[] {first, second}));
  }

  @Test
  void shouldReturnEmptyListWhenNoCallbacksAreDiscovered() {
    assertEquals(List.of(), RefundMcpToolCatalog.names(new ToolCallback[0]));
  }

  @Test
  void shouldReadCallbacksFromTheConfiguredProvider() {
    var provider = mock(SyncMcpToolCallbackProvider.class);
    var discovered = callback("getRefundPolicy");
    when(provider.getToolCallbacks()).thenReturn(new ToolCallback[] {discovered});
    var catalog = new RefundMcpToolCatalog(provider);

    assertEquals(List.of("getRefundPolicy"), catalog.names());
    assertEquals(1, catalog.count());
  }

  private ToolCallback callback(String name) {
    var definition = mock(ToolDefinition.class);
    when(definition.name()).thenReturn(name);
    var callback = mock(ToolCallback.class);
    when(callback.getToolDefinition()).thenReturn(definition);
    return callback;
  }
}
