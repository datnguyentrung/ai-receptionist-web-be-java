package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Training.EvaluationStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@RequiredArgsConstructor
public class SessionAttendanceQueryRepositoryImpl implements SessionAttendanceQueryRepository {
    private final EntityManager entityManager;

    @Override
    public AttendanceStatsRow summarize(Specification<SessionAttendance> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<SessionAttendance> root = query.from(SessionAttendance.class);
        Predicate predicate = specification.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }

        query.multiselect(
                cb.count(root),
                countWhen(cb, root, "attendanceStatus", AttendanceStatus.PRESENT),
                countWhen(cb, root, "attendanceStatus", AttendanceStatus.ABSENT),
                countWhen(cb, root, "attendanceStatus", AttendanceStatus.EXCUSED),
                countWhen(cb, root, "attendanceStatus", AttendanceStatus.MAKEUP),
                countWhen(cb, root, "attendanceStatus", AttendanceStatus.LATE),
                countWhen(cb, root, "evaluationStatus", EvaluationStatus.GOOD),
                countWhen(cb, root, "evaluationStatus", EvaluationStatus.AVERAGE),
                countWhen(cb, root, "evaluationStatus", EvaluationStatus.WEAK),
                countWhen(cb, root, "evaluationStatus", EvaluationStatus.PENDING)
        );

        Tuple result = entityManager.createQuery(query).getSingleResult();
        return new AttendanceStatsRow(
                longValue(result, 0),
                longValue(result, 1),
                longValue(result, 2),
                longValue(result, 3),
                longValue(result, 4),
                longValue(result, 5),
                longValue(result, 6),
                longValue(result, 7),
                longValue(result, 8),
                longValue(result, 9)
        );
    }

    private static <E extends Enum<E>> Expression<Long> countWhen(
            CriteriaBuilder cb,
            Root<SessionAttendance> root,
            String field,
            E value
    ) {
        return cb.sum(cb.<Long>selectCase()
                .when(cb.equal(root.get(field), value), 1L)
                .otherwise(0L));
    }

    private static long longValue(Tuple tuple, int index) {
        Number value = tuple.get(index, Number.class);
        return value == null ? 0 : value.longValue();
    }
}
