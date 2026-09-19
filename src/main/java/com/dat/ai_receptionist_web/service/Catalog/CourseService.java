package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseMapper;
import com.dat.ai_receptionist_web.repository.Catalog.ClassScheduleRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.service.Training.scheduling.CourseSessionPlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository repository;
    private final CourseMapper mapper;
    private final ClassScheduleRepository classScheduleRepository;
    private final CourseSessionPlanningService planningService;

    @Transactional(readOnly = true)
    public PageResponse<CourseDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAllDetailed(pageable), mapper::toSimpleResponse);
    }

    @Transactional(readOnly = true)
    public CourseDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public CourseDTO.Response create(CourseDTO.CreateRequest request) {
        Course entity = new Course();
        entity.setClassSchedule(classScheduleRepository.findById(request.classScheduleId())
                .orElseThrow(() -> new ApiException(CatalogErrorCode.CLASS_SCHEDULE_NOT_FOUND)));
        entity.setCapacity(request.capacity());
        entity.setStatus(request.status());
        entity.setName(request.name());
        Course saved = repository.save(entity);
        if (saved.getStatus() == CourseStatus.ACTIVE) {
            planningService.maintainGenerationHorizon();
        }
        return mapper.toResponse(saved);
    }

    @Transactional
    public CourseDTO.Response update(UUID id, CourseDTO.UpdateRequest request) {
        Course entity = find(id);
        entity.setName(request.name());
        entity.setCapacity(request.capacity());
        entity.setStatus(request.status());
        Course saved = repository.save(entity);
        if (saved.getStatus() == CourseStatus.ACTIVE) {
            planningService.maintainGenerationHorizon();
        }
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Course entity = find(id);
        entity.setStatus(CourseStatus.CANCELLED);
    }

    @Transactional
    public CourseDTO.CourseScheduleChangeResponse changeSchedule(
            UUID id, CourseDTO.ScheduleChangeRequest request) {
        return planningService.changeSchedule(id, request.classScheduleId(), request.effectiveFrom());
    }

    @Transactional
    public void cancelPendingScheduleChange(UUID id) {
        planningService.cancelPendingScheduleChange(id);
    }

    private Course find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_NOT_FOUND));
    }
}
