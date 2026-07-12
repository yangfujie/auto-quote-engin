package com.aqe.service;

import com.aqe.model.entity.StrategyDef;
import com.aqe.repository.StrategyDefRepository;
import com.aqe.repository.StrategyInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StrategyDefService {
    @Autowired
    private StrategyDefRepository repository;
    public StrategyDef save(StrategyDef def) {
        return repository.save(def);

    }

    public List<StrategyDef> findAll() {
        return repository.findAll();
    }
}
