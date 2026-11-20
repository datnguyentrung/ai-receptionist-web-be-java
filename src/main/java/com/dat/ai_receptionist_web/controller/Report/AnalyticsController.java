package com.dat.ai_receptionist_web.controller.Report;

import com.dat.ai_receptionist_web.dto.Analytics.StudentMovementAnalyticsDTO;
import com.dat.ai_receptionist_web.service.Report.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @GetMapping("/student-movements")
    public StudentMovementAnalyticsDTO.Response studentMovements(
            @ModelAttribute StudentMovementAnalyticsDTO.Request request
    ) {
        return analyticsService.studentMovements(request);
    }

}
