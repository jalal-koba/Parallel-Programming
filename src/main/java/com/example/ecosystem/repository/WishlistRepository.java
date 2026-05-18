package com.example.ecosystem.repository;

import com.example.ecosystem.Entity.Wishlist;
import com.example.ecosystem.Entity.User;
import com.example.ecosystem.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    // استرجاع قائمة الرغبات الخاصة بمستخدم معين
    List<Wishlist> findByUser(User user);

    // البحث عن منتج معين داخل قائمة رغبات المستخدم (لمنع التكرار)
    Optional<Wishlist> findByUserAndProduct(User user, Product product);

    // حذف منتج معين من قائمة الرغبات
    void deleteByUserAndProduct(User user, Product product);

    // حساب عدد المرات التي أضيف فيها منتج معين لقوائم الرغبات
    // مفيد لمهمة "علي" في تحليل "الاختناقات" والطلب المتوقع
    Long countByProduct(Product product);
}
