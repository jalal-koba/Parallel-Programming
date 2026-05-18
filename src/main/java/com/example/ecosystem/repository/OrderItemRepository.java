package com.example.ecosystem.repository;

import com.example.ecosystem.Entity.OrderItem;
import com.example.ecosystem.Entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // استرجاع كافة العناصر التابعة لطلب معين
    List<OrderItem> findByOrder(Order order);

    // البحث عن العناصر حسب معرف الطلب (مفيد للجرد السريع)
    List<OrderItem> findByOrderId(Long orderId);

    // البحث عن كافة الطلبات التي تضمنت منتجاً معيناً
    // مفيد لمهمة "مجد" في تحليل المبيعات ومعالجة الدفعات
    List<OrderItem> findByProductId(Long productId);
}
