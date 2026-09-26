package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionAttendanceRepository extends JpaRepository<SessionAttendance, UUID> {
    boolean existsByClassSession_ClassSessionIdAndStudentEnrollment_StudentEnrollmentId(
            UUID classSessionId, UUID studentEnrollmentId);

    boolean existsByClassSession_ClassSessionIdAndCourseStaffAssignment_CourseStaffAssignmentId(
            UUID classSessionId, UUID courseStaffAssignmentId);

    Optional<SessionAttendance> findByClassSession_ClassSessionIdAndStudentEnrollment_StudentEnrollmentId(
            UUID classSessionId, UUID studentEnrollmentId);

    Optional<SessionAttendance> findByClassSession_ClassSessionIdAndCourseStaffAssignment_CourseStaffAssignmentId(
            UUID classSessionId, UUID courseStaffAssignmentId);

    List<SessionAttendance> findByClassSession_ClassSessionId(UUID classSessionId);

    @Query(value = """
        select a
        from SessionAttendance a
        join fetch a.classSession cs
        left join fetch cs.course sessionCourse
        left join fetch sessionCourse.classSchedule sessionCourseSchedule
        left join fetch sessionCourseSchedule.branch
        left join fetch sessionCourse.nextClassSchedule sessionCourseNextSchedule
        left join fetch sessionCourseNextSchedule.branch
        left join fetch a.studentEnrollment e
        left join fetch e.studentPerson enrollmentStudent
        left join fetch e.classSchedule enrollmentSchedule
        left join fetch enrollmentSchedule.branch
        left join fetch e.coursePurchase purchase
        left join fetch purchase.coursePrice price
        left join fetch price.course enrollmentCourse
        left join fetch a.courseStaffAssignment participantAssignment
        left join fetch participantAssignment.staffPerson participantStaff
        left join fetch participantAssignment.course participantCourse
        left join fetch participantCourse.classSchedule participantCourseSchedule
        left join fetch participantCourseSchedule.branch
        left join fetch participantCourse.nextClassSchedule participantCourseNextSchedule
        left join fetch participantCourseNextSchedule.branch
        where cs.sessionDate between :fromDate and :toDate
          and (:courseId is null or cs.course.courseId = :courseId)
          and (:studentPersonId is null and :staffPersonId is null
               or :studentPersonId is not null and e.studentPerson.personId = :studentPersonId
               or :staffPersonId is not null and participantAssignment.staffPerson.personId = :staffPersonId)
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
                        and csa.course.courseId = cs.course.courseId
                        and csa.startDate <= cs.sessionDate
                        and (csa.endDate is null or csa.endDate >= cs.sessionDate)
                        and csa.assignmentStatus in (com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE, com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED)
                  )
              )
          )
    """,
            countQuery = """
        select count(a)
        from SessionAttendance a
        join a.classSession cs
        left join a.studentEnrollment e
        left join a.courseStaffAssignment participantAssignment
        where cs.sessionDate between :fromDate and :toDate
          and (:courseId is null or cs.course.courseId = :courseId)
          and (:studentPersonId is null and :staffPersonId is null
               or :studentPersonId is not null and e.studentPerson.personId = :studentPersonId
               or :staffPersonId is not null and participantAssignment.staffPerson.personId = :staffPersonId)
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
                        and csa.course.courseId = cs.course.courseId
                        and csa.startDate <= cs.sessionDate
                        and (csa.endDate is null or csa.endDate >= cs.sessionDate)
                        and csa.assignmentStatus in (com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE, com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED)
                  )
              )
          )
    """)
    Page<SessionAttendance> findAccessible(
            @Param("userId") UUID userId,
            @Param("activePersonId") UUID activePersonId,
            @Param("unrestricted") boolean unrestricted,
            @Param("self") boolean self,
            @Param("dependents") boolean dependents,
            @Param("assignedCourses") boolean assignedCourses,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("courseId") UUID courseId,
            @Param("studentPersonId") UUID studentPersonId,
            @Param("staffPersonId") UUID staffPersonId,
            Pageable pageable
    );

    @Query("""
        select a
        from SessionAttendance a
        join fetch a.classSession cs
        left join fetch cs.course sessionCourse
        left join fetch sessionCourse.classSchedule sessionCourseSchedule
        left join fetch sessionCourseSchedule.branch
        left join fetch sessionCourse.nextClassSchedule sessionCourseNextSchedule
        left join fetch sessionCourseNextSchedule.branch
        left join fetch a.studentEnrollment e
        left join fetch e.studentPerson enrollmentStudent
        left join fetch enrollmentStudent.position
        left join fetch e.classSchedule enrollmentSchedule
        left join fetch enrollmentSchedule.branch
        left join fetch e.coursePurchase
        left join fetch a.courseStaffAssignment participantAssignment
        left join fetch participantAssignment.staffPerson participantStaff
        left join fetch participantStaff.position
        left join fetch participantAssignment.course participantCourse
        left join fetch participantCourse.classSchedule participantCourseSchedule
        left join fetch participantCourseSchedule.branch
        left join fetch participantCourse.nextClassSchedule participantCourseNextSchedule
        left join fetch participantCourseNextSchedule.branch
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
                        and csa.course.courseId = cs.course.courseId
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
