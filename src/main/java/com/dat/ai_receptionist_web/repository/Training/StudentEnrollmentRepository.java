package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {
    long countByCoursePurchase_CoursePrice_Course_CourseId(UUID courseId);
    Optional<StudentEnrollment> findByCoursePurchase_CoursePurchaseId(UUID coursePurchaseId);

    @Query("""
        select e
        from StudentEnrollment e
        join e.coursePurchase p
        join p.coursePrice pr
        where e.studentPerson.personId = :personId
          and pr.course.courseId = :courseId
          and e.status = com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus.ACTIVE
          and e.startDate <= :sessionDate
          and e.endDate >= :sessionDate
    """)
    Optional<StudentEnrollment> findActiveEnrollmentForCourseOnDate(
            @Param("personId") UUID personId,
            @Param("courseId") UUID courseId,
            @Param("sessionDate") LocalDate sessionDate
    );

    @Query("""
        select e
        from StudentEnrollment e
        join e.coursePurchase p
        join p.coursePrice pr
        where pr.course.courseId = :courseId
          and e.status = com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus.ACTIVE
          and e.startDate <= :sessionDate
          and e.endDate >= :sessionDate
    """)
    List<StudentEnrollment> findActiveEnrollmentsForCourseOnDate(
            @Param("courseId") UUID courseId,
            @Param("sessionDate") LocalDate sessionDate
    );

    @Query("""
        select e
        from StudentEnrollment e
        left join fetch e.coursePurchase p
        join p.coursePrice pr
        left join fetch e.studentPerson enrollmentStudent
        left join fetch e.classSchedule enrollmentSchedule
        left join fetch enrollmentSchedule.branch
        where e.startDate <= :toDate
          and e.endDate >= :fromDate
          and (:courseId is null or pr.course.courseId = :courseId)
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
                        and csa.course.courseId = pr.course.courseId
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
            Pageable pageable
    );

    @Query("""
        select e
        from StudentEnrollment e
        left join fetch e.coursePurchase p
        join p.coursePrice pr
        left join fetch e.studentPerson enrollmentStudent
        left join fetch enrollmentStudent.position
        left join fetch e.classSchedule enrollmentSchedule
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
                        and csa.course.courseId = pr.course.courseId
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
