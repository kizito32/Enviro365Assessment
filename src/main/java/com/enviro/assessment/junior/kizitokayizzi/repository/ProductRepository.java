package com.enviro.assessment.junior.kizitokayizzi.repository;

import com.enviro.assessment.junior.kizitokayizzi.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByInvestorIdOrderById(Long investorId);

    // Row lock so two simultaneous withdrawals cannot both pass the balance check
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :productId and p.investor.id = :investorId")
    Optional<Product> findOwnedForUpdate(@Param("productId") Long productId,
                                         @Param("investorId") Long investorId);
}
