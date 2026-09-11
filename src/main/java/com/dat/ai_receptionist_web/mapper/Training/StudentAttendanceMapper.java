package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentAttendance;
import com.dat.ai_receptionist_web.dto.Training.StudentAttendanceDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StudentAttendanceMapper {
    @Mapping(target = "classSessionId", source = "classSession.classSessionId")
    @Mapping(target = "studentEnrollmentId", source = "studentEnrollment.studentEnrollmentId")
    @Mapping(target = "courseStaffAssignmentId", source = "courseStaffAssignment.courseStaffAssignmentId")
    @Mapping(target = "allowedActions", expression = "java(com.dat.ai_receptionist_web.dto.Training.StudentAttendanceDTO.AllowedActions.none())")
    StudentAttendanceDTO.Response toResponse(StudentAttendance entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "checkInTime", source = "checkInTime")
    @Mapping(target = "attendanceStatus", source = "attendanceStatus")
    @Mapping(target = "evaluationStatus", source = "evaluationStatus")
    @Mapping(target = "note", source = "note")
    void updateEntity(StudentAttendanceDTO.UpdateRequest request, @MappingTarget StudentAttendance entity);
}
