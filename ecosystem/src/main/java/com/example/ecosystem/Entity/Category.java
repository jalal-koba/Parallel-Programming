package com.example.ecosystem.Entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // هذه العلاقة اختيارية، نستخدمها إذا أردنا الوصول للمنتجات من خلال التصنيف
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    private List<Product> products;

    // الـ Constructors
    public Category() {
    }

    public Category(String name) {
        this.name = name;
    }

    // الـ Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }
}