package com.dat.ai_receptionist_web.domain.Training;

import com.dat.ai_receptionist_web.domain.Catalog.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLocation;
import com.dat.ai_receptionist_web.enums.Core.Weekday;
import com.dat.ai_receptionist_web.enums.Catalog.SessionType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ClassSessionScheduleSnapshot {
    @Column(name = "schedule_snapshot_id", nullable = false)
    private UUID scheduleId;

    @Column(name = "schedule_branch_id", nullable = false)
    private Long branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_weekday", nullable = false, length = 20)
    private Weekday weekday;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_level", nullable = false, length = 20)
    private ScheduleLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_location", nullable = false, length = 50)
    private ScheduleLocation location;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_session_type", nullable = false, length = 20)
    @Builder.Default
    private SessionType sessionType = SessionType.STANDARD;

    public static ClassSessionScheduleSnapshot from(Course course, ClassSchedule schedule) {
        return ClassSessionScheduleSnapshot.builder()
                .scheduleId(schedule.getScheduleId())
                .branchId(schedule.getBranch().getBranchId())
                .weekday(schedule.getWeekday())
                .level(course.getLevel())
                .location(schedule.getLocation())
                .build();
    }

    public static ClassSessionScheduleSnapshot from(CourseSchedule courseSchedule) {
        ClassSessionScheduleSnapshot snapshot = from(courseSchedule.getCourse(), courseSchedule.getClassSchedule());
        return ClassSessionScheduleSnapshot.builder()
                .scheduleId(snapshot.getScheduleId())
                .branchId(snapshot.getBranchId())
                .weekday(snapshot.getWeekday())
                .level(snapshot.getLevel())
                .location(snapshot.getLocation())
                .sessionType(courseSchedule.getSessionType())
                .build();
    }
}
