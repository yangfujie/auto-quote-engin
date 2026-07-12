// StrategyController.java
package com.aqe.controller;

import com.aqe.model.entity.StrategyDef;
import com.aqe.model.entity.StrategyInstance;
import com.aqe.service.StrategyDefService;
import com.aqe.service.StrategyInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/strategy")
public class StrategyController {
    @Autowired private StrategyDefService defService;
    @Autowired private StrategyInstanceService instanceService;

    @PostMapping("/def")
    public StrategyDef saveDef(@RequestBody StrategyDef def) {
        return defService.save(def);
    }

    @GetMapping("/def")
    public List<StrategyDef> listDef() {
        return defService.findAll();
    }

    @PostMapping("/instance")
    public StrategyInstance createInstance(@RequestBody StrategyInstance instance) {
        return instanceService.save(instance);
    }

    @GetMapping("/instance")
    public List<StrategyInstance> listInstances() {
        return instanceService.findAll();
    }

    @PutMapping("/instance/{id}/status")
    public void updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        instanceService.updateStatus(id, status);
    }
}
