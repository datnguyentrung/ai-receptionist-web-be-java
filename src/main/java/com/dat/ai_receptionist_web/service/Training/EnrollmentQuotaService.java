package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.repository.Catalog.TrainingPlanRequirementRepository;
import com.dat.ai_receptionist_web.repository.Finance.EnrollmentAddonPurchaseRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentSessionBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EnrollmentQuotaService {
    private final TrainingPlanRequirementRepository requirementRepository;
    private final EnrollmentAddonPurchaseRepository addonPurchaseRepository;
    private final StudentSessionBookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public long weeklyQuota(StudentEnrollment enrollment, LocalDate sessionDate) {
        LocalDate weekStart = sessionDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        long base = requirementRepository.findByTrainingPlan_TrainingPlanId(
                        enrollment.getCoursePurchase().getTrainingPlan().getTrainingPlanId())
                .stream().mapToLong(requirement -> requirement.getSessionsPerWeek()).sum();
        long addons = addonPurchaseRepository.findEffectiveForWeek(
                        enrollment.getStudentEnrollmentId(), weekStart, weekEnd)
                .stream().mapToLong(purchase -> purchase.getAddon().getExtraSessionsPerWeek()).sum();
        return base + addons;
    }

    @Transactional(readOnly = true)
    public long reservedNormalInWeek(StudentEnrollment enrollment, LocalDate sessionDate) {
        LocalDate weekStart = sessionDate.with(DayOfWeek.MONDAY);
        return bookingRepository.countNormalReservedInWeek(
                enrollment.getStudentEnrollmentId(), weekStart, weekStart.plusDays(6));
    }

    @Transactional(readOnly = true)
    public long totalGranted(StudentEnrollment enrollment) {
        long addons = addonPurchaseRepository.findAll().stream()
                .filter(purchase -> purchase.getStudentEnrollment().getStudentEnrollmentId()
                        .equals(enrollment.getStudentEnrollmentId()))
                .mapToLong(purchase -> purchase.getGrantedSessionCount())
                .sum();
        return enrollment.getCoursePurchase().getTrainingPlan().getIncludedSessionCount() + addons;
    }

    @Transactional(readOnly = true)
    public long normalDebits(StudentEnrollment enrollment) {
        return bookingRepository.countNormalSettled(enrollment.getStudentEnrollmentId());
    }
}
