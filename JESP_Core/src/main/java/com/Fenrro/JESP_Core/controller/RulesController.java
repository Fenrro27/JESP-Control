package com.Fenrro.JESP_Core.controller;

import com.Fenrro.JESP_Core.service.RuleService;
import com.Fenrro.JESP_Core.service.RulesEngine;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RulesController {

    private final RuleService ruleService;
    private final RulesEngine rulesEngine;

    @GetMapping
    public List<RuleService.RuleDto> list() {
        return ruleService.list();
    }

    @PostMapping
    public RuleService.RuleDto create(@Valid @RequestBody RuleService.RuleDto rule) {
        return ruleService.create(rule);
    }

    @PutMapping("/{id}")
    public RuleService.RuleDto update(@PathVariable Long id,
                                      @Valid @RequestBody RuleService.RuleDto rule) {
        return ruleService.update(id, rule);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ruleService.delete(id);
        rulesEngine.evaluateAll(false);
        return ResponseEntity.ok().build();
    }

    /** Fuerza una evaluación inmediata de todas las reglas. */
    @PostMapping("/evaluate")
    public String evaluate() {
        rulesEngine.evaluateAll(false);
        return "OK";
    }
}
