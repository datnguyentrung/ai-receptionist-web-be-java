package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.dto.Training.SessionAttendanceDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {ClassSessionMapper.class, StudentEnrollmentMapper.class, CourseStaffAssignmentMapper.class})
public interface SessionAttendanceMapper {
    @Mapping(target = "allowedActions", expression = "java(com.dat.ai_receptionist_web.dto.Training.SessionAttendanceDTO.AllowedActions.none())")
    SessionAttendanceDTO.Response toResponse(SessionAttendance entity);

    SessionAttendanceDTO.SimpleResponse toSimpleResponse(SessionAttendance entity);

    default SessionAttendanceDTO.Response toResponse(
            SessionAttendance entity,
            SessionAttendanceDTO.AllowedActions allowedActions
    ) {
        SessionAttendanceDTO.Response base = toResponse(entity);
        return new SessionAttendanceDTO.Response(
                base.sessionAttendanceId(),
                base.classSession(),
                base.studentEnrollment(),
                base.courseStaffAssignment(),
                base.checkInTime(),
                base.attendanceStatus(),
                base.evaluationStatus(),
                base.note(),
                allowedActions,
                base.createdAt(),
                base.updatedAt()
        );
    }

    default SessionAttendanceDTO.AllowedActions toAllowedActions(boolean update, boolean delete) {
        return new SessionAttendanceDTO.AllowedActions(update, delete);
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "checkInTime", source = "checkInTime")
    @Mapping(target = "attendanceStatus", source = "attendanceStatus")
    @Mapping(target = "evaluationStatus", source = "evaluationStatus")
    @Mapping(target = "note", source = "note")
    void updateEntity(SessionAttendanceDTO.UpdateRequest request, @MappingTarget SessionAttendance entity);
}
