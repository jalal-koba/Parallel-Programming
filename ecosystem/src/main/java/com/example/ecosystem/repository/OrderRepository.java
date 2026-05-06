package com.example.ecosystem.repository;

import com.example.ecosystem.Entity.Order;
import com.example.ecosystem.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // استرجاع سجل الطلبات الخاص بمستخدم معين
    List<Order> findByUser(User user);

    List<Order> findByUserId(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    // البحث عن الطلبات حسب حالتها (مثلاً: PENDING, COMPLETED)
    // مفيد لمهمة "مجد" في معالجة الدفعات (Batch Processing) للجرد اليومي
    List<Order> findByStatus(String status);

    // استرجاع الطلبات التي تمت في تاريخ معين أو بعده
    // يستخدم في التقارير والتحليلات لتقييم الأداء
    List<Order> findByCreatedAtAfter(java.time.LocalDateTime dateTime);
}
