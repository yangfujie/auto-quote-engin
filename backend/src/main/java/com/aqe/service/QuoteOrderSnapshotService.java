package com.aqe.service;

import com.aqe.model.entity.QuoteOrderSnapshot;
import com.aqe.repository.QuoteOrderSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuoteOrderSnapshotService {
    @Autowired
    private QuoteOrderSnapshotRepository repository;

    public QuoteOrderSnapshot save(QuoteOrderSnapshot snapshot) {
        return repository.save(snapshot);
    }

    public List<QuoteOrderSnapshot> findByStrategyInstanceId(Long strategyInstanceId) {
        return repository.findByStrategyInstanceIdOrderByCreateTimeDesc(strategyInstanceId);
    }
}
