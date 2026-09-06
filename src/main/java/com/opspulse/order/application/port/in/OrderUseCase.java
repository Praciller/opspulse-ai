package com.opspulse.order.application.port.in;

import com.opspulse.order.domain.Order;
import com.opspulse.order.domain.OrderStatus;
import com.opspulse.shared.web.PagedResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderUseCase {

    Order create(CreateOrderCommand command);

    Optional<Order> findById(UUID id);

    PagedResponse<Order> findAll(int page, int size, String search, OrderStatus status, String sortField, boolean ascending);

    Order update(UUID id, UpdateOrderCommand command);

    Order updateStatus(UUID id, UpdateOrderStatusCommand command);

    record ItemCommand(UUID productId, BigDecimal quantity, BigDecimal unitPrice) {}

    record CreateOrderCommand(
            String orderNumber,
            String customerName,
            LocalDate expectedShipDate,
            List<ItemCommand> items,
            UUID actorUserId,
            String requestId,
            String ipAddress) {}

    record UpdateOrderCommand(
            String customerName,
            LocalDate expectedShipDate,
            List<ItemCommand> items,
            long version,
            UUID actorUserId,
            String requestId,
            String ipAddress) {}

    record UpdateOrderStatusCommand(
            OrderStatus status,
            LocalDate actualShipDate,
            long version,
            UUID actorUserId,
            String requestId,
            String ipAddress) {}
}
