package com.dat.ai_receptionist_web.controller.Training;

import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.ClassSessionDTO;
import com.dat.ai_receptionist_web.service.Training.session.ClassSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/class-sessions")
@RequiredArgsConstructor
public class ClassSessionController {
    private final ClassSessionService service;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).CLASS_SESSION_READ.getCode())")
    public PageResponse<ClassSessionDTO.SimpleResponse> list(Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).CLASS_SESSION_READ.getCode())")
    public ClassSessionDTO.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).CLASS_SESSION_CREATE.getCode())")
    public ClassSessionDTO.Response create(@Valid @RequestBody ClassSessionDTO.CreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).CLASS_SESSION_UPDATE.getCode())")
    public ClassSessionDTO.Response update(
            @PathVariable UUID id,
            @Valid @RequestBody ClassSessionDTO.UpdateRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).CLASS_SESSION_DELETE.getCode())")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/{id}/attendance-reopen")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).CLASS_SESSION_UPDATE.getCode())")
    public ClassSessionDTO.Response reopenAttendance(
            @PathVariable UUID id,
            @Valid @RequestBody ClassSessionDTO.ReopenAttendanceRequest request
    ) {
        return service.reopenAttendance(id, request);
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).CLASS_SESSION_READ.getCode())")
    public List<ClassSessionDTO.CalendarResponse> calendar(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate
    ) {
        return service.getCalendar(fromDate, toDate);
    }
}
