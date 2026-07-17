package com.aqe.service;

import com.aqe.engine.FlowJsonToElConverter;
import com.aqe.engine.StrategyChainLoader;
import com.aqe.model.entity.StrategyDef;
import com.aqe.repository.StrategyDefRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StrategyDefService {

    @Autowired
    private StrategyDefRepository repository;

    @Autowired
    private FlowJsonToElConverter elConverter;

    @Autowired
    private StrategyChainLoader chainLoader;

    /**
     * 保存策略定义
     * 自动将 flow_json 转换为 LiteFlow EL 表达式，并热加载到 FlowBus
     */
    public StrategyDef save(StrategyDef def) {
        if (def.getFlowJson() != null && !def.getFlowJson().isEmpty()) {
            String el = elConverter.convert(def.getFlowJson());
            def.setChainExpression(el);
        }
        StrategyDef saved = repository.save(def);
        // 热加载：更新/注册 LiteFlow Chain
        chainLoader.reloadChain(saved);
        return saved;
    }

    public List<StrategyDef> findAll() {
        return repository.findAll();
    }
}
