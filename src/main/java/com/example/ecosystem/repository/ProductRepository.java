package com.example.ecosystem.repository;

import com.example.ecosystem.Entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 🔥 حل مشكلة الاختناق (Bottleneck): منع الـ N+1 Queries
     * يجلب جميع المنتجات مع الـ Category الخاصة بها في طلب SQL واحد مشترك (JOIN FETCH)
     * بدلاً من ضرب قاعدة البيانات بـ 1000 طلب منفصل لكل منتج تحت الضغط العالي.
     */
    @Query("SELECT p FROM Product p JOIN FETCH p.category")
    List<Product> findAllWithCategory();

    // البحث العادي - يستخدمه "علي" لتطبيق الـ Caching لاحقاً
    Optional<Product> findByName(String name);

    List<Product> findByCategoryId(Long categoryId);

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