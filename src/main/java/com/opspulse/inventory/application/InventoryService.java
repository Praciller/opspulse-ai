package com.opspulse.inventory.application;

import com.opspulse.inventory.application.port.in.InventoryUseCase;
import com.opspulse.inventory.application.port.out.InventoryMovementRepository;
import com.opspulse.inventory.domain.InventoryMovement;
import com.opspulse.inventory.domain.MovementType;
import com.opspulse.product.application.port.in.ProductUseCase;
import com.opspulse.shared.error.ApiException;
import com.opspulse.shared.error.ErrorCode;
import com.opspulse.shared.web.PagedResponse;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class InventoryService implements InventoryUseCase {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "movementType", "quantity");

    private final InventoryMovementRepository movements;
    private final ProductUseCase products;
    private final InventoryTransactionExecutor transactionExecutor;

    public InventoryService(
            InventoryMovementRepository movements,
            ProductUseCase products,
            InventoryTransactionExecutor transactionExecutor) {
        this.movements = movements;
        this.products = products;
        this.transactionExecutor = transactionExecutor;
    }

    @Override
    public InventoryMovement createMovement(CreateMovementCommand command) {
        boolean joinedTransaction = TransactionSynchronizationManager.isActualTransactionActive();
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return transactionExecutor.execute(command);
            } catch (CannotAcquireLockException exception) {
                if (joinedTransaction || attempt == 3) {
                    throw exception;
                }
            }
        }
        throw new IllegalStateException("inventory retry loop exhausted");
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<InventoryMovement> findAll(int page, int size, UUID productId, MovementType type, String sortField, boolean ascending) {
        requireSort(sortField);
        return movements.findAll(page, size, productId, type, sortField, ascending);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductInventory findByProduct(UUID productId, int page, int size) {
        var product = products.findById(productId).orElseThrow(() ->
                new ApiException(ErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND, "Product not found"));
        return new ProductInventory(product, movements.findAll(page, size, productId, null, "createdAt", false));
    }

    private static void requireSort(String field) {
        if (!SORT_FIELDS.contains(field)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Unsupported inventory sort field");
        }
    }

}
