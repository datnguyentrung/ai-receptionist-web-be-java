package com.dat.ai_receptionist_web.repository;

import com.dat.ai_receptionist_web.repository.Catalog.ClassScheduleRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CoursePriceRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Finance.CoursePurchaseRepository;
import com.dat.ai_receptionist_web.repository.Finance.WalletRepository;
import com.dat.ai_receptionist_web.repository.Finance.WalletTransactionRepository;
import com.dat.ai_receptionist_web.repository.Notification.NotificationRecipientRepository;
import com.dat.ai_receptionist_web.repository.Security.AuthSessionRepository;
import com.dat.ai_receptionist_web.repository.Security.RolePermissionRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRoleRepository;
import com.dat.ai_receptionist_web.repository.Skill.FitnessRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;

class ListFetchGraphContractTest {
    @Test
    void nPlusOneSensitiveListQueriesFetchTheirMappingGraph() throws Exception {
        assertDetailedListQuery(UserPersonRepository.class,
                "join fetch up.user",
                "join fetch up.person");
        assertDetailedListQuery(ClassScheduleRepository.class,
                "join fetch cs.branch");
        assertDetailedListQuery(CourseRepository.class,
                "join fetch c.classSchedule",
                "join fetch classSchedule.branch",
                "left join fetch c.nextClassSchedule",
                "left join fetch nextClassSchedule.branch");
        assertDetailedListQuery(CoursePriceRepository.class,
                "join fetch cp.course",
                "join fetch course.classSchedule",
                "join fetch classSchedule.branch",
                "left join fetch course.nextClassSchedule",
                "left join fetch nextClassSchedule.branch");
        assertDetailedListQuery(WalletRepository.class,
                "join fetch w.person");
        assertDetailedListQuery(WalletTransactionRepository.class,
                "join fetch tx.wallet",
                "join fetch wallet.person",
                "join fetch tx.createdByUser",
                "left join fetch tx.reviewedByUser");
        assertDetailedListQuery(CoursePurchaseRepository.class,
                "join fetch purchase.studentPerson",
                "join fetch purchase.coursePrice",
                "join fetch coursePrice.course",
                "join fetch course.classSchedule",
                "join fetch classSchedule.branch",
                "join fetch purchase.debitTransaction");
        assertDetailedListQuery(FitnessRecordRepository.class,
                "join fetch fr.student",
                "join fetch fr.fitness",
                "join fetch fr.recordedByCoach");
        assertDetailedListQuery(AuthSessionRepository.class,
                "join fetch s.user",
                "left join fetch s.activeUserPerson",
                "left join fetch activeUserPerson.user",
                "left join fetch activeUserPerson.person");
        assertDetailedListQuery(NotificationRecipientRepository.class,
                "join fetch nr.notification",
                "join fetch nr.recipientUser",
                "left join fetch nr.contextPerson");
        assertDetailedListQuery(UserRoleRepository.class,
                "join fetch ur.user",
                "join fetch ur.role");
        assertDetailedListQuery(RolePermissionRepository.class,
                "join fetch rp.role",
                "join fetch rp.permission");
    }

    private static void assertDetailedListQuery(Class<?> repositoryType, String... expectedFetches) throws Exception {
        Query query = repositoryType.getMethod("findAllDetailed", Pageable.class).getAnnotation(Query.class);

        assertThat(query.value()).contains(expectedFetches);
        assertThat(query.countQuery()).isNotBlank();
        assertThat(query.countQuery()).doesNotContain("fetch");
    }
}
