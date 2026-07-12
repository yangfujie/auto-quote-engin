package com.aqe.service;

import com.aqe.model.entity.QuoteOrder;
import com.aqe.repository.QuoteOrderRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuoteOrderService {
    @Autowired
    private QuoteOrderRepository repository;
    public QuoteOrder save(QuoteOrder order) {
        return repository.save(order);
    }

    public void update(QuoteOrder order) {

    }

    public List<QuoteOrder> findRecent(int i) {
        return null;
    }
}
