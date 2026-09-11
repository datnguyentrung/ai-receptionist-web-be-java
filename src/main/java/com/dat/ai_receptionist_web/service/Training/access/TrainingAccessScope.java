package com.dat.ai_receptionist_web.service.Training.access;

public record TrainingAccessScope(
        boolean unrestricted,
        boolean self,
        boolean dependents,
        boolean assignedCourses,
        boolean managedCourses
) {
    public static TrainingAccessScope deny() {
        return new TrainingAccessScope(false, false, false, false, false);
    }

    public boolean denied() {
        return !unrestricted && !self && !dependents && !assignedCourses && !managedCourses;
    }
}