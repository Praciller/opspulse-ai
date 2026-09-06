package com.opspulse.supplier.application.port.out;

import com.opspulse.shared.web.PagedResponse;
import com.opspulse.supplier.domain.Supplier;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository {

    Supplier save(Supplier supplier);

    Optional<Supplier> findById(UUID id);

    PagedResponse<Supplier> findAll(int page, int size, String search, String sortField, boolean ascending);
}
