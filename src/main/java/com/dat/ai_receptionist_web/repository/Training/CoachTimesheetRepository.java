package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.CoachTimesheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface CoachTimesheetRepository extends JpaRepository<CoachTimesheet, UUID> {
    @Query("""
        select t
        from CoachTimesheet t
        join t.classSession cs
        join t.courseStaffAssignment csa
        where cs.sessionDate between :fromDate and :toDate
          and (:courseId is null or cs.course.courseId = :courseId)
          and (
              :unrestricted = true
              or (
                  :self = true
                  and csa.staffPerson.personId = :activePersonId
                  and csa.course.courseId = cs.course.courseId
                  and csa.startDate <= cs.sessionDate
                  and (csa.endDate is null or csa.endDate >= cs.sessionDate)
                  and csa.assignmentStatus in (
                      com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE,
                      com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED
                  )
              )
              or (
                  :managedCourses = true
                  and exists (
                      select 1
                      from CourseStaffAssignment actorAssignment
                      where actorAssignment.staffPerson.personId = :activePersonId
                        and actorAssignment.course.courseId = cs.course.courseId
                        and actorAssignment.assignmentType = com.dat.ai_receptionist_web.enums.Training.AssignmentType.MANAGER
                        and actorAssignment.startDate <= cs.sessionDate
                        and (actorAssignment.endDate is null or actorAssignment.endDate >= cs.sessionDate)
                        and actorAssignment.assignmentStatus in (
                            com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE,
                            com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED
                        )
                  )
              )
          )
    """)
    Page<CoachTimesheet> findAccessible(
            @Param("activePersonId") UUID activePersonId,
            @Param("unrestricted") boolean unrestricted,
            @Param("self") boolean self,
            @Param("managedCourses") boolean managedCourses,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("courseId") UUID courseId,
            Pageable pageable
    );

    @Query("""
        select t
        from CoachTimesheet t
        join t.classSession cs
        join t.courseStaffAssignment csa
        where t.coachTimesheetId = :id
          and (
              :unrestricted = true
              or (
                  :self = true
                  and csa.staffPerson.personId = :activePersonId
                  and csa.course.courseId = cs.course.courseId
                  and csa.startDate <= cs.sessionDate
                  and (csa.endDate is null or csa.endDate >= cs.sessionDate)
                  and csa.assignmentStatus in (
                      com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE,
                      com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED
                  )
              )
              or (
                  :managedCourses = true
                  and exists (
                      select 1
                      from CourseStaffAssignment actorAssignment
                      where actorAssignment.staffPerson.personId = :activePersonId
                        and actorAssignment.course.courseId = cs.course.courseId
                        and actorAssignment.assignmentType = com.dat.ai_receptionist_web.enums.Training.AssignmentType.MANAGER
                        and actorAssignment.startDate <= cs.sessionDate
                        and (actorAssignment.endDate is null or actorAssignment.endDate >= cs.sessionDate)
                        and actorAssignment.assignmentStatus in (
                            com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE,
                            com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED
                        )
                  )
              )
          )
    """)
    Optional<CoachTimesheet> findAccessibleById(
            @Param("id") UUID id,
            @Param("activePersonId") UUID activePersonId,
            @Param("unrestricted") boolean unrestricted,
            @Param("self") boolean self,
            @Param("managedCourses") boolean managedCourses
    );
}