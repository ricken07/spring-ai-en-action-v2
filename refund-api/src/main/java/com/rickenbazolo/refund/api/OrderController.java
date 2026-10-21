package com.rickenbazolo.refund.api;

import com.rickenbazolo.refund.api.security.CurrentUser;
import com.rickenbazolo.refund.application.*;
import com.rickenbazolo.refund.domain.*;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class OrderController {
  private final OrderService orders;
  private final EligibilityService eligibility;
  private final RefundService refunds;
  private final CurrentUser currentUser;

  public OrderController(
      OrderService orders,
      EligibilityService eligibility,
      RefundService refunds,
      CurrentUser currentUser) {
    this.orders = orders;
    this.eligibility = eligibility;
    this.refunds = refunds;
    this.currentUser = currentUser;
  }

  @GetMapping("/orders/{id}")
  public Order get(@PathVariable String id) {
    try {
      return orders.getAuthorized(id, actor());
    } catch (ForbiddenOrderAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order access denied");
    } catch (NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
    }
  }

  @GetMapping("/orders/{id}/eligibility")
  public EligibilityDecision eligibility(@PathVariable String id) {
    return eligibility.evaluate(get(id));
  }

  @PostMapping("/orders/{id}/refund-drafts")
  public RefundDraft draft(@PathVariable String id) {
    try {
      return refunds.prepareDraft(id, actor());
    } catch (ForbiddenOrderAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order access denied");
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    } catch (NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
    }
  }

  private AuthenticatedActor actor() {
    String userId = currentUser.id();
    return new AuthenticatedActor(userId, Set.of(userId));
  }
}
