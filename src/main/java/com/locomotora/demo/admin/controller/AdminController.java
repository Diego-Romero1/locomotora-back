package com.locomotora.demo.admin.controller;

import com.locomotora.demo.admin.dto.AdminDtos.StatsResponse;
import com.locomotora.demo.admin.dto.AdminDtos.StatusRequest;
import com.locomotora.demo.admin.dto.AdminDtos.UserDetail;
import com.locomotora.demo.admin.dto.AdminDtos.UserPage;
import com.locomotora.demo.admin.service.AdminService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** "Actividad de usuarios" (solo rol ADMIN). {@code tz}: zona del telefono para los dias de los graficos. */
@RestController
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/admin/stats")
    public StatsResponse stats(@RequestParam(required = false) String tz) {
        return adminService.stats(tz);
    }

    @GetMapping("/admin/users")
    public UserPage users(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String tz
    ) {
        return adminService.users(q, status, page, tz);
    }

    @GetMapping("/admin/users/{id}")
    public UserDetail user(@PathVariable UUID id, @RequestParam(required = false) String tz) {
        return adminService.user(id, tz);
    }

    @PatchMapping("/admin/users/{id}/status")
    public UserDetail updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusRequest request,
            @RequestParam(required = false) String tz
    ) {
        return adminService.updateStatus(id, request, tz);
    }
}
