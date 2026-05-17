package com.example.ecosystem.repository;

import com.example.ecosystem.Entity.OrderItem;
import com.example.ecosystem.Entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // استرجاع كافة العناصر التابعة لطلب معين
    List<OrderItem> findByOrder(Order order);

    // البحث عن العناصر حسب معرف الطلب (مفيد للجرد السريع)
    @EntityGraph(attributePaths = {"product"})
    List<OrderItem> findByOrderId(Long orderId);

    // البحث عن كافة الطلبات التي تضمنت منتجاً معيناً
    // مفيد لمهمة "مجد" في تحليل المبيعات ومعالجة الدفعات
    List<OrderItem> findByProductId(Long productId);

    @Query("""
            SELECT oi
            FROM OrderItem oi
            JOIN FETCH oi.product
            JOIN oi.order o
            WHERE o.status = 'COMPLETED'
              AND o.createdAt >= :start
              AND o.createdAt < :end
            """)
    Page<OrderItem> findCompletedItemsForDailySales(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );
}
