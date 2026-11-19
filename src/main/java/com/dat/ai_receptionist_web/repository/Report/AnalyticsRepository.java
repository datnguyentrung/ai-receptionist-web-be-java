package com.dat.ai_receptionist_web.repository.Report;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Read-only analytics repository.
 *
 * <p>
 * The queries intentionally read the operational tables directly. They are the
 * source for every analytics endpoint and keep the "INACTIVE +
 * MAX(student_enrollment.end_date)" lifecycle rule in one place.
 */
@Repository
@RequiredArgsConstructor
public class AnalyticsRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public List<StudentFact> findStudentFacts(StudentQuery query) {
        var params = new MapSqlParameterSource()
                .addValue("from", query.from())
                .addValue("to", query.to())
                .addValue("asOf", query.asOf());
        StringBuilder sql = new StringBuilder("""
                WITH last_enrollment AS (
                    SELECT e.student_person_id,
                           MIN(e.start_date) AS first_enrollment_start,
                           MAX(e.end_date) AS last_enrollment_end,
                           COUNT(*) AS enrollment_count,
                           COUNT(*) FILTER (WHERE e.end_date IS NULL) AS open_end_count
                    FROM training.student_enrollment e
                    GROUP BY e.student_person_id
                ),
                enrollment_rows AS (
                    SELECT e.student_enrollment_id,
                           e.start_date AS enrollment_start,
                           e.end_date AS enrollment_end,
                           e.status AS enrollment_status,
                           e.created_at AS enrollment_created_at,
                           p.person_id,
                           p.person_code,
                           p.full_name,
                           p.birth_date,
                           p.gender,
                           p.current_belt,
                           p.status AS person_status,
                           p.start_date AS person_start_date,
                           le.first_enrollment_start,
                           le.last_enrollment_end,
                           le.enrollment_count,
                           le.open_end_count,
                           CASE
                               WHEN p.status = 'INACTIVE' THEN le.last_enrollment_end
                               ELSE NULL
                           END AS inferred_leave_date,
                           CASE
                               WHEN p.status <> 'INACTIVE' THEN NULL
                               WHEN le.last_enrollment_end IS NULL THEN 'MISSING_END_DATE'
                               WHEN le.open_end_count > 0 THEN 'OPEN_ENROLLMENT_CONFLICT'
                               WHEN le.last_enrollment_end > :asOf THEN 'FUTURE_END_DATE'
                               ELSE 'DERIVED'
                           END AS leave_date_quality,
                           cp.course_purchase_id,
                           cp.training_plan_id,
                           tp.name AS training_plan_name,
                           tp.level AS training_plan_level,
                           tp.duration_months AS training_plan_duration_months,
                           tp.included_session_count,
                           tp.base_price,
                           tp.final_price,
                           tp.status AS training_plan_status,
                           cs.course_schedule_id,
                           cs.start_date AS course_schedule_start_date,
                           cs.end_date AS course_schedule_end_date,
                           cs.status AS course_schedule_status,
                           cs.session_type,
                           c.course_id,
                           c.name AS course_name,
                           c.level AS course_level,
                           c.capacity,
                           c.status AS course_status,
                           cl.schedule_id AS class_schedule_id,
                           cl.weekday,
                           cl.start_time,
                           cl.end_time,
                           cl.status AS class_schedule_status,
                           b.branch_id,
                           b.name AS branch_name,
                           b.status AS branch_status,
                           b.opened_date AS branch_opened_date,
                           ses.effective_from,
                           ses.effective_to,
                           ses.status AS enrollment_schedule_status,
                           (
                               SELECT MAX(previous.end_date)
                               FROM training.student_enrollment previous
                               WHERE previous.student_person_id = e.student_person_id
                                 AND previous.student_enrollment_id <> e.student_enrollment_id
                                 AND previous.start_date < e.start_date
                           ) AS previous_enrollment_end,
                           ROW_NUMBER() OVER (
                               PARTITION BY e.student_enrollment_id
                               ORDER BY
                                   (ses.effective_from IS NULL OR ses.effective_from <= :asOf) DESC,
                                   ses.effective_from DESC NULLS LAST,
                                   ses.student_enrollment_schedule_id DESC NULLS LAST
                           ) AS schedule_rank
                    FROM training.student_enrollment e
                    JOIN core.person p ON p.person_id = e.student_person_id
                    JOIN last_enrollment le ON le.student_person_id = e.student_person_id
                    JOIN finance.course_purchase cp ON cp.course_purchase_id = e.course_purchase_id
                    LEFT JOIN catalog.training_plan tp ON tp.training_plan_id = cp.training_plan_id
                    LEFT JOIN training.student_enrollment_schedule ses
                           ON ses.student_enrollment_id = e.student_enrollment_id
                    LEFT JOIN catalog.course_schedule cs ON cs.course_schedule_id = ses.course_schedule_id
                    LEFT JOIN catalog.course c ON c.course_id = cs.course_id
                    LEFT JOIN catalog.class_schedule cl ON cl.schedule_id = cs.class_schedule_id
                    LEFT JOIN core.branch b ON b.branch_id = cl.branch_id
                    WHERE e.start_date <= :to
                      AND e.end_date >= :from
                """);
        appendStudentFilters(sql, params, query);
        sql.append("""
                )
                SELECT *
                FROM enrollment_rows
                WHERE schedule_rank = 1
                ORDER BY enrollment_start DESC, lower(full_name), student_enrollment_id
                """);
        return jdbc.query(sql.toString(), params, STUDENT_FACT_MAPPER);
    }

    public PersonSnapshot findPerson(UUID personId) {
        String sql = """
                WITH last_enrollment AS (
                    SELECT e.student_person_id,
                           MIN(e.start_date) AS first_enrollment_start,
                           MAX(e.end_date) AS last_enrollment_end,
                           COUNT(*) AS enrollment_count,
                           COUNT(*) FILTER (WHERE e.end_date IS NULL) AS open_end_count
                    FROM training.student_enrollment e
                    WHERE e.student_person_id = :personId
                    GROUP BY e.student_person_id
                )
                SELECT p.person_id, p.person_code, p.full_name, p.birth_date,
                       p.gender, p.current_belt, p.status AS person_status,
                       p.start_date AS person_start_date,
                       le.first_enrollment_start, le.last_enrollment_end,
                       le.enrollment_count, le.open_end_count,
                       CURRENT_DATE AS as_of,
                       CASE WHEN p.status = 'INACTIVE' THEN le.last_enrollment_end ELSE NULL END
                           AS inferred_leave_date,
                       CASE
                           WHEN p.status <> 'INACTIVE' THEN NULL
                           WHEN le.last_enrollment_end IS NULL THEN 'MISSING_END_DATE'
                           WHEN le.open_end_count > 0 THEN 'OPEN_ENROLLMENT_CONFLICT'
                           ELSE 'DERIVED'
                       END AS leave_date_quality
                FROM core.person p
                LEFT JOIN last_enrollment le ON le.student_person_id = p.person_id
                WHERE p.person_id = :personId
                """;
        return jdbc.query(sql, Map.of("personId", personId), PERSON_SNAPSHOT_MAPPER)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public List<AttendanceFact> findAttendanceFacts(AttendanceQuery query) {
        var params = new MapSqlParameterSource()
                .addValue("from", query.from())
                .addValue("to", query.to());
        StringBuilder sql = new StringBuilder("""
                WITH expected_attendance AS (
                    SELECT b.student_enrollment_id, b.class_session_id
                    FROM training.student_session_booking b
                    WHERE b.status <> 'CANCELLED'
                    UNION
                    SELECT a.student_enrollment_id, a.class_session_id
                    FROM training.session_attendance a
                    WHERE a.student_enrollment_id IS NOT NULL
                )
                SELECT p.person_id, p.person_code, p.full_name, p.status AS person_status,
                       p.birth_date, p.gender, p.current_belt,
                       e.student_enrollment_id, e.start_date AS enrollment_start,
                       e.end_date AS enrollment_end,
                       s.class_session_id, s.session_date, s.status AS session_status,
                       s.course_schedule_id, c.course_id, c.name AS course_name,
                       c.level AS course_level, b.branch_id, b.name AS branch_name,
                       cl.schedule_id AS class_schedule_id,
                       a.session_attendance_id,
                       a.attendance_status,
                       a.evaluation_status,
                       a.check_in_time
                FROM expected_attendance x
                JOIN training.student_enrollment e
                  ON e.student_enrollment_id = x.student_enrollment_id
                JOIN core.person p ON p.person_id = e.student_person_id
                JOIN training.class_session s ON s.class_session_id = x.class_session_id
                JOIN catalog.course_schedule cs ON cs.course_schedule_id = s.course_schedule_id
                JOIN catalog.course c ON c.course_id = cs.course_id
                JOIN catalog.class_schedule cl ON cl.schedule_id = cs.class_schedule_id
                JOIN core.branch b ON b.branch_id = cl.branch_id
                LEFT JOIN training.session_attendance a
                       ON a.class_session_id = x.class_session_id
                      AND a.student_enrollment_id = x.student_enrollment_id
                WHERE s.session_date BETWEEN :from AND :to
                """);
        appendAttendanceFilters(sql, params, query);
        sql.append(" ORDER BY s.session_date DESC, lower(p.full_name)");
        return jdbc.query(sql.toString(), params, ATTENDANCE_FACT_MAPPER);
    }

    public List<ClassFact> findClassFacts(ClassQuery query) {
        var params = new MapSqlParameterSource()
                .addValue("from", query.from())
                .addValue("to", query.to());
        StringBuilder sql = new StringBuilder("""
                SELECT cs.course_schedule_id, cs.start_date, cs.end_date, cs.status,
                       cs.session_type, c.course_id, c.name AS course_name,
                       c.level, c.capacity, c.status AS course_status,
                       cl.schedule_id AS class_schedule_id, cl.weekday,
                       cl.start_time, cl.end_time, cl.status AS class_schedule_status,
                       b.branch_id, b.name AS branch_name, b.status AS branch_status
                FROM catalog.course_schedule cs
                JOIN catalog.course c ON c.course_id = cs.course_id
                JOIN catalog.class_schedule cl ON cl.schedule_id = cs.class_schedule_id
                JOIN core.branch b ON b.branch_id = cl.branch_id
                WHERE cs.start_date <= :to
                  AND (cs.end_date IS NULL OR cs.end_date >= :from)
                """);
        appendClassFilters(sql, params, query);
        sql.append(" ORDER BY lower(b.name), lower(c.name), cl.weekday, cl.start_time");
        return jdbc.query(sql.toString(), params, CLASS_FACT_MAPPER);
    }

    public List<SessionFact> findSessionFacts(SessionQuery query) {
        var params = new MapSqlParameterSource()
                .addValue("from", query.from())
                .addValue("to", query.to());
        StringBuilder sql = new StringBuilder("""
                SELECT s.class_session_id, s.session_date, s.status AS session_status,
                       s.start_time, s.end_time,
                       cs.course_schedule_id, c.course_id, c.name AS course_name,
                       c.level, b.branch_id, b.name AS branch_name,
                       cl.schedule_id AS class_schedule_id, cl.weekday,
                       a.course_staff_assignment_id, a.staff_person_id,
                       sp.full_name AS staff_name, a.assignment_type,
                       a.assignment_status, a.start_date AS assignment_start,
                       a.end_date AS assignment_end,
                       t.coach_timesheet_id,
                       (s.session_date + t.check_in_time) AS coach_check_in_at,
                       (s.session_date + t.check_out_time) AS coach_check_out_at,
                       CASE WHEN t.check_in_time IS NOT NULL AND t.check_out_time IS NOT NULL
                            THEN TRUE ELSE FALSE END AS timesheet_complete,
                       (
                           SELECT COUNT(*) FROM training.session_attendance sa
                           WHERE sa.class_session_id = s.class_session_id
                             AND sa.attendance_status IN ('PRESENT', 'LATE', 'MAKEUP')
                       ) AS present_count,
                       (
                           SELECT COUNT(*) FROM training.session_attendance sa
                           WHERE sa.class_session_id = s.class_session_id
                       ) AS attendance_count
                FROM training.class_session s
                JOIN catalog.course_schedule cs ON cs.course_schedule_id = s.course_schedule_id
                JOIN catalog.course c ON c.course_id = cs.course_id
                JOIN catalog.class_schedule cl ON cl.schedule_id = cs.class_schedule_id
                JOIN core.branch b ON b.branch_id = cl.branch_id
                LEFT JOIN training.course_staff_assignment a
                       ON a.course_schedule_id = cs.course_schedule_id
                      AND a.start_date <= s.session_date
                      AND (a.end_date IS NULL OR a.end_date >= s.session_date)
                LEFT JOIN core.person sp ON sp.person_id = a.staff_person_id
                LEFT JOIN training.coach_timesheet t
                       ON t.class_session_id = s.class_session_id
                      AND t.course_staff_assignment_id = a.course_staff_assignment_id
                WHERE s.session_date BETWEEN :from AND :to
                """);
        appendSessionFilters(sql, params, query);
        sql.append(" ORDER BY s.session_date DESC, s.start_time, lower(coalesce(sp.full_name, ''))");
        return jdbc.query(sql.toString(), params, SESSION_FACT_MAPPER);
    }

    public List<FinanceFact> findFinanceFacts(FinanceQuery query) {
        var params = new MapSqlParameterSource()
                .addValue("from", query.from())
                .addValue("to", query.to());
        StringBuilder sql = new StringBuilder("""
                SELECT wt.wallet_transaction_id, wt.wallet_id, w.person_id,
                       p.person_code, p.full_name,
                       wt.type, wt.direction, wt.amount, wt.balance_before,
                       wt.balance_after, wt.status, wt.external_reference,
                       wt.note, wt.created_at, wt.reviewed_at,
                       cp.course_purchase_id, cp.training_plan_id,
                       tp.name AS training_plan_name,
                       eap.enrollment_addon_purchase_id,
                       tpa.training_plan_addon_id,
                       attributed.branch_id AS attributed_branch_id,
                       attributed.branch_name AS attributed_branch_name,
                       (
                           upper(coalesce(wt.external_reference, '')) LIKE '%MIGRATION%'
                           OR upper(coalesce(wt.external_reference, '')) LIKE '%IMPORT%'
                           OR upper(coalesce(wt.note, '')) LIKE '%MIGRATION%'
                       ) AS migration_synthetic
                FROM finance.wallet_transaction wt
                JOIN finance.wallet w ON w.wallet_id = wt.wallet_id
                JOIN core.person p ON p.person_id = w.person_id
                LEFT JOIN finance.course_purchase cp ON cp.debit_transaction_id = wt.wallet_transaction_id
                LEFT JOIN catalog.training_plan tp ON tp.training_plan_id = cp.training_plan_id
                LEFT JOIN finance.enrollment_addon_purchase eap
                       ON eap.wallet_transaction_id = wt.wallet_transaction_id
                LEFT JOIN catalog.training_plan_addon tpa
                       ON tpa.training_plan_addon_id = eap.training_plan_addon_id
                LEFT JOIN LATERAL (
                    SELECT b.branch_id, b.name AS branch_name
                    FROM training.student_enrollment se
                    JOIN training.student_enrollment_schedule ses
                      ON ses.student_enrollment_id = se.student_enrollment_id
                    JOIN catalog.course_schedule cs
                      ON cs.course_schedule_id = ses.course_schedule_id
                    JOIN catalog.class_schedule cl ON cl.schedule_id = cs.class_schedule_id
                    JOIN core.branch b ON b.branch_id = cl.branch_id
                    WHERE se.course_purchase_id = cp.course_purchase_id
                    ORDER BY se.start_date DESC, ses.effective_from DESC NULLS LAST
                    LIMIT 1
                ) attributed ON TRUE
                WHERE wt.created_at::date BETWEEN :from AND :to
                """);
        appendFinanceFilters(sql, params, query);
        sql.append(" ORDER BY wt.created_at DESC, wt.wallet_transaction_id");
        return jdbc.query(sql.toString(), params, FINANCE_FACT_MAPPER);
    }

    public List<BeltExamFact> findBeltExamFacts(DevelopmentQuery query) {
        var params = new MapSqlParameterSource()
                .addValue("from", query.from())
                .addValue("to", query.to())
                .addValue("asOf", query.asOf());
        StringBuilder sql = new StringBuilder("""
                SELECT x.belt_exam_id, x.person_id, x.person_code, x.full_name,
                       x.birth_date, x.current_belt, x.person_status,
                       x.exam_date, x.from_belt, x.target_belt, x.exam_type,
                       x.exam_result, x.branch_id, x.branch_name
                FROM (
                    SELECT be.belt_exam_id, p.person_id, p.person_code, p.full_name,
                           p.birth_date, p.current_belt, p.status AS person_status,
                           be.exam_date, be.from_belt, be.target_belt,
                           be.type AS exam_type, be.result AS exam_result,
                           c.level AS schedule_level,
                           b.branch_id, b.name AS branch_name,
                           ROW_NUMBER() OVER (
                               PARTITION BY be.belt_exam_id
                               ORDER BY se.start_date DESC, ses.effective_from DESC NULLS LAST
                           ) AS branch_rank
                    FROM training.belt_exam be
                    JOIN core.person p ON p.person_id = be.person_id
                    LEFT JOIN training.student_enrollment se
                           ON se.student_person_id = p.person_id
                    LEFT JOIN training.student_enrollment_schedule ses
                           ON ses.student_enrollment_id = se.student_enrollment_id
                    LEFT JOIN catalog.course_schedule cs
                           ON cs.course_schedule_id = ses.course_schedule_id
                    LEFT JOIN catalog.course c ON c.course_id = cs.course_id
                    LEFT JOIN catalog.class_schedule cl ON cl.schedule_id = cs.class_schedule_id
                    LEFT JOIN core.branch b ON b.branch_id = cl.branch_id
                    WHERE be.exam_date BETWEEN :from AND :to
                ) x
                WHERE x.branch_rank = 1
                """);
        appendDevelopmentFilters(sql, params, query);
        sql.append(" ORDER BY x.exam_date DESC, lower(x.full_name)");
        return jdbc.query(sql.toString(), params, BELT_EXAM_FACT_MAPPER);
    }

    public List<FitnessFact> findFitnessFacts(DevelopmentQuery query) {
        var params = new MapSqlParameterSource()
                .addValue("from", query.from())
                .addValue("to", query.to())
                .addValue("asOf", query.asOf());
        StringBuilder sql = new StringBuilder("""
                SELECT fr.fitness_record_id,
                       fr.fitness_id,
                       f.schedule_level,
                       f.amount,
                       f.duration AS fitness_target_duration,
                       fr.record_date,
                       fr.duration,
                       p.person_id,
                       p.person_code,
                       p.full_name,
                       p.birth_date,
                       p.current_belt,
                       p.status AS person_status,
                       b.branch_id,
                       b.name AS branch_name
                FROM skill.fitness_record fr
                JOIN skill.fitness f ON f.fitness_id = fr.fitness_id
                JOIN core.person p ON p.person_id = fr.student_person_id
                LEFT JOIN LATERAL (
                    SELECT branch.branch_id, branch.name
                    FROM training.student_enrollment se
                    JOIN training.student_enrollment_schedule ses
                      ON ses.student_enrollment_id = se.student_enrollment_id
                    JOIN catalog.course_schedule cs
                      ON cs.course_schedule_id = ses.course_schedule_id
                    JOIN catalog.class_schedule cl ON cl.schedule_id = cs.class_schedule_id
                    JOIN core.branch branch ON branch.branch_id = cl.branch_id
                    WHERE se.student_person_id = p.person_id
                    ORDER BY se.start_date DESC, ses.effective_from DESC NULLS LAST
                    LIMIT 1
                ) b ON TRUE
                WHERE fr.record_date BETWEEN :from AND :to
                """);
        appendDevelopmentFilters(sql, params, query);
        sql.append(" ORDER BY fr.record_date DESC, lower(p.full_name)");
        return jdbc.query(sql.toString(), params, FITNESS_FACT_MAPPER);
    }

    public List<BookingFact> findBookingFacts(UUID personId, LocalDate from, LocalDate to) {
        String sql = """
                SELECT b.student_session_booking_id, b.student_enrollment_id,
                       b.class_session_id, s.session_date, s.status AS session_status,
                       b.source, b.status, b.booked_at, b.cancelled_at, b.settled_at
                FROM training.student_session_booking b
                JOIN training.student_enrollment e
                  ON e.student_enrollment_id = b.student_enrollment_id
                JOIN training.class_session s ON s.class_session_id = b.class_session_id
                WHERE e.student_person_id = :personId
                  AND s.session_date BETWEEN :from AND :to
                ORDER BY s.session_date DESC
                """;
        return jdbc.query(sql,
                Map.of("personId", personId, "from", from, "to", to),
                BOOKING_FACT_MAPPER);
    }

    public List<AddonFact> findAddonFacts(LocalDate from, LocalDate to) {
        String sql = """
                SELECT addon.training_plan_addon_id,
                       addon.training_plan_id,
                       addon.name,
                       addon.extra_sessions_per_week,
                       addon.price,
                       COUNT(DISTINCT purchase.enrollment_addon_purchase_id) AS valid_usage_count
                FROM catalog.training_plan_addon addon
                LEFT JOIN finance.enrollment_addon_purchase purchase
                       ON purchase.training_plan_addon_id = addon.training_plan_addon_id
                      AND purchase.created_at::date BETWEEN :from AND :to
                GROUP BY addon.training_plan_addon_id, addon.training_plan_id,
                         addon.name, addon.extra_sessions_per_week, addon.price
                ORDER BY addon.extra_sessions_per_week, addon.name
                """;
        return jdbc.query(sql, Map.of("from", from, "to", to), ADDON_FACT_MAPPER);
    }

    private void appendStudentFilters(
            StringBuilder sql,
            MapSqlParameterSource params,
            StudentQuery query
    ) {
        appendLongFilter(sql, params, "b.branch_id", "branchIds", query.branchIds());
        appendUuidFilter(sql, params, "c.course_id", "courseIds", query.courseIds());
        appendUuidFilter(sql, params, "cp.training_plan_id", "trainingPlanIds", query.trainingPlanIds());
        appendUuidFilter(sql, params, "cs.course_schedule_id", "courseScheduleIds", query.courseScheduleIds());
        appendUuidValue(sql, params, "p.person_id", "personId", query.personId());
        appendStringFilter(sql, params, "c.level", "levels", query.levels());
        appendStringFilter(sql, params, "p.current_belt", "belts", query.belts());
        appendStringFilter(sql, params, "tp.status", "planStatuses", query.planStatuses());
        if (query.minAge() != null) {
            sql.append(" AND date_part('year', age(CAST(:asOf AS date), p.birth_date)) >= :minAge");
            params.addValue("minAge", query.minAge());
        }
        if (query.maxAge() != null) {
            sql.append(" AND date_part('year', age(CAST(:asOf AS date), p.birth_date)) <= :maxAge");
            params.addValue("maxAge", query.maxAge());
        }
        appendGenderFilter(sql, params, query.genders(), "p.gender");
    }

    private void appendAttendanceFilters(
            StringBuilder sql,
            MapSqlParameterSource params,
            AttendanceQuery query
    ) {
        appendLongFilter(sql, params, "b.branch_id", "branchIds", query.branchIds());
        appendUuidFilter(sql, params, "c.course_id", "courseIds", query.courseIds());
        appendUuidFilter(sql, params, "s.course_schedule_id", "courseScheduleIds",
                query.courseScheduleIds());
        appendUuidValue(sql, params, "s.course_schedule_id", "courseScheduleId",
                query.courseScheduleId());
        appendUuidValue(sql, params, "p.person_id", "personId", query.personId());
        appendStringFilter(sql, params, "c.level", "levels", query.levels());
        appendStringFilter(sql, params, "p.current_belt", "belts", query.belts());
        appendGenderFilter(sql, params, query.genders(), "p.gender");
        if (query.minAge() != null) {
            sql.append(" AND date_part('year', age(CAST(:asOf AS date), p.birth_date)) >= :minAge");
            params.addValue("asOf", query.asOf());
            params.addValue("minAge", query.minAge());
        }
        if (query.maxAge() != null) {
            sql.append(" AND date_part('year', age(CAST(:asOf AS date), p.birth_date)) <= :maxAge");
            params.addValue("asOf", query.asOf());
            params.addValue("maxAge", query.maxAge());
        }
    }

    private void appendClassFilters(
            StringBuilder sql,
            MapSqlParameterSource params,
            ClassQuery query
    ) {
        appendLongFilter(sql, params, "b.branch_id", "branchIds", query.branchIds());
        appendUuidFilter(sql, params, "c.course_id", "courseIds", query.courseIds());
        appendLongValue(sql, params, "b.branch_id", "branchId", query.branchId());
        appendStringFilter(sql, params, "c.level", "levels", query.levels());
        appendStringFilter(sql, params, "cs.status", "scheduleStatuses",
                query.scheduleStatuses());
        appendStringValue(sql, params, "cs.status", "status", query.status());
    }

    private void appendSessionFilters(
            StringBuilder sql,
            MapSqlParameterSource params,
            SessionQuery query
    ) {
        appendLongFilter(sql, params, "b.branch_id", "branchIds", query.branchIds());
        appendLongValue(sql, params, "b.branch_id", "branchId", query.branchId());
        appendUuidFilter(sql, params, "cs.course_schedule_id", "courseScheduleIds",
                query.courseScheduleIds());
        appendUuidValue(sql, params, "a.staff_person_id", "staffPersonId",
                query.staffPersonId());
        appendStringFilter(sql, params, "a.assignment_type", "assignmentTypes",
                query.assignmentTypes());
        appendStringFilter(sql, params, "a.assignment_status", "assignmentStatuses",
                query.assignmentStatuses());
        appendStringValue(sql, params, "s.status", "sessionStatus", query.sessionStatus());
        if (query.timesheetStatus() != null && !query.timesheetStatus().isBlank()) {
            if ("COMPLETE".equalsIgnoreCase(query.timesheetStatus())) {
                sql.append("""
                         AND t.check_in_time IS NOT NULL
                         AND t.check_out_time IS NOT NULL
                        """);
            } else if ("MISSING".equalsIgnoreCase(query.timesheetStatus())) {
                sql.append("""
                         AND (t.coach_timesheet_id IS NULL
                              OR t.check_in_time IS NULL
                              OR t.check_out_time IS NULL)
                        """);
            }
        }
    }

    private void appendFinanceFilters(
            StringBuilder sql,
            MapSqlParameterSource params,
            FinanceQuery query
    ) {
        appendLongFilter(sql, params, "attributed.branch_id", "branchIds", query.branchIds());
        appendLongValue(sql, params, "attributed.branch_id", "branchId", query.branchId());
        appendUuidFilter(sql, params, "cp.training_plan_id", "trainingPlanIds",
                query.trainingPlanIds());
        appendUuidValue(sql, params, "cp.training_plan_id", "trainingPlanId",
                query.trainingPlanId());
        appendStringFilter(sql, params, "wt.type", "transactionTypes", query.transactionTypes());
        appendStringValue(sql, params, "wt.type", "transactionType", query.transactionType());
        appendStringFilter(sql, params, "wt.direction", "directions", query.directions());
        appendStringValue(sql, params, "wt.direction", "direction", query.direction());
        appendStringFilter(sql, params, "wt.status", "statuses", query.statuses());
        appendStringValue(sql, params, "wt.status", "status", query.status());
    }

    private void appendDevelopmentFilters(
            StringBuilder sql,
            MapSqlParameterSource params,
            DevelopmentQuery query
    ) {
        appendLongFilter(sql, params, "branch_id", "branchIds", query.branchIds());
        appendLongValue(sql, params, "branch_id", "branchId", query.branchId());
        appendStringFilter(sql, params, "current_belt", "belts", query.belts());
        appendStringFilter(sql, params, "schedule_level", "levels", query.levels());
        appendStringFilter(sql, params, "exam_type", "examTypes", query.examTypes());
        appendStringFilter(sql, params, "exam_result", "examResults", query.examResults());
        appendStringValue(sql, params, "exam_result", "examResult", query.examResult());
        appendStringValue(sql, params, "exam_type", "examType", query.examType());
        appendStringValue(sql, params, "current_belt", "belt", query.belt());
        appendUuidValue(sql, params, "person_id", "personId", query.personId());
        if (query.minAge() != null) {
            sql.append(" AND date_part('year', age(CAST(:asOf AS date), birth_date)) >= :minAge");
            params.addValue("minAge", query.minAge());
        }
        if (query.maxAge() != null) {
            sql.append(" AND date_part('year', age(CAST(:asOf AS date), birth_date)) <= :maxAge");
            params.addValue("maxAge", query.maxAge());
        }
    }

    private void appendUuidFilter(
            StringBuilder sql,
            MapSqlParameterSource params,
            String column,
            String parameter,
            Collection<UUID> values
    ) {
        if (values != null && !values.isEmpty()) {
            sql.append(" AND ").append(column).append(" IN (:").append(parameter).append(')');
            params.addValue(parameter, values);
        }
    }

    private void appendLongFilter(
            StringBuilder sql,
            MapSqlParameterSource params,
            String column,
            String parameter,
            Collection<Long> values
    ) {
        if (values != null && !values.isEmpty()) {
            sql.append(" AND ").append(column).append(" IN (:").append(parameter).append(')');
            params.addValue(parameter, values);
        }
    }

    private void appendUuidValue(
            StringBuilder sql,
            MapSqlParameterSource params,
            String column,
            String parameter,
            UUID value
    ) {
        if (value != null) {
            sql.append(" AND ").append(column).append(" = :").append(parameter);
            params.addValue(parameter, value);
        }
    }

    private void appendLongValue(
            StringBuilder sql,
            MapSqlParameterSource params,
            String column,
            String parameter,
            Long value
    ) {
        if (value != null) {
            sql.append(" AND ").append(column).append(" = :").append(parameter);
            params.addValue(parameter, value);
        }
    }

    private void appendStringFilter(
            StringBuilder sql,
            MapSqlParameterSource params,
            String column,
            String parameter,
            Collection<String> values
    ) {
        if (values != null && !values.isEmpty()) {
            sql.append(" AND ").append(column).append(" IN (:").append(parameter).append(')');
            params.addValue(parameter, values);
        }
    }

    private void appendStringValue(
            StringBuilder sql,
            MapSqlParameterSource params,
            String column,
            String parameter,
            String value
    ) {
        if (value != null && !value.isBlank()) {
            sql.append(" AND ").append(column).append(" = :").append(parameter);
            params.addValue(parameter, value.toUpperCase());
        }
    }

    private void appendGenderFilter(
            StringBuilder sql,
            MapSqlParameterSource params,
            Collection<String> values,
            String column
    ) {
        if (values == null || values.isEmpty()) {
            return;
        }
        boolean male = values.stream().anyMatch(v -> "MALE".equalsIgnoreCase(v));
        boolean female = values.stream().anyMatch(v -> "FEMALE".equalsIgnoreCase(v));
        boolean unknown = values.stream().anyMatch(v -> "UNKNOWN".equalsIgnoreCase(v));
        sql.append(" AND (");
        if (male) {
            sql.append(column).append(" = TRUE");
        }
        if (female) {
            if (male) {
                sql.append(" OR ");
            }
            sql.append(column).append(" = FALSE");
        }
        if (unknown) {
            if (male || female) {
                sql.append(" OR ");
            }
            sql.append(column).append(" IS NULL");
        }
        if (!male && !female && !unknown) {
            sql.append("TRUE");
        }
        sql.append(')');
    }

    private static UUID nullableUuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).longValue();
    }

    private static Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).intValue();
    }

    private static LocalDate nullableDate(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDate.class);
    }

    private static LocalTime nullableTime(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalTime.class);
    }

    private static LocalDateTime nullableDateTime(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDateTime.class);
    }

    private static Boolean nullableBoolean(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, Boolean.class);
    }

    private static final RowMapper<StudentFact> STUDENT_FACT_MAPPER = (rs, rowNum) ->
            new StudentFact(
                    nullableUuid(rs, "person_id"),
                    rs.getString("person_code"),
                    rs.getString("full_name"),
                    nullableDate(rs, "birth_date"),
                    nullableBoolean(rs, "gender"),
                    rs.getString("current_belt"),
                    rs.getString("person_status"),
                    nullableDate(rs, "person_start_date"),
                    nullableDate(rs, "first_enrollment_start"),
                    nullableDate(rs, "last_enrollment_end"),
                    nullableDate(rs, "inferred_leave_date"),
                    rs.getString("leave_date_quality"),
                    nullableInteger(rs, "enrollment_count"),
                    nullableInteger(rs, "open_end_count"),
                    nullableUuid(rs, "student_enrollment_id"),
                    nullableDate(rs, "enrollment_start"),
                    nullableDate(rs, "enrollment_end"),
                    rs.getString("enrollment_status"),
                    nullableDateTime(rs, "enrollment_created_at"),
                    nullableUuid(rs, "course_purchase_id"),
                    nullableUuid(rs, "training_plan_id"),
                    rs.getString("training_plan_name"),
                    rs.getString("training_plan_level"),
                    nullableInteger(rs, "training_plan_duration_months"),
                    nullableInteger(rs, "included_session_count"),
                    rs.getBigDecimal("base_price"),
                    rs.getBigDecimal("final_price"),
                    rs.getString("training_plan_status"),
                    nullableUuid(rs, "course_schedule_id"),
                    nullableDate(rs, "course_schedule_start_date"),
                    nullableDate(rs, "course_schedule_end_date"),
                    rs.getString("course_schedule_status"),
                    rs.getString("session_type"),
                    nullableUuid(rs, "course_id"),
                    rs.getString("course_name"),
                    rs.getString("course_level"),
                    nullableInteger(rs, "capacity"),
                    rs.getString("course_status"),
                    nullableUuid(rs, "class_schedule_id"),
                    rs.getString("weekday"),
                    nullableTime(rs, "start_time"),
                    nullableTime(rs, "end_time"),
                    rs.getString("class_schedule_status"),
                    nullableLong(rs, "branch_id"),
                    rs.getString("branch_name"),
                    rs.getString("branch_status"),
                    nullableDate(rs, "branch_opened_date"),
                    nullableDate(rs, "effective_from"),
                    nullableDate(rs, "effective_to"),
                    rs.getString("enrollment_schedule_status"),
                    nullableDate(rs, "previous_enrollment_end")
            );

    private static final RowMapper<PersonSnapshot> PERSON_SNAPSHOT_MAPPER = (rs, rowNum) ->
            new PersonSnapshot(
                    nullableUuid(rs, "person_id"),
                    rs.getString("person_code"),
                    rs.getString("full_name"),
                    nullableDate(rs, "birth_date"),
                    nullableBoolean(rs, "gender"),
                    rs.getString("current_belt"),
                    rs.getString("person_status"),
                    nullableDate(rs, "person_start_date"),
                    nullableDate(rs, "first_enrollment_start"),
                    nullableDate(rs, "last_enrollment_end"),
                    nullableDate(rs, "inferred_leave_date"),
                    rs.getString("leave_date_quality"),
                    nullableInteger(rs, "enrollment_count")
            );

    private static final RowMapper<AttendanceFact> ATTENDANCE_FACT_MAPPER = (rs, rowNum) ->
            new AttendanceFact(
                    nullableUuid(rs, "person_id"),
                    rs.getString("person_code"),
                    rs.getString("full_name"),
                    rs.getString("person_status"),
                    nullableDate(rs, "birth_date"),
                    nullableBoolean(rs, "gender"),
                    rs.getString("current_belt"),
                    nullableUuid(rs, "student_enrollment_id"),
                    nullableDate(rs, "enrollment_start"),
                    nullableDate(rs, "enrollment_end"),
                    nullableUuid(rs, "class_session_id"),
                    nullableDate(rs, "session_date"),
                    rs.getString("session_status"),
                    nullableUuid(rs, "course_schedule_id"),
                    nullableUuid(rs, "course_id"),
                    rs.getString("course_name"),
                    rs.getString("course_level"),
                    nullableLong(rs, "branch_id"),
                    rs.getString("branch_name"),
                    nullableUuid(rs, "class_schedule_id"),
                    nullableUuid(rs, "session_attendance_id"),
                    rs.getString("attendance_status"),
                    rs.getString("evaluation_status"),
                    nullableDateTime(rs, "check_in_time")
            );

    private static final RowMapper<ClassFact> CLASS_FACT_MAPPER = (rs, rowNum) ->
            new ClassFact(
                    nullableUuid(rs, "course_schedule_id"),
                    nullableDate(rs, "start_date"),
                    nullableDate(rs, "end_date"),
                    rs.getString("status"),
                    rs.getString("session_type"),
                    nullableUuid(rs, "course_id"),
                    rs.getString("course_name"),
                    rs.getString("level"),
                    nullableInteger(rs, "capacity"),
                    rs.getString("course_status"),
                    nullableUuid(rs, "class_schedule_id"),
                    rs.getString("weekday"),
                    nullableTime(rs, "start_time"),
                    nullableTime(rs, "end_time"),
                    rs.getString("class_schedule_status"),
                    nullableLong(rs, "branch_id"),
                    rs.getString("branch_name"),
                    rs.getString("branch_status")
            );

    private static final RowMapper<SessionFact> SESSION_FACT_MAPPER = (rs, rowNum) ->
            new SessionFact(
                    nullableUuid(rs, "class_session_id"),
                    nullableDate(rs, "session_date"),
                    rs.getString("session_status"),
                    nullableTime(rs, "start_time"),
                    nullableTime(rs, "end_time"),
                    nullableUuid(rs, "course_schedule_id"),
                    nullableUuid(rs, "course_id"),
                    rs.getString("course_name"),
                    rs.getString("level"),
                    nullableLong(rs, "branch_id"),
                    rs.getString("branch_name"),
                    nullableUuid(rs, "class_schedule_id"),
                    rs.getString("weekday"),
                    nullableUuid(rs, "course_staff_assignment_id"),
                    nullableUuid(rs, "staff_person_id"),
                    rs.getString("staff_name"),
                    rs.getString("assignment_type"),
                    rs.getString("assignment_status"),
                    nullableDate(rs, "assignment_start"),
                    nullableDate(rs, "assignment_end"),
                    nullableUuid(rs, "coach_timesheet_id"),
                    nullableDateTime(rs, "coach_check_in_at"),
                    nullableDateTime(rs, "coach_check_out_at"),
                    nullableBoolean(rs, "timesheet_complete"),
                    nullableLong(rs, "present_count"),
                    nullableLong(rs, "attendance_count")
            );

    private static final RowMapper<FinanceFact> FINANCE_FACT_MAPPER = (rs, rowNum) ->
            new FinanceFact(
                    nullableUuid(rs, "wallet_transaction_id"),
                    nullableUuid(rs, "wallet_id"),
                    nullableUuid(rs, "person_id"),
                    rs.getString("person_code"),
                    rs.getString("full_name"),
                    rs.getString("type"),
                    rs.getString("direction"),
                    rs.getBigDecimal("amount"),
                    rs.getBigDecimal("balance_before"),
                    rs.getBigDecimal("balance_after"),
                    rs.getString("status"),
                    rs.getString("external_reference"),
                    rs.getString("note"),
                    nullableDateTime(rs, "created_at"),
                    nullableDateTime(rs, "reviewed_at"),
                    nullableUuid(rs, "course_purchase_id"),
                    nullableUuid(rs, "training_plan_id"),
                    rs.getString("training_plan_name"),
                    nullableUuid(rs, "enrollment_addon_purchase_id"),
                    nullableUuid(rs, "training_plan_addon_id"),
                    nullableLong(rs, "attributed_branch_id"),
                    rs.getString("attributed_branch_name"),
                    nullableBoolean(rs, "migration_synthetic")
            );

    private static final RowMapper<BeltExamFact> BELT_EXAM_FACT_MAPPER = (rs, rowNum) ->
            new BeltExamFact(
                    nullableUuid(rs, "belt_exam_id"),
                    nullableUuid(rs, "person_id"),
                    rs.getString("person_code"),
                    rs.getString("full_name"),
                    nullableDate(rs, "birth_date"),
                    rs.getString("current_belt"),
                    rs.getString("person_status"),
                    nullableDate(rs, "exam_date"),
                    rs.getString("from_belt"),
                    rs.getString("target_belt"),
                    rs.getString("exam_type"),
                    rs.getString("exam_result"),
                    nullableLong(rs, "branch_id"),
                    rs.getString("branch_name")
            );

    private static final RowMapper<FitnessFact> FITNESS_FACT_MAPPER = (rs, rowNum) ->
            new FitnessFact(
                    nullableLong(rs, "fitness_record_id"),
                    nullableLong(rs, "fitness_id"),
                    rs.getString("schedule_level"),
                    nullableInteger(rs, "amount"),
                    nullableInteger(rs, "fitness_target_duration"),
                    nullableDate(rs, "record_date"),
                    nullableInteger(rs, "duration"),
                    nullableUuid(rs, "person_id"),
                    rs.getString("person_code"),
                    rs.getString("full_name"),
                    nullableDate(rs, "birth_date"),
                    rs.getString("current_belt"),
                    rs.getString("person_status"),
                    nullableLong(rs, "branch_id"),
                    rs.getString("branch_name")
            );

    private static final RowMapper<BookingFact> BOOKING_FACT_MAPPER = (rs, rowNum) ->
            new BookingFact(
                    nullableUuid(rs, "student_session_booking_id"),
                    nullableUuid(rs, "student_enrollment_id"),
                    nullableUuid(rs, "class_session_id"),
                    nullableDate(rs, "session_date"),
                    rs.getString("session_status"),
                    rs.getString("source"),
                    rs.getString("status"),
                    nullableDateTime(rs, "booked_at"),
                    nullableDateTime(rs, "cancelled_at"),
                    nullableDateTime(rs, "settled_at")
            );

    private static final RowMapper<AddonFact> ADDON_FACT_MAPPER = (rs, rowNum) ->
            new AddonFact(
                    nullableUuid(rs, "training_plan_addon_id"),
                    nullableUuid(rs, "training_plan_id"),
                    rs.getString("name"),
                    nullableInteger(rs, "extra_sessions_per_week"),
                    rs.getBigDecimal("price"),
                    nullableLong(rs, "valid_usage_count")
            );

    public record StudentQuery(
            LocalDate from,
            LocalDate to,
            LocalDate asOf,
            List<Long> branchIds,
            List<UUID> courseIds,
            List<UUID> trainingPlanIds,
            List<String> levels,
            List<String> genders,
            Integer minAge,
            Integer maxAge,
            List<String> belts,
            List<String> planStatuses,
            List<UUID> courseScheduleIds,
            UUID personId
    ) {
    }

    public record StudentFact(
            UUID personId,
            String personCode,
            String fullName,
            LocalDate birthDate,
            Boolean gender,
            String currentBelt,
            String personStatus,
            LocalDate personStartDate,
            LocalDate firstEnrollmentStartDate,
            LocalDate lastEnrollmentEndDate,
            LocalDate inferredLeaveDate,
            String leaveDateQuality,
            Integer enrollmentCount,
            Integer openEndEnrollmentCount,
            UUID enrollmentId,
            LocalDate enrollmentStartDate,
            LocalDate enrollmentEndDate,
            String enrollmentStatus,
            LocalDateTime enrollmentCreatedAt,
            UUID coursePurchaseId,
            UUID trainingPlanId,
            String trainingPlanName,
            String trainingPlanLevel,
            Integer trainingPlanDurationMonths,
            Integer includedSessionCount,
            BigDecimal basePrice,
            BigDecimal finalPrice,
            String trainingPlanStatus,
            UUID courseScheduleId,
            LocalDate courseScheduleStartDate,
            LocalDate courseScheduleEndDate,
            String courseScheduleStatus,
            String sessionType,
            UUID courseId,
            String courseName,
            String courseLevel,
            Integer capacity,
            String courseStatus,
            UUID classScheduleId,
            String weekday,
            LocalTime startTime,
            LocalTime endTime,
            String classScheduleStatus,
            Long branchId,
            String branchName,
            String branchStatus,
            LocalDate branchOpenedDate,
            LocalDate scheduleEffectiveFrom,
            LocalDate scheduleEffectiveTo,
            String enrollmentScheduleStatus,
            LocalDate previousEnrollmentEndDate
    ) {
    }

    public record PersonSnapshot(
            UUID personId,
            String personCode,
            String fullName,
            LocalDate birthDate,
            Boolean gender,
            String currentBelt,
            String personStatus,
            LocalDate personStartDate,
            LocalDate firstEnrollmentStartDate,
            LocalDate lastEnrollmentEndDate,
            LocalDate inferredLeaveDate,
            String leaveDateQuality,
            Integer enrollmentCount
    ) {
    }

    public record AttendanceQuery(
            LocalDate from,
            LocalDate to,
            LocalDate asOf,
            List<Long> branchIds,
            List<UUID> courseIds,
            List<UUID> courseScheduleIds,
            UUID courseScheduleId,
            List<String> levels,
            List<String> belts,
            List<String> genders,
            Integer minAge,
            Integer maxAge,
            UUID personId
    ) {
    }

    public record AttendanceFact(
            UUID personId,
            String personCode,
            String fullName,
            String personStatus,
            LocalDate birthDate,
            Boolean gender,
            String currentBelt,
            UUID enrollmentId,
            LocalDate enrollmentStartDate,
            LocalDate enrollmentEndDate,
            UUID sessionId,
            LocalDate sessionDate,
            String sessionStatus,
            UUID courseScheduleId,
            UUID courseId,
            String courseName,
            String courseLevel,
            Long branchId,
            String branchName,
            UUID classScheduleId,
            UUID attendanceId,
            String attendanceStatus,
            String evaluationStatus,
            LocalDateTime checkInTime
    ) {
    }

    public record ClassQuery(
            LocalDate from,
            LocalDate to,
            List<Long> branchIds,
            List<UUID> courseIds,
            Long branchId,
            List<String> levels,
            List<String> scheduleStatuses,
            String status
    ) {
    }

    public record ClassFact(
            UUID courseScheduleId,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            String sessionType,
            UUID courseId,
            String courseName,
            String level,
            Integer capacity,
            String courseStatus,
            UUID classScheduleId,
            String weekday,
            LocalTime startTime,
            LocalTime endTime,
            String classScheduleStatus,
            Long branchId,
            String branchName,
            String branchStatus
    ) {
    }

    public record SessionQuery(
            LocalDate from,
            LocalDate to,
            List<Long> branchIds,
            Long branchId,
            List<UUID> courseScheduleIds,
            UUID staffPersonId,
            List<String> assignmentTypes,
            List<String> assignmentStatuses,
            String sessionStatus,
            String timesheetStatus
    ) {
    }

    public record SessionFact(
            UUID sessionId,
            LocalDate sessionDate,
            String sessionStatus,
            LocalTime startTime,
            LocalTime endTime,
            UUID courseScheduleId,
            UUID courseId,
            String courseName,
            String courseLevel,
            Long branchId,
            String branchName,
            UUID classScheduleId,
            String weekday,
            UUID courseStaffAssignmentId,
            UUID staffPersonId,
            String staffName,
            String assignmentType,
            String assignmentStatus,
            LocalDate assignmentStart,
            LocalDate assignmentEnd,
            UUID timesheetId,
            LocalDateTime checkInTime,
            LocalDateTime checkOutTime,
            Boolean timesheetComplete,
            Long presentCount,
            Long attendanceCount
    ) {
    }

    public record FinanceQuery(
            LocalDate from,
            LocalDate to,
            List<Long> branchIds,
            Long branchId,
            List<UUID> trainingPlanIds,
            UUID trainingPlanId,
            List<String> transactionTypes,
            String transactionType,
            List<String> directions,
            String direction,
            List<String> statuses,
            String status,
            Boolean includeExcluded
    ) {
    }

    public record FinanceFact(
            UUID transactionId,
            UUID walletId,
            UUID personId,
            String personCode,
            String fullName,
            String transactionType,
            String direction,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String status,
            String externalReference,
            String note,
            LocalDateTime createdAt,
            LocalDateTime reviewedAt,
            UUID coursePurchaseId,
            UUID trainingPlanId,
            String trainingPlanName,
            UUID enrollmentAddonPurchaseId,
            UUID trainingPlanAddonId,
            Long attributedBranchId,
            String attributedBranchName,
            Boolean migrationSynthetic
    ) {
    }

    public record DevelopmentQuery(
            LocalDate from,
            LocalDate to,
            LocalDate asOf,
            List<Long> branchIds,
            Long branchId,
            List<String> belts,
            String belt,
            List<String> levels,
            List<String> examTypes,
            String examType,
            List<String> examResults,
            String examResult,
            Integer minAge,
            Integer maxAge,
            UUID personId
    ) {
    }

    public record BeltExamFact(
            UUID examId,
            UUID personId,
            String personCode,
            String fullName,
            LocalDate birthDate,
            String currentBelt,
            String personStatus,
            LocalDate examDate,
            String fromBelt,
            String targetBelt,
            String examType,
            String examResult,
            Long branchId,
            String branchName
    ) {
    }

    public record FitnessFact(
            Long fitnessRecordId,
            Long fitnessId,
            String level,
            Integer amount,
            Integer targetDuration,
            LocalDate recordDate,
            Integer duration,
            UUID personId,
            String personCode,
            String fullName,
            LocalDate birthDate,
            String currentBelt,
            String personStatus,
            Long branchId,
            String branchName
    ) {
    }

    public record BookingFact(
            UUID bookingId,
            UUID enrollmentId,
            UUID sessionId,
            LocalDate sessionDate,
            String sessionStatus,
            String source,
            String status,
            LocalDateTime bookedAt,
            LocalDateTime cancelledAt,
            LocalDateTime settledAt
    ) {
    }

    public record AddonFact(
            UUID addonId,
            UUID trainingPlanId,
            String name,
            Integer extraSessionsPerWeek,
            BigDecimal price,
            Long validUsageCount
    ) {
    }
}
