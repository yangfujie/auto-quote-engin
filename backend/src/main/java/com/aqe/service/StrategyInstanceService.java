// StrategyInstanceService.java
package com.aqe.service;

import com.aqe.model.entity.StrategyInstance;
import com.aqe.repository.StrategyInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class StrategyInstanceService {
    @Autowired private StrategyInstanceRepository repository;

    public StrategyInstance save(StrategyInstance instance) {
        return repository.save(instance);
    }

    public List<StrategyInstance> findAll() {
        return repository.findAll();
    }

    public List<StrategyInstance> findRunningBySymbol(String symbol) {
        return repository.findBySymbolAndStatus(symbol, 1);
    }

    public void updateStatus(Long id, Integer status) {
        repository.findById(id).ifPresent(inst -> {
            inst.setStatus(status);
            repository.save(inst);
        });
    }
}