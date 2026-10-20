package com.rickenbazolo.refund.application;

import com.rickenbazolo.refund.domain.RefundPolicy;
import java.time.LocalDate;
import java.util.Optional;

public interface PolicyRepository {
  Optional<RefundPolicy> findApplicable(String type, LocalDate asOf);
}
