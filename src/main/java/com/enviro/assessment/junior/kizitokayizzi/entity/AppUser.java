package com.enviro.assessment.junior.kizitokayizzi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** Login account, linked to exactly one investor so a user can only ever see their own data. */
@Entity
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investor_id")
    private Investor investor;

    protected AppUser() {
        // required by JPA
    }

    public AppUser(String username, String passwordHash, Investor investor) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.investor = investor;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Investor getInvestor() {
        return investor;
    }
}
