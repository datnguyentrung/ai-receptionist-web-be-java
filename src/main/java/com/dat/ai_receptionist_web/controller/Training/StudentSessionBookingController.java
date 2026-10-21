package com.dat.ai_receptionist_web.controller.Training;

import com.dat.ai_receptionist_web.dto.Training.StudentSessionBookingDTO;
import com.dat.ai_receptionist_web.service.Training.StudentSessionBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/student-enrollments/{enrollmentId}/bookings")
@RequiredArgsConstructor
public class StudentSessionBookingController {
    private final StudentSessionBookingService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_UPDATE.getCode())")
    public StudentSessionBookingDTO.Response book(@PathVariable UUID enrollmentId,
                                                  @Valid @RequestBody StudentSessionBookingDTO.CreateRequest request) {
        return service.bookFlexible(enrollmentId, request.classSessionId());
    }

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_READ.getCode())")
    public List<StudentSessionBookingDTO.Response> list(@PathVariable UUID enrollmentId) {
        return service.list(enrollmentId);
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_READ.getCode())")
    public List<StudentSessionBookingDTO.SessionOption> options(@PathVariable UUID enrollmentId,
            @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.options(enrollmentId, from, to);
    }

    @GetMapping("/leave-options")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_READ.getCode())")
    public List<StudentSessionBookingDTO.Response> leaveOptions(@PathVariable UUID enrollmentId) {
        return service.leaveOptions(enrollmentId);
    }

    @GetMapping("/makeup-options")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_READ.getCode())")
    public List<StudentSessionBookingDTO.SessionOption> makeupOptions(@PathVariable UUID enrollmentId,
                                                                        @RequestParam UUID leaveBookingId) {
        return service.makeupOptions(enrollmentId, leaveBookingId);
    }

    @DeleteMapping("/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_UPDATE.getCode())")
    public void cancel(@PathVariable UUID enrollmentId, @PathVariable UUID bookingId) {
        service.cancel(enrollmentId, bookingId);
    }
}
