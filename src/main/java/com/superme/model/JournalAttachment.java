package com.superme.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class JournalAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filePath;   // e.g. /images/journal/abc.png
    private String fileType;

    @ManyToOne
    @JoinColumn(name = "journal_id")
    private JournalEntry journalEntry;
}