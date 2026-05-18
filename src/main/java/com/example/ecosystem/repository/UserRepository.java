package com.example.ecosystem.repository;

import com.example.ecosystem.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // البحث عن مستخدم بواسطة البريد الإلكتروني (مهم لعملية تسجيل الدخول)
    Optional<User> findByEmail(String email);

    // البحث عن مستخدم بواسطة اسم المستخدم[cite: 2]
    Optional<User> findByUsername(String username);

    // التحقق من وجود البريد الإلكتروني مسبقاً لضمان سلامة البيانات (Data Integrity)
    Boolean existsByEmail(String email);

    // التحقق من وجود اسم المستخدم مسبقاً[cite: 2]
    Boolean existsByUsername(String username);
}
