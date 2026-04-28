package com.superme.events;

import com.superme.model.Task;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TaskCompletedEvent extends ApplicationEvent {
    private final Task task;

    public TaskCompletedEvent(Object source, Task task) {
        super(source);
        this.task = task;
    }
}