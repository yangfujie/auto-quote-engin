package com.aqe.repository;

import com.aqe.model.entity.QuoteOrderSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuoteOrderSnapshotRepository extends JpaRepository<QuoteOrderSnapshot, Long> {
    List<QuoteOrderSnapshot> findByStrategyInstanceIdOrderByCreateTimeDesc(Long strategyInstanceId);
}
