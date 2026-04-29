package com.example.ecosystem.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private Float price;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Version // للتعامل مع العمليات المتوازية ومنع التضارب
    private Integer version;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    // Getters and Setters
}
