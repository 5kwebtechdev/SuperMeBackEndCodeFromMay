package com.superme.controller;

import com.superme.dto.JournalEntryRequest;
import com.superme.dto.JournalEntryResponseDTO;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.User;
import com.superme.service.JournalEntryService;
import com.superme.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/journal")      
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE, RequestMethod.OPTIONS
})
public class JournalEntryController {

    @Autowired
    private JournalEntryService journalEntryService;

    @Autowired
    private UserService userService;

    // POST /add
//    @PostMapping("/add")
//    public ResponseEntity<JournalEntryResponseDTO> addJournalEntry(
//            @RequestBody JournalEntryRequest request,
//            Principal principal) {
//
//        User user = getUserFromPrincipal(principal);
//        JournalEntryResponseDTO createdEntry = journalEntryService.createJournalEntry(request, user);
//        return ResponseEntity.status(HttpStatus.CREATED).body(createdEntry);
//    }

//    @PostMapping("/add")
//    public ResponseEntity<JournalEntryResponseDTO> addJournalEntry(
//            @ModelAttribute JournalEntryRequest request,
//            Principal principal) {
//
//        User user = getUserFromPrincipal(principal);
//        JournalEntryResponseDTO createdEntry =
//                journalEntryService.createJournalEntry(request, user);
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(createdEntry);
//    }
@PostMapping("/add")
public ResponseEntity<JournalEntryResponseDTO> addJournalEntry(
        @ModelAttribute JournalEntryRequest request,
        @RequestParam(value = "files", required = false) List<MultipartFile> files,
        Principal principal) {

    User user = getUserFromPrincipal(principal);

    // ✅ manually set files into DTO
    request.setFiles(files);

    JournalEntryResponseDTO createdEntry =
            journalEntryService.createJournalEntry(request, user);

    return ResponseEntity.status(HttpStatus.CREATED).body(createdEntry);
}

//    // PUT /update/{entryId}
//    @PutMapping("/update/{entryId}")
//    public ResponseEntity<JournalEntryResponseDTO> updateJournalEntry(
//            @PathVariable Long entryId,
//            @RequestBody JournalEntryRequest request,
//            Principal principal) {
//
//        User user = getUserFromPrincipal(principal);
//        return journalEntryService.updateJournalEntry(entryId, request, user)
//                .map(ResponseEntity::ok)
//                .orElseThrow(() -> new ResourceNotFoundException("Journal entry not found"));
//    }


//    @PutMapping("/update/{entryId}")
//    public ResponseEntity<JournalEntryResponseDTO> updateJournalEntry(
//            @PathVariable Long entryId,
//            @ModelAttribute JournalEntryRequest request,
//            Principal principal) {
//
//        User user = getUserFromPrincipal(principal);
//
//        return journalEntryService.updateJournalEntry(entryId, request, user)
//                .map(ResponseEntity::ok)
//                .orElseThrow(() -> new ResourceNotFoundException("Journal entry not found"));
//    }
        @PutMapping("/update/{entryId}")
        public ResponseEntity<JournalEntryResponseDTO> updateJournalEntry(
                @PathVariable Long entryId,
                @ModelAttribute JournalEntryRequest request,
                @RequestParam(value = "files", required = false) List<MultipartFile> files,
                Principal principal) {

            User user = getUserFromPrincipal(principal);

            request.setFiles(files);

            return journalEntryService.updateJournalEntry(entryId, request, user)
                    .map(ResponseEntity::ok)
                    .orElseThrow(() -> new ResourceNotFoundException("Journal entry not found"));
        }
    // DELETE /delete/{entryId}
    @DeleteMapping("/delete/{entryId}")
    public ResponseEntity<Void> deleteJournalEntry(
            @PathVariable Long entryId,
            Principal principal) {

        User user = getUserFromPrincipal(principal);
        boolean deleted = journalEntryService.deleteJournalEntry(entryId, user);

        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            throw new ResourceNotFoundException("Journal entry not found");
        }
    }

    // GET /entries (all entries for current user)
    @GetMapping("/entries")
    public ResponseEntity<List<JournalEntryResponseDTO>> getJournalEntriesForUser(
            @RequestParam(defaultValue = "TODAY") String type,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Principal principal) {

        User user = getUserFromPrincipal(principal);

        List<JournalEntryResponseDTO> entries =
                journalEntryService.getJournalEntries(user, type, month, year);

        if (entries.isEmpty()) {
            return ResponseEntity.noContent().build(); // ✅ 204 response
        }

        return ResponseEntity.ok(entries);
    }


    // GET /entries/{entryId} (single entry)
    @GetMapping("/entries/{entryId}")
    public ResponseEntity<JournalEntryResponseDTO> getJournalEntryById(
            @PathVariable Long entryId,
            Principal principal) {

        User user = getUserFromPrincipal(principal);
        return journalEntryService.getJournalEntryById(entryId, user)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry not found"));
    }

    // Helper method to get user from principal
    private User getUserFromPrincipal(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return userService.getUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Value("${file.upload-dir}")
    private String uploadDir;
    @GetMapping("/download/image/{fileName}")
    public ResponseEntity<byte[]> downloadImage(@PathVariable String fileName) {

        try {
            Path path = Paths.get(uploadDir + "/journal/" + fileName);

            if (!Files.exists(path)) {
                throw new RuntimeException("File not found: " + fileName);
            }

            byte[] fileBytes = Files.readAllBytes(path);

            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Content-Disposition", "inline; filename=\"" + fileName + "\"")
                    .body(fileBytes);

        } catch (Exception e) {
            throw new RuntimeException("Error while downloading file", e);
        }
    }








}