package com.dat.ai_receptionist_web.mapper.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO;
import com.dat.ai_receptionist_web.dto.Training.CourseStaffAssignmentDTO;
import com.dat.ai_receptionist_web.dto.Training.ClassSessionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ClassScheduleMapper.class, CourseMapper.class})
public interface CourseScheduleMapper {
    @Mapping(target = "courseId", source = "entity.course.courseId")
    @Mapping(target = "staffAssignments", source = "staffAssignments")
    CourseScheduleDTO.Response toResponse(
            CourseSchedule entity,
            List<CourseStaffAssignmentDTO.BriefResponse> staffAssignments
    );

    default CourseScheduleDTO.Response toResponse(CourseSchedule entity) {
        return toResponse(entity, List.of());
    }

    @Mapping(target = "learningProgress", ignore = true)
    CourseScheduleDTO.SimpleResponse toSimpleResponse(CourseSchedule entity);

    default CourseScheduleDTO.SimpleResponse withLearningProgress(
            CourseScheduleDTO.SimpleResponse base,
            ClassSessionDTO.LearningProgress learningProgress
    ) {
        return new CourseScheduleDTO.SimpleResponse(base.courseScheduleId(), base.course(), base.classSchedule(),
                base.startDate(), base.endDate(), base.status(), learningProgress);
    }
}
