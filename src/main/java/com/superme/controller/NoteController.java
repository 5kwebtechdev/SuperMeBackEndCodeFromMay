package com.superme.controller;

import com.superme.dto.NoteResponse;
import com.superme.exception.BusinessException;
import com.superme.exception.InternalServerErrorException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.exception.UnauthorizedException;
import com.superme.model.Note;
import com.superme.model.User;
import com.superme.service.NoteService;
import com.superme.service.UserService;
import com.superme.util.UserJwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/notes")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class NoteController {

    @Autowired
    private NoteService noteService;

    @Autowired
    private UserService userService;

    // DTO for note requests (no userId, no sharedWithParents)
    public static class NoteRequest {
        public String title;
//        public Long userId;
        public String content;
        public List<String> tags;
    }

    // Convert Note to NoteResponse DTO
    private NoteResponse toNoteResponse(Note note) {
        return NoteResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .tags(note.getTags())
                .createdDate(note.getCreatedDate())
                .createdTime(note.getCreatedTime())
                .updatedDate(note.getUpdatedDate())
                .updatedTime(note.getUpdatedTime())
                .userId(note.getUser() != null ? note.getUser().getId() : null)
                .build();
    }

    // Helper to extract userId from principal (handles Long or String)
    // Helper to extract userId from principal using the 'sub' claim (always a
    // String)
    private Long extractUserId(Principal principal) {
        try {
            // principal.getName() returns the 'sub' claim as String
            return Long.parseLong(principal.getName());
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid user principal");
        }
    }

    /**
     * Create a new note for the authenticated user.
     */
    @PostMapping("/add")
    public ResponseEntity<NoteResponse> createNote(  @RequestHeader("Authorization") String token,@RequestBody NoteRequest req) {

        try {
            Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            System.out.println(user.getName()+" is creating a note with title: " + req.title);
            Note saved = noteService.addNote(user, req.title, req.content, req.tags);
            return ResponseEntity.status(HttpStatus.CREATED).body(toNoteResponse(saved));
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to create note");
        }
    }

    /**
     * Update an existing note for the authenticated user.
     */
    @PutMapping("/update/{noteId}")
    public ResponseEntity<NoteResponse> updateNote(  @PathVariable Long noteId,
            @RequestBody NoteRequest req,@RequestHeader("Authorization") String token) {
        try {

            Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Note updated = noteService.updateNoteForUser(noteId, user, req.title, req.content, req.tags);
            if (updated == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(toNoteResponse(updated));
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to update note");
        }
    }

    /**
     * Delete a note for the authenticated user.
     */
    @DeleteMapping("/delete/{noteId}")
    public ResponseEntity<Void> deleteNote(@RequestHeader("Authorization") String token, @PathVariable Long noteId) {
        try {
            Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean deleted = noteService.deleteNoteForUser(noteId, user);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to delete note");
        }
    }

    /**
     * Get all notes created by the authenticated user.
     */
//    @GetMapping("/all")
//    public ResponseEntity<List<NoteResponse>> getMyNotes(@RequestParam Long userId) {
//        try {
//             List<Note> notes = noteService.getNotesByUserId(userId);
//            if (notes == null || notes.isEmpty()) {
//                throw new ResourceNotFoundException("No notes found for user");
//            }
//            List<NoteResponse> response = notes.stream().map(this::toNoteResponse).toList();
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//           throw new InternalServerErrorException("Failed to retrieve notes");
//        }
//    }




    @GetMapping("/all")
    public ResponseEntity<List<NoteResponse>> getMyNotes(
                                                         @RequestHeader("Authorization") String token
                                                         ) {

        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
//        User user = userService.getUserById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        try {
            List<Note> notes = noteService.getNotesByUserId(userId);

            List<NoteResponse> response = notes.stream()
                    .map(this::toNoteResponse)
                    .toList();

            return ResponseEntity.ok(response); // ✅ always return list

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to retrieve notes");
        }
    }





    /**
     * Get notes by tag for the authenticated user.
     */
    @GetMapping("/my/tag/{tag}")
    public ResponseEntity<List<NoteResponse>> getMyNotesByTag(
            @PathVariable String tag,
            @RequestParam Long userId) {

        List<Note> notes = noteService.getNotesByUserIdAndTag(userId, tag);

        if (notes == null || notes.isEmpty()) {
            throw new ResourceNotFoundException("No notes found for user with the specified tag");
        }

        List<NoteResponse> response = notes.stream()
                .map(this::toNoteResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Search notes by query (title/content/tags) for the authenticated user.
     */
    @GetMapping("/my/search")
    public ResponseEntity<List<NoteResponse>> searchMyNotes( @RequestParam String query,
                                                             @RequestParam Long userId) {
            List<Note> notes = noteService.searchNotesByUser(userId, query);
            if (notes == null || notes.isEmpty()) {
                throw new ResourceNotFoundException("No notes found matching the search query");
            }
            List<NoteResponse> response = notes.stream().map(this::toNoteResponse).toList();
            return ResponseEntity.ok(response);
    }
}