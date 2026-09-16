package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Training.StudentEnrollmentDTO;
import com.dat.ai_receptionist_web.mapper.Catalog.ClassScheduleMapper;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {PersonMapper.class, ClassScheduleMapper.class})
public interface StudentEnrollmentMapper {
    @Mapping(target = "coursePurchaseId", source = "coursePurchase.coursePurchaseId")
    StudentEnrollmentDTO.Response toResponse(StudentEnrollment entity);

    @Mapping(target = "coursePurchaseId", source = "coursePurchase.coursePurchaseId")
    StudentEnrollmentDTO.SimpleResponse toSimpleResponse(StudentEnrollment entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "studentPerson", ignore = true)
    @Mapping(target = "coursePurchase", ignore = true)
    @Mapping(target = "classSchedule", ignore = true)
    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "endDate", source = "endDate")
    @Mapping(target = "status", source = "status")
    void updateEntity(StudentEnrollmentDTO.UpdateRequest request, @MappingTarget StudentEnrollment entity);
}
