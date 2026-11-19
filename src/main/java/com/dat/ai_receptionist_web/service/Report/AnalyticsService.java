package com.dat.ai_receptionist_web.service.Report;

import com.dat.ai_receptionist_web.dto.Analytics.*;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.repository.Report.AnalyticsRepository;
import com.dat.ai_receptionist_web.repository.Report.AnalyticsRepository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {
    private static final int REACTIVATION_GAP_DAYS = 30;
    private static final int CONSECUTIVE_ABSENCE_LIMIT = 3;
    private static final int DAYS_WITHOUT_ATTENDANCE_LIMIT = 14;
    private static final BigDecimal ATTENDANCE_DROP_THRESHOLD = new BigDecimal("0.20");
    private static final Set<String> ATTENDED_STATUSES =
            Set.of("PRESENT", "LATE", "MAKEUP");
    private static final Set<String> COMPLETED_SESSION_STATUSES =
            Set.of("COMPLETED", "TERMINATED");

    private final AnalyticsRepository repository;

    public OverviewAnalyticsDTO.Response overview(OverviewAnalyticsDTO.Request request) {
        Scope scope = scope(request.from(), request.to(), request.asOf());
        StudentQuery studentQuery = new StudentQuery(
                scope.from(), scope.to(), scope.asOf(),
                request.branchIds(), request.courseIds(), request.trainingPlanIds(),
                request.levels(), request.genders(), request.minAge(), request.maxAge(),
                request.belts(), null, null, null);
        List<StudentFact> students = repository.findStudentFacts(studentQuery);
        List<AttendanceFact> attendance = attendance(scope, request.branchIds(),
                request.courseIds(), null, request.levels(), request.belts(),
                request.genders(), request.minAge(), request.maxAge());
        List<ClassFact> classes = repository.findClassFacts(
                new ClassQuery(scope.from(), scope.to(), request.branchIds(),
                        request.courseIds(), null, request.levels(), null, null));
        List<FinanceFact> finance = finance(scope, request.branchIds(),
                request.trainingPlanIds(), null);

        Map<UUID, StudentFact> latest = latestByPerson(students);
        Map<UUID, RiskAssessment> risks = riskAssessments(latest.values(), attendance, scope.asOf());
        List<MovementEvent> events = movementEvents(students, scope);
        List<ClassStat> classStats = classStats(classes, students, attendance, scope);

        int activeStudents = activeCount(students, scope.asOf());
        int newStudents = countEventPeople(events, scope, "NEW");
        int reactivated = countEventPeople(events, scope, "REACTIVATED");
        int inferredLeavers = countEventPeople(events, scope, "INFERRED_LEFT");
        int netMovement = newStudents + reactivated - inferredLeavers;

        Set<UUID> activeAtFrom = activePeople(students, scope.from());
        Set<UUID> activeAtTo = activePeople(students, scope.to());
        long retained = activeAtFrom.stream().filter(activeAtTo::contains).count();

        List<OverviewAnalyticsDTO.MonthlyTrend> monthly = months(scope).stream()
                .map(month -> {
                    LocalDate monthEnd = month.atEndOfMonth();
                    return new OverviewAnalyticsDTO.MonthlyTrend(
                            month,
                            countEventPeople(events, month, "NEW"),
                            countEventPeople(events, month, "REACTIVATED"),
                            countEventPeople(events, month, "INFERRED_LEFT"),
                            countEventPeople(events, month, "NEW")
                                    + countEventPeople(events, month, "REACTIVATED")
                                    - countEventPeople(events, month, "INFERRED_LEFT"),
                            activeCount(students, monthEnd)
                    );
                })
                .toList();

        Map<Long, List<StudentFact>> byBranch = latest.values().stream()
                .filter(fact -> fact.branchId() != null)
                .collect(Collectors.groupingBy(StudentFact::branchId,
                        LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<AttendanceFact>> attendanceByBranch =
                attendance.stream().filter(f -> f.branchId() != null)
                        .collect(Collectors.groupingBy(AttendanceFact::branchId));
        Map<Long, Long> riskByBranch = risks.values().stream()
                .filter(risk -> risk.fact().branchId() != null && !"LOW".equals(risk.level()))
                .collect(Collectors.groupingBy(
                        risk -> risk.fact().branchId(), Collectors.counting()));
        List<OverviewAnalyticsDTO.BranchMatrixItem> branchMatrix = byBranch.entrySet().stream()
                .map(entry -> {
                    List<StudentFact> branchStudents = entry.getValue();
                    Set<UUID> branchActive = activePeople(branchStudents, scope.asOf());
                    List<MovementEvent> branchEvents = events.stream()
                            .filter(event -> entry.getKey().equals(event.fact().branchId()))
                            .toList();
                    return new OverviewAnalyticsDTO.BranchMatrixItem(
                            entry.getKey(),
                            branchStudents.getFirst().branchName(),
                            branchActive.size(),
                            (int) branchEvents.stream()
                                    .filter(event -> "NEW".equals(event.type()))
                                    .map(MovementEvent::personId).distinct().count(),
                            (int) branchEvents.stream()
                                    .filter(event -> "INFERRED_LEFT".equals(event.type()))
                                    .map(MovementEvent::personId).distinct().count(),
                            attendanceRate(attendanceByBranch.getOrDefault(entry.getKey(), List.of())),
                            riskByBranch.getOrDefault(entry.getKey(), 0L).intValue()
                    );
                })
                .sorted(Comparator.comparing(
                        OverviewAnalyticsDTO.BranchMatrixItem::inferredLeavers,
                        Comparator.nullsLast(Integer::compareTo)).reversed())
                .toList();

        List<OverviewAnalyticsDTO.ClassAlert> classAlerts = classStats.stream()
                .filter(stat -> stat.inferredLeavers() > 0
                        || compare(stat.attendanceRate(), new BigDecimal("0.70")) < 0)
                .sorted(Comparator
                        .comparingInt(ClassStat::inferredLeavers).reversed()
                        .thenComparing(ClassStat::attendanceRate,
                                Comparator.nullsLast(BigDecimal::compareTo)))
                .limit(10)
                .map(stat -> new OverviewAnalyticsDTO.ClassAlert(
                        stat.classFact().courseScheduleId(),
                        stat.classFact().courseName(),
                        stat.classFact().branchName(),
                        stat.activeRoster(),
                        stat.inferredLeavers(),
                        stat.attendanceRate(),
                        stat.warnings()))
                .toList();

        long renewalsDue = students.stream()
                .filter(fact -> "ACTIVE".equals(fact.enrollmentStatus()))
                .filter(fact -> fact.enrollmentEndDate() != null)
                .filter(fact -> !fact.enrollmentEndDate().isBefore(scope.asOf()))
                .filter(fact -> !fact.enrollmentEndDate().isAfter(scope.asOf().plusDays(30)))
                .map(StudentFact::enrollmentId)
                .distinct()
                .count();

        BigDecimal averageFill = average(classStats.stream()
                .map(ClassStat::fillRate).filter(Objects::nonNull).toList());
        BigDecimal approvedRevenue = finance.stream()
                .filter(this::includedFinance)
                .map(FinanceFact::amount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int missingLeaveDates = (int) latest.values().stream()
                .filter(fact -> "INACTIVE".equals(fact.personStatus()))
                .filter(fact -> fact.inferredLeaveDate() == null)
                .count();
        int provisional = (int) students.stream().filter(this::provisionalDate).count();
        int excludedMigration = (int) finance.stream()
                .filter(fact -> Boolean.TRUE.equals(fact.migrationSynthetic()))
                .count();

        return new OverviewAnalyticsDTO.Response(
                new OverviewAnalyticsDTO.Summary(
                        activeStudents,
                        newStudents,
                        reactivated,
                        inferredLeavers,
                        netMovement,
                        activeAtFrom.isEmpty() ? zeroRate() : rate(retained, activeAtFrom.size()),
                        attendanceRate(attendance),
                        (int) risks.values().stream()
                                .filter(risk -> !"LOW".equals(risk.level())).count(),
                        (int) renewalsDue,
                        averageFill,
                        approvedRevenue
                ),
                monthly,
                branchMatrix,
                classAlerts,
                new OverviewAnalyticsDTO.DataQuality(excludedMigration, missingLeaveDates, provisional)
        );
    }

    public StudentMovementAnalyticsDTO.Response studentMovements(
            StudentMovementAnalyticsDTO.Request request
    ) {
        Scope scope = scope(request.from(), request.to(), request.asOf());
        StudentQuery query = new StudentQuery(
                scope.from(), scope.to(), scope.asOf(),
                request.branchIds(), request.courseIds(), null,
                request.levels(), request.genders(), request.minAge(), request.maxAge(),
                request.belts(), null, request.courseScheduleIds(), null);
        List<StudentFact> students = repository.findStudentFacts(query);
        List<MovementEvent> events = movementEvents(students, scope);

        List<StudentMovementAnalyticsDTO.MonthlyMovement> monthly = months(scope).stream()
                .map(month -> new StudentMovementAnalyticsDTO.MonthlyMovement(
                        month,
                        countEventPeople(events, month, "NEW"),
                        countEventPeople(events, month, "REACTIVATED"),
                        countEventPeople(events, month, "INFERRED_LEFT"),
                        countEventPeople(events, month, "TRANSFER_IN"),
                        countEventPeople(events, month, "TRANSFER_OUT"),
                        countEventPeople(events, month, "NEW")
                                + countEventPeople(events, month, "REACTIVATED")
                                - countEventPeople(events, month, "INFERRED_LEFT")
                ))
                .toList();

        List<StudentMovementAnalyticsDTO.AgeMovement> byAge = groupedMovement(
                events, event -> ageAt(event.date(), event.fact().birthDate()),
                (age, movement) -> new StudentMovementAnalyticsDTO.AgeMovement(
                        age, movement.newCount(), movement.leftCount(),
                        movement.newCount() - movement.leftCount()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();

        List<StudentMovementAnalyticsDTO.MonthlyAgeMovement> byMonthAndAge = months(scope).stream()
                .flatMap(month -> groupedMovement(
                        events.stream()
                                .filter(event -> YearMonth.from(event.date()).equals(month))
                                .toList(),
                        event -> ageAt(event.date(), event.fact().birthDate()),
                        (age, movement) -> new StudentMovementAnalyticsDTO.MonthlyAgeMovement(
                                month, age, movement.newCount(), movement.leftCount()))
                        .entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(Map.Entry::getValue))
                .toList();

        List<StudentMovementAnalyticsDTO.GenderMovement> byGender = groupedMovement(
                events, event -> gender(event.fact().gender()),
                (gender, movement) -> new StudentMovementAnalyticsDTO.GenderMovement(
                        gender, movement.newCount(), movement.leftCount(),
                        movement.newCount() - movement.leftCount()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();

        List<StudentMovementAnalyticsDTO.BeltMovement> byBelt = groupedMovement(
                events, event -> Objects.requireNonNullElse(event.fact().currentBelt(), "UNKNOWN"),
                (belt, movement) -> new StudentMovementAnalyticsDTO.BeltMovement(
                        belt, movement.newCount(), movement.leftCount(),
                        movement.newCount() - movement.leftCount(), "CURRENT_BELT"))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();

        List<StudentMovementAnalyticsDTO.BranchMovement> byBranch = groupedMovement(
                events.stream().filter(event -> event.fact().branchId() != null).toList(),
                event -> event.fact().branchId(),
                (branchId, movement) -> new StudentMovementAnalyticsDTO.BranchMovement(
                        branchId, movement.branchName(), movement.newCount(),
                        movement.leftCount(), movement.newCount() - movement.leftCount()))
                .entrySet().stream()
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparing(
                        StudentMovementAnalyticsDTO.BranchMovement::inferredLeavers).reversed())
                .toList();

        List<StudentMovementAnalyticsDTO.ClassMovement> byClass = groupedMovement(
                events.stream().filter(event -> event.fact().courseScheduleId() != null).toList(),
                event -> event.fact().courseScheduleId(),
                (scheduleId, movement) -> new StudentMovementAnalyticsDTO.ClassMovement(
                        scheduleId, movement.courseName(),
                        scheduleLabel(movement.sampleFact()),
                        movement.newCount(), movement.leftCount(),
                        movement.transferIn(), movement.transferOut()))
                .values().stream()
                .sorted(Comparator.comparing(
                        StudentMovementAnalyticsDTO.ClassMovement::inferredLeavers).reversed())
                .toList();

        int provisional = (int) students.stream().filter(this::provisionalDate).count();
        int unknownLeaveDates = (int) students.stream()
                .filter(fact -> "INACTIVE".equals(fact.personStatus()))
                .filter(fact -> fact.inferredLeaveDate() == null)
                .map(StudentFact::personId)
                .distinct()
                .count();

        return new StudentMovementAnalyticsDTO.Response(
                new StudentMovementAnalyticsDTO.Summary(
                        countEventPeople(events, scope, "NEW"),
                        countEventPeople(events, scope, "REACTIVATED"),
                        countEventPeople(events, scope, "INFERRED_LEFT"),
                        countEventPeople(events, scope, "TRANSFER_IN"),
                        countEventPeople(events, scope, "TRANSFER_OUT"),
                        countEventPeople(events, scope, "NEW")
                                + countEventPeople(events, scope, "REACTIVATED")
                                - countEventPeople(events, scope, "INFERRED_LEFT")
                ),
                monthly,
                byAge,
                byMonthAndAge,
                byGender,
                byBelt,
                byBranch,
                byClass,
                new StudentMovementAnalyticsDTO.DataQuality(
                        unknownLeaveDates,
                        provisional,
                        "Current person.status is used for inferred leave; it is not a complete historical event log."
                )
        );
    }

    public RetentionAnalyticsDTO.Response retention(RetentionAnalyticsDTO.Request request) {
        Scope scope = scope(request.from(), request.to(), request.asOf());
        int graceDays = positiveOrDefault(request.graceDays(), 7);
        StudentQuery query = new StudentQuery(
                scope.from(), scope.to(), scope.asOf(),
                request.branchIds(), request.courseIds(), request.trainingPlanIds(),
                null, request.genders(), request.minAge(), request.maxAge(),
                null, null, null, null);
        List<StudentFact> students = repository.findStudentFacts(query);
        Map<UUID, StudentFact> latest = latestByPerson(students);
        List<MovementEvent> events = movementEvents(students, scope);
        RetentionStats retentionStats = retentionStats(students, latest, scope, graceDays);

        List<RetentionAnalyticsDTO.CohortRow> cohorts = cohorts(students, scope);
        List<RetentionAnalyticsDTO.TenureRow> byTenure = tenureRows(latest);
        List<RetentionAnalyticsDTO.SegmentRow> bySegment = retentionSegments(students);
        List<RetentionAnalyticsDTO.ExpiryRow> expiries = expiries(students,
                YearMonth.from(scope.from()), YearMonth.from(scope.to()), graceDays);
        int provisional = (int) students.stream().filter(this::provisionalDate).count();
        int missingHistory = (int) latest.values().stream()
                .filter(fact -> "INACTIVE".equals(fact.personStatus()))
                .filter(fact -> fact.inferredLeaveDate() == null)
                .count();

        return new RetentionAnalyticsDTO.Response(
                new RetentionAnalyticsDTO.Summary(
                        retentionStats.eligibleStudents(),
                        countEventPeople(events, scope, "INFERRED_LEFT"),
                        rate(retentionStats.inferredLeavers(), retentionStats.eligibleStudents()),
                        retentionStats.renewalRate(),
                        retentionStats.medianTenureDays(),
                        countEventPeople(events, scope, "REACTIVATED"),
                        (int) latest.values().stream()
                                .filter(fact -> fact.inferredLeaveDate() != null)
                                .filter(fact -> fact.firstEnrollmentStartDate() != null)
                                .filter(fact -> ChronoUnit.DAYS.between(
                                        fact.firstEnrollmentStartDate(),
                                        fact.inferredLeaveDate()) <= 90)
                                .count()
                ),
                cohorts,
                byTenure,
                bySegment,
                expiries,
                new RetentionAnalyticsDTO.DataQuality(
                        false,
                        provisional,
                        missingHistory
                )
        );
    }

    public ClassHealthAnalyticsDTO.Response classes(ClassHealthAnalyticsDTO.Request request) {
        Scope scope = scope(request.from(), request.to(), request.asOf());
        StudentQuery studentQuery = new StudentQuery(
                scope.from(), scope.to(), scope.asOf(),
                request.branchIds(), request.courseIds(), null,
                request.levels(), null, null, null, null, null, null, null);
        List<StudentFact> students = repository.findStudentFacts(studentQuery);
        List<AttendanceFact> attendance = attendance(scope, request.branchIds(),
                request.courseIds(), null, request.levels(), null, null, null, null);
        List<ClassFact> classes = repository.findClassFacts(
                new ClassQuery(scope.from(), scope.to(), request.branchIds(),
                        request.courseIds(), null, request.levels(),
                        request.scheduleStatuses(), null));
        List<ClassStat> stats = sortClasses(
                classStats(classes, students, attendance, scope),
                request.sortBy(), request.sortDirection());

        int atRiskClasses = (int) stats.stream()
                .filter(stat -> stat.warnings().stream()
                        .anyMatch(code -> code.contains("ATTENDANCE") || code.contains("CHURN")))
                .count();
        int totalRoster = stats.stream().mapToInt(ClassStat::activeRoster).sum();

        List<ClassHealthAnalyticsDTO.ClassRow> rows = stats.stream()
                .map(stat -> new ClassHealthAnalyticsDTO.ClassRow(
                        stat.classFact().courseScheduleId(),
                        stat.classFact().courseId(),
                        stat.classFact().courseName(),
                        stat.classFact().classScheduleId(),
                        stat.classFact().branchId(),
                        stat.classFact().branchName(),
                        stat.classFact().weekday(),
                        stat.classFact().startTime(),
                        stat.classFact().endTime(),
                        stat.classFact().status(),
                        stat.classFact().capacity(),
                        "COURSE_CAPACITY_SHARED",
                        stat.activeRoster(),
                        stat.newStudents(),
                        stat.inferredLeavers(),
                        stat.transferIn(),
                        stat.transferOut(),
                        stat.newStudents() + stat.transferIn()
                                - stat.inferredLeavers() - stat.transferOut(),
                        stat.attendanceRate(),
                        stat.fillRate(),
                        stat.atRiskStudents(),
                        stat.warnings()
                ))
                .toList();

        List<ClassHealthAnalyticsDTO.MonthlyTrend> monthly = new ArrayList<>();
        for (ClassFact classFact : classes) {
            for (YearMonth month : months(scope)) {
                LocalDate end = month.atEndOfMonth();
                List<StudentFact> roster = students.stream()
                        .filter(fact -> classFact.courseScheduleId().equals(fact.courseScheduleId()))
                        .filter(fact -> enrollmentActiveAt(fact, end))
                        .toList();
                List<AttendanceFact> monthAttendance = attendance.stream()
                        .filter(fact -> classFact.courseScheduleId()
                                .equals(fact.courseScheduleId()))
                        .filter(fact -> YearMonth.from(fact.sessionDate()).equals(month))
                        .toList();
                monthly.add(new ClassHealthAnalyticsDTO.MonthlyTrend(
                        month,
                        classFact.courseScheduleId(),
                        distinctPeople(roster),
                        (int) students.stream()
                                .filter(fact -> classFact.courseScheduleId()
                                        .equals(fact.courseScheduleId()))
                                .filter(fact -> YearMonth.from(fact.enrollmentStartDate()).equals(month))
                                .map(StudentFact::personId).distinct().count(),
                        (int) students.stream()
                                .filter(fact -> classFact.courseScheduleId()
                                        .equals(fact.courseScheduleId()))
                                .filter(fact -> fact.inferredLeaveDate() != null)
                                .filter(fact -> YearMonth.from(fact.inferredLeaveDate()).equals(month))
                                .map(StudentFact::personId).distinct().count(),
                        attendanceRate(monthAttendance)
                ));
            }
        }

        return new ClassHealthAnalyticsDTO.Response(
                new ClassHealthAnalyticsDTO.Summary(
                        stats.size(),
                        (int) stats.stream().filter(stat -> stat.inferredLeavers() > 0).count(),
                        atRiskClasses,
                        totalRoster,
                        average(stats.stream().map(ClassStat::attendanceRate)
                                .filter(Objects::nonNull).toList())
                ),
                rows,
                monthly
        );
    }

    public AttendanceRiskAnalyticsDTO.Response attendanceRisk(
            AttendanceRiskAnalyticsDTO.Request request
    ) {
        Scope scope = scope(request.from(), request.to(), request.asOf());
        AttendanceQuery attendanceQuery = new AttendanceQuery(
                minDate(scope.from(), scope.asOf().minusDays(90)),
                maxDate(scope.to(), scope.asOf()),
                scope.asOf(),
                request.branchIds(),
                request.courseIds(),
                request.courseScheduleIds(),
                null,
                null,
                null,
                null,
                request.minAge(),
                request.maxAge(),
                null
        );
        List<AttendanceFact> allAttendance = repository.findAttendanceFacts(attendanceQuery);
        List<StudentFact> students = repository.findStudentFacts(new StudentQuery(
                scope.from(), scope.to(), scope.asOf(), request.branchIds(),
                request.courseIds(), null, null, null, request.minAge(),
                request.maxAge(), null, null, request.courseScheduleIds(), null));
        Map<UUID, StudentFact> latest = new LinkedHashMap<>(latestByPerson(students));
        for (AttendanceFact fact : allAttendance) {
            latest.putIfAbsent(fact.personId(),
                    studentFactFromAttendance(fact, null));
        }
        List<AttendanceFact> periodAttendance = allAttendance.stream()
                .filter(fact -> !fact.sessionDate().isBefore(scope.from()))
                .filter(fact -> !fact.sessionDate().isAfter(scope.to()))
                .toList();
        List<ClassFact> classes = repository.findClassFacts(
                new ClassQuery(scope.from(), scope.to(), request.branchIds(),
                        request.courseIds(), null, null, null, null));
        Map<UUID, RiskAssessment> allRisks =
                riskAssessments(latest.values(), allAttendance, scope.asOf());
        Set<String> riskLevels = normalizeSet(request.riskLevels());
        final Map<UUID, RiskAssessment> risks;
        if (!riskLevels.isEmpty()) {
            risks = allRisks.entrySet().stream()
                    .filter(entry -> riskLevels.contains(entry.getValue().level()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (left, right) -> left, LinkedHashMap::new));
        } else {
            risks = allRisks;
        }

        Map<String, Integer> reasonCounts = risks.values().stream()
                .flatMap(risk -> risk.reasons().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(x -> 1)));
        List<AttendanceRiskAnalyticsDTO.RiskReason> reasons = reasonCounts.entrySet().stream()
                .map(entry -> new AttendanceRiskAnalyticsDTO.RiskReason(
                        entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(
                        AttendanceRiskAnalyticsDTO.RiskReason::studentCount).reversed())
                .toList();

        List<AttendanceRiskAnalyticsDTO.ClassRow> byClass = classes.stream()
                .map(classFact -> {
                    List<AttendanceFact> classAttendance = periodAttendance.stream()
                            .filter(fact -> classFact.courseScheduleId()
                                    .equals(fact.courseScheduleId()))
                            .toList();
                    int highRisk = (int) risks.values().stream()
                            .filter(risk -> classFact.courseScheduleId()
                                    .equals(risk.fact().courseScheduleId()))
                            .filter(risk -> "HIGH".equals(risk.level()))
                            .count();
                    return new AttendanceRiskAnalyticsDTO.ClassRow(
                            classFact.courseScheduleId(),
                            classFact.courseName(),
                            classAttendance.size(),
                            attendanceRate(classAttendance),
                            highRisk
                    );
                })
                .toList();

        Map<LocalDate, List<AttendanceFact>> weekly = periodAttendance.stream()
                .collect(Collectors.groupingBy(fact -> fact.sessionDate()
                        .with(DayOfWeek.MONDAY)));
        List<AttendanceRiskAnalyticsDTO.WeeklyTrend> weeklyTrend = weekly.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AttendanceRiskAnalyticsDTO.WeeklyTrend(
                        entry.getKey(),
                        attendanceRate(entry.getValue()),
                        countStatus(entry.getValue(), "ABSENT"),
                        countStatus(entry.getValue(), "EXCUSED")
                ))
                .toList();

        return new AttendanceRiskAnalyticsDTO.Response(
                new AttendanceRiskAnalyticsDTO.Summary(
                        periodAttendance.size(),
                        countStatus(periodAttendance, "PRESENT"),
                        countStatus(periodAttendance, "LATE"),
                        countStatus(periodAttendance, "MAKEUP"),
                        countStatus(periodAttendance, "ABSENT"),
                        countStatus(periodAttendance, "EXCUSED"),
                        attendanceRate(periodAttendance),
                        (int) risks.values().stream()
                                .filter(risk -> "HIGH".equals(risk.level())).count(),
                        (int) risks.values().stream()
                                .filter(risk -> "MEDIUM".equals(risk.level())).count()
                ),
                byClass,
                weeklyTrend,
                reasons,
                new AttendanceRiskAnalyticsDTO.Thresholds(
                        CONSECUTIVE_ABSENCE_LIMIT,
                        DAYS_WITHOUT_ATTENDANCE_LIMIT,
                        ATTENDANCE_DROP_THRESHOLD
                )
        );
    }

    public RecruitmentAnalyticsDTO.Response recruitment(
            RecruitmentAnalyticsDTO.Request request
    ) {
        Scope scope = scope(request.from(), request.to(),
                request.to() == null ? LocalDate.now() : request.to());
        StudentQuery query = new StudentQuery(
                scope.from(), scope.to(), scope.asOf(),
                request.branchIds(), request.courseIds(), null,
                request.levels(), request.genders(), request.minAge(), request.maxAge(),
                null, null, null, null);
        List<StudentFact> students = repository.findStudentFacts(query);
        List<MovementEvent> events = movementEvents(students, scope);
        Map<UUID, StudentFact> latest = latestByPerson(students);
        List<ClassFact> classes = repository.findClassFacts(
                new ClassQuery(scope.from(), scope.to(), request.branchIds(),
                        request.courseIds(), null, request.levels(), null, null));

        List<RecruitmentAnalyticsDTO.MonthlyRow> monthly = months(scope).stream()
                .map(month -> new RecruitmentAnalyticsDTO.MonthlyRow(
                        month,
                        countEventPeople(events, month, "NEW"),
                        countEventPeople(events, month, "REACTIVATED"),
                        countEventPeople(events, month, "INFERRED_LEFT"),
                        countEventPeople(events, month, "NEW")
                                + countEventPeople(events, month, "REACTIVATED")
                                - countEventPeople(events, month, "INFERRED_LEFT")
                ))
                .toList();

        List<RecruitmentAnalyticsDTO.AgeRow> byAge = events.stream()
                .filter(event -> "NEW".equals(event.type()))
                .collect(Collectors.groupingBy(
                        event -> new MonthAge(
                                YearMonth.from(event.date()),
                                ageAt(event.date(), event.fact().birthDate())),
                        Collectors.mapping(MovementEvent::personId, Collectors.toSet())))
                .entrySet().stream()
                .map(entry -> new RecruitmentAnalyticsDTO.AgeRow(
                        entry.getKey().month(),
                        entry.getKey().age(),
                        entry.getValue().size()))
                .sorted(Comparator.comparing(RecruitmentAnalyticsDTO.AgeRow::month)
                        .thenComparing(RecruitmentAnalyticsDTO.AgeRow::age))
                .toList();

        List<RecruitmentAnalyticsDTO.BranchRow> byBranch = events.stream()
                .filter(event -> "NEW".equals(event.type()))
                .filter(event -> event.fact().branchId() != null)
                .collect(Collectors.groupingBy(
                        event -> new MonthBranch(
                                YearMonth.from(event.date()),
                                event.fact().branchId(),
                                event.fact().branchName()),
                        Collectors.mapping(MovementEvent::personId, Collectors.toSet())))
                .entrySet().stream()
                .map(entry -> new RecruitmentAnalyticsDTO.BranchRow(
                        entry.getKey().month(),
                        entry.getKey().branchId(),
                        entry.getKey().branchName(),
                        entry.getValue().size()))
                .sorted(Comparator.comparing(RecruitmentAnalyticsDTO.BranchRow::month)
                        .thenComparing(RecruitmentAnalyticsDTO.BranchRow::branchName))
                .toList();

        List<ClassStat> classStats = classStats(classes, students, List.of(), scope);
        List<RecruitmentAnalyticsDTO.ClassRow> byClass = classStats.stream()
                .map(stat -> {
                    int firstTime = (int) students.stream()
                            .filter(fact -> stat.classFact().courseScheduleId()
                                    .equals(fact.courseScheduleId()))
                            .filter(fact -> inRange(fact.firstEnrollmentStartDate(),
                                    scope.from(), scope.to()))
                            .map(StudentFact::personId)
                            .distinct()
                            .count();
                    int available = Objects.requireNonNullElse(stat.classFact().capacity(), 0)
                            - stat.activeRoster();
                    return new RecruitmentAnalyticsDTO.ClassRow(
                            stat.classFact().courseScheduleId(),
                            stat.classFact().courseName(),
                            stat.classFact().branchName(),
                            firstTime,
                            stat.activeRoster(),
                            Math.max(available, 0)
                    );
                })
                .sorted(Comparator.comparing(
                        RecruitmentAnalyticsDTO.ClassRow::firstTimeStudents).reversed())
                .toList();

        List<RecruitmentAnalyticsDTO.YearComparisonRow> yearComparison = List.of();
        if (Boolean.TRUE.equals(request.comparePreviousYear())) {
            LocalDate previousStart = LocalDate.of(scope.from().getYear() - 1, 1, 1);
            List<StudentFact> comparisonFacts = repository.findStudentFacts(new StudentQuery(
                    previousStart, scope.to(), scope.asOf(), request.branchIds(),
                    request.courseIds(), null, request.levels(), request.genders(),
                    request.minAge(), request.maxAge(), null, null, null, null));
            yearComparison = IntStream.rangeClosed(1, 12).mapToObj(month -> {
                int current = (int) comparisonFacts.stream()
                        .filter(fact -> fact.firstEnrollmentStartDate() != null)
                        .filter(fact -> fact.firstEnrollmentStartDate().getYear()
                                == scope.from().getYear())
                        .filter(fact -> fact.firstEnrollmentStartDate().getMonthValue() == month)
                        .map(StudentFact::personId).distinct().count();
                int previous = (int) comparisonFacts.stream()
                        .filter(fact -> fact.firstEnrollmentStartDate() != null)
                        .filter(fact -> fact.firstEnrollmentStartDate().getYear()
                                == scope.from().getYear() - 1)
                        .filter(fact -> fact.firstEnrollmentStartDate().getMonthValue() == month)
                        .map(StudentFact::personId).distinct().count();
                return new RecruitmentAnalyticsDTO.YearComparisonRow(
                        month, current, previous,
                        previous == 0 ? null : rate(current - previous, previous));
            }).toList();
        }

        int availableSeats = classStats.stream()
                .mapToInt(stat -> Math.max(
                        Objects.requireNonNullElse(stat.classFact().capacity(), 0)
                                - stat.activeRoster(), 0))
                .sum();
        Integer firstTime = countEventPeople(events, scope, "NEW");
        int monthCount = Math.max(months(scope).size(), 1);
        YearMonth peakMonth = monthly.stream()
                .max(Comparator.comparing(RecruitmentAnalyticsDTO.MonthlyRow::firstTimeStudents))
                .map(RecruitmentAnalyticsDTO.MonthlyRow::month)
                .orElse(null);
        return new RecruitmentAnalyticsDTO.Response(
                new RecruitmentAnalyticsDTO.Summary(
                        firstTime,
                        countEventPeople(events, scope, "REACTIVATED"),
                        rate(firstTime.longValue(), monthCount),
                        peakMonth,
                        availableSeats
                ),
                monthly,
                byAge,
                byBranch,
                byClass,
                yearComparison,
                new RecruitmentAnalyticsDTO.DataAvailability(false, false, false, false)
        );
    }

    public CoursePlanAnalyticsDTO.Response coursePlans(
            CoursePlanAnalyticsDTO.Request request
    ) {
        Scope scope = scope(request.from(), request.to(), request.to());
        List<UUID> requestedPlanIds = safeList(request.trainingPlanIds());
        List<StudentFact> students = repository.findStudentFacts(new StudentQuery(
                scope.from(), scope.to(), scope.asOf(), request.branchIds(),
                request.courseIds(), requestedPlanIds, request.levels(),
                null, null, null, null, request.planStatuses(), null, null));
        List<AttendanceFact> attendance = attendance(scope, request.branchIds(),
                request.courseIds(), null, request.levels(), null, null, null, null);
        List<AddonFact> addons = repository.findAddonFacts(scope.from(), scope.to());
        Map<UUID, StudentFact> latest = latestByPerson(students);
        if (!requestedPlanIds.isEmpty()) {
            Set<UUID> planFilter = new HashSet<>(requestedPlanIds);
            latest = latest.entrySet().stream()
                    .filter(entry -> planFilter.contains(entry.getValue().trainingPlanId()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (left, right) -> left, LinkedHashMap::new));
        }

        Map<UUID, List<StudentFact>> byPlan = students.stream()
                .filter(fact -> fact.trainingPlanId() != null)
                .collect(Collectors.groupingBy(StudentFact::trainingPlanId,
                        LinkedHashMap::new, Collectors.toList()));
        Map<UUID, List<AttendanceFact>> attendanceByEnrollment =
                attendance.stream().filter(fact -> fact.enrollmentId() != null)
                        .collect(Collectors.groupingBy(AttendanceFact::enrollmentId));

        List<CoursePlanAnalyticsDTO.PlanRow> plans = byPlan.entrySet().stream()
                .map(entry -> {
                    StudentFact sample = entry.getValue().getFirst();
                    long active = entry.getValue().stream()
                            .filter(fact -> enrollmentActiveAt(fact, scope.asOf()))
                            .map(StudentFact::personId).distinct().count();
                    long leavers = entry.getValue().stream()
                            .filter(fact -> "INACTIVE".equals(fact.personStatus()))
                            .filter(fact -> inRange(fact.inferredLeaveDate(), scope.from(), scope.to()))
                            .map(StudentFact::personId).distinct().count();
                    long renewed = entry.getValue().stream()
                            .filter(fact -> hasSubsequentEnrollment(fact, students))
                            .map(StudentFact::personId).distinct().count();
                    long eligible = entry.getValue().stream()
                            .filter(fact -> fact.enrollmentEndDate() != null)
                            .map(StudentFact::personId).distinct().count();
                    return new CoursePlanAnalyticsDTO.PlanRow(
                            entry.getKey(),
                            sample.trainingPlanName(),
                            sample.trainingPlanLevel(),
                            sample.trainingPlanDurationMonths(),
                            sample.includedSessionCount(),
                            sample.basePrice(),
                            sample.finalPrice(),
                            entry.getValue().stream().map(StudentFact::enrollmentId)
                                    .distinct().toList().size(),
                            (int) active,
                            rate(renewed, eligible),
                            (int) leavers
                    );
                })
                .sorted(Comparator.comparing(CoursePlanAnalyticsDTO.PlanRow::validPurchaseCount)
                        .reversed())
                .toList();

        Map<UUID, List<StudentFact>> byCourse = latest.values().stream()
                .filter(fact -> fact.courseId() != null)
                .collect(Collectors.groupingBy(StudentFact::courseId,
                        LinkedHashMap::new, Collectors.toList()));
        List<CoursePlanAnalyticsDTO.CourseRow> courses = byCourse.entrySet().stream()
                .map(entry -> {
                    StudentFact sample = entry.getValue().getFirst();
                    List<AttendanceFact> courseAttendance = attendance.stream()
                            .filter(fact -> entry.getKey().equals(fact.courseId()))
                            .toList();
                    return new CoursePlanAnalyticsDTO.CourseRow(
                            entry.getKey(),
                            sample.courseName(),
                            sample.courseLevel(),
                            (int) entry.getValue().stream()
                                    .filter(fact -> enrollmentActiveAt(fact, scope.asOf()))
                                    .count(),
                            (int) entry.getValue().stream()
                                    .filter(fact -> "INACTIVE".equals(fact.personStatus()))
                                    .filter(fact -> inRange(fact.inferredLeaveDate(),
                                            scope.from(), scope.to()))
                                    .map(StudentFact::personId).distinct().count(),
                            attendanceRate(courseAttendance)
                    );
                })
                .toList();

        List<CoursePlanAnalyticsDTO.UtilizationRow> utilization = plans.stream()
                .map(plan -> {
                    List<StudentFact> planStudents = byPlan.get(plan.trainingPlanId());
                    long used = planStudents.stream()
                            .flatMap(fact -> attendanceByEnrollment
                                    .getOrDefault(fact.enrollmentId(), List.of()).stream())
                            .filter(fact -> isAttended(fact.attendanceStatus()))
                            .map(AttendanceFact::sessionId)
                            .distinct()
                            .count();
                    int included = Objects.requireNonNullElse(plan.includedSessionCount(), 0)
                            * Math.max(plan.activeStudents(), 1);
                    return new CoursePlanAnalyticsDTO.UtilizationRow(
                            plan.trainingPlanId(), included, (int) used, rate(used, included));
                })
                .toList();

        List<CoursePlanAnalyticsDTO.AddonRow> addonRows = addons.stream()
                .filter(addon -> requestedPlanIds.isEmpty()
                        || requestedPlanIds.contains(addon.trainingPlanId()))
                .map(addon -> new CoursePlanAnalyticsDTO.AddonRow(
                        addon.addonId(),
                        addon.extraSessionsPerWeek(),
                        addon.price(),
                        Objects.requireNonNullElse(addon.validUsageCount(), 0L).intValue()))
                .toList();

        int activeEnrollments = (int) students.stream()
                .filter(fact -> enrollmentActiveAt(fact, scope.asOf())).count();
        long renewed = latest.values().stream()
                .filter(fact -> hasSubsequentEnrollment(fact, students)).count();
        long eligible = latest.values().stream()
                .filter(fact -> fact.enrollmentEndDate() != null).count();
        BigDecimal avgDuration = average(plans.stream()
                .map(plan -> plan.durationMonths() == null ? null
                        : BigDecimal.valueOf(plan.durationMonths()))
                .filter(Objects::nonNull).toList());
        BigDecimal averageUtilization = average(utilization.stream()
                .map(CoursePlanAnalyticsDTO.UtilizationRow::utilizationRate).toList());

        return new CoursePlanAnalyticsDTO.Response(
                new CoursePlanAnalyticsDTO.Summary(
                        students.stream().map(StudentFact::enrollmentId).distinct()
                                .toList().size(),
                        activeEnrollments,
                        rate(renewed, eligible),
                        avgDuration,
                        averageUtilization
                ),
                plans,
                courses,
                utilization,
                addonRows
        );
    }

    public BranchAnalyticsDTO.Response branches(BranchAnalyticsDTO.Request request) {
        Scope scope = scope(request.from(), request.to(), request.asOf());
        List<StudentFact> students = repository.findStudentFacts(new StudentQuery(
                scope.from(), scope.to(), scope.asOf(), request.branchIds(),
                request.courseIds(), null, request.levels(), null, null,
                null, null, null, null, null));
        List<AttendanceFact> attendance = attendance(scope, request.branchIds(),
                request.courseIds(), null, request.levels(), null, null, null, null);
        List<ClassFact> classes = repository.findClassFacts(
                new ClassQuery(scope.from(), scope.to(), request.branchIds(),
                        request.courseIds(), null, request.levels(), null, null));
        List<MovementEvent> events = movementEvents(students, scope);
        List<ClassStat> classStats = classStats(classes, students, attendance, scope);

        Map<Long, List<StudentFact>> byBranch = latestByPerson(students).values().stream()
                .filter(fact -> fact.branchId() != null)
                .collect(Collectors.groupingBy(StudentFact::branchId,
                        LinkedHashMap::new, Collectors.toList()));
        List<BranchAnalyticsDTO.BranchRow> rows = byBranch.entrySet().stream()
                .map(entry -> {
                    StudentFact sample = entry.getValue().getFirst();
                    List<MovementEvent> branchEvents = events.stream()
                            .filter(event -> entry.getKey().equals(event.fact().branchId()))
                            .toList();
                    int newCount = distinctPeople(branchEvents.stream()
                            .filter(event -> "NEW".equals(event.type()))
                            .map(MovementEvent::fact).toList());
                    int reactivated = distinctPeople(branchEvents.stream()
                            .filter(event -> "REACTIVATED".equals(event.type()))
                            .map(MovementEvent::fact).toList());
                    int leavers = distinctPeople(branchEvents.stream()
                            .filter(event -> "INFERRED_LEFT".equals(event.type()))
                            .map(MovementEvent::fact).toList());
                    int transferIn = (int) branchEvents.stream()
                            .filter(event -> "TRANSFER_IN".equals(event.type()))
                            .map(MovementEvent::personId).distinct().count();
                    int transferOut = (int) branchEvents.stream()
                            .filter(event -> "TRANSFER_OUT".equals(event.type()))
                            .map(MovementEvent::personId).distinct().count();
                    int activeClasses = (int) classStats.stream()
                            .filter(stat -> entry.getKey().equals(stat.classFact().branchId()))
                            .count();
                    int availableSeats = classStats.stream()
                            .filter(stat -> entry.getKey().equals(stat.classFact().branchId()))
                            .mapToInt(stat -> Math.max(
                                    Objects.requireNonNullElse(
                                            stat.classFact().capacity(), 0)
                                            - stat.activeRoster(), 0))
                            .sum();
                    List<AttendanceFact> branchAttendance = attendance.stream()
                            .filter(fact -> entry.getKey().equals(fact.branchId())).toList();
                    return new BranchAnalyticsDTO.BranchRow(
                            entry.getKey(),
                            sample.branchName(),
                            sample.branchStatus(),
                            sample.branchOpenedDate(),
                            activePeople(entry.getValue(), scope.asOf()).size(),
                            newCount,
                            reactivated,
                            leavers,
                            transferIn,
                            transferOut,
                            newCount + reactivated + transferIn
                                    - leavers - transferOut,
                            attendanceRate(branchAttendance),
                            activeClasses,
                            availableSeats
                    );
                })
                .sorted(Comparator.comparing(BranchAnalyticsDTO.BranchRow::inferredLeavers)
                        .reversed())
                .toList();

        List<BranchAnalyticsDTO.MonthlyRow> monthly = new ArrayList<>();
        for (Long branchId : byBranch.keySet()) {
            for (YearMonth month : months(scope)) {
                List<StudentFact> branchFacts = byBranch.get(branchId);
                monthly.add(new BranchAnalyticsDTO.MonthlyRow(
                        month,
                        branchId,
                        countEventPeople(events.stream()
                                .filter(event -> branchId.equals(event.fact().branchId()))
                                .toList(), month, "NEW"),
                        countEventPeople(events.stream()
                                .filter(event -> branchId.equals(event.fact().branchId()))
                                .toList(), month, "INFERRED_LEFT"),
                        activePeople(branchFacts, month.atEndOfMonth()).size()
                ));
            }
        }

        List<BranchAnalyticsDTO.TransferRow> transferMatrix = transferEvents(students, scope).stream()
                .filter(event -> event.fromBranchId() != null && event.toBranchId() != null)
                .collect(Collectors.groupingBy(
                        event -> new BranchPair(event.fromBranchId(), event.toBranchId()),
                        LinkedHashMap::new,
                        Collectors.mapping(TransferEvent::personId, Collectors.toSet())))
                .entrySet().stream()
                .map(entry -> new BranchAnalyticsDTO.TransferRow(
                        entry.getKey().fromBranchId(),
                        entry.getKey().toBranchId(),
                        entry.getValue().size()))
                .toList();

        return new BranchAnalyticsDTO.Response(
                new BranchAnalyticsDTO.Summary(
                        rows.size(),
                        rows.stream().mapToInt(BranchAnalyticsDTO.BranchRow::activeStudents).sum(),
                        rows.stream().mapToInt(BranchAnalyticsDTO.BranchRow::inferredLeavers).sum(),
                        attendanceRate(attendance)
                ),
                rows,
                monthly,
                transferMatrix
        );
    }

    public CoachAnalyticsDTO.Response coaches(CoachAnalyticsDTO.Request request) {
        Scope scope = scope(request.from(), request.to(), request.to());
        SessionQuery query = new SessionQuery(
                scope.from(), scope.to(), request.branchIds(), null,
                request.courseScheduleIds(), null, request.assignmentTypes(),
                request.assignmentStatuses(), null, null);
        List<SessionFact> sessions = repository.findSessionFacts(query);

        Map<UUID, List<SessionFact>> byCoach = sessions.stream()
                .filter(fact -> fact.staffPersonId() != null)
                .collect(Collectors.groupingBy(SessionFact::staffPersonId,
                        LinkedHashMap::new, Collectors.toList()));
        List<CoachAnalyticsDTO.CoachRow> coaches = byCoach.entrySet().stream()
                .map(entry -> {
                    List<SessionFact> coachSessions = entry.getValue();
                    Set<UUID> sessionIds = coachSessions.stream().map(SessionFact::sessionId)
                            .collect(Collectors.toSet());
                    Set<UUID> completedSessions = coachSessions.stream()
                            .filter(fact -> isCompletedSession(fact.sessionStatus()))
                            .map(SessionFact::sessionId)
                            .collect(Collectors.toSet());
                    long timesheets = coachSessions.stream()
                            .filter(fact -> Boolean.TRUE.equals(fact.timesheetComplete()))
                            .map(SessionFact::sessionId).distinct().count();
                    long missing = completedSessions.stream()
                            .filter(sessionId -> coachSessions.stream()
                                    .filter(fact -> sessionId.equals(fact.sessionId()))
                                    .noneMatch(fact -> Boolean.TRUE
                                            .equals(fact.timesheetComplete())))
                            .count();
                    List<AttendanceFact> attendance = attendanceForSessions(
                            sessionIds, scope);
                    return new CoachAnalyticsDTO.CoachRow(
                            entry.getKey(),
                            coachSessions.getFirst().staffName(),
                            coachSessions.stream().map(SessionFact::courseScheduleId)
                                    .distinct().toList().size(),
                            sessionIds.size(),
                            completedSessions.size(),
                            (int) timesheets,
                            (int) missing,
                            attendanceRate(attendance)
                    );
                })
                .sorted(Comparator.comparing(CoachAnalyticsDTO.CoachRow::missingTimesheetCount)
                        .reversed())
                .toList();

        List<CoachAnalyticsDTO.AssignmentRow> assignments = sessions.stream()
                .filter(fact -> fact.staffPersonId() != null)
                .collect(Collectors.toMap(
                        fact -> fact.courseStaffAssignmentId() == null
                                ? "none-" + fact.staffPersonId() + fact.courseScheduleId()
                                : fact.courseStaffAssignmentId().toString(),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new))
                .values().stream()
                .map(fact -> new CoachAnalyticsDTO.AssignmentRow(
                        fact.staffPersonId(),
                        fact.courseScheduleId(),
                        fact.assignmentType(),
                        fact.assignmentStart(),
                        fact.assignmentEnd(),
                        fact.assignmentStatus()
                ))
                .toList();

        List<CoachAnalyticsDTO.MonthlyRow> monthly = new ArrayList<>();
        for (Map.Entry<UUID, List<SessionFact>> entry : byCoach.entrySet()) {
            for (YearMonth month : months(scope)) {
                List<SessionFact> monthlySessions = entry.getValue().stream()
                        .filter(fact -> YearMonth.from(fact.sessionDate()).equals(month))
                        .toList();
                monthly.add(new CoachAnalyticsDTO.MonthlyRow(
                        month,
                        entry.getKey(),
                        monthlySessions.stream().map(SessionFact::sessionId).distinct()
                                .toList().size(),
                        (int) monthlySessions.stream()
                                .filter(fact -> isCompletedSession(fact.sessionStatus()))
                                .map(SessionFact::sessionId).distinct().count(),
                        (int) monthlySessions.stream()
                                .filter(fact -> isCompletedSession(fact.sessionStatus()))
                                .filter(fact -> !Boolean.TRUE.equals(fact.timesheetComplete()))
                                .map(SessionFact::sessionId).distinct().count()
                ));
            }
        }

        long assignedSessions = sessions.stream()
                .filter(fact -> fact.staffPersonId() != null)
                .map(SessionFact::sessionId).distinct().count();
        long completedSessions = sessions.stream()
                .filter(fact -> fact.staffPersonId() != null)
                .filter(fact -> isCompletedSession(fact.sessionStatus()))
                .map(SessionFact::sessionId).distinct().count();
        long completedTimesheets = sessions.stream()
                .filter(fact -> fact.staffPersonId() != null)
                .filter(fact -> Boolean.TRUE.equals(fact.timesheetComplete()))
                .map(SessionFact::sessionId).distinct().count();
        long missing = sessions.stream()
                .filter(fact -> fact.staffPersonId() != null)
                .filter(fact -> isCompletedSession(fact.sessionStatus()))
                .filter(fact -> !Boolean.TRUE.equals(fact.timesheetComplete()))
                .map(SessionFact::sessionId).distinct().count();
        return new CoachAnalyticsDTO.Response(
                new CoachAnalyticsDTO.Summary(
                        coaches.size(),
                        (int) assignedSessions,
                        (int) completedSessions,
                        (int) missing,
                        rate(completedTimesheets, completedSessions)
                ),
                coaches,
                assignments,
                monthly
        );
    }

    public DevelopmentAnalyticsDTO.Response development(
            DevelopmentAnalyticsDTO.Request request
    ) {
        Scope scope = scope(request.from(), request.to(), request.to());
        DevelopmentQuery query = new DevelopmentQuery(
                scope.from(), scope.to(), scope.asOf(), request.branchIds(), null,
                request.belts(), null, request.levels(), request.examTypes(), null,
                request.examResults(), null, request.minAge(), request.maxAge(), null);
        List<BeltExamFact> exams = repository.findBeltExamFacts(query);
        DevelopmentQuery fitnessQuery = new DevelopmentQuery(
                scope.from(), scope.to(), scope.asOf(), request.branchIds(), null,
                request.belts(), null, request.levels(), null, null,
                null, null, request.minAge(), request.maxAge(), null);
        List<FitnessFact> fitness = repository.findFitnessFacts(fitnessQuery);

        int attempts = exams.size();
        int passed = (int) exams.stream()
                .filter(exam -> "PASSED".equals(exam.examResult())).count();
        List<DevelopmentAnalyticsDTO.BeltDistributionRow> beltDistribution = exams.stream()
                .filter(exam -> exam.examDate() != null)
                .collect(Collectors.groupingBy(
                        exam -> Objects.requireNonNullElse(exam.currentBelt(), "UNKNOWN"),
                        Collectors.mapping(BeltExamFact::personId, Collectors.toSet())))
                .entrySet().stream()
                .map(entry -> new DevelopmentAnalyticsDTO.BeltDistributionRow(
                        entry.getKey(), entry.getValue().size()))
                .toList();

        List<DevelopmentAnalyticsDTO.ExamTrendRow> examTrend = exams.stream()
                .collect(Collectors.groupingBy(
                        exam -> new MonthExamType(
                                YearMonth.from(exam.examDate()),
                                Objects.requireNonNullElse(exam.examType(), "UNKNOWN"))))
                .entrySet().stream()
                .map(entry -> new DevelopmentAnalyticsDTO.ExamTrendRow(
                        entry.getKey().month(),
                        entry.getKey().examType(),
                        countExamResult(entry.getValue(), "PASSED"),
                        countExamResult(entry.getValue(), "FAILED"),
                        countExamResult(entry.getValue(), "ABSENT"),
                        countExamResult(entry.getValue(), "PENDING")
                ))
                .sorted(Comparator.comparing(DevelopmentAnalyticsDTO.ExamTrendRow::month))
                .toList();

        List<DevelopmentAnalyticsDTO.AgeRow> byAge = exams.stream()
                .collect(Collectors.groupingBy(exam -> ageAt(exam.examDate(), exam.birthDate())))
                .entrySet().stream()
                .map(entry -> new DevelopmentAnalyticsDTO.AgeRow(
                        entry.getKey(),
                        entry.getValue().size(),
                        rate(entry.getValue().stream()
                                        .filter(exam -> "PASSED".equals(exam.examResult())).count(),
                                entry.getValue().size())))
                .sorted(Comparator.comparing(DevelopmentAnalyticsDTO.AgeRow::age))
                .toList();

        List<DevelopmentAnalyticsDTO.BranchRow> byBranch = exams.stream()
                .filter(exam -> exam.branchId() != null)
                .collect(Collectors.groupingBy(BeltExamFact::branchId))
                .entrySet().stream()
                .map(entry -> new DevelopmentAnalyticsDTO.BranchRow(
                        entry.getKey(),
                        entry.getValue().size(),
                        rate(entry.getValue().stream()
                                        .filter(exam -> "PASSED".equals(exam.examResult())).count(),
                                entry.getValue().size())))
                .toList();

        List<DevelopmentAnalyticsDTO.FitnessRow> fitnessRows = fitness.stream()
                .collect(Collectors.groupingBy(FitnessFact::fitnessId))
                .entrySet().stream()
                .map(entry -> new DevelopmentAnalyticsDTO.FitnessRow(
                        entry.getKey(),
                        entry.getValue().getFirst().level(),
                        entry.getValue().size(),
                        entry.getValue().stream().map(FitnessFact::personId).distinct()
                                .toList().size(),
                        average(entry.getValue().stream()
                                .map(fact -> BigDecimal.valueOf(
                                        Objects.requireNonNullElse(fact.duration(), 0)))
                                .toList())
                ))
                .toList();

        return new DevelopmentAnalyticsDTO.Response(
                new DevelopmentAnalyticsDTO.Summary(
                        exams.stream().map(BeltExamFact::personId).distinct().toList().size(),
                        attempts,
                        passed,
                        rate(passed, attempts),
                        fitness.stream().map(FitnessFact::personId).distinct().toList().size(),
                        fitness.size()
                ),
                beltDistribution,
                examTrend,
                byAge,
                byBranch,
                fitnessRows
        );
    }

    public FinanceAnalyticsDTO.Response finance(FinanceAnalyticsDTO.Request request) {
        Scope scope = scope(request.from(), request.to(), request.to());
        FinanceQuery query = new FinanceQuery(
                scope.from(), scope.to(), request.branchIds(), null,
                request.trainingPlanIds(), null, request.transactionTypes(), null,
                request.directions(), null, request.statuses(), null, false);
        List<FinanceFact> all = repository.findFinanceFacts(query);
        List<FinanceFact> included = all.stream().filter(this::includedFinance).toList();

        List<FinanceAnalyticsDTO.MonthlyRow> monthly = months(scope).stream()
                .map(month -> {
                    List<FinanceFact> rows = included.stream()
                            .filter(fact -> YearMonth.from(fact.createdAt()).equals(month))
                            .toList();
                    return new FinanceAnalyticsDTO.MonthlyRow(
                            month,
                            amount(rows, "TOP_UP"),
                            amount(rows, "TRAINING_PLAN_PURCHASE"),
                            amount(rows, "TRAINING_ADDON_PURCHASE"),
                            amount(rows, "REFUND")
                    );
                })
                .toList();

        List<FinanceAnalyticsDTO.PlanRow> byPlan = included.stream()
                .filter(fact -> fact.trainingPlanId() != null)
                .collect(Collectors.groupingBy(FinanceFact::trainingPlanId))
                .entrySet().stream()
                .map(entry -> new FinanceAnalyticsDTO.PlanRow(
                        entry.getKey(),
                        (int) entry.getValue().stream()
                                .filter(fact -> "TRAINING_PLAN_PURCHASE"
                                        .equals(fact.transactionType()))
                                .map(FinanceFact::transactionId).distinct().count(),
                        entry.getValue().stream()
                                .filter(fact -> "TRAINING_PLAN_PURCHASE"
                                        .equals(fact.transactionType()))
                                .map(FinanceFact::amount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                ))
                .toList();

        List<FinanceAnalyticsDTO.BranchRow> byBranch = included.stream()
                .collect(Collectors.groupingBy(fact -> Objects.requireNonNullElse(
                        fact.attributedBranchId(), ZERO_BRANCH_ID)))
                .entrySet().stream()
                .map(entry -> new FinanceAnalyticsDTO.BranchRow(
                        ZERO_BRANCH_ID.equals(entry.getKey()) ? null : entry.getKey(),
                        entry.getValue().stream().map(FinanceFact::amount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        ZERO_BRANCH_ID.equals(entry.getKey())
                                ? "UNATTRIBUTED" : "LAST_ENROLLMENT_SCHEDULE",
                        ZERO_BRANCH_ID.equals(entry.getKey())
                                ? entry.getValue().stream().map(FinanceFact::amount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                : BigDecimal.ZERO
                ))
                .toList();

        List<FinanceAnalyticsDTO.StatusRow> byStatus = all.stream()
                .collect(Collectors.groupingBy(FinanceFact::status))
                .entrySet().stream()
                .map(entry -> new FinanceAnalyticsDTO.StatusRow(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream().map(FinanceFact::amount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                ))
                .toList();

        return new FinanceAnalyticsDTO.Response(
                new FinanceAnalyticsDTO.Summary(
                        amount(included, "TOP_UP"),
                        amount(included, "TRAINING_PLAN_PURCHASE"),
                        amount(included, "TRAINING_ADDON_PURCHASE"),
                        amount(included, "REFUND"),
                        included.size(),
                        (int) all.stream()
                                .filter(fact -> Boolean.TRUE.equals(fact.migrationSynthetic()))
                                .count()
                ),
                monthly,
                byPlan,
                byBranch,
                byStatus,
                new FinanceAnalyticsDTO.DataQuality(
                        (int) all.stream().filter(fact -> "PENDING".equals(fact.status())).count(),
                        (int) all.stream()
                                .filter(fact -> Boolean.TRUE.equals(fact.migrationSynthetic()))
                                .count(),
                        (int) included.stream()
                                .filter(fact -> fact.attributedBranchId() == null).count()
                )
        );
    }

    public PageResponse<OverviewAnalyticsDTO.MovementDetailResponse> overviewMovements(
            OverviewAnalyticsDTO.MovementDetailRequest request
    ) {
        Scope scope = scope(request.from(), request.to(), request.to());
        List<StudentFact> students = repository.findStudentFacts(new StudentQuery(
                scope.from(), scope.to(), scope.asOf(), safeList(request.branchIds()),
                null, null, null, null, null, null, null, null, null, null));
        String metric = request.metric() == null ? "NEW" : request.metric().toUpperCase();
        List<MovementEvent> events = movementEvents(students, scope).stream()
                .filter(event -> metric.equals(event.type()))
                .sorted(Comparator.comparing(MovementEvent::date).reversed()
                        .thenComparing(event -> event.fact().fullName(),
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        List<OverviewAnalyticsDTO.MovementDetailResponse> items = events.stream()
                .map(event -> new OverviewAnalyticsDTO.MovementDetailResponse(
                        event.fact().personId(),
                        event.fact().personCode(),
                        event.fact().fullName(),
                        event.fact().personStatus(),
                        event.type(),
                        event.date(),
                        event.fact().branchId(),
                        event.fact().branchName(),
                        event.fact().courseScheduleId(),
                        event.fact().courseName(),
                        event.fact().inferredLeaveDate(),
                        event.fact().leaveDateQuality()
                ))
                .toList();
        return page(items, request.page(), request.size());
    }

    public PageResponse<StudentMovementAnalyticsDTO.StudentsResponse> studentMovementStudents(
            StudentMovementAnalyticsDTO.StudentsRequest request
    ) {
        Scope scope = scope(request.from(), request.to(), request.to());
        Long branchId = request.branchId();
        UUID courseId = request.courseId();
        UUID scheduleId = request.courseScheduleId();
        List<StudentFact> students = repository.findStudentFacts(new StudentQuery(
                scope.from(), scope.to(), scope.asOf(),
                branchId == null ? List.of() : List.of(branchId),
                courseId == null ? List.of() : List.of(courseId),
                null, null,
                request.gender() == null ? List.of() : List.of(request.gender()),
                request.age(), request.age(), null, null,
                scheduleId == null ? List.of() : List.of(scheduleId), null));
        String eventFilter = request.event() == null ? null : request.event().toUpperCase();
        List<MovementEvent> events = movementEvents(students, scope).stream()
                .filter(event -> eventFilter == null || eventFilter.equals(event.type()))
                .filter(event -> request.month() == null
                        || YearMonth.from(event.date()).equals(request.month()))
                .filter(event -> request.age() == null
                        || ageAt(event.date(), event.fact().birthDate()) == request.age())
                .filter(event -> request.belt() == null
                        || request.belt().equalsIgnoreCase(event.fact().currentBelt()))
                .sorted(Comparator.comparing(MovementEvent::date).reversed())
                .toList();
        List<StudentMovementAnalyticsDTO.StudentsResponse> items = events.stream()
                .map(event -> new StudentMovementAnalyticsDTO.StudentsResponse(
                        event.fact().personId(),
                        event.fact().personCode(),
                        event.fact().fullName(),
                        event.fact().birthDate(),
                        ageAt(event.date(), event.fact().birthDate()),
                        gender(event.fact().gender()),
                        event.fact().currentBelt(),
                        event.fact().personStatus(),
                        event.type(),
                        event.date(),
                        event.fact().firstEnrollmentStartDate(),
                        event.fact().enrollmentStartDate(),
                        event.fact().lastEnrollmentEndDate(),
                        event.fact().inferredLeaveDate(),
                        event.fact().inferredLeaveDate() == null
                                ? null : "LAST_ENROLLMENT_END_DATE",
                        event.fact().leaveDateQuality(),
                        event.fact().courseId(),
                        event.fact().courseName(),
                        event.fact().courseScheduleId(),
                        scheduleLabel(event.fact()),
                        event.fact().branchId(),
                        event.fact().branchName(),
                        event.fact().enrollmentStatus(),
                        monthsBetween(event.fact().enrollmentStartDate(), event.date())
                ))
                .toList();
        return page(items, request.page(), request.size());
    }

    public StudentMovementAnalyticsDTO.TimelineResponse studentTimeline(
            UUID personId,
            StudentMovementAnalyticsDTO.TimelineRequest request
    ) {
        Scope scope = scope(request.from(), request.to(), request.to());
        PersonSnapshot person = repository.findPerson(personId);
        if (person == null) {
            throw new IllegalArgumentException("Person not found: " + personId);
        }
        List<StudentFact> facts = repository.findStudentFacts(new StudentQuery(
                scope.from(), scope.to(), scope.asOf(), null, null, null, null,
                null, null, null, null, null, null, personId));
        List<AttendanceFact> attendance = Boolean.FALSE.equals(request.includeAttendance())
                ? List.of()
                : repository.findAttendanceFacts(new AttendanceQuery(
                scope.from(), scope.to(), scope.asOf(), null, null, null,
                null, null, null, null, null, null, personId));
        List<BookingFact> bookings = Boolean.FALSE.equals(request.includeBookings())
                ? List.of()
                : repository.findBookingFacts(personId, scope.from(), scope.to());

        List<StudentMovementAnalyticsDTO.TimelineEnrollment> enrollments = facts.stream()
                .collect(Collectors.toMap(
                        StudentFact::enrollmentId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new))
                .values().stream()
                .map(fact -> new StudentMovementAnalyticsDTO.TimelineEnrollment(
                        fact.enrollmentId(),
                        fact.enrollmentStartDate(),
                        fact.enrollmentEndDate(),
                        fact.enrollmentStatus(),
                        fact.coursePurchaseId(),
                        fact.trainingPlanId()
                ))
                .toList();
        List<StudentMovementAnalyticsDTO.TimelineSchedule> schedules = facts.stream()
                .map(fact -> new StudentMovementAnalyticsDTO.TimelineSchedule(
                        fact.enrollmentId(),
                        fact.courseScheduleId(),
                        fact.courseId(),
                        fact.courseName(),
                        fact.branchId(),
                        fact.scheduleEffectiveFrom(),
                        fact.scheduleEffectiveTo(),
                        fact.enrollmentScheduleStatus()
                ))
                .toList();
        List<StudentMovementAnalyticsDTO.TimelineAttendance> attendanceItems = attendance.stream()
                .map(fact -> new StudentMovementAnalyticsDTO.TimelineAttendance(
                        fact.sessionId(),
                        fact.sessionDate(),
                        fact.attendanceStatus(),
                        fact.evaluationStatus()
                ))
                .toList();
        List<StudentMovementAnalyticsDTO.TimelineBooking> bookingItems = bookings.stream()
                .map(fact -> new StudentMovementAnalyticsDTO.TimelineBooking(
                        fact.sessionId(),
                        fact.source(),
                        fact.status()
                ))
                .toList();

        List<StudentMovementAnalyticsDTO.TimelineBeltExam> beltExams =
                Boolean.FALSE.equals(request.includeBeltExams())
                        ? List.of()
                        : repository.findBeltExamFacts(new DevelopmentQuery(
                                scope.from(), scope.to(), scope.asOf(), null, null,
                                null, null, null, null, null, null, null, null,
                                null, personId))
                        .stream()
                        .map(exam -> new StudentMovementAnalyticsDTO.TimelineBeltExam(
                                exam.examDate(),
                                exam.fromBelt(),
                                exam.targetBelt(),
                                exam.examType(),
                                exam.examResult()
                        ))
                        .toList();

        List<StudentMovementAnalyticsDTO.TimelineTimelineEvent> events = new ArrayList<>();
        for (StudentFact fact : facts) {
            if (fact.enrollmentStartDate() != null) {
                events.add(new StudentMovementAnalyticsDTO.TimelineTimelineEvent(
                        fact.enrollmentStartDate(), "ENROLLMENT_STARTED",
                        "STUDENT_ENROLLMENT", fact.enrollmentId(), BigDecimal.ONE));
            }
            if (fact.enrollmentEndDate() != null) {
                events.add(new StudentMovementAnalyticsDTO.TimelineTimelineEvent(
                        fact.enrollmentEndDate(), "ENROLLMENT_ENDED",
                        "STUDENT_ENROLLMENT", fact.enrollmentId(), BigDecimal.ONE));
            }
        }
        if (person.inferredLeaveDate() != null) {
            events.add(new StudentMovementAnalyticsDTO.TimelineTimelineEvent(
                    person.inferredLeaveDate(), "INFERRED_LEFT", "PERSON",
                    person.personId(), new BigDecimal("0.75")));
        }
        beltExams.forEach(exam -> events.add(
                new StudentMovementAnalyticsDTO.TimelineTimelineEvent(
                        exam.examDate(), "BELT_EXAM", "BELT_EXAM",
                        null, BigDecimal.ONE)));
        events.sort(Comparator.comparing(
                StudentMovementAnalyticsDTO.TimelineTimelineEvent::eventDate).reversed());

        return new StudentMovementAnalyticsDTO.TimelineResponse(
                new StudentMovementAnalyticsDTO.TimelineStudent(
                        person.personId(), person.personCode(), person.fullName(),
                        person.birthDate(), gender(person.gender()), person.currentBelt(),
                        person.personStatus(), person.personStartDate()
                ),
                new StudentMovementAnalyticsDTO.TimelineLeaveSummary(
                        person.lastEnrollmentEndDate(),
                        person.inferredLeaveDate(),
                        person.inferredLeaveDate() == null
                                ? null : "LAST_ENROLLMENT_END_DATE",
                        person.leaveDateQuality()
                ),
                enrollments,
                schedules,
                attendanceItems,
                bookingItems,
                beltExams,
                events
        );
    }

    public PageResponse<RetentionAnalyticsDTO.StudentsResponse> retentionStudents(
            RetentionAnalyticsDTO.StudentsRequest request
    ) {
        Scope scope = scope(request.from(), request.to(), request.to());
        List<StudentFact> students = repository.findStudentFacts(new StudentQuery(
                scope.from(), scope.to(), scope.asOf(),
                request.branchId() == null ? List.of() : List.of(request.branchId()),
                null, request.trainingPlanId() == null ? List.of() : List.of(request.trainingPlanId()),
                null, null, null, null, null, null, null, null));
        Map<UUID, StudentFact> latest = latestByPerson(students);
        List<RetentionAnalyticsDTO.StudentsResponse> items = latest.values().stream()
                .filter(fact -> request.cohortMonth() == null
                        || YearMonth.from(fact.firstEnrollmentStartDate())
                        .equals(request.cohortMonth()))
                .filter(fact -> request.tenureBand() == null
                        || tenureBand(fact).equalsIgnoreCase(request.tenureBand()))
                .filter(fact -> request.segment() == null
                        || retentionSegmentMatches(fact, request.segment()))
                .map(fact -> {
                    int tenure = fact.inferredLeaveDate() == null
                            ? (int) ChronoUnit.DAYS.between(
                            fact.firstEnrollmentStartDate(), scope.asOf())
                            : (int) ChronoUnit.DAYS.between(
                            fact.firstEnrollmentStartDate(), fact.inferredLeaveDate());
                    return new RetentionAnalyticsDTO.StudentsResponse(
                            fact.personId(),
                            fact.personCode(),
                            fact.fullName(),
                            fact.personStatus(),
                            fact.firstEnrollmentStartDate(),
                            fact.lastEnrollmentEndDate(),
                            fact.inferredLeaveDate(),
                            Math.max(tenure, 0),
                            fact.enrollmentCount(),
                            (int) students.stream()
                                    .filter(candidate -> candidate.personId()
                                            .equals(fact.personId()))
                                    .filter(candidate -> candidate.previousEnrollmentEndDate() != null)
                                    .map(StudentFact::enrollmentId).distinct().count(),
                            fact.trainingPlanId(),
                            fact.courseId(),
                            fact.courseScheduleId(),
                            fact.branchId(),
                            fact.leaveDateQuality()
                    );
                })
                .sorted(Comparator.comparing(
                        RetentionAnalyticsDTO.StudentsResponse::inferredLeaveDate,
                        Comparator.nullsLast(LocalDate::compareTo)).reversed())
                .toList();
        return page(items, request.page(), request.size());
    }

    public ClassHealthAnalyticsDTO.DetailResponse classDetail(
            UUID courseScheduleId,
            ClassHealthAnalyticsDTO.DetailRequest request
    ) {
        Scope scope = scope(request.from(), request.to(), request.asOf());
        List<ClassFact> classFacts = repository.findClassFacts(
                new ClassQuery(scope.from(), scope.to(), null, null,
                        null, null, null, null));
        ClassFact classFact = classFacts.stream()
                .filter(fact -> courseScheduleId.equals(fact.courseScheduleId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Course schedule not found: " + courseScheduleId));
        List<StudentFact> students = repository.findStudentFacts(new StudentQuery(
                scope.from(), scope.to(), scope.asOf(), null, null, null, null,
                null, null, null, null, null, List.of(courseScheduleId), null));
        List<AttendanceFact> attendance = attendance(scope, null, null,
                List.of(courseScheduleId), null, null, null, null, null);
        ClassStat stat = classStats(List.of(classFact), students, attendance, scope).getFirst();
        Map<UUID, RiskAssessment> risks = riskAssessments(
                latestByPerson(students).values(), attendance, scope.asOf());
        List<ClassHealthAnalyticsDTO.DetailRiskDistribution> riskDistribution =
                risks.values().stream()
                        .collect(Collectors.groupingBy(
                                RiskAssessment::level, Collectors.counting()))
                        .entrySet().stream()
                        .map(entry -> new ClassHealthAnalyticsDTO.DetailRiskDistribution(
                                entry.getKey(), entry.getValue().intValue()))
                        .toList();
        List<SessionFact> sessionFacts = repository.findSessionFacts(new SessionQuery(
                scope.from(), scope.to(), null, null, List.of(courseScheduleId),
                null, null, null, null, null));
        List<ClassHealthAnalyticsDTO.DetailStaffAssignment> staffAssignments =
                sessionFacts.stream()
                        .filter(fact -> fact.staffPersonId() != null)
                        .collect(Collectors.toMap(
                                fact -> fact.courseStaffAssignmentId() == null
                                        ? "none-" + fact.staffPersonId()
                                        : fact.courseStaffAssignmentId().toString(),
                                Function.identity(), (left, right) -> left,
                                LinkedHashMap::new))
                        .values().stream()
                        .map(fact -> new ClassHealthAnalyticsDTO.DetailStaffAssignment(
                                fact.staffPersonId(), fact.assignmentType(),
                                fact.assignmentStatus(), fact.assignmentStart(),
                                fact.assignmentEnd()))
                        .toList();
        List<ClassHealthAnalyticsDTO.DetailMonthlyTrend> monthly = months(scope).stream()
                .map(month -> {
                    LocalDate end = month.atEndOfMonth();
                    List<StudentFact> roster = students.stream()
                            .filter(fact -> enrollmentActiveAt(fact, end)).toList();
                    return new ClassHealthAnalyticsDTO.DetailMonthlyTrend(
                            month,
                            distinctPeople(roster),
                            (int) students.stream()
                                    .filter(fact -> YearMonth.from(
                                            fact.enrollmentStartDate()).equals(month))
                                    .map(StudentFact::personId).distinct().count(),
                            (int) students.stream()
                                    .filter(fact -> fact.inferredLeaveDate() != null)
                                    .filter(fact -> YearMonth.from(
                                            fact.inferredLeaveDate()).equals(month))
                                    .map(StudentFact::personId).distinct().count(),
                            attendanceRate(attendance.stream()
                                    .filter(fact -> YearMonth.from(
                                            fact.sessionDate()).equals(month)).toList())
                    );
                })
                .toList();
        return new ClassHealthAnalyticsDTO.DetailResponse(
                new ClassHealthAnalyticsDTO.DetailClassInfo(
                        classFact.courseScheduleId(), classFact.courseId(),
                        classFact.courseName(), classFact.classScheduleId(),
                        classFact.branchId(), classFact.branchName(),
                        classFact.weekday(), classFact.startTime(), classFact.endTime(),
                        classFact.status(), classFact.sessionType()
                ),
                new ClassHealthAnalyticsDTO.DetailSummary(
                        stat.activeRoster(), stat.newStudents(), stat.inferredLeavers(),
                        stat.transferIn(), stat.transferOut(), stat.attendanceRate(),
                        stat.atRiskStudents(), classFact.capacity(), stat.fillRate()
                ),
                monthly,
                riskDistribution,
                staffAssignments
        );
    }

    public PageResponse<ClassHealthAnalyticsDTO.StudentsResponse> classStudents(
            UUID courseScheduleId,
            ClassHealthAnalyticsDTO.StudentsRequest request
    ) {
        Scope scope = scope(request.from(), request.to(), request.asOf());
        List<StudentFact> students = repository.findStudentFacts(new StudentQuery(
                scope.from(), scope.to(), scope.asOf(), null, null, null, null,
                null, null, null, null, null, List.of(courseScheduleId), null));
        List<AttendanceFact> attendance = attendance(scope, null, null,
                List.of(courseScheduleId), null, null, null, null, null);
        List<MovementEvent> classEvents = movementEvents(students, scope).stream()
                .filter(event -> courseScheduleId.equals(event.fact().courseScheduleId()))
                .toList();
        Set<UUID> selected = new HashSet<>();
        String segment = request.segment() == null ? "ACTIVE" : request.segment().toUpperCase();
        switch (segment) {
            case "NEW" -> classEvents.stream().filter(event -> "NEW".equals(event.type()))
                    .map(MovementEvent::personId).forEach(selected::add);
            case "INFERRED_LEFT" -> classEvents.stream()
                    .filter(event -> "INFERRED_LEFT".equals(event.type()))
                    .map(MovementEvent::personId).forEach(selected::add);
            case "TRANSFER_IN" -> classEvents.stream()
                    .filter(event -> "TRANSFER_IN".equals(event.type()))
                    .map(MovementEvent::personId).forEach(selected::add);
            case "TRANSFER_OUT" -> classEvents.stream()
                    .filter(event -> "TRANSFER_OUT".equals(event.type()))
                    .map(MovementEvent::personId).forEach(selected::add);
            case "AT_RISK" -> riskAssessments(
                    latestByPerson(students).values(), attendance, scope.asOf())
                    .values().stream()
                    .filter(risk -> !"LOW".equals(risk.level()))
                    .map(risk -> risk.fact().personId()).forEach(selected::add);
            default -> students.stream()
                    .filter(fact -> enrollmentActiveAt(fact, scope.asOf()))
                    .map(StudentFact::personId).forEach(selected::add);
        }
        Map<UUID, StudentFact> latest = latestByPerson(students);
        Map<UUID, RiskAssessment> risks = riskAssessments(
                latest.values(), attendance, scope.asOf());
        List<ClassHealthAnalyticsDTO.StudentsResponse> items = selected.stream()
                .map(latest::get)
                .filter(Objects::nonNull)
                .map(fact -> {
                    RiskAssessment risk = risks.get(fact.personId());
                    return new ClassHealthAnalyticsDTO.StudentsResponse(
                            fact.personId(), fact.personCode(), fact.fullName(),
                            ageAt(scope.asOf(), fact.birthDate()),
                            gender(fact.gender()), fact.currentBelt(),
                            fact.personStatus(), fact.enrollmentId(),
                            fact.enrollmentStartDate(), fact.enrollmentEndDate(),
                            fact.scheduleEffectiveFrom(), fact.scheduleEffectiveTo(),
                            fact.inferredLeaveDate(), fact.leaveDateQuality(),
                            risk == null ? null : risk.rate30d(),
                            risk == null ? 0 : risk.consecutiveAbsences(),
                            risk == null ? "LOW" : risk.level()
                    );
                })
                .sorted(Comparator.comparing(
                        ClassHealthAnalyticsDTO.StudentsResponse::fullName,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        return page(items, request.page(), request.size());
    }

    public PageResponse<AttendanceRiskAnalyticsDTO.StudentsResponse> attendanceRiskStudents(
            AttendanceRiskAnalyticsDTO.StudentsRequest request
    ) {
        Scope scope = scope(request.from(), request.to(), request.to());
        List<AttendanceFact> attendance = repository.findAttendanceFacts(new AttendanceQuery(
                minDate(scope.from(), scope.asOf().minusDays(90)),
                maxDate(scope.to(), scope.asOf()), scope.asOf(),
                request.branchId() == null ? List.of() : List.of(request.branchId()),
                null, null, request.courseScheduleId(), null, null, null,
                null, null, null));
        Map<UUID, StudentFact> latest = latestStudentsFromAttendance(attendance, scope.asOf());
        List<AttendanceRiskAnalyticsDTO.StudentsResponse> items = riskAssessments(
                latest.values(), attendance, scope.asOf()).values().stream()
                .filter(risk -> request.riskLevel() == null
                        || request.riskLevel().equalsIgnoreCase(risk.level()))
                .filter(risk -> request.riskReason() == null
                        || risk.reasons().contains(request.riskReason().toUpperCase()))
                .sorted(Comparator.comparing(RiskAssessment::level)
                        .thenComparing(RiskAssessment::rate30d,
                                Comparator.nullsFirst(BigDecimal::compareTo)))
                .map(risk -> new AttendanceRiskAnalyticsDTO.StudentsResponse(
                        risk.fact().personId(),
                        risk.fact().personCode(),
                        risk.fact().fullName(),
                        risk.fact().personStatus(),
                        risk.fact().branchId(),
                        risk.fact().courseScheduleId(),
                        risk.lastAttendanceDate(),
                        risk.daysSinceLastAttendance(),
                        risk.rate30d(),
                        risk.previousRate30d(),
                        risk.consecutiveAbsences(),
                        risk.excusedAbsences30d(),
                        risk.level(),
                        risk.reasons()
                ))
                .toList();
        return page(items, request.page(), request.size());
    }

    public PageResponse<RecruitmentAnalyticsDTO.StudentsResponse> recruitmentStudents(
            RecruitmentAnalyticsDTO.StudentsRequest request
    ) {
        Scope scope = scope(request.from(), request.to(), request.to());
        List<StudentFact> students = repository.findStudentFacts(new StudentQuery(
                scope.from(), scope.to(), scope.asOf(),
                request.branchId() == null ? List.of() : List.of(request.branchId()),
                request.courseId() == null ? List.of() : List.of(request.courseId()),
                null, null,
                request.gender() == null ? List.of() : List.of(request.gender()),
                request.age(), request.age(), null, null,
                request.courseScheduleId() == null
                        ? List.of() : List.of(request.courseScheduleId()), null));
        List<RecruitmentAnalyticsDTO.StudentsResponse> items = latestByPerson(students).values()
                .stream()
                .filter(fact -> inRange(fact.firstEnrollmentStartDate(),
                        scope.from(), scope.to()))
                .filter(fact -> request.month() == null
                        || YearMonth.from(fact.firstEnrollmentStartDate())
                        .equals(request.month()))
                .filter(fact -> request.age() == null
                        || ageAt(fact.firstEnrollmentStartDate(),
                        fact.birthDate()) == request.age())
                .filter(fact -> request.acquisitionType() == null
                        || acquisitionType(fact).equalsIgnoreCase(request.acquisitionType()))
                .map(fact -> new RecruitmentAnalyticsDTO.StudentsResponse(
                        fact.personId(),
                        fact.personCode(),
                        fact.fullName(),
                        fact.birthDate(),
                        ageAt(fact.firstEnrollmentStartDate(), fact.birthDate()),
                        gender(fact.gender()),
                        fact.firstEnrollmentStartDate(),
                        fact.courseId(),
                        fact.courseName(),
                        fact.courseScheduleId(),
                        fact.branchId(),
                        fact.trainingPlanId(),
                        acquisitionType(fact),
                        "ENROLLMENT_DERIVED"
                ))
                .sorted(Comparator.comparing(
                        RecruitmentAnalyticsDTO.StudentsResponse::firstEnrollmentStartDate,
                        Comparator.nullsLast(LocalDate::compareTo)).reversed())
                .toList();
        return page(items, request.page(), request.size());
    }

    public PageResponse<CoursePlanAnalyticsDTO.StudentsResponse> trainingPlanStudents(
            UUID trainingPlanId,
            CoursePlanAnalyticsDTO.StudentsRequest request
    ) {
        Scope scope = scope(request.from(), request.to(), request.to());
        List<StudentFact> allStudentFacts = repository.findStudentFacts(new StudentQuery(
                scope.from(), scope.to(), scope.asOf(),
                request.branchId() == null ? List.of() : List.of(request.branchId()),
                null, List.of(trainingPlanId), null, null, null, null,
                null, null, null, null));
        List<AttendanceFact> attendance = attendance(scope,
                request.branchId() == null ? List.of() : List.of(request.branchId()),
                null, null, null, null, null, null, null);
        Map<UUID, Long> attendedByEnrollment = attendance.stream()
                .filter(fact -> fact.enrollmentId() != null)
                .filter(fact -> isAttended(fact.attendanceStatus()))
                .collect(Collectors.groupingBy(AttendanceFact::enrollmentId,
                        Collectors.mapping(AttendanceFact::sessionId,
                                Collectors.collectingAndThen(Collectors.toSet(),
                                        set -> (long) set.size()))));
        String segment = request.segment() == null
                ? null : request.segment().toUpperCase();
        List<CoursePlanAnalyticsDTO.StudentsResponse> items = allStudentFacts.stream()
                .filter(fact -> segment == null || switch (segment) {
                    case "ACTIVE" -> enrollmentActiveAt(fact, scope.asOf());
                    case "INFERRED_LEFT" -> "INACTIVE".equals(fact.personStatus());
                    case "NOT_RENEWED" -> !hasSubsequentEnrollment(fact, allStudentFacts);
                    case "RENEWED" -> hasSubsequentEnrollment(fact, allStudentFacts);
                    default -> true;
                })
                .map(fact -> {
                    long used = attendedByEnrollment.getOrDefault(fact.enrollmentId(), 0L);
                    int included = Objects.requireNonNullElse(fact.includedSessionCount(), 0);
                    return new CoursePlanAnalyticsDTO.StudentsResponse(
                            fact.personId(),
                            fact.personCode(),
                            fact.fullName(),
                            fact.coursePurchaseId(),
                            fact.enrollmentId(),
                            fact.enrollmentStartDate(),
                            fact.enrollmentEndDate(),
                            fact.enrollmentStatus(),
                            fact.includedSessionCount(),
                            (int) used,
                            rate(used, included),
                            hasSubsequentEnrollment(fact, allStudentFacts),
                            fact.inferredLeaveDate(),
                            fact.personStatus()
                    );
                })
                .sorted(Comparator.comparing(
                        CoursePlanAnalyticsDTO.StudentsResponse::enrollmentStartDate,
                        Comparator.nullsLast(LocalDate::compareTo)).reversed())
                .toList();
        return page(items, request.page(), request.size());
    }

    public PageResponse<BranchAnalyticsDTO.ClassesResponse> branchClasses(
            Long branchId,
            BranchAnalyticsDTO.ClassesRequest request
    ) {
        Scope scope = scope(request.from(), request.to(), request.asOf());
        List<StudentFact> students = repository.findStudentFacts(new StudentQuery(
                scope.from(), scope.to(), scope.asOf(), List.of(branchId),
                null, null, null, null, null, null, null, null, null, null));
        List<AttendanceFact> attendance = attendance(scope, List.of(branchId),
                null, null, null, null, null, null, null);
        List<ClassFact> classes = repository.findClassFacts(new ClassQuery(
                scope.from(), scope.to(), List.of(branchId), null, branchId,
                null, null, request.status()));
        List<ClassStat> stats = sortClasses(
                classStats(classes, students, attendance, scope),
                request.sortBy(), null);
        List<BranchAnalyticsDTO.ClassesResponse> items = stats.stream()
                .map(stat -> new BranchAnalyticsDTO.ClassesResponse(
                        stat.classFact().courseScheduleId(),
                        stat.classFact().courseId(),
                        stat.classFact().courseName(),
                        stat.classFact().classScheduleId(),
                        stat.classFact().weekday(),
                        stat.classFact().startTime(),
                        stat.classFact().endTime(),
                        stat.classFact().status(),
                        stat.activeRoster(),
                        stat.newStudents(),
                        stat.inferredLeavers(),
                        stat.transferIn(),
                        stat.transferOut(),
                        stat.attendanceRate(),
                        stat.classFact().capacity(),
                        stat.fillRate(),
                        stat.warnings()
                ))
                .toList();
        return page(items, request.page(), request.size());
    }

    public PageResponse<CoachAnalyticsDTO.SessionsResponse> coachSessions(
            UUID staffPersonId,
            CoachAnalyticsDTO.SessionsRequest request
    ) {
        Scope scope = scope(request.from(), request.to(), request.to());
        List<SessionFact> sessions = repository.findSessionFacts(new SessionQuery(
                scope.from(), scope.to(), null, request.branchId(), null,
                staffPersonId, null, null, request.sessionStatus(),
                request.timesheetStatus()));
        Map<UUID, List<AttendanceFact>> attendanceBySession = attendance(scope,
                request.branchId() == null ? List.of() : List.of(request.branchId()),
                null, null, null, null, null, null, null)
                .stream().collect(Collectors.groupingBy(AttendanceFact::sessionId));
        List<CoachAnalyticsDTO.SessionsResponse> items = sessions.stream()
                .collect(Collectors.toMap(SessionFact::sessionId, Function.identity(),
                        (left, right) -> right, LinkedHashMap::new))
                .values().stream()
                .map(fact -> new CoachAnalyticsDTO.SessionsResponse(
                        fact.sessionId(),
                        fact.sessionDate(),
                        fact.courseScheduleId(),
                        fact.courseName(),
                        fact.branchId(),
                        fact.assignmentType(),
                        fact.sessionStatus(),
                        fact.startTime(),
                        fact.endTime(),
                        fact.checkInTime(),
                        fact.checkOutTime(),
                        fact.timesheetComplete(),
                        attendanceRate(attendanceBySession.getOrDefault(
                                fact.sessionId(), List.of()))
                ))
                .toList();
        return page(items, request.page(), request.size());
    }

    public PageResponse<DevelopmentAnalyticsDTO.StudentsResponse> developmentStudents(
            DevelopmentAnalyticsDTO.StudentsRequest request
    ) {
        Scope scope = scope(request.from(), request.to(), request.to());
        DevelopmentQuery examQuery = new DevelopmentQuery(
                scope.from(),
                scope.to(),
                scope.asOf(),
                request.branchId() == null ? List.of() : List.of(request.branchId()),
                request.branchId(),
                request.belt() == null ? List.of() : List.of(request.belt()),
                request.belt(),
                null,
                request.examType() == null ? List.of() : List.of(request.examType()),
                request.examType(),
                request.examResult() == null ? List.of() : List.of(request.examResult()),
                request.examResult(),
                null,
                null,
                null
        );
        List<BeltExamFact> exams = repository.findBeltExamFacts(examQuery);
        DevelopmentQuery fitnessQuery = new DevelopmentQuery(
                scope.from(),
                scope.to(),
                scope.asOf(),
                request.branchId() == null ? List.of() : List.of(request.branchId()),
                request.branchId(),
                request.belt() == null ? List.of() : List.of(request.belt()),
                request.belt(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        List<FitnessFact> fitness = repository.findFitnessFacts(fitnessQuery);
        if (request.fitnessId() != null) {
            fitness = fitness.stream()
                    .filter(fact -> request.fitnessId().equals(fact.fitnessId()))
                    .toList();
        }
        Map<UUID, BeltExamFact> latestExam = exams.stream()
                .collect(Collectors.toMap(BeltExamFact::personId, Function.identity(),
                        (left, right) -> left.examDate() != null
                                && (right.examDate() == null
                                || left.examDate().isAfter(right.examDate())) ? left : right,
                        LinkedHashMap::new));
        Map<UUID, FitnessFact> latestFitness = fitness.stream()
                .collect(Collectors.toMap(FitnessFact::personId, Function.identity(),
                        (left, right) -> left.recordDate().isAfter(right.recordDate())
                                ? left : right, LinkedHashMap::new));
        Set<UUID> people = new LinkedHashSet<>();
        people.addAll(latestExam.keySet());
        people.addAll(latestFitness.keySet());
        List<DevelopmentAnalyticsDTO.StudentsResponse> items = people.stream()
                .map(personId -> {
                    BeltExamFact exam = latestExam.get(personId);
                    FitnessFact fit = latestFitness.get(personId);
                    BeltExamFact sample = exam != null ? exam : null;
                    return new DevelopmentAnalyticsDTO.StudentsResponse(
                            personId,
                            sample != null ? sample.personCode() : fit.personCode(),
                            sample != null ? sample.fullName() : fit.fullName(),
                            sample != null ? sample.birthDate() : fit.birthDate(),
                            sample != null ? sample.currentBelt() : fit.currentBelt(),
                            exam == null ? null : exam.examDate(),
                            exam == null ? null : exam.fromBelt(),
                            exam == null ? null : exam.targetBelt(),
                            exam == null ? null : exam.examType(),
                            exam == null ? null : exam.examResult(),
                            fit == null ? null : fit.fitnessId(),
                            fit == null ? null : fit.recordDate(),
                            fit == null || fit.duration() == null ? null
                                    : BigDecimal.valueOf(fit.duration())
                    );
                })
                .toList();
        return page(items, request.page(), request.size());
    }

    public PageResponse<FinanceAnalyticsDTO.TransactionsResponse> financeTransactions(
            FinanceAnalyticsDTO.TransactionsRequest request
    ) {
        Scope scope = scope(request.from(), request.to(), request.to());
        FinanceQuery query = new FinanceQuery(
                scope.from(), scope.to(),
                request.branchId() == null ? List.of() : List.of(request.branchId()),
                request.branchId(),
                request.trainingPlanId() == null
                        ? List.of() : List.of(request.trainingPlanId()),
                request.trainingPlanId(),
                request.transactionType() == null
                        ? List.of() : List.of(request.transactionType()),
                request.transactionType(),
                request.direction() == null ? List.of() : List.of(request.direction()),
                request.direction(),
                request.status() == null ? List.of() : List.of(request.status()),
                request.status(),
                request.includeExcluded());
        boolean includeExcluded = Boolean.TRUE.equals(request.includeExcluded());
        List<FinanceAnalyticsDTO.TransactionsResponse> items =
                repository.findFinanceFacts(query).stream()
                        .filter(fact -> includeExcluded || includedFinance(fact))
                        .map(fact -> new FinanceAnalyticsDTO.TransactionsResponse(
                                fact.transactionId(),
                                fact.personId(),
                                fact.transactionType(),
                                fact.direction(),
                                fact.amount(),
                                fact.balanceBefore(),
                                fact.balanceAfter(),
                                fact.status(),
                                fact.createdAt(),
                                fact.reviewedAt(),
                                fact.coursePurchaseId(),
                                fact.trainingPlanId(),
                                fact.attributedBranchId(),
                                fact.migrationSynthetic(),
                                includedFinance(fact),
                                exclusionReason(fact)
                        ))
                        .toList();
        return page(items, request.page(), request.size());
    }

    private List<AttendanceFact> attendance(
            Scope scope,
            Collection<Long> branchIds,
            Collection<UUID> courseIds,
            Collection<UUID> courseScheduleIds,
            Collection<String> levels,
            Collection<String> belts,
            Collection<String> genders,
            Integer minAge,
            Integer maxAge
    ) {
        return repository.findAttendanceFacts(new AttendanceQuery(
                minDate(scope.from(), scope.asOf().minusDays(90)),
                maxDate(scope.to(), scope.asOf()),
                scope.asOf(),
                asList(branchIds),
                asList(courseIds),
                asList(courseScheduleIds),
                null,
                asList(levels),
                asList(belts),
                asList(genders),
                minAge,
                maxAge,
                null
        ));
    }

    private List<FinanceFact> finance(
            Scope scope,
            Collection<Long> branchIds,
            Collection<UUID> trainingPlanIds,
            Boolean includeExcluded
    ) {
        return repository.findFinanceFacts(new FinanceQuery(
                scope.from(),
                scope.to(),
                asList(branchIds),
                null,
                asList(trainingPlanIds),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                includeExcluded
        ));
    }

    private List<AttendanceFact> attendanceForSessions(
            Collection<UUID> sessionIds,
            Scope scope
    ) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> ids = sessionIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return attendance(scope, null, null, null, null, null,
                null, null, null).stream()
                .filter(fact -> ids.contains(fact.sessionId()))
                .toList();
    }

    private Map<UUID, StudentFact> latestByPerson(Collection<StudentFact> facts) {
        Comparator<StudentFact> comparator = Comparator
                .comparing(StudentFact::enrollmentStartDate,
                        Comparator.nullsFirst(LocalDate::compareTo))
                .thenComparing(StudentFact::enrollmentId,
                        Comparator.nullsFirst(UUID::compareTo));
        Map<UUID, StudentFact> result = new LinkedHashMap<>();
        for (StudentFact fact : facts) {
            if (fact.personId() != null) {
                result.merge(fact.personId(), fact,
                        (left, right) -> comparator.compare(left, right) >= 0 ? left : right);
            }
        }
        return result;
    }

    private Map<UUID, StudentFact> earliestByPerson(Collection<StudentFact> facts) {
        Comparator<StudentFact> comparator = Comparator
                .comparing(StudentFact::enrollmentStartDate,
                        Comparator.nullsLast(LocalDate::compareTo))
                .thenComparing(StudentFact::enrollmentId,
                        Comparator.nullsLast(UUID::compareTo));
        Map<UUID, StudentFact> result = new LinkedHashMap<>();
        for (StudentFact fact : facts) {
            if (fact.personId() != null) {
                result.merge(fact.personId(), fact,
                        (left, right) -> comparator.compare(left, right) <= 0 ? left : right);
            }
        }
        return result;
    }

    private Map<UUID, StudentFact> latestStudentsFromAttendance(
            Collection<AttendanceFact> attendance,
            LocalDate asOf
    ) {
        Map<UUID, AttendanceFact> latestAttendance = new LinkedHashMap<>();
        for (AttendanceFact fact : attendance) {
            if (fact.personId() != null) {
                latestAttendance.merge(fact.personId(), fact,
                        (left, right) -> left.sessionDate().isAfter(right.sessionDate())
                                ? left : right);
            }
        }
        return latestAttendance.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> studentFactFromAttendance(entry.getValue(), null),
                (left, right) -> left,
                LinkedHashMap::new));
    }

    private StudentFact studentFactFromAttendance(AttendanceFact fact,
                                                  StudentFact fallback) {
        if (fallback != null) {
            return fallback;
        }
        return new StudentFact(
                fact.personId(), fact.personCode(), fact.fullName(), fact.birthDate(),
                fact.gender(), fact.currentBelt(), fact.personStatus(), null,
                fact.enrollmentStartDate(), fact.enrollmentEndDate(),
                "INACTIVE".equals(fact.personStatus())
                        ? fact.enrollmentEndDate() : null,
                "INACTIVE".equals(fact.personStatus()) && fact.enrollmentEndDate() != null
                        ? "DERIVED" : null,
                null, null, fact.enrollmentId(), fact.enrollmentStartDate(),
                fact.enrollmentEndDate(), null, null, null, null, null, null,
                null, null, null, null, null,
                fact.courseScheduleId(), null, null, null, null,
                fact.courseId(), fact.courseName(), fact.courseLevel(), null, null,
                fact.classScheduleId(), null, null, null, null,
                fact.branchId(), fact.branchName(), null, null, null, null, null, null
        );
    }

    private List<MovementEvent> movementEvents(
            Collection<StudentFact> facts,
            Scope scope
    ) {
        List<MovementEvent> events = new ArrayList<>();
        for (StudentFact fact : earliestByPerson(facts).values()) {
            if (inRange(fact.firstEnrollmentStartDate(), scope.from(), scope.to())
                    && sameDate(fact.firstEnrollmentStartDate(), fact.enrollmentStartDate())) {
                events.add(new MovementEvent("NEW", fact.firstEnrollmentStartDate(), fact));
            }
        }
        for (StudentFact fact : facts) {
            if (fact.enrollmentStartDate() == null
                    || !inRange(fact.enrollmentStartDate(), scope.from(), scope.to())) {
                continue;
            }
            if (fact.previousEnrollmentEndDate() != null
                    && ChronoUnit.DAYS.between(fact.previousEnrollmentEndDate(),
                    fact.enrollmentStartDate()) >= REACTIVATION_GAP_DAYS) {
                events.add(new MovementEvent("REACTIVATED", fact.enrollmentStartDate(), fact));
            }
        }
        for (StudentFact fact : latestByPerson(facts).values()) {
            if ("INACTIVE".equals(fact.personStatus())
                    && inRange(fact.inferredLeaveDate(), scope.from(), scope.to())
                    && !fact.inferredLeaveDate().isAfter(scope.asOf())) {
                events.add(new MovementEvent("INFERRED_LEFT", fact.inferredLeaveDate(), fact));
            }
        }
        for (TransferEvent transfer : transferEvents(facts, scope)) {
            events.add(new MovementEvent("TRANSFER_OUT", transfer.date(), transfer.fromFact()));
            events.add(new MovementEvent("TRANSFER_IN", transfer.date(), transfer.toFact()));
        }
        return events;
    }

    private List<TransferEvent> transferEvents(Collection<StudentFact> facts,
                                               Scope scope) {
        Map<UUID, List<StudentFact>> byPerson = facts.stream()
                .filter(fact -> fact.enrollmentStartDate() != null)
                .collect(Collectors.groupingBy(StudentFact::personId));
        List<TransferEvent> transfers = new ArrayList<>();
        for (Map.Entry<UUID, List<StudentFact>> entry : byPerson.entrySet()) {
            List<StudentFact> ordered = entry.getValue().stream()
                    .collect(Collectors.toMap(StudentFact::enrollmentId, Function.identity(),
                            (left, right) -> left, LinkedHashMap::new))
                    .values().stream()
                    .sorted(Comparator.comparing(StudentFact::enrollmentStartDate))
                    .toList();
            for (int index = 1; index < ordered.size(); index++) {
                StudentFact previous = ordered.get(index - 1);
                StudentFact current = ordered.get(index);
                if (inRange(current.enrollmentStartDate(), scope.from(), scope.to())
                        && previous.branchId() != null
                        && current.branchId() != null
                        && !previous.branchId().equals(current.branchId())) {
                    transfers.add(new TransferEvent(
                            entry.getKey(),
                            previous.branchId(),
                            current.branchId(),
                            current.enrollmentStartDate(),
                            previous,
                            current
                    ));
                }
            }
        }
        return transfers;
    }

    private List<ClassStat> classStats(
            Collection<ClassFact> classes,
            Collection<StudentFact> students,
            Collection<AttendanceFact> attendance,
            Scope scope
    ) {
        List<MovementEvent> events = movementEvents(students, scope);
        Map<UUID, RiskAssessment> risks = riskAssessments(
                latestByPerson(students).values(), attendance, scope.asOf());
        return classes.stream().map(classFact -> {
            List<StudentFact> roster = students.stream()
                    .filter(fact -> classFact.courseScheduleId().equals(fact.courseScheduleId()))
                    .filter(fact -> enrollmentActiveAt(fact, scope.asOf()))
                    .toList();
            int activeRoster = distinctPeople(roster);
            int newStudents = (int) students.stream()
                    .filter(fact -> classFact.courseScheduleId().equals(fact.courseScheduleId()))
                    .filter(fact -> inRange(fact.firstEnrollmentStartDate(),
                            scope.from(), scope.to()))
                    .map(StudentFact::personId).distinct().count();
            int leavers = (int) events.stream()
                    .filter(event -> "INFERRED_LEFT".equals(event.type()))
                    .filter(event -> classFact.courseScheduleId()
                            .equals(event.fact().courseScheduleId()))
                    .map(MovementEvent::personId).distinct().count();
            int transferIn = (int) events.stream()
                    .filter(event -> "TRANSFER_IN".equals(event.type()))
                    .filter(event -> classFact.courseScheduleId()
                            .equals(event.fact().courseScheduleId()))
                    .map(MovementEvent::personId).distinct().count();
            int transferOut = (int) events.stream()
                    .filter(event -> "TRANSFER_OUT".equals(event.type()))
                    .filter(event -> classFact.courseScheduleId()
                            .equals(event.fact().courseScheduleId()))
                    .map(MovementEvent::personId).distinct().count();
            BigDecimal attendanceRate = attendanceRate(attendance.stream()
                    .filter(fact -> classFact.courseScheduleId()
                            .equals(fact.courseScheduleId())).toList());
            int capacity = Objects.requireNonNullElse(classFact.capacity(), 0);
            BigDecimal fillRate = capacity <= 0 ? null : rate(activeRoster, capacity);
            int atRisk = (int) risks.values().stream()
                    .filter(risk -> classFact.courseScheduleId()
                            .equals(risk.fact().courseScheduleId()))
                    .filter(risk -> !"LOW".equals(risk.level()))
                    .count();
            List<String> warnings = new ArrayList<>();
            if (attendanceRate != null
                    && attendanceRate.compareTo(new BigDecimal("0.70")) < 0) {
                warnings.add("LOW_ATTENDANCE");
            }
            if (leavers > 0) {
                warnings.add("INFERRED_LEAVERS");
            }
            if (fillRate != null && fillRate.compareTo(new BigDecimal("0.50")) < 0) {
                warnings.add("LOW_FILL_RATE");
            }
            if (fillRate != null && fillRate.compareTo(BigDecimal.ONE) > 0) {
                warnings.add("OVER_CAPACITY");
            }
            if (atRisk > 0) {
                warnings.add("AT_RISK_STUDENTS");
            }
            return new ClassStat(classFact, activeRoster, newStudents, leavers,
                    transferIn, transferOut, attendanceRate, fillRate, atRisk,
                    List.copyOf(warnings));
        }).toList();
    }

    private List<ClassStat> sortClasses(
            List<ClassStat> stats,
            String sortBy,
            String sortDirection
    ) {
        Comparator<ClassStat> comparator = switch (normalizeSort(sortBy)) {
            case "attendance" -> Comparator.comparing(ClassStat::attendanceRate,
                    Comparator.nullsLast(BigDecimal::compareTo));
            case "fill", "fillrate" -> Comparator.comparing(ClassStat::fillRate,
                    Comparator.nullsLast(BigDecimal::compareTo));
            case "roster", "activeroster" -> Comparator.comparingInt(ClassStat::activeRoster);
            case "leavers", "inferredleavers" -> Comparator.comparingInt(ClassStat::inferredLeavers);
            default -> Comparator.comparing(
                    stat -> stat.classFact().courseName(),
                    Comparator.nullsLast(String::compareToIgnoreCase));
        };
        if ("DESC".equalsIgnoreCase(sortDirection)
                || (sortDirection == null && !"course".equals(normalizeSort(sortBy)))) {
            comparator = comparator.reversed();
        }
        return stats.stream().sorted(comparator).toList();
    }

    private String normalizeSort(String sortBy) {
        return sortBy == null ? "course"
                : sortBy.replace("_", "").replace("-", "").toLowerCase();
    }

    private Map<UUID, RiskAssessment> riskAssessments(
            Collection<StudentFact> students,
            Collection<AttendanceFact> attendance,
            LocalDate asOf
    ) {
        Map<UUID, List<AttendanceFact>> byPerson = attendance.stream()
                .filter(fact -> fact.personId() != null)
                .collect(Collectors.groupingBy(AttendanceFact::personId));
        Map<UUID, RiskAssessment> result = new LinkedHashMap<>();
        for (StudentFact student : students) {
            if (student.personId() == null) {
                continue;
            }
            List<AttendanceFact> personAttendance = byPerson.getOrDefault(
                            student.personId(), List.of()).stream()
                    .filter(fact -> fact.sessionDate() != null)
                    .filter(fact -> !fact.sessionDate().isAfter(asOf))
                    .sorted(Comparator.comparing(AttendanceFact::sessionDate).reversed())
                    .toList();
            LocalDate currentStart = asOf.minusDays(30);
            LocalDate previousStart = asOf.minusDays(60);
            List<AttendanceFact> last30 = personAttendance.stream()
                    .filter(fact -> !fact.sessionDate().isBefore(currentStart))
                    .toList();
            List<AttendanceFact> previous30 = personAttendance.stream()
                    .filter(fact -> !fact.sessionDate().isBefore(previousStart))
                    .filter(fact -> fact.sessionDate().isBefore(currentStart))
                    .toList();
            LocalDate lastAttendanceDate = personAttendance.stream()
                    .filter(fact -> isAttended(fact.attendanceStatus()))
                    .map(AttendanceFact::sessionDate)
                    .findFirst()
                    .orElse(null);
            int daysSinceLastAttendance = lastAttendanceDate == null
                    ? Integer.MAX_VALUE
                    : (int) ChronoUnit.DAYS.between(lastAttendanceDate, asOf);
            int consecutiveAbsences = 0;
            for (AttendanceFact fact : personAttendance) {
                if (isAttended(fact.attendanceStatus())) {
                    break;
                }
                if ("ABSENT".equals(fact.attendanceStatus()) || fact.attendanceStatus() == null) {
                    consecutiveAbsences++;
                }
            }
            BigDecimal currentRate = attendanceRate(last30);
            BigDecimal previousRate = attendanceRate(previous30);
            int excused = countStatus(last30, "EXCUSED");
            List<String> reasons = new ArrayList<>();
            if (consecutiveAbsences >= CONSECUTIVE_ABSENCE_LIMIT) {
                reasons.add("CONSECUTIVE_ABSENCES");
            }
            if (daysSinceLastAttendance >= DAYS_WITHOUT_ATTENDANCE_LIMIT) {
                reasons.add("NO_ATTENDANCE_14_DAYS");
            }
            if (currentRate != null && previousRate != null
                    && previousRate.subtract(currentRate)
                    .compareTo(ATTENDANCE_DROP_THRESHOLD) >= 0) {
                reasons.add("ATTENDANCE_DROP");
            }
            if (currentRate != null && currentRate.compareTo(new BigDecimal("0.60")) < 0) {
                reasons.add("LOW_ATTENDANCE");
            }
            if (excused >= 2) {
                reasons.add("RECENT_EXCUSED_ABSENCES");
            }
            String level = "LOW";
            if (consecutiveAbsences >= CONSECUTIVE_ABSENCE_LIMIT
                    || daysSinceLastAttendance >= DAYS_WITHOUT_ATTENDANCE_LIMIT
                    || (currentRate != null
                    && currentRate.compareTo(new BigDecimal("0.40")) < 0)) {
                level = "HIGH";
            } else if (!reasons.isEmpty()) {
                level = "MEDIUM";
            }
            result.put(student.personId(), new RiskAssessment(
                    student,
                    lastAttendanceDate,
                    daysSinceLastAttendance == Integer.MAX_VALUE
                            ? null : daysSinceLastAttendance,
                    currentRate,
                    previousRate,
                    consecutiveAbsences,
                    excused,
                    level,
                    List.copyOf(reasons)
            ));
        }
        return result;
    }

    private RetentionStats retentionStats(
            List<StudentFact> facts,
            Map<UUID, StudentFact> latest,
            Scope scope,
            int graceDays
    ) {
        int eligible = latest.size();
        int leavers = (int) latest.values().stream()
                .filter(fact -> "INACTIVE".equals(fact.personStatus()))
                .filter(fact -> inRange(fact.inferredLeaveDate(), scope.from(), scope.to()))
                .count();
        long eligibleRenewals = facts.stream()
                .filter(fact -> fact.enrollmentEndDate() != null)
                .filter(fact -> !fact.enrollmentEndDate().isAfter(scope.asOf()))
                .map(StudentFact::enrollmentId).distinct().count();
        long renewed = facts.stream()
                .filter(fact -> fact.enrollmentEndDate() != null)
                .filter(fact -> !fact.enrollmentEndDate().isAfter(scope.asOf()))
                .filter(fact -> hasSubsequentEnrollment(fact, facts, graceDays))
                .map(StudentFact::enrollmentId).distinct().count();
        List<Integer> tenure = latest.values().stream()
                .filter(fact -> fact.firstEnrollmentStartDate() != null)
                .map(fact -> (int) ChronoUnit.DAYS.between(
                        fact.firstEnrollmentStartDate(),
                        fact.inferredLeaveDate() == null
                                ? scope.asOf() : fact.inferredLeaveDate()))
                .sorted()
                .toList();
        int median = tenure.isEmpty() ? 0
                : tenure.get((tenure.size() - 1) / 2);
        return new RetentionStats(
                eligible,
                leavers,
                rate(renewed, eligibleRenewals),
                median
        );
    }

    private List<RetentionAnalyticsDTO.CohortRow> cohorts(
            List<StudentFact> facts,
            Scope scope
    ) {
        Map<YearMonth, List<StudentFact>> byCohort = latestByPerson(facts).values().stream()
                .filter(fact -> fact.firstEnrollmentStartDate() != null)
                .filter(fact -> inRange(fact.firstEnrollmentStartDate(), scope.from(), scope.to()))
                .collect(Collectors.groupingBy(fact -> YearMonth.from(
                        fact.firstEnrollmentStartDate()), LinkedHashMap::new, Collectors.toList()));
        return byCohort.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    LocalDate cohortDate = entry.getKey().atDay(1);
                    List<StudentFact> cohortFacts = facts.stream()
                            .filter(fact -> entry.getValue().stream()
                                    .anyMatch(student -> student.personId()
                                            .equals(fact.personId())))
                            .toList();
                    return new RetentionAnalyticsDTO.CohortRow(
                            entry.getKey(),
                            entry.getValue().size(),
                            cohortRetention(cohortFacts, cohortDate, 1),
                            cohortRetention(cohortFacts, cohortDate, 2),
                            cohortRetention(cohortFacts, cohortDate, 3),
                            cohortRetention(cohortFacts, cohortDate, 6),
                            cohortRetention(cohortFacts, cohortDate, 12),
                            !cohortDate.plusMonths(12).isAfter(scope.to())
                    );
                })
                .toList();
    }

    private BigDecimal cohortRetention(
            List<StudentFact> cohortFacts,
            LocalDate cohortDate,
            int month
    ) {
        long initial = cohortFacts.stream().map(StudentFact::personId).distinct().count();
        long retained = cohortFacts.stream()
                .filter(fact -> enrollmentActiveAt(fact, cohortDate.plusMonths(month)))
                .map(StudentFact::personId).distinct().count();
        return rate(retained, initial);
    }

    private List<RetentionAnalyticsDTO.TenureRow> tenureRows(
            Map<UUID, StudentFact> latest
    ) {
        Map<String, List<StudentFact>> groups = latest.values().stream()
                .collect(Collectors.groupingBy(this::tenureBand,
                        LinkedHashMap::new, Collectors.toList()));
        return groups.entrySet().stream().map(entry -> new RetentionAnalyticsDTO.TenureRow(
                entry.getKey(), entry.getValue().size(),
                (int) entry.getValue().stream()
                        .filter(fact -> "INACTIVE".equals(fact.personStatus())).count()
        )).toList();
    }

    private List<RetentionAnalyticsDTO.SegmentRow> retentionSegments(List<StudentFact> facts) {
        Map<UUID, StudentFact> latest = latestByPerson(facts);
        List<RetentionAnalyticsDTO.SegmentRow> rows = new ArrayList<>();
        addSegmentRows(rows, "BRANCH", latest.values(), StudentFact::branchId,
                StudentFact::branchName);
        addSegmentRows(rows, "TRAINING_PLAN", latest.values(), StudentFact::trainingPlanId,
                StudentFact::trainingPlanName);
        Map<String, List<StudentFact>> byGender = latest.values().stream()
                .collect(Collectors.groupingBy(fact -> gender(fact.gender())));
        byGender.forEach((key, value) -> rows.add(new RetentionAnalyticsDTO.SegmentRow(
                "GENDER", key, value.size(),
                (int) value.stream().filter(fact -> "INACTIVE".equals(fact.personStatus()))
                        .count(),
                rate(value.stream().filter(fact -> "INACTIVE".equals(fact.personStatus()))
                        .count(), value.size())
        )));
        return rows;
    }

    private <K> void addSegmentRows(
            List<RetentionAnalyticsDTO.SegmentRow> rows,
            String dimension,
            Collection<StudentFact> facts,
            Function<StudentFact, K> key,
            Function<StudentFact, String> label
    ) {
        facts.stream().filter(fact -> key.apply(fact) != null)
                .collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()))
                .forEach((id, values) -> rows.add(new RetentionAnalyticsDTO.SegmentRow(
                        dimension,
                        label.apply(values.getFirst()),
                        values.size(),
                        (int) values.stream()
                                .filter(fact -> "INACTIVE".equals(fact.personStatus())).count(),
                        rate(values.stream()
                                        .filter(fact -> "INACTIVE".equals(fact.personStatus())).count(),
                                values.size())
                )));
    }

    private List<RetentionAnalyticsDTO.ExpiryRow> expiries(
            List<StudentFact> facts,
            YearMonth from,
            YearMonth to,
            int graceDays
    ) {
        return months(from, to).stream().map(month -> {
            List<StudentFact> due = facts.stream()
                    .filter(fact -> fact.enrollmentEndDate() != null)
                    .filter(fact -> YearMonth.from(fact.enrollmentEndDate()).equals(month))
                    .toList();
            int renewed = (int) due.stream()
                    .filter(fact -> hasSubsequentEnrollment(fact, facts, graceDays))
                    .map(StudentFact::enrollmentId).distinct().count();
            int pending = (int) due.stream()
                    .filter(fact -> !hasSubsequentEnrollment(fact, facts, graceDays))
                    .filter(fact -> fact.enrollmentEndDate()
                            .plusDays(graceDays).isAfter(LocalDate.now()))
                    .count();
            return new RetentionAnalyticsDTO.ExpiryRow(
                    month,
                    due.size(),
                    renewed,
                    Math.max(due.size() - renewed - pending, 0),
                    pending
            );
        }).toList();
    }

    private boolean hasSubsequentEnrollment(StudentFact fact,
                                            Collection<StudentFact> facts) {
        return hasSubsequentEnrollment(fact, facts, 7);
    }

    private boolean hasSubsequentEnrollment(
            StudentFact fact,
            Collection<StudentFact> facts,
            int graceDays
    ) {
        if (fact.enrollmentEndDate() == null || fact.personId() == null) {
            return false;
        }
        LocalDate maxDate = fact.enrollmentEndDate().plusDays(graceDays);
        return facts.stream()
                .anyMatch(candidate -> fact.personId().equals(candidate.personId())
                        && candidate.enrollmentId() != null
                        && !candidate.enrollmentId().equals(fact.enrollmentId())
                        && candidate.enrollmentStartDate() != null
                        && candidate.enrollmentStartDate().isAfter(fact.enrollmentStartDate())
                        && !candidate.enrollmentStartDate().isAfter(maxDate));
    }

    private String tenureBand(StudentFact fact) {
        if (fact.firstEnrollmentStartDate() == null) {
            return "UNKNOWN";
        }
        long days = ChronoUnit.DAYS.between(fact.firstEnrollmentStartDate(),
                fact.inferredLeaveDate() == null ? LocalDate.now() : fact.inferredLeaveDate());
        if (days <= 30) {
            return "0_30_DAYS";
        }
        if (days <= 90) {
            return "31_90_DAYS";
        }
        if (days <= 180) {
            return "91_180_DAYS";
        }
        if (days <= 365) {
            return "181_365_DAYS";
        }
        return "OVER_365_DAYS";
    }

    private boolean retentionSegmentMatches(StudentFact fact,
                                            String segment) {
        String value = segment.toUpperCase();
        if (value.startsWith("BRANCH:")) {
            return fact.branchId() != null
                    && fact.branchId().toString().equalsIgnoreCase(value.substring(7));
        }
        if (value.startsWith("TRAINING_PLAN:")) {
            return fact.trainingPlanId() != null
                    && fact.trainingPlanId().toString().equalsIgnoreCase(value.substring(14));
        }
        if (value.startsWith("GENDER:")) {
            return gender(fact.gender()).equalsIgnoreCase(value.substring(7));
        }
        return tenureBand(fact).equalsIgnoreCase(value)
                || Objects.requireNonNullElse(fact.branchName(), "")
                .equalsIgnoreCase(value)
                || Objects.requireNonNullElse(fact.trainingPlanName(), "")
                .equalsIgnoreCase(value);
    }

    private <K, R> Map<K, R> groupedMovement(
            Collection<MovementEvent> events,
            Function<MovementEvent, K> keyFunction,
            java.util.function.BiFunction<K, MovementCounter, R> mapper
    ) {
        Map<K, MovementAccumulator> accumulators = new LinkedHashMap<>();
        for (MovementEvent event : events) {
            K key = keyFunction.apply(event);
            if (key == null) {
                continue;
            }
            accumulators.computeIfAbsent(key, ignored -> new MovementAccumulator())
                    .add(event);
        }
        Map<K, R> result = new LinkedHashMap<>();
        accumulators.forEach((key, accumulator) ->
                result.put(key, mapper.apply(key, accumulator.toCounter())));
        return result;
    }

    private static final class MovementAccumulator {
        private final Set<UUID> newPeople = new HashSet<>();
        private final Set<UUID> leftPeople = new HashSet<>();
        private final Set<UUID> transferIn = new HashSet<>();
        private final Set<UUID> transferOut = new HashSet<>();
        private StudentFact sampleFact;
        private String branchName;
        private String courseName;

        void add(MovementEvent event) {
            sampleFact = sampleFact == null ? event.fact() : sampleFact;
            branchName = branchName != null ? branchName : event.fact().branchName();
            courseName = courseName != null ? courseName : event.fact().courseName();
            switch (event.type()) {
                case "NEW" -> newPeople.add(event.personId());
                case "INFERRED_LEFT" -> leftPeople.add(event.personId());
                case "TRANSFER_IN" -> transferIn.add(event.personId());
                case "TRANSFER_OUT" -> transferOut.add(event.personId());
                default -> {
                }
            }
        }

        MovementCounter toCounter() {
            return new MovementCounter(
                    newPeople.size(), leftPeople.size(), transferIn.size(),
                    transferOut.size(), sampleFact, branchName, courseName);
        }
    }

    private int countEventPeople(
            Collection<MovementEvent> events,
            LocalDate from,
            LocalDate to,
            String type
    ) {
        return (int) events.stream()
                .filter(event -> type.equals(event.type()))
                .filter(event -> inRange(event.date(), from, to))
                .map(MovementEvent::personId)
                .distinct()
                .count();
    }

    private int countEventPeople(Collection<MovementEvent> events,
                                 Scope scope,
                                 String type) {
        return countEventPeople(events, scope.from(), scope.to(), type);
    }

    private int countEventPeople(
            Collection<MovementEvent> events,
            YearMonth month,
            String type
    ) {
        return countEventPeople(events, month.atDay(1), month.atEndOfMonth(), type);
    }

    private int activeCount(Collection<StudentFact> facts,
                            LocalDate date) {
        return activePeople(facts, date).size();
    }

    private Set<UUID> activePeople(Collection<StudentFact> facts,
                                   LocalDate date) {
        return facts.stream()
                .filter(fact -> enrollmentActiveAt(fact, date))
                .map(StudentFact::personId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean enrollmentActiveAt(StudentFact fact,
                                       LocalDate date) {
        return fact.enrollmentStartDate() != null
                && fact.enrollmentEndDate() != null
                && !fact.enrollmentStartDate().isAfter(date)
                && !fact.enrollmentEndDate().isBefore(date)
                && !"CANCELLED".equals(fact.enrollmentStatus())
                && !isKnownLeaverOn(fact, date);
    }

    private boolean isKnownLeaverOn(StudentFact fact,
                                    LocalDate date) {
        return "INACTIVE".equals(fact.personStatus())
                && fact.inferredLeaveDate() != null
                && !date.isBefore(fact.inferredLeaveDate());
    }

    private static boolean isAttended(String attendanceStatus) {
        return attendanceStatus != null && ATTENDED_STATUSES.contains(attendanceStatus);
    }

    private static boolean isCompletedSession(String sessionStatus) {
        return sessionStatus != null && COMPLETED_SESSION_STATUSES.contains(sessionStatus);
    }

    private BigDecimal attendanceRate(Collection<AttendanceFact> facts) {
        if (facts == null || facts.isEmpty()) {
            return null;
        }
        long attended = facts.stream()
                .filter(fact -> isAttended(fact.attendanceStatus()))
                .count();
        return rate(attended, facts.size());
    }

    private int countStatus(Collection<AttendanceFact> facts,
                            String status) {
        return (int) facts.stream().filter(fact -> status.equals(fact.attendanceStatus())).count();
    }

    private int countExamResult(Collection<BeltExamFact> exams,
                                String result) {
        return (int) exams.stream().filter(exam -> result.equals(exam.examResult())).count();
    }

    private BigDecimal amount(Collection<FinanceFact> facts,
                              String type) {
        return facts.stream()
                .filter(fact -> type.equals(fact.transactionType()))
                .map(FinanceFact::amount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean includedFinance(FinanceFact fact) {
        return "APPROVED".equals(fact.status())
                && !Boolean.TRUE.equals(fact.migrationSynthetic());
    }

    private String exclusionReason(FinanceFact fact) {
        if (includedFinance(fact)) {
            return null;
        }
        if (Boolean.TRUE.equals(fact.migrationSynthetic())) {
            return "MIGRATION_SYNTHETIC";
        }
        if (!"APPROVED".equals(fact.status())) {
            return "NOT_APPROVED";
        }
        return "EXCLUDED";
    }

    private boolean provisionalDate(StudentFact fact) {
        if (fact.enrollmentStartDate() == null || fact.enrollmentCreatedAt() == null) {
            return false;
        }
        return ChronoUnit.DAYS.between(fact.enrollmentStartDate(),
                fact.enrollmentCreatedAt().toLocalDate()) > 90;
    }

    private String acquisitionType(StudentFact fact) {
        if (fact.previousEnrollmentEndDate() != null) {
            return "REACTIVATED";
        }
        return "FIRST_TIME";
    }

    private BigDecimal rate(long numerator,
                            long denominator) {
        if (denominator == 0) {
            return zeroRate();
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(Integer numerator,
                            Integer denominator) {
        return rate(
                numerator == null ? 0L : numerator.longValue(),
                denominator == null ? 0L : denominator.longValue()
        );
    }

    private BigDecimal zeroRate() {
        return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal average(Collection<BigDecimal> values) {
        List<BigDecimal> nonNull = values.stream().filter(Objects::nonNull).toList();
        if (nonNull.isEmpty()) {
            return zeroRate();
        }
        return nonNull.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(nonNull.size()), 4, RoundingMode.HALF_UP);
    }

    private int compare(BigDecimal left,
                        BigDecimal right) {
        if (left == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private String gender(Boolean value) {
        if (value == null) {
            return "UNKNOWN";
        }
        return value ? "MALE" : "FEMALE";
    }

    private Integer ageAt(LocalDate date,
                          LocalDate birthDate) {
        if (date == null || birthDate == null) {
            return null;
        }
        return (int) ChronoUnit.YEARS.between(birthDate, date);
    }

    private int monthsBetween(LocalDate start,
                              LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0;
        }
        return (int) ChronoUnit.MONTHS.between(start, end);
    }

    private String scheduleLabel(StudentFact fact) {
        if (fact == null || fact.weekday() == null) {
            return null;
        }
        return fact.weekday()
                + (fact.startTime() == null ? "" : " " + fact.startTime())
                + (fact.endTime() == null ? "" : "-" + fact.endTime());
    }

    private int distinctPeople(Collection<StudentFact> facts) {
        return (int) facts.stream().map(StudentFact::personId).filter(Objects::nonNull)
                .distinct().count();
    }

    private boolean inRange(LocalDate date,
                            LocalDate from,
                            LocalDate to) {
        return date != null && !date.isBefore(from) && !date.isAfter(to);
    }

    private boolean sameDate(LocalDate left,
                             LocalDate right) {
        return Objects.equals(left, right);
    }

    private int positiveOrDefault(Integer value,
                                  int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private Set<String> normalizeSet(Collection<String> values) {
        return asList(values).stream()
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private <T> List<T> asList(Collection<T> values) {
        return values == null ? List.of()
                : values.stream().filter(Objects::nonNull).toList();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private LocalDate minDate(LocalDate first,
                              LocalDate second) {
        return first.isBefore(second) ? first : second;
    }

    private LocalDate maxDate(LocalDate first,
                              LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    private Scope scope(LocalDate from,
                        LocalDate to,
                        LocalDate asOf) {
        LocalDate resolvedAsOf = asOf == null ? LocalDate.now() : asOf;
        LocalDate resolvedTo = to == null ? resolvedAsOf : to;
        LocalDate resolvedFrom = from == null
                ? LocalDate.of(resolvedTo.getYear(), 1, 1)
                : from;
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
        return new Scope(resolvedFrom, resolvedTo, resolvedAsOf);
    }

    private List<YearMonth> months(Scope scope) {
        return months(YearMonth.from(scope.from()), YearMonth.from(scope.to()));
    }

    private List<YearMonth> months(YearMonth from,
                                   YearMonth to) {
        if (from.isAfter(to)) {
            return List.of();
        }
        int count = (to.getYear() - from.getYear()) * 12
                + to.getMonthValue() - from.getMonthValue() + 1;
        return IntStream.range(0, count).mapToObj(from::plusMonths).toList();
    }

    private <T> PageResponse<T> page(List<T> values,
                                     Integer page,
                                     Integer size) {
        int pageNumber = page == null || page < 0 ? 0 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 500);
        int fromIndex = Math.min(pageNumber * pageSize, values.size());
        int toIndex = Math.min(fromIndex + pageSize, values.size());
        List<T> content = values.subList(fromIndex, toIndex);
        int totalPages = values.isEmpty() ? 0 : (int) Math.ceil((double) values.size() / pageSize);
        return PageResponse.<T>builder()
                .content(content)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalElements(values.size())
                .totalPages(totalPages)
                .first(pageNumber == 0)
                .last(pageNumber + 1 >= totalPages)
                .empty(content.isEmpty())
                .build();
    }

    private static final Long ZERO_BRANCH_ID = Long.MIN_VALUE;

    private record Scope(
            LocalDate from,
            LocalDate to,
            LocalDate asOf
    ) {
    }

    private record MovementEvent(
            String type,
            LocalDate date,
            StudentFact fact
    ) {
        UUID personId() {
            return fact.personId();
        }
    }

    private record TransferEvent(
            UUID personId,
            Long fromBranchId,
            Long toBranchId,
            LocalDate date,
            StudentFact fromFact,
            StudentFact toFact
    ) {
    }

    private record BranchPair(
            Long fromBranchId,
            Long toBranchId
    ) {
    }

    private record MonthAge(
            YearMonth month,
            Integer age
    ) {
    }

    private record MonthBranch(
            YearMonth month,
            Long branchId,
            String branchName
    ) {
    }

    private record MonthExamType(
            YearMonth month,
            String examType
    ) {
    }

    private record MovementCounter(
            int newCount,
            int leftCount,
            int transferIn,
            int transferOut,
            StudentFact sampleFact,
            String branchName,
            String courseName
    ) {
    }

    private record ClassStat(
            ClassFact classFact,
            int activeRoster,
            int newStudents,
            int inferredLeavers,
            int transferIn,
            int transferOut,
            BigDecimal attendanceRate,
            BigDecimal fillRate,
            int atRiskStudents,
            List<String> warnings
    ) {
    }

    private record RiskAssessment(
            StudentFact fact,
            LocalDate lastAttendanceDate,
            Integer daysSinceLastAttendance,
            BigDecimal rate30d,
            BigDecimal previousRate30d,
            int consecutiveAbsences,
            int excusedAbsences30d,
            String level,
            List<String> reasons
    ) {
    }

    private record RetentionStats(
            int eligibleStudents,
            int inferredLeavers,
            BigDecimal renewalRate,
            int medianTenureDays
    ) {
    }
}
