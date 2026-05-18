package com.example.ecosystem.repository;

import com.example.ecosystem.Entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // البحث عن تصنيف بواسطة الاسم (مفيد عند إضافة منتجات جديدة أو الفلترة)
    Optional<Category> findByName(String name);

    // التحقق من وجود الاسم مسبقاً لضمان سلامة البيانات (Data Integrity)
    Boolean existsByName(String name);
}
