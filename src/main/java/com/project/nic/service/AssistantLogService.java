package com.project.nic.service;

import com.project.nic.model.AssistantLog;
import com.project.nic.repository.AssistantLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssistantLogService {

    @Autowired
    private AssistantLogRepository assistantLogRepository;

    public List<AssistantLog> getAll() { return assistantLogRepository.findAll(); }

    public Optional<AssistantLog> getById(Long id) { return assistantLogRepository.findById(id); }

    public AssistantLog save(AssistantLog log) { return assistantLogRepository.save(log); }

    public void delete(Long id) { assistantLogRepository.deleteById(id); }
}

