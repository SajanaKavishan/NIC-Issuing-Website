
package com.project.nic.service;

import com.project.nic.model.RenewNic;
import com.project.nic.repository.RenewNicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RenewNicService {
    @Autowired
    private RenewNicRepository repository;

    public RenewNic save(RenewNic renewNic) {
        return repository.save(renewNic);
    }

    public List<RenewNic> findAll() {
        return repository.findAll();
    }
}
