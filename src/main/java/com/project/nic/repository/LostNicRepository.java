
package com.project.nic.repository;

import com.project.nic.model.LostNic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LostNicRepository extends JpaRepository<LostNic, Long> {
}
