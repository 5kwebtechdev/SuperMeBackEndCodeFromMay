package com.superme.controller;


import com.superme.dto.TaskRequest;
import com.superme.dto.TaskResponse;
import com.superme.dto.TaskResponseTodayDto;
import com.superme.dto.TaskSearchResponse;
import com.superme.exception.BusinessException;
import com.superme.exception.InternalServerErrorException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.exception.UnauthorizedActionException;
import com.superme.model.FamilyMember;
import com.superme.model.Task;
import com.superme.model.TaskCompletion;
import com.superme.model.User;
import com.superme.service.FamilyMemberService;
import com.superme.service.TaskService;
import com.superme.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/tasks")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @Autowired
    private FamilyMemberService familyMemberService;

    @GetMapping("/sort/status")
    public ResponseEntity<List<TaskRequest>> getTasksSortedByStatus(Principal principal) {
        User user = getUserFromPrincipal(principal);
        List<Task> tasks = taskService.getAllTasksForUser(user);
        if (tasks == null || tasks.isEmpty()) {
            throw new ResourceNotFoundException("No tasks found for user");
        }
        tasks = tasks.stream()
                .sorted((t1, t2) -> t1.getStatus().compareTo(t2.getStatus()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(tasks.stream().map(taskService::toTaskRequest).collect(Collectors.toList()));
    }

    @GetMapping("/sort/priority")
    public ResponseEntity<List<TaskRequest>> getTasksSortedByPriority(Principal principal) {
        User user = getUserFromPrincipal(principal);
        List<Task> tasks = taskService.getAllTasksForUser(user);
        if (tasks == null || tasks.isEmpty()) {
            throw new ResourceNotFoundException("No tasks found for user");
        }
        tasks = tasks.stream()
                .sorted((t1, t2) -> t1.getPriority().compareTo(t2.getPriority()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(tasks.stream().map(taskService::toTaskRequest).collect(Collectors.toList()));
    }

    @GetMapping("/sort/routine")
    public ResponseEntity<List<TaskRequest>> getTasksSortedByRoutine(Principal principal) {
        User user = getUserFromPrincipal(principal);
        List<Task> tasks = taskService.getTasksForUserByDay(user, LocalDate.now());
        if (tasks == null || tasks.isEmpty()) {
            throw new ResourceNotFoundException("No tasks found for today");
        }
        tasks = tasks.stream()
                .sorted((t1, t2) -> t1.getRoutine().compareTo(t2.getRoutine()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(tasks.stream().map(taskService::toTaskRequest).collect(Collectors.toList()));
    }

    /**
     * GET /tasks/sorted?filterBy=status|priority|routine&direction=asc|desc
     * Returns all tasks for the user sorted by the specified filter.
     */
    @GetMapping("/sorted")
    public ResponseEntity<List<TaskRequest>> getSortedTasksForUser(
            Principal principal,
            @RequestParam(name = "filterBy", required = false, defaultValue = "priority") String filterBy,
            @RequestParam(name = "direction", required = false, defaultValue = "asc") String direction) {
        User user = getUserFromPrincipal(principal);
        List<Task> tasks = taskService.getAllTasksForUser(user);
        if (tasks == null || tasks.isEmpty()) {
            throw new ResourceNotFoundException("No tasks found for user");
        }

        // Sort tasks based on filterBy and direction
        tasks = tasks.stream()
                .sorted((t1, t2) -> {
                    int cmp = 0;
                    switch (filterBy.toLowerCase()) {
                        case "status":
                            cmp = t1.getStatus().compareTo(t2.getStatus());
                            break;
                        case "priority":
                            cmp = t1.getPriority().compareTo(t2.getPriority());
                            break;
                        case "routine":
                            cmp = t1.getRoutine().compareTo(t2.getRoutine());
                            break;
                        default:
                            cmp = t1.getPriority().compareTo(t2.getPriority());
                    }
                    return "desc".equalsIgnoreCase(direction) ? -cmp : cmp;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(tasks.stream().map(taskService::toTaskRequest).collect(Collectors.toList()));
    }

    @PostMapping("/add")
    public ResponseEntity<TaskResponse> addTask(Principal principal,
                                                @RequestBody TaskRequest request) {

        User user = getUserFromPrincipal(principal);

        // CLEAN ARCHITECTURE: Let service handle DTO → Entity conversion
        Task task = taskService.fromTaskRequest(request);

        // Service handles business logic and saving - FIXED: Use request.getDaysOfWeek()
        Task savedTask = taskService.addTask(task, user, request);

        // CLEAN ARCHITECTURE: Let service handle Entity → DTO conversion
        TaskResponse response = taskService.toTaskResponse(savedTask);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(Principal principal,
                                                   @PathVariable Long taskId,
                                                   @RequestBody TaskRequest request) {
        User user = getUserFromPrincipal(principal);

        request.setTaskId(taskId);
        // FIX: Use the conversion method instead of manually setting fields
        Task taskUpdates = taskService.fromTaskRequest(request);

        // Set daysOfWeek based on toggles or from request
        if (request.isEveryday()) {
            taskUpdates.setDaysOfWeek(java.util.EnumSet.allOf(java.time.DayOfWeek.class));
        } else if (request.isEveryWeekend()) {
            taskUpdates.setDaysOfWeek(java.util.EnumSet.of(java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY));
        } else if (request.getDaysOfWeek() != null && !request.getDaysOfWeek().isEmpty()) {
            taskUpdates.setDaysOfWeek(request.getDaysOfWeek());
        }

        // Rest of your existing code remains the same...
        if (user.getFamily() == null) {
            java.util.Optional<Task> updated = taskService.updateTask(taskId, taskUpdates, user);
            return updated.map(taskService::toTaskResponse)
                    .map(ResponseEntity::ok)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Task not found or you do not have permission to update this task."));
        }

        FamilyMember member = familyMemberService.getByUser(user)
                .orElseThrow(() -> new UnauthorizedActionException("User is not a family member."));

        java.util.Optional<Task> updated;
        if ("parent".equals(member.getRelationship()) && request.getForUserId() != null) {
            User child = userService.getUserById(request.getForUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Child user not found"));
            FamilyMember childMember = familyMemberService.getByUser(child)
                    .orElseThrow(() -> new UnauthorizedActionException(
                            "Target user is not a family member."));
            if (!"child".equals(childMember.getRelationship()) ||
                    child.getFamily() == null || user.getFamily() == null ||
                    !child.getFamily().equals(user.getFamily())) {
                throw new UnauthorizedActionException("Can only update tasks for your own child.");
            }
            updated = taskService.updateTask(taskId, taskUpdates, child);
        } else {
            updated = taskService.updateTask(taskId, taskUpdates, user);
        }

        return updated.map(taskService::toTaskResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task not found or you do not have permission to update this task."));
    }

    @DeleteMapping("/delete/{taskId}")
    public ResponseEntity<String> deleteTask(Principal principal,
                                             @PathVariable Long taskId) {
        User user = getUserFromPrincipal(principal);

        boolean deleted = taskService.deleteTask(taskId, user);
        if (deleted) {
            return ResponseEntity.ok("Task deleted successfully.");
        } else {
            throw new ResourceNotFoundException(
                    "Task not found or you do not have permission to delete this task.");
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<TaskResponse>> getAllTasksForUser(Principal principal) {
        User user = getUserFromPrincipal(principal);
        List<Task> tasks = taskService.getAllTasksForUser(user);

        // CLEAN ARCHITECTURE: Use service method for conversion
        List<TaskResponse> taskResponses = tasks.stream()
                .map(taskService::toTaskResponse)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(taskResponses);
    }

    @GetMapping("/day")
    public ResponseEntity<List<TaskResponse>> getTasksForUserByDay(
            Principal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        User user = getUserFromPrincipal(principal);
        List<Task> tasks = taskService.getTasksForUserByDay(user, date);

        // CLEAN ARCHITECTURE: Use service method for conversion
        List<TaskResponse> taskResponses = tasks.stream()
                .map(taskService::toTaskResponse)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(taskResponses);
    }

    // ✅ ADD: New search endpoint
    @GetMapping("/search")
    public ResponseEntity<TaskSearchResponse> searchTasks(
            Principal principal,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Task.TaskStatus status,
            @RequestParam(required = false) Task.Priority priority,
            @RequestParam(required = false) String routine,
            @RequestParam(required = false) Boolean everyday,
            @RequestParam(required = false) Boolean everyWeekend,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAt, // ✅

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate updatedAt, // ✅

            @RequestParam(required = false) Integer rewardCoins,
            @RequestParam(required = false) Set<String> tags,
            @RequestParam(required = false) Set<DayOfWeek> daysOfWeek,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir) {
        // Get user from principal for security
        User user = getUserFromPrincipal(principal);

        List<Task> tasks = taskService.searchTasks(
                user,
                title, description, status, priority, routine,
                everyday, everyWeekend, startDate, endDate,
                createdAt, updatedAt, rewardCoins, tags, daysOfWeek, // ✅ Parameters match service method
                sortBy, sortDir);

        if (tasks == null || tasks.isEmpty()) {
            throw new ResourceNotFoundException("No tasks found matching the search criteria");
        }
        long totalTasks = tasks.size();
        long completedTasks = tasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.COMPLETED)
                .count();

        double completionRate = totalTasks > 0 ? (completedTasks * 100.0) / totalTasks : 0.0;



        List<TaskResponse> taskResponseList =  tasks.stream().map(taskService::toTaskResponse).toList();

        return ResponseEntity.ok(new TaskSearchResponse(taskResponseList, totalTasks, completedTasks, completionRate));
    }

    private User getUserFromPrincipal(Principal principal) {
        String principalName = principal.getName();
        try {
            Long userId = Long.parseLong(principalName);
            return userService.getUserById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        } catch (NumberFormatException e) {
            return userService.getUserByEmail(principalName)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
    }

    @GetMapping("/family/{familyId}")
    public ResponseEntity<List<TaskRequest>> getTasksByFamily(@PathVariable Long familyId) {
        List<Task> tasks = taskService.getTasksByFamily(familyId);
        return ResponseEntity.ok(tasks.stream().map(taskService::toTaskRequest).collect(Collectors.toList()));
    }

    @GetMapping("/debug")
    public ResponseEntity<String> debugTasks() {
        return ResponseEntity.ok("Debug endpoint for tasks - functionality can be implemented as needed");
    }

    @PostMapping("/complete/{taskId}")
    public ResponseEntity<?> completeTask(Principal principal,
                                          @PathVariable Long taskId) {
        User user = getUserFromPrincipal(principal);

        try {
            // ✅ FIXED: Use TaskCompletion instead of Task
            Optional<TaskCompletion> completionOpt = taskService.completeTask(taskId, user);
            if (completionOpt.isPresent()) {
                TaskCompletion completion = completionOpt.get();
                return ResponseEntity.ok(java.util.Map.of(
                        "message", "Task completed successfully for today!",
                        "coinsEarned", completion.getCoinsEarned(),
                        "taskId", taskId,
                        "completionDate", completion.getCompletionDate(),
                        "completedAt", java.time.LocalDateTime.now()));
            } else {
                throw new BusinessException("Task not found or not scheduled for today");
            }
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to complete task");
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TaskRequest>> getTasksByStatus(
            Principal principal,
            @PathVariable String status) {
        User user = getUserFromPrincipal(principal);

        Task.TaskStatus taskStatus;
        try {
            taskStatus = Task.TaskStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Invalid status. Valid values are: PENDING, IN_PROGRESS, COMPLETED");
        }

        List<Task> tasks = taskService.getTodayTasksByStatus(user, taskStatus);
        if (tasks == null || tasks.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No tasks found with status: " + status);
        }

        return ResponseEntity.ok(tasks.stream().map(taskService::toTaskRequest).collect(Collectors.toList()));
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<TaskRequest>> getTasksByPriority(
            Principal principal,
            @PathVariable String priority) {
        User user = getUserFromPrincipal(principal);

        Task.Priority taskPriority;
        try {
            taskPriority = Task.Priority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Invalid priority. Valid values are: LOW, MEDIUM, HIGH");
        }

        List<Task> tasks = taskService.getTodayTasksByPriority(user, taskPriority);
        if (tasks == null || tasks.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No tasks found with priority: " + priority);
        }

        return ResponseEntity.ok(tasks.stream().map(taskService::toTaskRequest).collect(Collectors.toList()));
    }

    @GetMapping("/routine/{routine}")
    public ResponseEntity<List<TaskRequest>> getTasksByRoutine(
            Principal principal,
            @PathVariable String routine) {
        User user = getUserFromPrincipal(principal);

        String taskRoutine;
        try {
            taskRoutine = routine.toUpperCase();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Invalid routine. Valid values are: MORNING, AFTERNOON, EVENING, NIGHT");
        }

        List<Task> tasks = taskService.getTodayTasksByRoutine(user, taskRoutine);
        if (tasks == null || tasks.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No tasks found with routine: " + routine);
        }

        return ResponseEntity.ok(tasks.stream().map(taskService::toTaskRequest).collect(Collectors.toList()));
    }





    @GetMapping("/task-by-id")
    public ResponseEntity<TaskResponseTodayDto> getTaskById(@RequestParam Long taskId,
                                                            @RequestParam LocalDate date) {
            TaskResponseTodayDto response = taskService.getTaskResponseByIdAndDate(taskId,date);
            return ResponseEntity.ok(response);
    }









    // 🔹 Get tasks for a month
    @GetMapping("/month")
    public ResponseEntity<List<TaskResponse>> getTasksForMonth(
            @RequestParam int year,
            @RequestParam int month) {

        List<TaskResponse> response = taskService.getTasksForMonth(year, month);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{taskId}/month")
    public ResponseEntity<?> getTaskDetailsForMonth(
            Principal principal,
            @PathVariable Long taskId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        User user = getUserFromPrincipal(principal);
        return ResponseEntity.ok(
                taskService.getTaskDetailsForMonth(taskId, year, month,user)
        );
    }



    // ✅ ADD: Get today's completions
    @GetMapping("/tasks-by-date")
    public ResponseEntity<List<TaskResponseTodayDto>> getTodayTask(Principal principal,
                                                                   @RequestParam(required = false) LocalDate date,
                                                                   @RequestParam(required = false, defaultValue = "DAY") String viewType,
                                                                   @RequestParam(required = false) Long forUserId,
                                                                   @RequestParam(required = false) String status,
                                                                   @RequestParam(required = false) String routine,
                                                                   @RequestParam(required = false) String priority) {
        User user = getUserFromPrincipal(principal);
        List<TaskResponseTodayDto> completions = taskService.getTaskByDate(user,forUserId,date,viewType,status,routine,priority);
        return ResponseEntity.ok(completions);
    }

    @PostMapping("/lets-go/{taskId}")
    public ResponseEntity<?> startTask(Principal principal, @PathVariable Long taskId) {
        User user = getUserFromPrincipal(principal);
        return taskService.startTaskForToday(user, taskId);
    }

    @PostMapping("/abort/{taskId}")
    public ResponseEntity<?> abortTask(Principal principal, @PathVariable Long taskId) {
        User user = getUserFromPrincipal(principal);
        return taskService.abortTaskForToday(user, taskId);
    }

    // ✅ ADD: Get completion history for a task
    @GetMapping("/{taskId}/completions")
    public ResponseEntity<List<TaskCompletion>> getTaskCompletionHistory(
            Principal principal,
            @PathVariable Long taskId) {
        User user = getUserFromPrincipal(principal);
        List<TaskCompletion> completions = taskService.getTaskCompletionHistory(taskId);
        return ResponseEntity.ok(completions);
    }

}