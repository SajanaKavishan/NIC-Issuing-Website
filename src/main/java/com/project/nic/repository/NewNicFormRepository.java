package com.project.nic.repository;

import com.project.nic.model.NewNicForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;

public interface NewNicFormRepository extends JpaRepository<NewNicForm, Long> {
    List<NewNicForm> findByUserId(Long userId);
    boolean existsByUserIdAndStatusIn(Long userId, Collection<String> statuses);
}
