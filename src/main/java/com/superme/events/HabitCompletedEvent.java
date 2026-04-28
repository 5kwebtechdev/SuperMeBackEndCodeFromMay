package com.superme.events;

import com.superme.model.Habit;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class HabitCompletedEvent extends ApplicationEvent {
    private final Habit habit;

    public HabitCompletedEvent(Object source, Habit habit) {
        super(source);
        this.habit = habit;
    }
}