package com.project.nic.repository;

import com.project.nic.model.AssistanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssistanceRequestRepository extends JpaRepository<AssistanceRequest, Long> {
    List<AssistanceRequest> findByStatus(String status);
    List<AssistanceRequest> findByUserId(Long userId);
    List<AssistanceRequest> findByEmail(String email);
}