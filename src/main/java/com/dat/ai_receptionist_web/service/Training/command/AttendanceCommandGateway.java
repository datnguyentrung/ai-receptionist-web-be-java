package com.dat.ai_receptionist_web.service.Training.command;

import com.dat.ai_receptionist_web.dto.Training.command.FaceCheckInCommandMessage;
import com.dat.ai_receptionist_web.dto.Training.command.SessionAttendanceCommandMessage;

public interface AttendanceCommandGateway {
    void enqueueFaceCheckIn(FaceCheckInCommandMessage message);

    void enqueueSessionAttendance(SessionAttendanceCommandMessage message);
}
