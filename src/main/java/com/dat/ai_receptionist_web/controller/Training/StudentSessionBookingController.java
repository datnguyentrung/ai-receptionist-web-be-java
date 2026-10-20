package com.dat.ai_receptionist_web.controller.Training;

import com.dat.ai_receptionist_web.dto.Training.StudentSessionBookingDTO;
import com.dat.ai_receptionist_web.service.Training.StudentSessionBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    @DeleteMapping("/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_UPDATE.getCode())")
    public void cancel(@PathVariable UUID enrollmentId, @PathVariable UUID bookingId) {
        service.cancel(enrollmentId, bookingId);
    }
}
