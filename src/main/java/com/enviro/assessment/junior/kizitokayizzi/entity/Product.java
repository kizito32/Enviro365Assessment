package com.enviro.assessment.junior.kizitokayizzi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investor_id")
    private Investor investor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType type;

    @Column(nullable = false)
    private String name;

    // BigDecimal, never double, for money
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    protected Product() {
        // required by JPA
    }

    public Product(Investor investor, ProductType type, String name, BigDecimal balance) {
        this.investor = investor;
        this.type = type;
        this.name = name;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public Investor getInvestor() {
        return investor;
    }

    public ProductType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
