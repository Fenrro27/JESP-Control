package com.Fenrro.JESP_Core.repository;

import com.Fenrro.JESP_Core.entity.RuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleRepository extends JpaRepository<RuleEntity, Long> {
    List<RuleEntity> findAllByOrderByPriorityAscIdAsc();
    List<RuleEntity> findByEnabledTrueOrderByPriorityAscIdAsc();
}
