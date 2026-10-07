package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.enums.Core.Belt;
import com.dat.ai_receptionist_web.enums.Core.PersonStatus;
import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Training.EvaluationStatus;
import com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionAttendanceRepository extends
        JpaRepository<SessionAttendance, UUID>,
        JpaSpecificationExecutor<SessionAttendance>,
        SessionAttendanceQueryRepository {

    boolean existsByClassSession_ClassSessionIdAndStudentEnrollment_StudentEnrollmentId(
            UUID classSessionId, UUID studentEnrollmentId);

    boolean existsByClassSession_ClassSessionIdAndCourseStaffAssignment_CourseStaffAssignmentId(
            UUID classSessionId, UUID courseStaffAssignmentId);

    Optional<SessionAttendance> findByClassSession_ClassSessionIdAndStudentEnrollment_StudentEnrollmentId(
            UUID classSessionId, UUID studentEnrollmentId);

    Optional<SessionAttendance> findByClassSession_ClassSessionIdAndCourseStaffAssignment_CourseStaffAssignmentId(
            UUID classSessionId, UUID courseStaffAssignmentId);

    List<SessionAttendance> findByClassSession_ClassSessionId(UUID classSessionId);

    @Query("""
        select e.studentEnrollmentId as studentEnrollmentId,
               sp.personId as studentPersonId,
               sp.fullName as studentFullName,
               sp.gender as studentGender,
               sp.birthDate as studentBirthDate,
               sp.personCode as studentPersonCode,
               sp.currentBelt as studentCurrentBelt,
               sp.status as studentStatus,
               sp.faceImagePath as studentFaceImagePath,
               p.coursePurchaseId as coursePurchaseId,
               e.startDate as enrollmentStartDate,
               e.endDate as enrollmentEndDate,
               e.status as enrollmentStatus,
               a.sessionAttendanceId as sessionAttendanceId,
               a.checkInTime as checkInTime,
               a.attendanceStatus as attendanceStatus,
               a.evaluationStatus as evaluationStatus,
               a.note as attendanceNote
        from StudentEnrollment e
        join e.studentPerson sp
        join e.coursePurchase p
        join p.coursePrice price
        left join SessionAttendance a
          on a.classSession.classSessionId = :classSessionId
         and a.studentEnrollment.studentEnrollmentId = e.studentEnrollmentId
        where price.course.courseId = :courseId
          and e.status = com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus.ACTIVE
          and e.startDate <= :sessionDate
          and e.endDate >= :sessionDate
        order by lower(sp.fullName) asc, e.studentEnrollmentId asc
    """)
    List<ClassSessionEvaluationRow> findClassSessionEvaluationRows(
            @Param("classSessionId") UUID classSessionId,
            @Param("courseId") UUID courseId,
            @Param("sessionDate") LocalDate sessionDate
    );

    interface ClassSessionEvaluationRow {
        UUID getStudentEnrollmentId();

        UUID getStudentPersonId();

        String getStudentFullName();

        Boolean getStudentGender();

        LocalDate getStudentBirthDate();

        String getStudentPersonCode();

        Belt getStudentCurrentBelt();

        PersonStatus getStudentStatus();

        String getStudentFaceImagePath();

        UUID getCoursePurchaseId();

        LocalDate getEnrollmentStartDate();

        LocalDate getEnrollmentEndDate();

        StudentEnrollmentStatus getEnrollmentStatus();

        UUID getSessionAttendanceId();

        LocalDateTime getCheckInTime();

        AttendanceStatus getAttendanceStatus();

        EvaluationStatus getEvaluationStatus();

        String getAttendanceNote();
    }

    @Override
    @NonNull
    @EntityGraph(attributePaths = {
            "classSession.courseSchedule.classSchedule.branch",
            "studentEnrollment.studentPerson",
            "studentEnrollment.schedules.courseSchedule.classSchedule.branch",
            "studentEnrollment.coursePurchase",
            "courseStaffAssignment.staffPerson",
            "courseStaffAssignment.courseSchedule.classSchedule.branch"
    })
    Page<SessionAttendance> findAll(
            @NonNull Specification<SessionAttendance> specification,
            @NonNull Specification<SessionAttendance> countSpecification,
            @NonNull Pageable pageable
    );

    @Query("""
        select a
        from SessionAttendance a
        join fetch a.classSession cs
        left join fetch cs.course
        left join fetch cs.courseSchedule sessionCourseSchedule
        left join fetch sessionCourseSchedule.classSchedule sessionClassSchedule
        left join fetch sessionClassSchedule.branch
        left join fetch a.studentEnrollment e
        left join fetch e.studentPerson enrollmentStudent
        left join fetch enrollmentStudent.position
        left join fetch e.schedules enrollmentScheduleLink
        left join fetch enrollmentScheduleLink.courseSchedule enrollmentCourseSchedule
        left join fetch enrollmentCourseSchedule.classSchedule enrollmentSchedule
        left join fetch enrollmentSchedule.branch
        left join fetch e.coursePurchase
        left join fetch a.courseStaffAssignment participantAssignment
        left join fetch participantAssignment.staffPerson participantStaff
        left join fetch participantStaff.position
        left join fetch participantAssignment.courseSchedule participantCourseSchedule
        left join fetch participantCourseSchedule.classSchedule participantClassSchedule
        left join fetch participantClassSchedule.branch
        where a.sessionAttendanceId = :id
          and (
              :unrestricted = true
              or (:self = true and e.studentPerson.personId = :activePersonId)
              or (:self = true and participantAssignment.staffPerson.personId = :activePersonId)
              or (
                  :dependents = true
                  and exists (
                      select 1
                      from UserPerson up
                      where up.user.userId = :userId
                        and up.person.personId = e.studentPerson.personId
                        and up.relationshipType = com.dat.ai_receptionist_web.enums.Security.RelationshipType.GUARDIAN
                        and up.active = true
                  )
              )
              or (
                  :assignedCourses = true
                  and exists (
                      select 1
                      from CourseStaffAssignment csa
                      where csa.staffPerson.personId = :activePersonId
                        and csa.courseSchedule.courseScheduleId = cs.courseSchedule.courseScheduleId
                        and csa.startDate <= cs.sessionDate
                        and (csa.endDate is null or csa.endDate >= cs.sessionDate)
                        and csa.assignmentStatus in (com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE, com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED)
                  )
              )
          )
    """)
    Optional<SessionAttendance> findAccessibleById(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("activePersonId") UUID activePersonId,
            @Param("unrestricted") boolean unrestricted,
            @Param("self") boolean self,
            @Param("dependents") boolean dependents,
            @Param("assignedCourses") boolean assignedCourses
    );
}
