package com.javaeasybank.risk.repository;

import com.javaeasybank.risk.enums.BlacklistType;
import com.javaeasybank.risk.entity.Blacklist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlackListRepository extends JpaRepository<Blacklist, Long> {
    //用於攔截單筆
    @Query("SELECT b FROM Blacklist b WHERE b.listType = :type AND b.listValue = :value AND b.status = true AND (b.expireAt IS NULL OR b.expireAt > CURRENT_TIMESTAMP)")
    Optional<Blacklist> findActiveBlacklist(BlacklistType type, String value);

    @Query("""
        SELECT b FROM Blacklist b
        WHERE (:activated IS NULL OR b.status = :activated)
        AND (
            :activated IS NULL 
            OR :activated = false 
            OR (b.expireAt IS NULL OR b.expireAt > CURRENT_TIMESTAMP)
        )
    """)
    Page<Blacklist> findByFilter(@Param("activated") Boolean activated, Pageable pageable);

    @Query("SELECT b FROM Blacklist b WHERE b.listType = :type AND b.listValue = :value")
    Optional<Blacklist> findByBusinessKey(BlacklistType type, String value);

    @Query("""
    SELECT b.listType FROM Blacklist b 
    WHERE b.status = true 
    AND (b.expireAt IS NULL OR b.expireAt > CURRENT_TIMESTAMP)
    AND (
        (b.listType = 'ID_CARD' AND b.listValue = :idCard) OR
        (b.listType = 'PHONE' AND b.listValue = :phone) OR
        (b.listType = 'EMAIL' AND b.listValue = :email)
    )
""")
    List<BlacklistType> findHitTypes(
            @Param("idCard") String idCard,
            @Param("phone") String phone,
            @Param("email") String email
    );
}
