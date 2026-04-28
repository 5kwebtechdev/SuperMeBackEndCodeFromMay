package com.superme.events;

import com.superme.model.Task;
import com.superme.model.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TaskAddedEvent extends ApplicationEvent {
    private final Task task;
    private final User user;

    public TaskAddedEvent(Object source, Task task, User user) {
        super(source);
        this.task = task;
        this.user = user;
    }
}