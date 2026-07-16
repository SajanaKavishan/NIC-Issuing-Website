package com.project.nic.service;

import com.project.nic.model.NewNicForm;
import com.project.nic.repository.NewNicFormRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NewNicFormService {

    @Autowired
    private NewNicFormRepository repository;

    public NewNicForm save(NewNicForm form) {
        return repository.save(form);
    }
}
