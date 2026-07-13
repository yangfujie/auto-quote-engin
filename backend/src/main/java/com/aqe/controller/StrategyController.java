// StrategyController.java
package com.aqe.controller;

import com.aqe.model.entity.StrategyDef;
import com.aqe.model.entity.StrategyInstance;
import com.aqe.service.StrategyDefService;
import com.aqe.service.StrategyInstanceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "策略管理")
@RestController
@RequestMapping("/api/strategy")
public class StrategyController {
    @Autowired private StrategyDefService defService;
    @Autowired private StrategyInstanceService instanceService;

    @ApiOperation("保存策略定义")
    @PostMapping("/def")
    public StrategyDef saveDef(@RequestBody StrategyDef def) {
        return defService.save(def);
    }

    @ApiOperation("查询所有策略定义")
    @GetMapping("/def")
    public List<StrategyDef> listDef() {
        return defService.findAll();
    }

    @ApiOperation("创建策略实例")
    @PostMapping("/instance")
    public StrategyInstance createInstance(@RequestBody StrategyInstance instance) {
        return instanceService.save(instance);
    }

    @ApiOperation("查询所有策略实例")
    @GetMapping("/instance")
    public List<StrategyInstance> listInstances() {
        return instanceService.findAll();
    }

    @ApiOperation("更新策略实例状态")
    @PutMapping("/instance/{id}/status")
    public void updateStatus(@ApiParam("实例ID") @PathVariable Long id,
                             @ApiParam("状态值") @RequestParam Integer status) {
        instanceService.updateStatus(id, status);
    }
}
