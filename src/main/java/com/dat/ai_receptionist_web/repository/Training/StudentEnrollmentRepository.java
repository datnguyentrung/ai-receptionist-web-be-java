package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select e from StudentEnrollment e
        join fetch e.coursePurchase p
        left join fetch p.trainingPlan
        where e.studentEnrollmentId = :id
    """)
    Optional<StudentEnrollment> findDetailedByIdForUpdate(@Param("id") UUID id);
    @Query("""
        select count(distinct e) from StudentEnrollment e
        join e.schedules link
        where link.courseSchedule.course.courseId = :courseId
    """)
    long countByCourseScheduleCourseId(@Param("courseId") UUID courseId);

    @Query("""
        select count(distinct e)
        from StudentEnrollment e
        join e.schedules link
        join link.courseSchedule courseSchedule
        join courseSchedule.course course
        where course.courseId = :courseId
          and e.status = com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus.ACTIVE
          and e.startDate <= :currentDate
          and e.endDate >= :currentDate
          and (link.effectiveFrom is null or link.effectiveFrom <= :currentDate)
          and (link.effectiveTo is null or link.effectiveTo >= :currentDate)
    """)
    long countCurrentStudentsByCourseId(
            @Param("courseId") UUID courseId,
            @Param("currentDate") LocalDate currentDate
    );

    @Query("""
        select course.courseId as courseId, count(distinct e) as studentCount
        from StudentEnrollment e
        join e.schedules link
        join link.courseSchedule courseSchedule
        join courseSchedule.course course
        where course.courseId in :courseIds
          and e.status = com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus.ACTIVE
          and e.startDate <= :currentDate
          and e.endDate >= :currentDate
          and (link.effectiveFrom is null or link.effectiveFrom <= :currentDate)
          and (link.effectiveTo is null or link.effectiveTo >= :currentDate)
        group by course.courseId
    """)
    List<CourseStudentCount> countCurrentStudentsByCourseIds(
            @Param("courseIds") List<UUID> courseIds,
            @Param("currentDate") LocalDate currentDate
    );

    interface CourseStudentCount {
        UUID getCourseId();

        long getStudentCount();
    }

    Optional<StudentEnrollment> findByCoursePurchase_CoursePurchaseId(UUID coursePurchaseId);

    @Query("""
        select distinct e
        from StudentEnrollment e
        join e.schedules link
        join link.courseSchedule courseSchedule
        join courseSchedule.course course
        where e.studentPerson.personId = :personId
          and course.courseId = :courseId
          and e.status = com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus.ACTIVE
          and e.startDate <= :sessionDate
          and e.endDate >= :sessionDate
          and (link.effectiveFrom is null or link.effectiveFrom <= :sessionDate)
          and (link.effectiveTo is null or link.effectiveTo >= :sessionDate)
    """)
    Optional<StudentEnrollment> findActiveEnrollmentForCourseOnDate(
            @Param("personId") UUID personId,
            @Param("courseId") UUID courseId,
            @Param("sessionDate") LocalDate sessionDate
    );

    @Query("""
        select distinct e
        from StudentEnrollment e
        join e.schedules link
        join link.courseSchedule courseSchedule
        join courseSchedule.course course
        where course.courseId = :courseId
          and e.status = com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus.ACTIVE
          and e.startDate <= :sessionDate
          and e.endDate >= :sessionDate
          and (link.effectiveFrom is null or link.effectiveFrom <= :sessionDate)
          and (link.effectiveTo is null or link.effectiveTo >= :sessionDate)
    """)
    List<StudentEnrollment> findActiveEnrollmentsForCourseOnDate(
            @Param("courseId") UUID courseId,
            @Param("sessionDate") LocalDate sessionDate
    );

    @Query("""
        select distinct e
        from StudentEnrollment e
        left join fetch e.coursePurchase p
        left join fetch p.trainingPlan
        join e.schedules enrollmentScheduleLink
        join enrollmentScheduleLink.courseSchedule enrollmentCourseSchedule
        join enrollmentCourseSchedule.course course
        left join fetch e.studentPerson enrollmentStudent
        left join fetch enrollmentStudent.position
        where e.startDate <= :toDate
          and e.endDate >= :fromDate
          and (:courseId is null or course.courseId = :courseId)
          and (enrollmentScheduleLink.effectiveFrom is null or enrollmentScheduleLink.effectiveFrom <= :toDate)
          and (enrollmentScheduleLink.effectiveTo is null or enrollmentScheduleLink.effectiveTo >= :fromDate)
          and (:studentPersonId is null or e.studentPerson.personId = :studentPersonId)
          and (:status is null or e.status = :status)
          and (:search = ''
              or lower(enrollmentStudent.fullName) like lower(concat('%', :search, '%'))
              or lower(coalesce(enrollmentStudent.personCode, '')) like lower(concat('%', :search, '%')))
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
                        and csa.courseSchedule.course.courseId = course.courseId
                        and csa.startDate <= :toDate
                        and (csa.endDate is null or csa.endDate >= :fromDate)
                        and csa.assignmentStatus in (com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE, com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED)
                  )
              )
          )
    """)
    Page<StudentEnrollment> findAccessible(
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
            @Param("status") com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
        select e
        from StudentEnrollment e
        left join fetch e.coursePurchase p
        join fetch p.trainingPlan
        left join fetch e.studentPerson enrollmentStudent
        left join fetch enrollmentStudent.position
        left join fetch e.schedules enrollmentScheduleLink
        left join fetch enrollmentScheduleLink.courseSchedule enrollmentCourseSchedule
        left join fetch enrollmentCourseSchedule.classSchedule enrollmentSchedule
        left join fetch enrollmentSchedule.branch
        where e.studentEnrollmentId = :id
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
                        and csa.courseSchedule.course.courseId = enrollmentCourseSchedule.course.courseId
                        and csa.startDate <= e.endDate
                        and (csa.endDate is null or csa.endDate >= e.startDate)
                        and csa.assignmentStatus in (com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE, com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED)
                  )
              )
          )
    """)
    Optional<StudentEnrollment> findAccessibleById(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("activePersonId") UUID activePersonId,
            @Param("unrestricted") boolean unrestricted,
            @Param("self") boolean self,
            @Param("dependents") boolean dependents,
            @Param("assignedCourses") boolean assignedCourses
    );
}
