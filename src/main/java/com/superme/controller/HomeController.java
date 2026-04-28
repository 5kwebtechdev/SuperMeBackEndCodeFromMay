
// package com.mindfull.controller;

// import com.mindfull.dto.*;
// import com.mindfull.model.User;
// import com.mindfull.service.*;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import java.security.Principal;

// @RestController
// @RequestMapping("/home")
// public class HomeController {

// @Autowired
// private UserService userService;
// @Autowired
// private HabitService habitService;
// @Autowired
// private TaskService taskService;
// @Autowired
// private ChallengeService challengeService;
// @Autowired
// private ExperimentService experimentService;
// @Autowired
// private ReelService reelService;
// @Autowired
// private GuideService guideService;
// @Autowired
// private JournalService journalService;
// @Autowired
// private CourseService courseService;

// @GetMapping("/dashboard")
// public HomeScreenResponse getHomeScreen(Principal principal) {
// User user = userService.getUserFromPrincipal(principal);

// HomeScreenResponse response = new HomeScreenResponse();
// // User stats
// response.setCoins(user.getCoins());
// response.setBadges(user.getBadges() != null ? user.getBadges().size() : 0);
// response.setStreaks(user.getStreaks());
// response.setTrophies(user.getTrophies());

// // Day summary
// response.setHabits(habitService.getHabitsForToday(user));
// response.setTasks(taskService.getTasksForToday(user));
// response.setChallenges(challengeService.getChallengesForToday(user));

// // Featured content
// response.setFeaturedExperiments(experimentService.getFeaturedExperiments());
// response.setFeaturedReels(reelService.getFeaturedReels());
// response.setGuides(guideService.getGuidesForUser(user));

// // Journal
// response.setTodayJournal(journalService.getTodayJournal(user));

// // Ongoing courses/quests
// response.setOngoingCourses(courseService.getOngoingCourses(user));

// return response;
// }
// }
