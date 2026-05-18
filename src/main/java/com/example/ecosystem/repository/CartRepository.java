package com.example.ecosystem.repository;

import com.example.ecosystem.Entity.Cart;
import com.example.ecosystem.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // استرجاع العربة الخاصة بمستخدم معين
    Optional<Cart> findByUser(User user);

    // استرجاع العربة باستخدام معرف المستخدم مباشرة (مفيد لتقليل عدد الاستعلامات)
    Optional<Cart> findByUserId(Long userId);
}
