package com.superme.service;

import com.superme.dto.MoodTransitionDto;
import com.superme.enums.Relationship;
import com.superme.exception.BusinessException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.*;
import com.superme.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;


@Service
public class MoodService {

    @Autowired
    private MoodTransitionRepository moodTransitionRepository;

    private final FamilyMemberRepository familyMemberRepository;

    public MoodService(MoodTransitionRepository moodTransitionRepository, FamilyMemberRepository familyMemberRepository, MoodRepository moodRepository, UserRepository userRepository, CalendarEventRepository calendarEventRepository) {
        this.moodTransitionRepository = moodTransitionRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.moodRepository = moodRepository;
        this.userRepository = userRepository;
        this.calendarEventRepository = calendarEventRepository;
    }

    /**
     * Returns the Mood for a user and date, or null if not found.
     */
    public Mood getMoodForUserByDate(Long userId, java.time.LocalDate date, java.time.LocalTime time) {
        return getMood(userId, date, time).orElse(null);
    }

    @Autowired
    private MoodRepository moodRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    public Mood setMood(Long userId, java.time.LocalDate date, java.time.LocalTime time, String value) {
        User user = userRepository.findById(userId).orElseThrow();
        // Optionally, find or create a calendar event for this mood
        CalendarEvent moodEvent = calendarEventRepository.findByCreatedByIdAndTypeAndStartDateBetween(
                userId, "MOOD", null, null).stream().findFirst().orElse(null); // Update as needed for
        // LocalDate/LocalTime

        Mood mood = new Mood();

        mood.setUser(user);
        mood.setValue(value);
        mood.setDate(date);
        mood.setTime(time);
        mood.setCreatedDate(LocalDate.now());
        mood.setCreatedTime(LocalTime.now());
        mood.setCalendarEvent(moodEvent);

        return moodRepository.save(mood);
    }

    public Optional<Mood> getMood(Long userId, java.time.LocalDate date, java.time.LocalTime time) {
        User user = userRepository.findById(userId).orElseThrow();
        return moodRepository.findByUserAndDateAndTime(user, date, time);
    }

    public int countMoodsByUserAndDate(Long userId, LocalDate today) {
        return moodRepository.countByUserIdAndDate(userId, today);
    }

    public List<MoodTransitionDto> getTransition(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isParent = user.getRelationship() == Relationship.PARENT;
        LocalDate today = LocalDate.now();

        // 🔹 Determine target users (children if parent, else self)
        List<User> targetUsers;
        if (isParent) {
            List<FamilyMember> familyMembers = familyMemberRepository.findByFamily(user.getFamily());
            targetUsers = familyMembers.stream()
                    .filter(fm -> fm.getUser().getRelationship() == Relationship.CHILD)
                    .map(FamilyMember::getUser)
                    .toList();
        } else {
            targetUsers = List.of(user);
        }

        List<MoodTransitionDto> transitions = new ArrayList<>();

        for (User targetUser : targetUsers) {
            List<Mood> moodsToday = moodRepository.findByUserIdAndDateOrderByTimeAsc(targetUser.getId(), today);

            if (moodsToday.isEmpty()) {
                continue;
            }

            String fromMood, toMood;
            if (moodsToday.size() == 1) {
                fromMood = moodsToday.get(0).getValue();
                toMood = fromMood;
            } else {
                fromMood = moodsToday.get(0).getValue();
                toMood = moodsToday.get(1).getValue();
            }

            MoodTransition transition = moodTransitionRepository.findTransition(
                    fromMood.isBlank() ? null : fromMood,
                    toMood.isBlank() ? null : toMood,
                    isParent
            );

            if (transition != null) {
                transitions.add(MoodTransitionDto.builder()
                        .userId(targetUser.getId())
                        .userName(targetUser.getName())
                        .date(today)
                        .fromMood(transition.getMoodFrom())
                        .toMood(transition.getMoodTo())
                        .parentVersion(isParent)
                        .title(transition.getTitle())
                        .firstLine(transition.getFirstLine())
                        .secondLine(transition.getSecondLine())
                        .build());
            }
        }
        return transitions;
    }

    public List<MoodTransitionDto> getMonthlyTransitions(
            User user,        // 👈 keep User object (from authenticated context)
            Long forUserId,   // 👈 nullable child filter
            int year,
            int month) {

        // 1️⃣ Define date range for month
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 2️⃣ Determine target user for mood lookup
        User targetUser;

        if (user.getRelationship() == Relationship.PARENT && forUserId != null) {
            // parent + specific child
            targetUser = userRepository.findById(forUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Child not found with id: " + forUserId));

            if (!targetUser.getFamily().equals(user.getFamily())
                    || targetUser.getRelationship() != Relationship.CHILD) {
                throw new IllegalArgumentException("forUserId must be a child of the parent");
            }
        } else if (user.getRelationship() == Relationship.PARENT) {
            // parent + no forUserId → use parent themselves (or throw if you want all children)
            targetUser = user;
        } else {
            // child/self → use themselves
            targetUser = user;
        }

        boolean isParent = user.getRelationship() == Relationship.PARENT;

        // 3️⃣ Fetch all moods for target user that month
        List<Mood> moods = moodRepository.findByUserAndDateBetweenOrderByDateAscTimeAsc(
                targetUser, startDate, endDate);

        // 4️⃣ Group moods by date (preserve order)
        Map<LocalDate, List<Mood>> moodsByDate = moods.stream()
                .collect(Collectors.groupingBy(Mood::getDate, LinkedHashMap::new, Collectors.toList()));

        // 5️⃣ Prepare result
        List<MoodTransitionDto> results = new ArrayList<>();

        for (Map.Entry<LocalDate, List<Mood>> entry : moodsByDate.entrySet()) {
            List<Mood> dailyMoods = entry.getValue();

            if (dailyMoods.isEmpty()) continue;

            String fromMood;
            String toMood;

            if (dailyMoods.size() == 1) {
                fromMood = dailyMoods.get(0).getValue();
                toMood = fromMood;
            } else {
                fromMood = dailyMoods.get(0).getValue();
                toMood = dailyMoods.get(dailyMoods.size() - 1).getValue();
            }

            // 6️⃣ Fetch transition from table
            MoodTransition transition = moodTransitionRepository.findTransition(
                    fromMood.isBlank() ? null : fromMood,
                    toMood.isBlank() ? null : toMood,
                    isParent
            );

            // 7️⃣ Build DTO
            MoodTransitionDto dto = MoodTransitionDto.builder()
                    .userId(targetUser.getId())      // 👈 use targetUser
                    .userName(targetUser.getName())  // 👈 use targetUser
                    .date(entry.getKey())
                    .fromMood(fromMood)
                    .toMood(toMood)
                    .title(transition != null ? transition.getTitle() : "No Title")
                    .firstLine(transition != null ? transition.getFirstLine() : "No First Line")
                    .secondLine(transition != null ? transition.getSecondLine() : "No Second Line")
                    .parentVersion(isParent)
                    .build();

            results.add(dto);
        }

        return results;
    }



}
