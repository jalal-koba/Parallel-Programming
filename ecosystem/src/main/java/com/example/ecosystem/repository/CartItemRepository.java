package com.example.ecosystem.repository;

import com.example.ecosystem.Entity.CartItem;
import com.example.ecosystem.Entity.Cart;
import com.example.ecosystem.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // البحث عن منتج معين داخل عربة معينة (لتحديث الكمية بدلاً من إضافة عنصر جديد)
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

    // حذف كافة العناصر عند إتمام الطلب وتحويل العربة إلى طلب فعلي (Order)
    void deleteByCart(Cart cart);
}
