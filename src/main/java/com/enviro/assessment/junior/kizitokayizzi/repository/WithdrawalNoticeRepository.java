package com.enviro.assessment.junior.kizitokayizzi.repository;

import com.enviro.assessment.junior.kizitokayizzi.entity.WithdrawalNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WithdrawalNoticeRepository extends JpaRepository<WithdrawalNotice, Long> {

    // "from" is inclusive, "to" is exclusive; productId is optional
    @Query("""
            select w from WithdrawalNotice w
            join fetch w.product p
            where p.investor.id = :investorId
              and (:productId is null or p.id = :productId)
              and w.createdAt >= :from
              and w.createdAt < :to
            order by w.createdAt desc, w.id desc
            """)
    List<WithdrawalNotice> search(@Param("investorId") Long investorId,
                                  @Param("productId") Long productId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);
}
