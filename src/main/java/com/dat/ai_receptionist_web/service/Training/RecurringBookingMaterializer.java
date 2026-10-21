package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.StudentSessionBooking;
import com.dat.ai_receptionist_web.enums.Training.BookingSource;
import com.dat.ai_receptionist_web.enums.Training.BookingStatus;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentScheduleRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentSessionBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;

/** Idempotently materializes future fixed schedules into attendance-authorizing bookings. */
@Service
@RequiredArgsConstructor
public class RecurringBookingMaterializer {
    private final StudentEnrollmentScheduleRepository scheduleRepository;
    private final StudentSessionBookingRepository bookingRepository;

    public void materialize(Collection<ClassSession> sessions) {
        for (ClassSession session : sessions) {
            for (var assignment : scheduleRepository.findEffectiveByCourseScheduleAndDate(
                    session.getCourseSchedule().getCourseScheduleId(), session.getSessionDate())) {
                if (bookingRepository.existsByEnrollment_StudentEnrollmentIdAndClassSession_ClassSessionId(
                        assignment.getStudentEnrollment().getStudentEnrollmentId(), session.getClassSessionId())) {
                    continue;
                }
                bookingRepository.save(StudentSessionBooking.builder()
                        .enrollment(assignment.getStudentEnrollment()).classSession(session)
                        .requirement(assignment.getRequirement()).source(BookingSource.RECURRING)
                        .status(BookingStatus.RESERVED).bookedAt(LocalDateTime.now()).build());
            }
        }
    }
}
