package com.rickenbazolo.refund.api.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.rickenbazolo.refund.api.lab.LabExecutionController;
import com.rickenbazolo.refund.api.lab.LabExecutionRequest;
import com.rickenbazolo.refund.api.lab.LabExecutionResult;
import com.rickenbazolo.refund.api.lab.LabExecutionService;
import com.rickenbazolo.refund.api.lab.LabExecutionService.OrderAccessDeniedException;
import com.rickenbazolo.refund.api.lab.LabScenarioCatalog;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = {
      RefundRagController.class,
      RefundToolsController.class,
      LabExecutionController.class
    })
@Import({RefundSecurityConfig.class, CurrentUser.class, LabScenarioCatalog.class})
@ActiveProfiles("demo")
class RefundSecurityIntegrationTest {

  @Autowired private MockMvc mvc;

  @MockitoBean private RefundRagService ragService;
  @MockitoBean private RefundPolicyIngestionService ingestionService;
  @MockitoBean private RefundToolChatService chatService;
  @MockitoBean private RefundDraftCommandService draftService;
  @MockitoBean private LabExecutionService labExecutionService;

  @Test
  void shouldRequireAuthenticationForAiApi() throws Exception {
    mvc.perform(get("/")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/ai/mcp/tools")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/lab/scenarios")).andExpect(status().isUnauthorized());
    mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/ai/rag/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"proof\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "alice", roles = "USER")
  void shouldServeLabScenarioCatalogToAuthenticatedUser() throws Exception {
    mvc.perform(get("/api/lab/scenarios")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "alice", roles = "USER")
  void shouldPassAuthenticatedPrincipalToLabExecution() throws Exception {
    when(labExecutionService.execute(eq("alice"), any(LabExecutionRequest.class)))
        .thenReturn(
            new LabExecutionResult("exec-1", "CHAT", "SUCCESS", "answer", List.of(), Map.of()));

    mvc.perform(
            post("/api/lab/executions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"scenarioId\":\"CHAT\",\"question\":\"hello\",\"reranking\":false,\"confirmed\":false}"))
        .andExpect(status().isOk());

    verify(labExecutionService).execute(eq("alice"), any(LabExecutionRequest.class));

    mvc.perform(
            post("/api/lab/executions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"scenarioId\":\"CHAT\",\"question\":\"hello\",\"reranking\":false,\"confirmed\":false,\"userId\":\"bob\"}"))
        .andExpect(status().isOk());

    verify(labExecutionService, org.mockito.Mockito.times(2))
        .execute(eq("alice"), any(LabExecutionRequest.class));
    verify(labExecutionService, never()).execute(eq("bob"), any(LabExecutionRequest.class));
  }

  @Test
  @WithMockUser(username = "bob", roles = "USER")
  void shouldReturnForbiddenWhenBusinessServiceDeniesOrderAccess() throws Exception {
    when(labExecutionService.execute(eq("bob"), any(LabExecutionRequest.class)))
        .thenThrow(new OrderAccessDeniedException());

    mvc.perform(
            post("/api/lab/executions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"scenarioId\":\"TOOLS\",\"question\":\"Inspect"
                        + " CMD-1042\",\"reranking\":false,\"confirmed\":false}"))
        .andExpect(status().isForbidden());
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
