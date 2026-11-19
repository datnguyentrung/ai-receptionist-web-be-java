package com.dat.ai_receptionist_web.domain.Skill;

import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import jakarta.persistence.*;
import lombok.*;

/**
 * Mẫu bài tập thể lực theo cấp độ, số lượng và thời lượng thực hiện.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fitness", schema = "skill")
public class Fitness {
    /** Mã định danh duy nhất của bài tập thể lực. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fitness_id")
    private Long fitnessId;

    /** Cấp độ áp dụng của bài tập thể lực. */
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_level", nullable = false, length = 20)
    private ScheduleLevel scheduleLevel;

    /** Số lượng bài tập hoặc động tác cần thực hiện. */
    @Column(name = "amount", nullable = false)
    private int amount;

    /** Thời lượng thực hiện bài tập. */
    @Column(name = "duration", nullable = false)
    private int duration;
}
