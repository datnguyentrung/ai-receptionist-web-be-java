package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.LeaveRequest;
import com.dat.ai_receptionist_web.dto.Training.LeaveRequestDTO;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import com.dat.ai_receptionist_web.mapper.Security.UserMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {PersonMapper.class, ClassSessionMapper.class, UserMapper.class})
public interface LeaveRequestMapper {
    LeaveRequestDTO.Response toResponse(LeaveRequest entity);

    LeaveRequestDTO.SimpleResponse toSimpleResponse(LeaveRequest entity);
}
