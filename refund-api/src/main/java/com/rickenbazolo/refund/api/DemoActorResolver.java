package com.rickenbazolo.refund.api;

import com.rickenbazolo.refund.domain.AuthenticatedActor;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class DemoActorResolver {
  private final boolean enabled;

  public DemoActorResolver(@Value("${lab.demo-identities-enabled:false}") boolean enabled) {
    this.enabled = enabled;
  }

  public AuthenticatedActor fromDemoHeader(String actor) {
    if (!enabled)
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Demo identity mode disabled");
    return switch (actor == null ? "" : actor) {
      case "alice" -> new AuthenticatedActor("alice", Set.of("alice"));
      case "bob" -> new AuthenticatedActor("bob", Set.of("bob"));
      default -> throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown demo actor");
    };
  }
}
