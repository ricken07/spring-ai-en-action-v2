package com.rickenbazolo.refund.application;

import com.rickenbazolo.refund.domain.Order;
import java.util.Optional;

public interface OrderRepository {
  Optional<Order> findById(String id);
}
