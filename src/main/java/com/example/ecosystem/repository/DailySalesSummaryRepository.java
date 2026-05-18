package com.example.ecosystem.repository;

import com.example.ecosystem.Entity.DailySalesSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailySalesSummaryRepository extends JpaRepository<DailySalesSummary, Long> {
    List<DailySalesSummary> findByReportDate(LocalDate reportDate);

    Optional<DailySalesSummary> findByReportDateAndProductId(LocalDate reportDate, Long productId);

    @Modifying(clearAutomatically = true)
    @Transactional
    void deleteByReportDate(LocalDate reportDate);
}
