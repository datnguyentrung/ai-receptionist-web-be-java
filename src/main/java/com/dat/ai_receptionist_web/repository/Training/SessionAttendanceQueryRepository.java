package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import org.springframework.data.jpa.domain.Specification;

public interface SessionAttendanceQueryRepository {
    AttendanceStatsRow summarize(Specification<SessionAttendance> specification);

    record AttendanceStatsRow(
            long total,
            long present,
            long absent,
            long excused,
            long makeup,
            long late,
            long evaluationGood,
            long evaluationAverage,
            long evaluationWeak,
            long evaluationPending
    ) {
    }
}
