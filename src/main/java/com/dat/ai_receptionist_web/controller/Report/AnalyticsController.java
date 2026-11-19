package com.dat.ai_receptionist_web.controller.Report;

import com.dat.ai_receptionist_web.dto.Analytics.AttendanceRiskAnalyticsDTO;
import com.dat.ai_receptionist_web.dto.Analytics.BranchAnalyticsDTO;
import com.dat.ai_receptionist_web.dto.Analytics.ClassHealthAnalyticsDTO;
import com.dat.ai_receptionist_web.dto.Analytics.CoachAnalyticsDTO;
import com.dat.ai_receptionist_web.dto.Analytics.CoursePlanAnalyticsDTO;
import com.dat.ai_receptionist_web.dto.Analytics.DevelopmentAnalyticsDTO;
import com.dat.ai_receptionist_web.dto.Analytics.FinanceAnalyticsDTO;
import com.dat.ai_receptionist_web.dto.Analytics.OverviewAnalyticsDTO;
import com.dat.ai_receptionist_web.dto.Analytics.RecruitmentAnalyticsDTO;
import com.dat.ai_receptionist_web.dto.Analytics.RetentionAnalyticsDTO;
import com.dat.ai_receptionist_web.dto.Analytics.StudentMovementAnalyticsDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Report.AnalyticsService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    /**
     * A01.
     */
    @GetMapping("/overview")
    public OverviewAnalyticsDTO.Response overview(
            @ModelAttribute OverviewAnalyticsDTO.Request request
    ) {
        return analyticsService.overview(request);
    }

    /**
     * A02.
     */
    @GetMapping("/student-movements")
    public StudentMovementAnalyticsDTO.Response studentMovements(
            @ModelAttribute StudentMovementAnalyticsDTO.Request request
    ) {
        return analyticsService.studentMovements(request);
    }

    /**
     * A03.
     */
    @GetMapping("/retention")
    public RetentionAnalyticsDTO.Response retention(
            @ModelAttribute RetentionAnalyticsDTO.Request request
    ) {
        return analyticsService.retention(request);
    }

    /**
     * A04.
     */
    @GetMapping("/classes")
    public ClassHealthAnalyticsDTO.Response classes(
            @ModelAttribute ClassHealthAnalyticsDTO.Request request
    ) {
        return analyticsService.classes(request);
    }

    /**
     * A05.
     */
    @GetMapping("/attendance-risk")
    public AttendanceRiskAnalyticsDTO.Response attendanceRisk(
            @ModelAttribute AttendanceRiskAnalyticsDTO.Request request
    ) {
        return analyticsService.attendanceRisk(request);
    }

    /**
     * A06.
     */
    @GetMapping("/recruitment")
    public RecruitmentAnalyticsDTO.Response recruitment(
            @ModelAttribute RecruitmentAnalyticsDTO.Request request
    ) {
        return analyticsService.recruitment(request);
    }

    /**
     * A07.
     */
    @GetMapping("/courses-plans")
    public CoursePlanAnalyticsDTO.Response coursePlans(
            @ModelAttribute CoursePlanAnalyticsDTO.Request request
    ) {
        return analyticsService.coursePlans(request);
    }

    /**
     * A08.
     */
    @GetMapping("/branches")
    public BranchAnalyticsDTO.Response branches(
            @ModelAttribute BranchAnalyticsDTO.Request request
    ) {
        return analyticsService.branches(request);
    }

    /**
     * A09.
     */
    @GetMapping("/coaches")
    public CoachAnalyticsDTO.Response coaches(
            @ModelAttribute CoachAnalyticsDTO.Request request
    ) {
        return analyticsService.coaches(request);
    }

    /**
     * A10.
     */
    @GetMapping("/development")
    public DevelopmentAnalyticsDTO.Response development(
            @ModelAttribute DevelopmentAnalyticsDTO.Request request
    ) {
        return analyticsService.development(request);
    }

    /**
     * A11.
     */
    @GetMapping("/finance")
    public FinanceAnalyticsDTO.Response finance(
            @ModelAttribute FinanceAnalyticsDTO.Request request
    ) {
        return analyticsService.finance(request);
    }

    /**
     * A12.
     */
    @GetMapping("/overview/movements")
    public PageResponse<OverviewAnalyticsDTO.MovementDetailResponse> overviewMovements(
            @ModelAttribute OverviewAnalyticsDTO.MovementDetailRequest request
    ) {
        return analyticsService.overviewMovements(request);
    }

    /**
     * A13.
     */
    @GetMapping("/student-movements/students")
    public PageResponse<StudentMovementAnalyticsDTO.StudentsResponse> studentMovementStudents(
            @ModelAttribute StudentMovementAnalyticsDTO.StudentsRequest request
    ) {
        return analyticsService.studentMovementStudents(request);
    }

    /**
     * A14.
     */
    @GetMapping("/students/{personId}/timeline")
    public StudentMovementAnalyticsDTO.TimelineResponse studentTimeline(
            @PathVariable UUID personId,
            @ModelAttribute StudentMovementAnalyticsDTO.TimelineRequest request
    ) {
        return analyticsService.studentTimeline(personId, request);
    }

    /**
     * A15.
     */
    @GetMapping("/retention/students")
    public PageResponse<RetentionAnalyticsDTO.StudentsResponse> retentionStudents(
            @ModelAttribute RetentionAnalyticsDTO.StudentsRequest request
    ) {
        return analyticsService.retentionStudents(request);
    }

    /**
     * A16.
     */
    @GetMapping("/classes/{courseScheduleId}")
    public ClassHealthAnalyticsDTO.DetailResponse classDetail(
            @PathVariable UUID courseScheduleId,
            @ModelAttribute ClassHealthAnalyticsDTO.DetailRequest request
    ) {
        return analyticsService.classDetail(courseScheduleId, request);
    }

    /**
     * A17.
     */
    @GetMapping("/classes/{courseScheduleId}/students")
    public PageResponse<ClassHealthAnalyticsDTO.StudentsResponse> classStudents(
            @PathVariable UUID courseScheduleId,
            @ModelAttribute ClassHealthAnalyticsDTO.StudentsRequest request
    ) {
        return analyticsService.classStudents(courseScheduleId, request);
    }

    /**
     * A18.
     */
    @GetMapping("/attendance-risk/students")
    public PageResponse<AttendanceRiskAnalyticsDTO.StudentsResponse> attendanceRiskStudents(
            @ModelAttribute AttendanceRiskAnalyticsDTO.StudentsRequest request
    ) {
        return analyticsService.attendanceRiskStudents(request);
    }

    /**
     * A19.
     */
    @GetMapping("/recruitment/students")
    public PageResponse<RecruitmentAnalyticsDTO.StudentsResponse> recruitmentStudents(
            @ModelAttribute RecruitmentAnalyticsDTO.StudentsRequest request
    ) {
        return analyticsService.recruitmentStudents(request);
    }

    /**
     * A20.
     */
    @GetMapping("/courses-plans/{trainingPlanId}/students")
    public PageResponse<CoursePlanAnalyticsDTO.StudentsResponse> trainingPlanStudents(
            @PathVariable UUID trainingPlanId,
            @ModelAttribute CoursePlanAnalyticsDTO.StudentsRequest request
    ) {
        return analyticsService.trainingPlanStudents(trainingPlanId, request);
    }

    /**
     * A21.
     */
    @GetMapping("/branches/{branchId}/classes")
    public PageResponse<BranchAnalyticsDTO.ClassesResponse> branchClasses(
            @PathVariable Long branchId,
            @ModelAttribute BranchAnalyticsDTO.ClassesRequest request
    ) {
        return analyticsService.branchClasses(branchId, request);
    }

    /**
     * A22.
     */
    @GetMapping("/coaches/{staffPersonId}/sessions")
    public PageResponse<CoachAnalyticsDTO.SessionsResponse> coachSessions(
            @PathVariable UUID staffPersonId,
            @ModelAttribute CoachAnalyticsDTO.SessionsRequest request
    ) {
        return analyticsService.coachSessions(staffPersonId, request);
    }

    /**
     * A23.
     */
    @GetMapping("/development/students")
    public PageResponse<DevelopmentAnalyticsDTO.StudentsResponse> developmentStudents(
            @ModelAttribute DevelopmentAnalyticsDTO.StudentsRequest request
    ) {
        return analyticsService.developmentStudents(request);
    }

    /**
     * A24.
     */
    @GetMapping("/finance/transactions")
    public PageResponse<FinanceAnalyticsDTO.TransactionsResponse> financeTransactions(
            @ModelAttribute FinanceAnalyticsDTO.TransactionsRequest request
    ) {
        return analyticsService.financeTransactions(request);
    }
}
