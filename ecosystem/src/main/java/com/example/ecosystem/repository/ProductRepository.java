package com.example.ecosystem.repository;

import com.example.ecosystem.Entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // البحث العادي - يستخدمه "علي" لتطبيق الـ Caching لاحقاً
    Optional<Product> findByName(String name);

    /**
     * مهمة بلال وجلال: القفل التشاؤمي (Pessimistic Locking)
     * يستخدم عند تحديث المخزون تحت ضغط عالٍ جداً لضمان عدم حدوث Race Condition.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithPessimisticLock(@Param("id") Long id);

    // التحقق من توفر الكمية قبل إتمام الطلب لضمان سلامة البيانات
    @Query("SELECT p.stockQuantity FROM Product p WHERE p.id = :id")
    Integer getStockQuantityById(@Param("id") Long id);
}
