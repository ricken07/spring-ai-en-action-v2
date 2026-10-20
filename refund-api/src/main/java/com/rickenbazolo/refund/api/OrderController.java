package com.rickenbazolo.refund.api;

import com.rickenbazolo.refund.application.*;
import com.rickenbazolo.refund.domain.*;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class OrderController {
  private final OrderService orders;
  private final EligibilityService eligibility;
  private final RefundService refunds;
  private final DemoActorResolver identities;

  public OrderController(
      OrderService orders,
      EligibilityService eligibility,
      RefundService refunds,
      DemoActorResolver identities) {
    this.orders = orders;
    this.eligibility = eligibility;
    this.refunds = refunds;
    this.identities = identities;
  }

  @GetMapping("/orders/{id}")
  public Order get(
      @PathVariable String id,
      @RequestHeader(value = "X-Demo-Actor", required = false) String header) {
    try {
      return orders.getAuthorized(id, identities.fromDemoHeader(header));
    } catch (ForbiddenOrderAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order access denied");
    } catch (NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
    }
  }

  @GetMapping("/orders/{id}/eligibility")
  public EligibilityDecision eligibility(
      @PathVariable String id,
      @RequestHeader(value = "X-Demo-Actor", required = false) String header) {
    return eligibility.evaluate(get(id, header));
  }

  @PostMapping("/orders/{id}/refund-drafts")
  public RefundDraft draft(
      @PathVariable String id,
      @RequestHeader(value = "X-Demo-Actor", required = false) String header) {
    try {
      return refunds.prepareDraft(id, identities.fromDemoHeader(header));
    } catch (ForbiddenOrderAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order access denied");
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    } catch (NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
    }
  }
}
