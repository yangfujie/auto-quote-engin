package com.aqe.repository;


import com.aqe.model.entity.StrategyInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StrategyInstanceRepository extends JpaRepository<StrategyInstance, Long> {
    List<StrategyInstance> findBySymbolAndStatus(String symbol, int i);
}
