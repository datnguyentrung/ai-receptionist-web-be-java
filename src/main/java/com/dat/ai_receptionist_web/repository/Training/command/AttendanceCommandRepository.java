package com.dat.ai_receptionist_web.repository.Training.command;

import com.dat.ai_receptionist_web.domain.Training.command.AttendanceCommand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AttendanceCommandRepository extends JpaRepository<AttendanceCommand, UUID> {
}
