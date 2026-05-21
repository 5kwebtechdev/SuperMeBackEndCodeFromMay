package com.superme.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorRegistrationService {

    private static final String ADMIN_EMAIL = "mohitj5kwt@gmail.com";

    private final EmailService emailService;

    public void sendTutorRegistrationEmail(Map<String, String> formParams,
                                           MultipartFile profilePic,
                                           List<MultipartFile> documents) {
        // Build named attachments so admin can clearly identify each file
        Map<String, MultipartFile> attachments = new LinkedHashMap<>();
        if (profilePic != null && !profilePic.isEmpty()) {
            attachments.put("Profile_Picture" + ext(profilePic), profilePic);
        }
        if (documents != null) {
            int i = 1;
            for (MultipartFile doc : documents) {
                if (doc != null && !doc.isEmpty()) {
                    attachments.put("Document_" + i + ext(doc), doc);
                    i++;
                }
            }
        }

        String subject = "New Tutor Registration - "
                + tp(formParams, "title") + " " + tp(formParams, "entity_name")
                + " [" + tp(formParams, "entity_type") + "] - "
                + tp(formParams, "selected_category");

        String html = buildEmailHtml(formParams, !attachments.isEmpty());
        emailService.sendHtmlMailWithAttachments(ADMIN_EMAIL, subject, html, attachments);
        log.info("Tutor registration email sent for: {} ({})",
                tp(formParams, "entity_name"), tp(formParams, "email"));
    }

    // ── Email HTML builder ────────────────────────────────────────────────────

    private String buildEmailHtml(Map<String, String> f, boolean hasAttachments) {
        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
          .append("<style>")
          .append("body{font-family:Arial,sans-serif;background:#f4f6f9;margin:0;padding:20px;}")
          .append(".container{max-width:700px;margin:0 auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);}")
          .append(".header{background:#4f46e5;padding:24px 32px;color:#fff;}")
          .append(".header h1{margin:0;font-size:22px;}")
          .append(".header p{margin:6px 0 0;opacity:0.85;font-size:14px;}")
          .append(".badge{display:inline-block;background:rgba(255,255,255,0.2);border-radius:20px;padding:4px 14px;font-size:13px;margin-top:8px;}")
          .append(".section{padding:20px 32px;border-bottom:1px solid #f0f0f0;}")
          .append(".section:last-child{border-bottom:none;}")
          .append(".section-title{font-size:13px;font-weight:700;color:#4f46e5;text-transform:uppercase;letter-spacing:0.5px;margin:0 0 14px;}")
          .append(".grid{display:grid;grid-template-columns:1fr 1fr;gap:12px;}")
          .append(".field label{display:block;font-size:11px;color:#888;margin-bottom:3px;text-transform:uppercase;letter-spacing:0.4px;}")
          .append(".field span{display:block;font-size:14px;color:#222;font-weight:500;}")
          .append(".tag-list{background:#f8f8ff;border:1px solid #e8e8f0;border-radius:6px;padding:10px 14px;font-size:13px;color:#333;line-height:1.7;}")
          .append(".attachment-note{background:#f0f4ff;border:1px solid #c7d7ff;border-radius:6px;padding:12px 16px;font-size:13px;color:#3730a3;}")
          .append(".footer{background:#f8f8ff;padding:16px 32px;font-size:12px;color:#888;text-align:center;}")
          .append("</style></head><body>")
          .append("<div class='container'>");

        // Header
        sb.append("<div class='header'>")
          .append("<h1>New Tutor Registration Request</h1>")
          .append("<p>Submitted at ").append(safe(f, "time")).append("</p>")
          .append("<span class='badge'>").append(safe(f, "entity_type")).append("</span>")
          .append("</div>");

        // Attachment note
        if (hasAttachments) {
            sb.append("<div class='section'>")
              .append("<div class='attachment-note'>")
              .append("&#128206; Profile picture and documents are attached to this email.")
              .append("</div></div>");
        }

        // Tutor profile
        sb.append("<div class='section'>")
          .append("<p class='section-title'>Tutor Profile</p>")
          .append("<div class='grid'>")
          .append(field("Full Name", tp(f, "title") + " " + tp(f, "entity_name")))
          .append(field("Gender", tp(f, "gender")))
          .append(field("Date of Birth", tp(f, "date_of_birth")))
          .append(field("Age", hasVal(f, "calculated_age") ? tp(f, "calculated_age") + " years" : "-"))
          .append(field("Qualification", tp(f, "qualification")))
          .append(field("Achievement / Award", tp(f, "achievement")))
          .append("</div></div>");

        // Contact
        sb.append("<div class='section'>")
          .append("<p class='section-title'>Contact Information</p>")
          .append("<div class='grid'>")
          .append(field("Email", tp(f, "email")))
          .append(field("Phone", tp(f, "phone")))
          .append(field("Address", tp(f, "address")))
          .append(field("City", tp(f, "city")))
          .append(field("State", tp(f, "state")))
          .append(field("Pincode", tp(f, "pincode")))
          .append("</div></div>");

        // Teaching category + category-specific fields
        sb.append("<div class='section'>")
          .append("<p class='section-title'>Teaching Category</p>")
          .append("<div class='grid'>")
          .append(field("Category", tp(f, "selected_category")))
          .append("</div>");
        appendCategoryFields(sb, f);
        sb.append("</div>");

        // Teaching details
        sb.append("<div class='section'>")
          .append("<p class='section-title'>Teaching Details</p>")
          .append("<div class='grid'>")
          .append(field("Teaching Mode", tp(f, "selected_teach_mode")))
          .append(field("Experience", tp(f, "selected_experience")))
          .append(field("Fee Type", tp(f, "selected_fee_type")))
          .append(field("Available Days", tp(f, "selected_days")))
          .append(field("Start Time", tp(f, "start_time")))
          .append(field("End Time", tp(f, "end_time")))
          .append("</div>");
        if (hasVal(f, "selected_spoken_langs")) {
            sb.append("<div style='margin-top:14px;'><div class='field'><label>Spoken Languages</label>")
              .append("<div class='tag-list'>").append(tp(f, "selected_spoken_langs")).append("</div></div></div>");
        }
        sb.append("</div>");

        sb.append("<div class='footer'>")
          .append("This email was automatically generated by the SuperMe tutor registration system.")
          .append("</div></div></body></html>");

        return sb.toString();
    }

    private void appendCategoryFields(StringBuilder sb, Map<String, String> f) {
        switch (tp(f, "selected_category")) {
            case "School":
                tagBlock(sb, "Boards",   tp(f, "selected_boards"));
                tagBlock(sb, "Classes",  tp(f, "selected_classes"));
                tagBlock(sb, "Subjects", tp(f, "selected_school_subjects"));
                break;
            case "College":
                tagBlock(sb, "Degrees",  tp(f, "selected_degree"));
                tagBlock(sb, "Years",    tp(f, "selected_year"));
                break;
            case "Exams":
                tagBlock(sb, "Exams Covered", tp(f, "selected_exams"));
                break;
            case "Hobbies":
                tagBlock(sb, "Skills",    tp(f, "selected_hobbies_skills"));
                tagBlock(sb, "Level",     tp(f, "selected_hobbies_level"));
                tagBlock(sb, "Age Group", tp(f, "selected_hobbies_age"));
                break;
            case "Languages":
                tagBlock(sb, "Languages", tp(f, "selected_lang_list"));
                tagBlock(sb, "Level",     tp(f, "selected_lang_level"));
                break;
            case "Sports":
                tagBlock(sb, "Sports / Activities", tp(f, "selected_sports_acts"));
                tagBlock(sb, "Age Group",            tp(f, "selected_sports_age"));
                break;
            case "Others":
                tagBlock(sb, "Skills", tp(f, "selected_others_skills"));
                tagBlock(sb, "Level",  tp(f, "selected_others_level"));
                break;
            default:
                break;
        }
    }

    private void tagBlock(StringBuilder sb, String label, String value) {
        if (value == null || value.isBlank()) return;
        sb.append("<div style='margin-top:14px;'><div class='field'><label>").append(label).append("</label>")
          .append("<div class='tag-list'>").append(value).append("</div></div></div>");
    }

    private String field(String label, String value) {
        return "<div class='field'><label>" + label + "</label><span>"
                + (value != null && !value.isBlank() ? value : "-") + "</span></div>";
    }

    private String ext(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) return name.substring(name.lastIndexOf("."));
        return "";
    }

    private String tp(Map<String, String> map, String key) {
        String val = map.get("template_params[" + key + "]");
        return val != null ? val : "";
    }

    private boolean hasVal(Map<String, String> map, String key) {
        return !tp(map, key).isBlank();
    }

    private String safe(Map<String, String> map, String key) {
        String val = tp(map, key);
        return val.isBlank() ? "-" : val;
    }
}
