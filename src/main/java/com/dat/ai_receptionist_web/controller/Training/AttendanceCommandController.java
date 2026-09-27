package com.dat.ai_receptionist_web.controller.Training;

import com.dat.ai_receptionist_web.dto.Training.command.AttendanceCommandDTO;
import com.dat.ai_receptionist_web.service.Training.command.AttendanceCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance-commands")
@RequiredArgsConstructor
public class AttendanceCommandController {
    private final AttendanceCommandService service;

    @GetMapping("/{requestId}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SESSION_ATTENDANCE_READ.getCode())"
            + " or hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COACH_TIMESHEET_READ.getCode())")
    public AttendanceCommandDTO.Response get(@PathVariable UUID requestId) {
        return service.get(requestId);
    }
}
