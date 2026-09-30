package com.dat.ai_receptionist_web.specification;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Core.Position;
import com.dat.ai_receptionist_web.dto.Core.PersonListFilter;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PersonSpecification {
    private static final String STUDENT_POSITION_CODE = "STUDENT";

    private PersonSpecification() {
    }

    public static Specification<Person> matching(PersonListFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Person, Position> position = root.join("position", JoinType.LEFT);

            if (filter.positionId() != null) {
                predicates.add(cb.equal(position.get("positionId"), filter.positionId()));
            }
            if (StringUtils.hasText(filter.search())) {
                String pattern = "%" + filter.search().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName").as(String.class)), pattern),
                        cb.like(cb.lower(root.get("personCode").as(String.class)), pattern)
                ));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.currentBelt() != null) {
                predicates.add(cb.equal(root.get("currentBelt"), filter.currentBelt()));
            }
            if (filter.gender() != null) {
                predicates.add(cb.equal(root.get("gender"), filter.gender()));
            }
            if (filter.isStudent() != null) {
                Predicate studentPosition = cb.equal(
                        cb.upper(position.get("code").as(String.class)),
                        STUDENT_POSITION_CODE);
                predicates.add(filter.isStudent()
                        ? studentPosition
                        : cb.or(cb.isNull(position.get("positionId")), cb.not(studentPosition)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
