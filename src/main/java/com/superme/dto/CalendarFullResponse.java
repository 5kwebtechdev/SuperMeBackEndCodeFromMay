package com.superme.dto;

import com.superme.model.CalendarEvent;
import com.superme.model.Habit;
import com.superme.model.Task;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarFullResponse {

    // ✅ DTO, not entity
    private List<CalendarEventResponseDTO> events;

    private List<TaskDTO> tasks;
    private List<HabitDTO> habits;

    // Statistics
    private Integer totalTasks;
    private Integer totalTasksCompleted;
    private int taskCompletionRate;
    private Integer totalHabits;
    private Integer totalHabitsCompleted;
    private int habitCompletionRate;

    // Backward-compatible constructor
    public CalendarFullResponse(
            List<CalendarEventResponseDTO> events,
            List<TaskDTO> tasks,
            List<HabitDTO> habits
    ) {
        this.events = events;
        this.tasks = tasks;
        this.habits = habits;

        this.totalTasks = tasks != null ? tasks.size() : 0;
        this.totalTasksCompleted = calculateCompletedTasks(tasks);
        this.taskCompletionRate = calculateTaskCompletionRate();

        this.totalHabits = habits != null ? habits.size() : 0;
        this.totalHabitsCompleted = calculateCompletedHabits(habits);
        this.habitCompletionRate = calculateHabitCompletionRate();
    }

    private Integer calculateCompletedTasks(List<TaskDTO> tasks) {
        if (tasks == null) return 0;
        return (int) tasks.stream()
                .filter(task -> "COMPLETED".equalsIgnoreCase(task.getStatus()))
                .count();
    }

    private Integer calculateCompletedHabits(List<HabitDTO> habits) {
        if (habits == null) return 0;
        return (int) habits.stream()
                .filter(habit -> "COMPLETED".equalsIgnoreCase(habit.getStatus()))
                .count();
    }

    private int calculateTaskCompletionRate() {
        if (totalTasks <= 0) return 0;

        return (int) ((totalTasksCompleted * 100L) / totalTasks);
    }


    private int calculateHabitCompletionRate() {
        if (totalHabits <= 0) return 0;

        return (int) ((totalHabitsCompleted * 100L) / totalHabits);
    }
}
