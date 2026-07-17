
package com.project.nic.repository;

import com.project.nic.model.RenewNic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RenewNicRepository extends JpaRepository<RenewNic, Long> {
    List<RenewNic> findByUserId(Long userId);
    Optional<RenewNic> findFirstByOldNicNumberIgnoreCaseOrderByIdDesc(String oldNicNumber);
}
