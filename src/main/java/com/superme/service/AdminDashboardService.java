package com.superme.service;

import com.superme.admin.dto.AdminDashboardKpiResponse;
import com.superme.admin.dto.RecentActivityDTO;
import com.superme.enums.Relationship;
import com.superme.model.Article;
import com.superme.model.Challenge;
import com.superme.model.Tutor;
import com.superme.model.User;
import com.superme.repository.ArticleRepository;
import com.superme.repository.ChallengeRepository;
import com.superme.repository.NoteRepository;
import com.superme.repository.TutorRepository;
import com.superme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final ChallengeRepository challengeRepository;
    private final NoteRepository noteRepository;
    private final TutorRepository tutorRepository;

    private static final DateTimeFormatter ACTIVITY_TS_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a, d MMM yyyy", Locale.ENGLISH);

    private String fmt(LocalDateTime dt) {
        return dt != null ? dt.format(ACTIVITY_TS_FORMAT) : null;
    }

    public AdminDashboardKpiResponse getKpiCards() {
        LocalDate today = LocalDate.now();

        long totalActiveUsers = userRepository.countActiveUsers();

        // Total non-deleted learning content (articles have no deleted field, so all are counted)
        long totalArticles = articleRepository.count();
        long totalChallenges = challengeRepository.countNonDeleted();
        long learningAdded = totalArticles + totalChallenges;

        long notesCreatedToday = noteRepository.countByCreatedDate(today);

        return AdminDashboardKpiResponse.builder()
                .totalActiveUsers(totalActiveUsers)
                .learningAdded(learningAdded)
                .notesCreatedToday(notesCreatedToday)
                .build();
    }

    public Map<String, Object> getRecentActivities(int page, int size) {
        // Fetch enough rows from each source to cover up to the requested page,
        // merge + sort in memory, then slice the correct page.
        int fetchCount = (page + 1) * size;

        Pageable recent      = PageRequest.of(0, fetchCount, Sort.by(Sort.Direction.DESC, "createdAt"));
        Pageable recentUsers = PageRequest.of(0, fetchCount, Sort.by(Sort.Direction.DESC, "createdDateTime"));

        List<ActivityEntry> entries = new ArrayList<>();

        for (Challenge c : challengeRepository.findRecentNonDeleted(recent)) {
            if (c.getCreatedAt() != null)
                entries.add(new ActivityEntry(c.getCreatedAt(), c.getName(), "Challenge Added"));
        }

        for (Article a : articleRepository.findAll(recent).getContent()) {
            if (a.getCreatedAt() != null)
                entries.add(new ActivityEntry(a.getCreatedAt(), a.getTitle(), "Article Added"));
        }

        for (User u : userRepository.findRecentByRelationship(Relationship.CHILD, recentUsers)) {
            if (u.getCreatedDateTime() != null)
                entries.add(new ActivityEntry(u.getCreatedDateTime(), u.getName(), "Child User Created"));
        }

        for (User u : userRepository.findRecentByRelationship(Relationship.PARENT, recentUsers)) {
            if (u.getCreatedDateTime() != null)
                entries.add(new ActivityEntry(u.getCreatedDateTime(), u.getName(), "Parent User Created"));
        }

        for (User u : userRepository.findRecentByRelationship(Relationship.SELF, recentUsers)) {
            if (u.getCreatedDateTime() != null)
                entries.add(new ActivityEntry(u.getCreatedDateTime(), u.getName(), "Individual User Created"));
        }

        for (Tutor t : tutorRepository.findAll(PageRequest.of(0, fetchCount, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent()) {
            if (t.getCreatedAt() != null)
                entries.add(new ActivityEntry(t.getCreatedAt(), t.getName(), "Tutor Added"));
        }

        // Total count across all sources (for pagination metadata)
        long total = challengeRepository.countNonDeleted()
                   + articleRepository.count()
                   + userRepository.countByRelationship(Relationship.CHILD)
                   + userRepository.countByRelationship(Relationship.PARENT)
                   + userRepository.countByRelationship(Relationship.SELF)
                   + tutorRepository.count();

        // Sort merged list, then slice the requested page
        List<RecentActivityDTO> sorted = entries.stream()
                .sorted(Comparator.comparing(ActivityEntry::rawTimestamp).reversed())
                .map(e -> RecentActivityDTO.builder()
                        .name(e.name())
                        .userType("Admin")
                        .activityType(e.activityType())
                        .timestamp(fmt(e.rawTimestamp()))
                        .build())
                .collect(Collectors.toList());

        int start = page * size;
        int end   = Math.min(start + size, sorted.size());
        List<RecentActivityDTO> content = start < sorted.size() ? sorted.subList(start, end) : List.of();

        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("size", size);
        pagination.put("total", total);
        pagination.put("totalPages", totalPages);
        pagination.put("hasNext", page < totalPages - 1);
        pagination.put("hasPrevious", page > 0);

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("pagination", pagination);
        return result;
    }

    private record ActivityEntry(LocalDateTime rawTimestamp, String name, String activityType) {}
}