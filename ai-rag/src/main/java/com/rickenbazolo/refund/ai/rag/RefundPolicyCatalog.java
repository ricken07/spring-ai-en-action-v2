package com.rickenbazolo.refund.ai.rag;

import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class RefundPolicyCatalog {

  public record PolicySource(String policyId, String version, Resource resource) {}

  public List<PolicySource> sources() {
    return List.of(
        policy("REF-DEFECT-001", "defective-product.txt"),
        policy("REF-PROOF-001", "proof-of-purchase.txt"),
        policy("REF-WINDOW-001", "return-window.txt"));
  }

  private PolicySource policy(String id, String filename) {
    return new PolicySource(id, "1.0", new ClassPathResource("refund-policies/" + filename));
  }
}
