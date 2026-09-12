package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseStaffAssignmentRepository extends JpaRepository<CourseStaffAssignment, UUID> {
    @Query("""
        select a
        from CourseStaffAssignment a
        where a.staffPerson.personId = :staffPersonId
          and a.course.courseId = :courseId
          and a.startDate <= :sessionDate
          and (a.endDate is null or a.endDate >= :sessionDate)
          and a.assignmentStatus in (
              com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE,
              com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED
          )
        order by a.startDate desc, a.courseStaffAssignmentId
    """)
    List<CourseStaffAssignment> findEffectiveAssignmentsForStaffCourseOnDate(
            @Param("staffPersonId") UUID staffPersonId,
            @Param("courseId") UUID courseId,
            @Param("sessionDate") LocalDate sessionDate
    );

    @Query("""
        select a
        from CourseStaffAssignment a
        where a.staffPerson.personId = :staffPersonId
          and a.course.courseId = :courseId
          and a.assignmentType = :assignmentType
          and a.startDate <= :sessionDate
          and (a.endDate is null or a.endDate >= :sessionDate)
          and a.assignmentStatus in (
              com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE,
              com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED
          )
        order by a.startDate desc, a.courseStaffAssignmentId
    """)
    List<CourseStaffAssignment> findEffectiveAssignmentsForStaffCourseTypeOnDate(
            @Param("staffPersonId") UUID staffPersonId,
            @Param("courseId") UUID courseId,
            @Param("assignmentType") com.dat.ai_receptionist_web.enums.Training.AssignmentType assignmentType,
            @Param("sessionDate") LocalDate sessionDate
    );

    @Query("""
        select a
        from CourseStaffAssignment a
        where a.course.courseId = :courseId
          and a.assignmentType = com.dat.ai_receptionist_web.enums.Training.AssignmentType.ASSISTANT_COACH
          and a.startDate <= :sessionDate
          and (a.endDate is null or a.endDate >= :sessionDate)
          and a.assignmentStatus in (
              com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE,
              com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED
          )
        order by a.staffPerson.personId, a.startDate desc, a.courseStaffAssignmentId
    """)
    List<CourseStaffAssignment> findEffectiveAssistantAssignmentsForCourseOnDate(
            @Param("courseId") UUID courseId,
            @Param("sessionDate") LocalDate sessionDate
    );

    @Query("""
        select count(a) > 0
        from CourseStaffAssignment a
        where a.staffPerson.personId = :staffPersonId
          and a.course.courseId = :courseId
          and a.startDate <= :toDate
          and (a.endDate is null or a.endDate >= :fromDate)
          and a.assignmentStatus in (
              com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE,
              com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED
          )
    """)
    boolean existsEffectiveAssignmentForStaffCoursePeriod(
            @Param("staffPersonId") UUID staffPersonId,
            @Param("courseId") UUID courseId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("""
        select count(a) > 0
        from CourseStaffAssignment a
        where a.staffPerson.personId = :staffPersonId
          and a.course.courseId = :courseId
          and a.assignmentType = com.dat.ai_receptionist_web.enums.Training.AssignmentType.MANAGER
          and a.startDate <= :startDate
          and (:endDate is null and a.endDate is null
               or :endDate is not null and (a.endDate is null or a.endDate >= :endDate))
          and a.assignmentStatus in (
              com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE,
              com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED
          )
    """)
    boolean existsManagerAssignmentCoveringPeriod(
            @Param("staffPersonId") UUID staffPersonId,
            @Param("courseId") UUID courseId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        select a
        from CourseStaffAssignment a
        where :unrestricted = true
           or (:self = true and a.staffPerson.personId = :activePersonId)
           or (
               :managedCourses = true
               and exists (
                   select 1
                   from CourseStaffAssignment managerAssignment
                   where managerAssignment.staffPerson.personId = :activePersonId
                     and managerAssignment.course.courseId = a.course.courseId
                     and managerAssignment.assignmentType = com.dat.ai_receptionist_web.enums.Training.AssignmentType.MANAGER
                     and (a.endDate is null or managerAssignment.startDate <= a.endDate)
                     and (managerAssignment.endDate is null or managerAssignment.endDate >= a.startDate)
                     and managerAssignment.assignmentStatus in (
                         com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE,
                         com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED
                     )
               )
           )
    """)
    Page<CourseStaffAssignment> findAccessible(
            @Param("activePersonId") UUID activePersonId,
            @Param("unrestricted") boolean unrestricted,
            @Param("self") boolean self,
            @Param("managedCourses") boolean managedCourses,
            Pageable pageable
    );

    @Query("""
        select a
        from CourseStaffAssignment a
        where a.courseStaffAssignmentId = :id
          and (
              :unrestricted = true
              or (:self = true and a.staffPerson.personId = :activePersonId)
              or (
                  :managedCourses = true
                  and exists (
                      select 1
                      from CourseStaffAssignment managerAssignment
                      where managerAssignment.staffPerson.personId = :activePersonId
                        and managerAssignment.course.courseId = a.course.courseId
                        and managerAssignment.assignmentType = com.dat.ai_receptionist_web.enums.Training.AssignmentType.MANAGER
                        and (a.endDate is null or managerAssignment.startDate <= a.endDate)
                        and (managerAssignment.endDate is null or managerAssignment.endDate >= a.startDate)
                        and managerAssignment.assignmentStatus in (
                            com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE,
                            com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED
                        )
                  )
              )
          )
    """)
    Optional<CourseStaffAssignment> findAccessibleById(
            @Param("id") UUID id,
            @Param("activePersonId") UUID activePersonId,
            @Param("unrestricted") boolean unrestricted,
            @Param("self") boolean self,
            @Param("managedCourses") boolean managedCourses
    );
}
