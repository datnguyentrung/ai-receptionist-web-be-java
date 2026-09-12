package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.dto.Training.SessionAttendanceDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SessionAttendanceMapper {
    @Mapping(target = "classSessionId", source = "classSession.classSessionId")
    @Mapping(target = "studentEnrollmentId", source = "studentEnrollment.studentEnrollmentId")
    @Mapping(target = "courseStaffAssignmentId", source = "courseStaffAssignment.courseStaffAssignmentId")
    @Mapping(target = "allowedActions", expression = "java(com.dat.ai_receptionist_web.dto.Training.SessionAttendanceDTO.AllowedActions.none())")
    SessionAttendanceDTO.Response toResponse(SessionAttendance entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "checkInTime", source = "checkInTime")
    @Mapping(target = "attendanceStatus", source = "attendanceStatus")
    @Mapping(target = "evaluationStatus", source = "evaluationStatus")
    @Mapping(target = "note", source = "note")
    void updateEntity(SessionAttendanceDTO.UpdateRequest request, @MappingTarget SessionAttendance entity);
}
