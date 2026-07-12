package com.aqe.repository;

import com.aqe.model.entity.QuoteOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteOrderRepository extends JpaRepository<QuoteOrder, Long> {

}
