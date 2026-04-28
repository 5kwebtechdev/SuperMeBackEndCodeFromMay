//package com.superme.dto;
//
//import com.superme.model.HabitCompletion;
//import com.superme.service.HabitService;
//import lombok.Data;
//import java.time.LocalDate;
//import java.time.LocalTime;
//
//@Data
//public class HabitCompletionResponse {
//    private Long id;
//    private HabitResponse habit;
//    private LocalDate completionDate;
//    private LocalTime completionTime;
//    private int coinsEarned;
//    private boolean completed;
//    private String status;
//
//    // You'll need to inject HabitService or pass it as parameter
//    private final HabitService habitService;
//
//    public HabitCompletionResponse(HabitService habitService) {
//        this.habitService = habitService;
//    }
//
//    // Constructor from HabitCompletion entity
//    public HabitCompletionResponse(HabitCompletion completion, HabitService habitService) {
//        this.habitService = habitService;
//        this.id = completion.getId();
//
//        // Use the service method instead of direct constructor
//        this.habit = habitService.convertToHabitResponseWithMetrics(completion.getHabit());
//
//        this.completionDate = completion.getCompletionDate();
//        this.completionTime = completion.getCompletionTime();
//        this.coinsEarned = completion.getCoinsEarned() != null ? completion.getCoinsEarned() : 0;
//        this.completed = Boolean.TRUE.equals(completion.getCompleted());
//        this.status = completion.getStatus() != null ? completion.getStatus().name() : "PENDING";
//    }
//
//    public HabitCompletionResponse() {
//        this.habitService = null; // Or handle dependency injection properly
//    }
//}