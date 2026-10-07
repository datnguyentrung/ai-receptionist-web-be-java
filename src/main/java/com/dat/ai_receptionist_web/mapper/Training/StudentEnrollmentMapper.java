package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Training.StudentEnrollmentDTO;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseScheduleMapper;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {PersonMapper.class})
public abstract class StudentEnrollmentMapper {
    @org.springframework.beans.factory.annotation.Autowired
    protected CourseScheduleMapper courseScheduleMapper;
    @Mapping(target = "coursePurchaseId", source = "coursePurchase.coursePurchaseId")
    @Mapping(target = "courseSchedules", expression = "java(entity.getSchedules().stream().map(link -> courseScheduleMapper.toResponse(link.getCourseSchedule())).toList())")
    public abstract StudentEnrollmentDTO.Response toResponse(StudentEnrollment entity);

    @Mapping(target = "coursePurchaseId", source = "coursePurchase.coursePurchaseId")
    @Mapping(target = "courseSchedules", expression = "java(entity.getSchedules().stream().map(link -> courseScheduleMapper.toSimpleResponse(link.getCourseSchedule())).toList())")
    public abstract StudentEnrollmentDTO.SimpleResponse toSimpleResponse(StudentEnrollment entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "studentPerson", ignore = true)
    @Mapping(target = "coursePurchase", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "endDate", source = "endDate")
    @Mapping(target = "status", source = "status")
    public abstract void updateEntity(StudentEnrollmentDTO.UpdateRequest request, @MappingTarget StudentEnrollment entity);
}
