package com.superme.dto;

import com.superme.model.Task;
import com.superme.model.Habit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDayData {
    private List<Task> tasks;
    private List<Habit> habits;
    private LocalDate date;
    private int habitCount;
    private int taskCount;

    // public int getHabitCount() {
    //     return habits != null ? habits.size() : 0;
    // }

    // public void setHabitCount(int habitCount) {
    //     this.habitCount = habitCount;
    // }

    // public int getTaskCount() {
    //     return tasks != null ? tasks.size() : 0;
    // }

    // public void setTaskCount(int taskCount) {
    //     this.taskCount = taskCount;
    // }

    public int getHabitCount() {
        return habits != null ? habits.size() : 0;
    }

    public int getQuestCount() {
        return tasks != null ? tasks.size() : 0;
    }
}
