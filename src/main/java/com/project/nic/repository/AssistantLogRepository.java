package com.project.nic.repository;

import com.project.nic.model.AssistantLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssistantLogRepository extends JpaRepository<AssistantLog, Long> {
}

