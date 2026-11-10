package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.dto.Training.CourseStaffAssignmentDTO;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseScheduleMapper;
import com.dat.ai_receptionist_web.dto.Training.ClassSessionDTO;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {PersonMapper.class, CourseScheduleMapper.class})
public interface CourseStaffAssignmentMapper {
    CourseStaffAssignmentDTO.Response toResponse(CourseStaffAssignment entity);

    CourseStaffAssignmentDTO.SimpleResponse toSimpleResponse(CourseStaffAssignment entity);

    CourseStaffAssignmentDTO.BriefResponse toBriefResponse(CourseStaffAssignment entity);

    default CourseStaffAssignmentDTO.SimpleResponse withLearningProgress(
            CourseStaffAssignmentDTO.SimpleResponse base,
            ClassSessionDTO.LearningProgress learningProgress
    ) {
        var schedule = base.courseSchedule();
        var enrichedSchedule = new com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO.SimpleResponse(
                schedule.courseScheduleId(), schedule.course(), schedule.classSchedule(), schedule.startDate(),
                schedule.endDate(), schedule.status(), learningProgress);
        return new CourseStaffAssignmentDTO.SimpleResponse(base.courseStaffAssignmentId(), base.staffPerson(),
                enrichedSchedule, base.assignmentType(), base.startDate(), base.endDate(), base.assignmentStatus());
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "staffPerson", ignore = true)
    @Mapping(target = "courseSchedule", ignore = true)
    @Mapping(target = "assignmentType", source = "assignmentType")
    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "endDate", source = "endDate")
    @Mapping(target = "assignmentStatus", source = "assignmentStatus")
    @Mapping(target = "note", source = "note")
    void updateEntity(CourseStaffAssignmentDTO.UpdateRequest request, @MappingTarget CourseStaffAssignment entity);
}
