package com.rickenbazolo.refund.ai.tools;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

class RefundDraftCommandServiceTest {

  private final RefundBusinessOperations business = mock(RefundBusinessOperations.class);
  private final RefundDraftCommandService service = new RefundDraftCommandService(business);

  @Test
  void shouldRejectUnconfirmedDraftWithoutCallingBusiness() {
    assertThrows(
        IllegalStateException.class, () -> service.prepareDraft("alice", "CMD-1042", false));
    verifyNoInteractions(business);
  }

  @Test
  void shouldDelegateConfirmedDraft() {
    var expected = new RefundToolDtos.DraftView("DRAFT-001", "CMD-1042", "DRAFT");
    when(business.createDraft("alice", "CMD-1042")).thenReturn(expected);

    var result = service.prepareDraft("alice", "CMD-1042", true);

    assertEquals(expected, result);
    verify(business).createDraft("alice", "CMD-1042");
  }

  @Test
  void shouldValidateIdentityAndOrderBeforeCallingBusiness() {
    assertThrows(SecurityException.class, () -> service.prepareDraft(" ", "CMD-1042", true));
    assertThrows(IllegalArgumentException.class, () -> service.prepareDraft("alice", " ", true));
    verifyNoInteractions(business);
  }
}
