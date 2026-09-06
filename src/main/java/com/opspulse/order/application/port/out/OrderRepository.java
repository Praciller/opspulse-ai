package com.opspulse.order.application.port.out;

import com.opspulse.order.domain.Order;
import com.opspulse.order.domain.OrderStatus;
import com.opspulse.shared.web.PagedResponse;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    boolean existsByOrderNumber(String orderNumber);

    Order save(Order order);

    Optional<Order> findById(UUID id);

    PagedResponse<Order> findAll(int page, int size, String search, OrderStatus status, String sortField, boolean ascending);
}
