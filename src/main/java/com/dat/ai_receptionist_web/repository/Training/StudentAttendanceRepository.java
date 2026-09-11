package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentAttendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, UUID> {
    boolean existsByClassSession_ClassSessionIdAndStudentEnrollment_StudentEnrollmentId(
            UUID classSessionId, UUID studentEnrollmentId);

    List<StudentAttendance> findByClassSession_ClassSessionId(UUID classSessionId);

    @Query("""
        select a
        from StudentAttendance a
        join a.classSession cs
        join a.studentEnrollment e
        where cs.sessionDate between :fromDate and :toDate
          and (:courseId is null or cs.course.courseId = :courseId)
          and (:studentPersonId is null or e.studentPerson.personId = :studentPersonId)
          and (
              :unrestricted = true
              or (:self = true and e.studentPerson.personId = :activePersonId)
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
    Page<StudentAttendance> findAccessible(
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
            Pageable pageable
    );

    @Query("""
        select a
        from StudentAttendance a
        join a.classSession cs
        join a.studentEnrollment e
        where a.studentAttendanceId = :id
          and (
              :unrestricted = true
              or (:self = true and e.studentPerson.personId = :activePersonId)
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
    Optional<StudentAttendance> findAccessibleById(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("activePersonId") UUID activePersonId,
            @Param("unrestricted") boolean unrestricted,
            @Param("self") boolean self,
            @Param("dependents") boolean dependents,
            @Param("assignedCourses") boolean assignedCourses
    );
}
