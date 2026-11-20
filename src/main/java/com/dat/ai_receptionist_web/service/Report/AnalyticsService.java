package com.dat.ai_receptionist_web.service.Report;

import com.dat.ai_receptionist_web.dto.Analytics.StudentMovementAnalyticsDTO;
import com.dat.ai_receptionist_web.enums.Core.Gender;
import com.dat.ai_receptionist_web.enums.Training.AgeGroup;
import com.dat.ai_receptionist_web.enums.Training.EventStatus;
import com.dat.ai_receptionist_web.enums.Training.EventType;
import com.dat.ai_receptionist_web.enums.Training.MovementType;
import com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus;
import com.dat.ai_receptionist_web.repository.Report.AnalyticsRepository;
import com.dat.ai_receptionist_web.repository.Report.AnalyticsRepository.MovementFact;

import java.time.LocalDate;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {
    private final AnalyticsRepository repository;

    public StudentMovementAnalyticsDTO.Response studentMovements(StudentMovementAnalyticsDTO.Request request) {
        YearMonth fromMonth = request.fromMonth() == null ? YearMonth.now().minusYears(1) : request.fromMonth();
        YearMonth toMonth = request.toMonth() == null ? YearMonth.now() : request.toMonth();
        if (fromMonth.isAfter(toMonth)) throw new IllegalArgumentException("fromMonth must not be after toMonth");
        LocalDate from = fromMonth.atDay(1);
        LocalDate to = toMonth.atEndOfMonth();
        List<MovementFact> facts = new ArrayList<>(repository.findStudentMovements(
                EventStatus.VALID,
                EventType.NEW,
                from,
                to
        ));
        facts.addAll(repository.findNewStudents(from, to));
        facts.removeIf(fact -> !matches(fact, request));
        List<StudentMovementAnalyticsDTO.YearlyMovement> years = new ArrayList<>();

        for (Year year = Year.of(fromMonth.getYear()); !year.isAfter(Year.of(toMonth.getYear())); year = year.plusYears(1)) {
            List<StudentMovementAnalyticsDTO.QuarterlyMovement> quarters = new ArrayList<>();
            for (int quarter = 1; quarter <= 4; quarter++) {
                List<StudentMovementAnalyticsDTO.MonthlyMovement> months = new ArrayList<>();
                YearMonth quarterStart = YearMonth.of(
                        year.getValue(),
                        (quarter - 1) * 3 + 1
                );
                for (int offset = 0; offset < 3; offset++) {
                    YearMonth month = quarterStart.plusMonths(offset);
                    if (month.isBefore(fromMonth) || month.isAfter(toMonth)) continue;
                    List<MovementFact> monthFacts = facts.stream().filter(f -> YearMonth.from(f.eventDate()).equals(month)).toList();
                    Map<AgeGroup, StudentMovementAnalyticsDTO.Movement> byAge = new EnumMap<>(AgeGroup.class);
                    Map<Gender, StudentMovementAnalyticsDTO.Movement> byGender = new EnumMap<>(Gender.class);
                    monthFacts.forEach(f -> {
                        add(
                                byAge,
                                ageGroup(
                                        f,
                                        month.atEndOfMonth()
                                ),
                                f
                        );
                        add(
                                byGender,
                                gender(f.gender()),
                                f
                        );
                    });
                    months.add(new StudentMovementAnalyticsDTO.MonthlyMovement(
                            month,
                            counts(monthFacts),
                            byAge,
                            byGender
                    ));
                }
                if (!months.isEmpty()) quarters.add(new StudentMovementAnalyticsDTO.QuarterlyMovement(
                        quarter,
                        sum(months.stream().map(StudentMovementAnalyticsDTO.MonthlyMovement::counts).toList()),
                        months
                ));
            }
            if (!quarters.isEmpty()) years.add(new StudentMovementAnalyticsDTO.YearlyMovement(
                    year,
                    sum(quarters.stream().map(StudentMovementAnalyticsDTO.QuarterlyMovement::counts).toList()),
                    quarters
            ));
        }
        return new StudentMovementAnalyticsDTO.Response(
                repository.countActiveStudents(to, StudentEnrollmentStatus.CANCELLED),
                repository.countStudents(
                        from,
                        to,
                        StudentEnrollmentStatus.CANCELLED
                ),
                years
        );
    }

    private Map<MovementType, Integer> counts(Collection<MovementFact> facts) {
        Map<MovementType, Integer> result = new EnumMap<>(MovementType.class);
        facts.forEach(f -> result.merge(
                type(f.eventType()),
                1,
                Integer::sum
        ));
        return result;
    }

    private <K> void add(
            Map<K, StudentMovementAnalyticsDTO.Movement> target,
            K key,
            MovementFact fact
    ) {
        Map<MovementType, Integer> merged = new EnumMap<>(MovementType.class);
        if (target.containsKey(key)) merged.putAll(target.get(key).counts());
        merged.merge(
                type(fact.eventType()),
                1,
                Integer::sum
        );
        target.put(
                key,
                new StudentMovementAnalyticsDTO.Movement(merged)
        );
    }

    private Map<MovementType, Integer> sum(Collection<Map<MovementType, Integer>> maps) {
        Map<MovementType, Integer> result = new EnumMap<>(MovementType.class);
        maps.forEach(m -> m.forEach((type, count) -> result.merge(
                type,
                count,
                Integer::sum
        )));
        return result;
    }

    private MovementType type(String value) {
        return MovementType.valueOf(value);
    }

    private AgeGroup ageGroup(
            MovementFact fact,
            LocalDate on
    ) {
        return fact.birthDate() == null ? AgeGroup.AGE_30_OR_MORE
                : AgeGroup.fromAge(Period.between(
                fact.birthDate(),
                on
        ).getYears());
    }

    private Gender gender(Boolean value) {
        return value == null ? Gender.OTHER : value ? Gender.MALE : Gender.FEMALE;
    }

    private boolean matches(MovementFact fact, StudentMovementAnalyticsDTO.Request request) {
        if (request.genders() != null && !request.genders().isEmpty()
                && !request.genders().contains(gender(fact.gender()).name())) return false;
        if (fact.birthDate() == null) return request.minAge() == null && request.maxAge() == null;
        int age = Period.between(fact.birthDate(), fact.eventDate()).getYears();
        return (request.minAge() == null || age >= request.minAge())
                && (request.maxAge() == null || age <= request.maxAge());
    }
}
