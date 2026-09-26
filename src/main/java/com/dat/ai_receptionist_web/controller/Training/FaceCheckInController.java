package com.dat.ai_receptionist_web.controller.Training;

import com.dat.ai_receptionist_web.dto.Training.FaceCheckInResponse;
import com.dat.ai_receptionist_web.service.Training.FaceCheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/training")
@RequiredArgsConstructor
public class FaceCheckInController {
    private final FaceCheckInService service;

    @PostMapping(value = "/face-check-in", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SESSION_ATTENDANCE_CREATE.getCode())"
            + " or hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COACH_TIMESHEET_CREATE.getCode())")
    public FaceCheckInResponse checkIn(@RequestPart("file") MultipartFile file) {
        return service.checkIn(file);
    }
}
