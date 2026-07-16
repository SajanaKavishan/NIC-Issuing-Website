
package com.project.nic.repository;

import com.project.nic.model.LostNic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LostNicRepository extends JpaRepository<LostNic, Long> {
    List<LostNic> findByUserId(Long userId);
}
