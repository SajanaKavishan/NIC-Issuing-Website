
package com.project.nic.repository;

import com.project.nic.model.LostNic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LostNicRepository extends JpaRepository<LostNic, Long> {
    List<LostNic> findByUserId(Long userId);
    Optional<LostNic> findFirstByNicNumberIgnoreCaseOrderByIdDesc(String nicNumber);
}
