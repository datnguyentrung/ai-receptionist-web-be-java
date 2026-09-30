package com.dat.ai_receptionist_web.specification;

import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Training.SessionAttendanceFilter;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Training.access.TrainingAccessScope;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class SessionAttendanceSpecification {
    private SessionAttendanceSpecification() {
    }

    public static Specification<SessionAttendance> matching(
            SessionAttendanceFilter filter,
            AccessContext context,
            TrainingAccessScope scope
    ) {
        return (root, query, cb) -> {
            Join<SessionAttendance, ClassSession> session = root.join("classSession", JoinType.INNER);
            Join<SessionAttendance, StudentEnrollment> enrollment = root.join("studentEnrollment", JoinType.LEFT);
            Join<SessionAttendance, CourseStaffAssignment> participantAssignment =
                    root.join("courseStaffAssignment", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(session.get("sessionDate"), filter.from(), filter.to()));

            if (filter.courseId() != null) {
                predicates.add(cb.equal(session.get("course").get("courseId"), filter.courseId()));
            }

            List<Predicate> participantPredicates = new ArrayList<>(2);
            if (filter.studentPersonId() != null) {
                participantPredicates.add(cb.equal(
                        enrollment.get("studentPerson").get("personId"), filter.studentPersonId()));
            }
            if (filter.staffPersonId() != null) {
                participantPredicates.add(cb.equal(
                        participantAssignment.get("staffPerson").get("personId"), filter.staffPersonId()));
            }
            if (!participantPredicates.isEmpty()) {
                predicates.add(cb.or(participantPredicates.toArray(Predicate[]::new)));
            }

            if (filter.branchId() != null) {
                predicates.add(cb.equal(
                        session.get("scheduleSnapshot").get("branchId"), filter.branchId()));
            }
            if (filter.weekday() != null) {
                predicates.add(cb.equal(
                        session.get("scheduleSnapshot").get("weekday"), filter.weekday()));
            }
            if (filter.scheduleLevel() != null) {
                predicates.add(cb.equal(
                        session.get("scheduleSnapshot").get("level"), filter.scheduleLevel()));
            }
            if (filter.location() != null) {
                predicates.add(cb.equal(
                        session.get("scheduleSnapshot").get("location"), filter.location()));
            }

            predicates.add(accessPredicate(
                    query, cb, session, enrollment, participantAssignment, context, scope));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate accessPredicate(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Join<SessionAttendance, ClassSession> session,
            Join<SessionAttendance, StudentEnrollment> enrollment,
            Join<SessionAttendance, CourseStaffAssignment> participantAssignment,
            AccessContext context,
            TrainingAccessScope scope
    ) {
        if (scope.unrestricted()) {
            return cb.conjunction();
        }

        List<Predicate> allowed = new ArrayList<>();
        if (scope.self() && context.activePersonId() != null) {
            allowed.add(cb.equal(
                    enrollment.get("studentPerson").get("personId"), context.activePersonId()));
            allowed.add(cb.equal(
                    participantAssignment.get("staffPerson").get("personId"), context.activePersonId()));
        }

        if (scope.dependents() && context.userId() != null) {
            Subquery<Integer> dependent = query.subquery(Integer.class);
            Root<UserPerson> userPerson = dependent.from(UserPerson.class);
            dependent.select(cb.literal(1));
            dependent.where(
                    cb.equal(userPerson.get("user").get("userId"), context.userId()),
                    cb.equal(userPerson.get("person").get("personId"),
                            enrollment.get("studentPerson").get("personId")),
                    cb.equal(userPerson.get("relationshipType"), RelationshipType.GUARDIAN),
                    cb.isTrue(userPerson.get("active"))
            );
            allowed.add(cb.exists(dependent));
        }

        if (scope.assignedCourses() && context.activePersonId() != null) {
            Subquery<Integer> assignedCourse = query.subquery(Integer.class);
            Root<CourseStaffAssignment> assignment = assignedCourse.from(CourseStaffAssignment.class);
            assignedCourse.select(cb.literal(1));
            assignedCourse.where(
                    cb.equal(assignment.get("staffPerson").get("personId"), context.activePersonId()),
                    cb.equal(assignment.get("course").get("courseId"),
                            session.get("course").get("courseId")),
                    cb.lessThanOrEqualTo(assignment.get("startDate"), session.get("sessionDate")),
                    cb.or(
                            cb.isNull(assignment.get("endDate")),
                            cb.greaterThanOrEqualTo(assignment.get("endDate"), session.get("sessionDate"))
                    ),
                    assignment.get("assignmentStatus").in(
                            CourseStaffAssignmentStatus.ACTIVE,
                            CourseStaffAssignmentStatus.ENDED)
            );
            allowed.add(cb.exists(assignedCourse));
        }

        return allowed.isEmpty()
                ? cb.disjunction()
                : cb.or(allowed.toArray(Predicate[]::new));
    }
}
