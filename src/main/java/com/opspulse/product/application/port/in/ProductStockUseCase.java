package com.opspulse.product.application.port.in;

import com.opspulse.product.domain.Product;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface ProductStockUseCase {

    Product lockForUpdate(UUID productId);

    Product saveStock(Product lockedProduct, BigDecimal balance, UUID actorUserId, Instant now);
}
