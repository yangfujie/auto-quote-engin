package com.aqe.controller;

import com.aqe.engine.StrategyChainLoader;
import com.aqe.model.entity.StrategyDef;
import com.aqe.repository.StrategyDefRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 引擎管理接口
 * 提供策略链热加载/刷新功能
 */
@Api(tags = "引擎管理")
@RestController
@RequestMapping("/api/engine")
public class EngineController {

    @Autowired
    private StrategyChainLoader chainLoader;

    @Autowired
    private StrategyDefRepository strategyDefRepository;

    @ApiOperation("重新加载所有策略链（热刷新）")
    @PostMapping("/reload")
    public String reloadAll() {
        chainLoader.loadAllChains();
        return "All chains reloaded successfully";
    }

    @ApiOperation("重新加载指定策略链")
    @PostMapping("/reload/{defId}")
    public String reloadOne(@PathVariable Long defId) {
        StrategyDef def = strategyDefRepository.findById(defId)
                .orElseThrow(() -> new RuntimeException("Strategy def not found: " + defId));
        chainLoader.reloadChain(def);
        return "Chain reloaded: chain_" + defId;
    }
}
