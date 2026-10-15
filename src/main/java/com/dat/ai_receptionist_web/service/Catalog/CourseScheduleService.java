package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseScheduleMapper;
import com.dat.ai_receptionist_web.repository.Catalog.ClassScheduleRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseScheduleService {
    private final CourseScheduleRepository repository;
    private final CourseRepository courseRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final CourseScheduleMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<CourseScheduleDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAllDetailed(pageable), mapper::toSimpleResponse);
    }

    @Transactional(readOnly = true)
    public CourseScheduleDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public CourseScheduleDTO.Response create(CourseScheduleDTO.CreateRequest request) {
        CourseSchedule schedule = new CourseSchedule();
        schedule.setCourse(courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_NOT_FOUND)));
        apply(schedule, request.classScheduleId(), request.startDate(), request.endDate(), request.status());
        return mapper.toResponse(repository.save(schedule));
    }

    @Transactional
    public CourseScheduleDTO.Response update(UUID id, CourseScheduleDTO.UpdateRequest request) {
        CourseSchedule schedule = find(id);
        apply(schedule, request.classScheduleId(), request.startDate(), request.endDate(), request.status());
        return mapper.toResponse(repository.save(schedule));
    }

    @Transactional
    public void delete(UUID id) {
        find(id).setStatus(ScheduleStatus.INACTIVE);
    }

    private CourseSchedule find(UUID id) {
        return repository.findDetailedById(id)
                .orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_SCHEDULE_NOT_FOUND));
    }

    private void apply(CourseSchedule schedule, UUID classScheduleId,
                       java.time.LocalDate startDate, java.time.LocalDate endDate, ScheduleStatus status) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new ApiException(CatalogErrorCode.COURSE_SCHEDULE_CHANGE_CONFLICT,
                    "Schedule end date must not precede start date");
        }
        schedule.setClassSchedule(classScheduleRepository.findById(classScheduleId)
                .orElseThrow(() -> new ApiException(CatalogErrorCode.CLASS_SCHEDULE_NOT_FOUND)));
        schedule.setStartDate(startDate);
        schedule.setEndDate(endDate);
        schedule.setStatus(status);
    }
}
