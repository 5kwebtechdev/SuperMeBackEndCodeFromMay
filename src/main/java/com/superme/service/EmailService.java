package com.superme.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendMail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public void sendHtmlMail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("HTML email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email. Please try again later.", e);
        }
    }

    // attachments: key = display filename shown in email, value = file
    public void sendHtmlMailWithAttachments(String to, String subject, String htmlContent,
                                            Map<String, MultipartFile> attachments) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            if (attachments != null) {
                for (Map.Entry<String, MultipartFile> entry : attachments.entrySet()) {
                    MultipartFile file = entry.getValue();
                    if (file != null && !file.isEmpty()) {
                        helper.addAttachment(entry.getKey(), new ByteArrayResource(file.getBytes()));
                    }
                }
            }

            mailSender.send(message);
            log.info("HTML email with {} attachment(s) sent to: {}",
                    attachments != null ? attachments.size() : 0, to);
        } catch (MessagingException | IOException e) {
            log.error("Failed to send HTML email with attachments to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email. Please try again later.", e);
        }
    }
}