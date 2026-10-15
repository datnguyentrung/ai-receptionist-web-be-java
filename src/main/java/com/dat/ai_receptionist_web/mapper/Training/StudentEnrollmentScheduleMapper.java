package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentEnrollmentSchedule;
import com.dat.ai_receptionist_web.dto.Training.StudentEnrollmentScheduleDTO;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseScheduleMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CourseScheduleMapper.class)
public interface StudentEnrollmentScheduleMapper {
    @Mapping(target = "studentEnrollmentId", source = "studentEnrollment.studentEnrollmentId")
    StudentEnrollmentScheduleDTO.Response toResponse(StudentEnrollmentSchedule entity);

    @Mapping(target = "studentEnrollmentId", source = "studentEnrollment.studentEnrollmentId")
    StudentEnrollmentScheduleDTO.SimpleResponse toSimpleResponse(StudentEnrollmentSchedule entity);
}
