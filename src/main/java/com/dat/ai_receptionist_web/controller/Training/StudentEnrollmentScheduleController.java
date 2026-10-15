package com.dat.ai_receptionist_web.controller.Training;

import com.dat.ai_receptionist_web.dto.Training.StudentEnrollmentScheduleDTO;
import com.dat.ai_receptionist_web.service.Training.StudentEnrollmentScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student-enrollment-schedules")
@RequiredArgsConstructor
public class StudentEnrollmentScheduleController {
    private final StudentEnrollmentScheduleService service;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_READ.getCode())")
    public List<StudentEnrollmentScheduleDTO.SimpleResponse> list(@RequestParam UUID studentEnrollmentId) {
        return service.list(studentEnrollmentId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_READ.getCode())")
    public StudentEnrollmentScheduleDTO.Response get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_UPDATE.getCode())")
    public StudentEnrollmentScheduleDTO.Response create(
            @Valid @RequestBody StudentEnrollmentScheduleDTO.CreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_UPDATE.getCode())")
    public StudentEnrollmentScheduleDTO.Response update(
            @PathVariable UUID id, @Valid @RequestBody StudentEnrollmentScheduleDTO.UpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_UPDATE.getCode())")
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
