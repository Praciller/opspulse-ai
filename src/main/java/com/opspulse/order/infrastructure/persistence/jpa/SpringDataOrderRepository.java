package com.opspulse.order.infrastructure.persistence.jpa;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {

    boolean existsByOrderNumberIgnoreCase(String orderNumber);

    @Query("""
            select distinct o from OrderJpaEntity o
            where (:status is null or o.status = :status)
              and (:search is null
                or lower(o.orderNumber) like lower(concat('%', :search, '%'))
                or lower(o.customerName) like lower(concat('%', :search, '%')))
            """)
    Page<OrderJpaEntity> search(
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable);
}
