package com.rickenbazolo.refund.api.security;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rickenbazolo.refund.ai.rag.RefundPolicyIngestionService;
import com.rickenbazolo.refund.ai.rag.RefundRagService;
import com.rickenbazolo.refund.ai.tools.RefundDraftCommandService;
import com.rickenbazolo.refund.ai.tools.RefundToolChatService;
import com.rickenbazolo.refund.ai.tools.RefundToolChatService.ToolChatResult;
import com.rickenbazolo.refund.api.RefundRagController;
import com.rickenbazolo.refund.api.RefundToolsController;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {RefundRagController.class, RefundToolsController.class})
@Import({RefundSecurityConfig.class, CurrentUser.class})
@ActiveProfiles("demo")
class RefundSecurityIntegrationTest {

  @Autowired private MockMvc mvc;

  @MockitoBean private RefundRagService ragService;
  @MockitoBean private RefundPolicyIngestionService ingestionService;
  @MockitoBean private RefundToolChatService chatService;
  @MockitoBean private RefundDraftCommandService draftService;

  @Test
  void shouldRequireAuthenticationForAiApi() throws Exception {
    mvc.perform(get("/api/ai/mcp/tools")).andExpect(status().isUnauthorized());
    mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/ai/rag/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"proof\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "alice", roles = "USER")
  void shouldDenyRegularUserAccessToPolicyIngestion() throws Exception {
    mvc.perform(post("/api/ai/rag/admin/ingest").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
    verify(ingestionService, never()).ingest();
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"USER", "ADMIN"})
  void shouldAllowAdminToIngestPolicies() throws Exception {
    when(ingestionService.ingest()).thenReturn(3);

    mvc.perform(post("/api/ai/rag/admin/ingest").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
    verify(ingestionService).ingest();
  }

  @Test
  @WithMockUser(username = "bob", roles = "USER")
  void shouldUseAuthenticatedPrincipalInsteadOfClientIdentityFields() throws Exception {
    when(chatService.ask("bob", "Inspect CMD-1042"))
        .thenReturn(new ToolChatResult("request-1", "Order access denied", List.of()));

    mvc.perform(
            post("/api/ai/tools/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Inspect CMD-1042\",\"userId\":\"alice\"}"))
        .andExpect(status().isOk());

    verify(chatService).ask("bob", "Inspect CMD-1042");
  }

  @Test
  @WithMockUser(username = "alice", roles = "USER")
  void shouldProtectActuatorEndpointsFromRegularUsers() throws Exception {
    mvc.perform(get("/actuator/metrics")).andExpect(status().isForbidden());
  }
}
