package com.opspulse.product.application.port.out;

import com.opspulse.product.domain.Product;
import com.opspulse.shared.web.PagedResponse;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    boolean existsBySku(String sku);

    Product save(Product product);

    Optional<Product> findById(UUID id);

    Optional<Product> findByIdForUpdate(UUID id);

    boolean hasInventoryMovements(UUID id);

    PagedResponse<Product> findAll(
            int page, int size, String search, String sortField, boolean ascending);
}
