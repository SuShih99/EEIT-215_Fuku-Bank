package com.javaeasybank.risk.repository;

import com.javaeasybank.risk.entity.ReviewTask;
import com.javaeasybank.risk.enums.BusinessScene;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewTaskRepository extends JpaRepository<ReviewTask, Long> {

    @Query("SELECT t FROM ReviewTask t WHERE " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:scene IS NULL OR t.scene = :scene) AND " +
            "(:priority IS NULL OR t.priority = :priority)")
    Page<ReviewTask> findByFilter(
            @Param("status") String status,
            @Param("scene") BusinessScene scene,
            @Param("priority") Integer priority,
            Pageable pageable);

}

