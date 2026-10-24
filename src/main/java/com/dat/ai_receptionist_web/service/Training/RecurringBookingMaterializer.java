package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollmentSchedule;
import com.dat.ai_receptionist_web.domain.Training.StudentSessionBooking;
import com.dat.ai_receptionist_web.enums.Training.BookingSource;
import com.dat.ai_receptionist_web.enums.Training.BookingStatus;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentScheduleRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentSessionBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Idempotently materializes future fixed schedules into attendance-authorizing bookings. */
@Service
@RequiredArgsConstructor
public class RecurringBookingMaterializer {
    private final StudentEnrollmentScheduleRepository scheduleRepository;
    private final StudentSessionBookingRepository bookingRepository;

    public void materialize(Collection<ClassSession> sessions) {
        if (sessions.isEmpty()) {
            return;
        }

        List<ClassSession> sessionList = List.copyOf(sessions);
        LocalDate fromDate = sessionList.stream().map(ClassSession::getSessionDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate untilDate = sessionList.stream().map(ClassSession::getSessionDate).max(LocalDate::compareTo).orElseThrow();
        var courseScheduleIds = sessionList.stream()
                .map(session -> session.getCourseSchedule().getCourseScheduleId())
                .collect(Collectors.toSet());
        Map<UUID, List<StudentEnrollmentSchedule>> assignmentsByCourseSchedule = scheduleRepository
                .findPotentiallyEffectiveByCourseScheduleIdsAndDateRange(courseScheduleIds, fromDate, untilDate)
                .stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getCourseSchedule().getCourseScheduleId(),
                        HashMap::new,
                        Collectors.toList()));
        Set<EnrollmentSessionKey> existingBookings = bookingRepository
                .findEnrollmentSessionPairsByClassSessionIds(sessionList.stream()
                        .map(ClassSession::getClassSessionId).toList())
                .stream()
                .map(pair -> new EnrollmentSessionKey(pair.getEnrollmentId(), pair.getClassSessionId()))
                .collect(Collectors.toCollection(HashSet::new));
        List<StudentSessionBooking> bookings = new java.util.ArrayList<>();
        LocalDateTime bookedAt = LocalDateTime.now();

        for (ClassSession session : sessionList) {
            for (StudentEnrollmentSchedule assignment : assignmentsByCourseSchedule
                    .getOrDefault(session.getCourseSchedule().getCourseScheduleId(), List.of())) {
                if (!isEffectiveOn(assignment, session.getSessionDate())) {
                    continue;
                }
                EnrollmentSessionKey key = new EnrollmentSessionKey(
                        assignment.getStudentEnrollment().getStudentEnrollmentId(), session.getClassSessionId());
                if (existingBookings.add(key)) {
                    bookings.add(StudentSessionBooking.builder()
                            .enrollment(assignment.getStudentEnrollment()).classSession(session)
                            .requirement(assignment.getRequirement()).source(BookingSource.RECURRING)
                            .status(BookingStatus.RESERVED).bookedAt(bookedAt).build());
                }
            }
        }
        bookingRepository.saveAll(bookings);
    }

    private boolean isEffectiveOn(StudentEnrollmentSchedule assignment, LocalDate date) {
        return assignment.getEffectiveFrom() != null
                && !assignment.getEffectiveFrom().isAfter(date)
                && (assignment.getEffectiveTo() == null || !assignment.getEffectiveTo().isBefore(date))
                && !assignment.getStudentEnrollment().getStartDate().isAfter(date)
                && !assignment.getStudentEnrollment().getEndDate().isBefore(date);
    }

    private record EnrollmentSessionKey(UUID enrollmentId, UUID classSessionId) { }
}
