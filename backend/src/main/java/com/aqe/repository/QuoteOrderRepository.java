package com.aqe.repository;

import com.aqe.model.entity.QuoteOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuoteOrderRepository extends JpaRepository<QuoteOrder, Long> {
    List<QuoteOrder> findTop100ByOrderByCreateTimeDesc();
}
