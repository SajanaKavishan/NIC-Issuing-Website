package com.project.nic.controller;

import com.project.nic.dto.ApiDtos.LostNicDto;
import com.project.nic.dto.ApiDtos.NewNicFormDto;
import com.project.nic.dto.ApiDtos.RenewNicDto;
import com.project.nic.service.AuthSessionService;
import com.project.nic.service.LostNicService;
import com.project.nic.service.NewNicFormService;
import com.project.nic.service.RenewNicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private NewNicFormService newNicFormService;

    @Autowired
    private RenewNicService renewNicService;

    @Autowired
    private LostNicService lostNicService;

    @GetMapping("/mine")
    public ResponseEntity<?> getMyApplications(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        Optional<AuthSessionService.SessionUser> sessionUser = authSessionService.findByToken(token);
        if (sessionUser.isEmpty()) {
            return ResponseEntity.status(403).body("Login required");
        }

        Long userId = sessionUser.get().userId();
        Map<String, Object> applications = new LinkedHashMap<>();
        applications.put("newNic", newNicFormService.findByUserId(userId).stream().map(NewNicFormDto::from).collect(Collectors.toList()));
        applications.put("renewNic", renewNicService.findByUserId(userId).stream().map(RenewNicDto::from).collect(Collectors.toList()));
        applications.put("lostNic", lostNicService.findByUserId(userId).stream().map(LostNicDto::from).collect(Collectors.toList()));

        return ResponseEntity.ok(applications);
    }
}
