package com.dat.ai_receptionist_web.service.Report;

import com.dat.ai_receptionist_web.dto.Report.YearlySummaryDTO;
import com.dat.ai_receptionist_web.dto.Training.SessionAttendanceDTO;
import com.dat.ai_receptionist_web.enums.Report.ExamEligibility;
import com.dat.ai_receptionist_web.repository.Training.SessionAttendanceQueryRepository.AttendanceStatsRow;
import com.dat.ai_receptionist_web.repository.Training.SessionAttendanceRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonYearlySummaryService {
    private final SessionAttendanceRepository attendanceRepository;

    @Transactional(readOnly = true)
    public YearlySummaryDTO.Response yearlySummary(UUID personId, int year) {
        List<YearlySummaryDTO.QuarterSummary> quarters = IntStream.rangeClosed(1, 4)
                .mapToObj(quarter -> summary(personId, year, quarter)).toList();
        return new YearlySummaryDTO.Response(year, quarters);
    }

    private YearlySummaryDTO.QuarterSummary summary(UUID personId, int year, int quarter) {
        LocalDate start = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
        LocalDate end = YearMonth.from(start.plusMonths(2)).atEndOfMonth();
        Specification<com.dat.ai_receptionist_web.domain.Training.SessionAttendance> specification =
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("studentEnrollment").get("studentPerson").get("personId"), personId),
                        cb.between(root.get("classSession").get("sessionDate"), start, end));
        SessionAttendanceDTO.AttendanceStats stats = toStats(attendanceRepository.summarize(specification));
        double attendanceScore = attendanceScore(stats);
        double performanceScore = stats.evalGoodCount() * 5.0 + stats.evalAverageCount() * 3.0;
        return new YearlySummaryDTO.QuarterSummary(
                quarter, stats, attendanceScore, performanceScore, attendanceScore + performanceScore,
                eligibility(stats, attendanceScore, performanceScore, end));
    }

    private static SessionAttendanceDTO.AttendanceStats toStats(AttendanceStatsRow row) {
        double rate = row.total() == 0 ? 0.0 : (double) (row.present() + row.makeup() + row.late()) / row.total() * 100.0;
        return new SessionAttendanceDTO.AttendanceStats(row.total(), rate, row.present(), row.absent(), row.excused(),
                row.makeup(), row.late(), row.evaluationGood(), row.evaluationAverage(), row.evaluationWeak(), row.evaluationPending());
    }

    private static double attendanceScore(SessionAttendanceDTO.AttendanceStats stats) {
        if (stats.totalRecords() == 0) return 0.0;
        if (stats.absentCount() == 0 && stats.lateCount() == 0 && stats.excusedCount() == 0) return 5.0;
        return 4.0 - stats.absentCount() - stats.excusedCount() * .5 + stats.makeupCount() * .5 - stats.lateCount() * .5;
    }

    private static ExamEligibility eligibility(SessionAttendanceDTO.AttendanceStats stats, double attendance, double performance, LocalDate end) {
        if (stats.totalRecords() == 0) return ExamEligibility.NONE;
        if (!LocalDate.now().isAfter(end) || stats.evalPendingCount() > 0) return ExamEligibility.PENDING;
        if (stats.absentCount() > 2 || attendance < 0 || stats.evalWeakCount() > 3) return ExamEligibility.NOT_ELIGIBLE;
        long attended = stats.presentCount() + stats.lateCount() + stats.makeupCount();
        if (attended >= 16 && attendance == 5.0 && performance >= 85.0) return ExamEligibility.EXEMPTED;
        return stats.evalGoodCount() >= 8 ? ExamEligibility.ELIGIBLE : ExamEligibility.NOT_ELIGIBLE;
    }
}
