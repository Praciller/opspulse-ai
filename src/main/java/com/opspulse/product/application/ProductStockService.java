package com.opspulse.product.application;

import com.opspulse.product.application.port.in.ProductStockUseCase;
import com.opspulse.product.application.port.out.ProductRepository;
import com.opspulse.product.domain.Product;
import com.opspulse.shared.error.ApiException;
import com.opspulse.shared.error.ErrorCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductStockService implements ProductStockUseCase {

    private final ProductRepository products;

    public ProductStockService(ProductRepository products) {
        this.products = products;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Product lockForUpdate(UUID productId) {
        return products.findByIdForUpdate(productId).orElseThrow(() ->
                new ApiException(ErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND, "Product not found"));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Product saveStock(Product lockedProduct, BigDecimal balance, UUID actorUserId, Instant now) {
        return products.save(lockedProduct.changeStock(balance, actorUserId, now));
    }
}
