package com.locomotora.demo.activity;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ActivityController {
    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /** La app esta abierta. {@code tz}: zona del telefono, para sumar el tiempo a su dia local. */
    @PostMapping("/activity/ping")
    public ResponseEntity<Void> ping(@RequestParam(required = false) String tz) {
        activityService.ping(tz);
        return ResponseEntity.noContent().build();
    }
}
