package com.superme.service;

import com.superme.model.Challenge;
import com.superme.model.ChallengeOption;
import com.superme.model.ChallengeAttachment;
import com.superme.repository.ChallengeOptionRepository;
import com.superme.repository.ChallengeAttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChallengeAssetService {

    private final ChallengeOptionRepository optionRepo;
    private final ChallengeAttachmentRepository attachmentRepo;

    public ChallengeAssetService(ChallengeOptionRepository optionRepo,
                                 ChallengeAttachmentRepository attachmentRepo) {
        this.optionRepo = optionRepo;
        this.attachmentRepo = attachmentRepo;
    }

    @Transactional
    public void saveOrUpdateOptions(Challenge challenge, List<ChallengeOption> options) {
        if (options != null && !options.isEmpty()) {
            // Remove old options
            optionRepo.deleteAll(optionRepo.findByChallengeId(challenge.getId()));

            // Set challenge reference and save new options
            options.forEach(opt -> opt.setChallenge(challenge));
            optionRepo.saveAll(options);
        }
    }

    @Transactional
    public void saveOrUpdateAttachments(Challenge challenge, List<ChallengeAttachment> attachments) {
        if (attachments != null && !attachments.isEmpty()) {
            // Remove old attachments
            attachmentRepo.deleteAll(attachmentRepo.findByChallengeId(challenge.getId()));

            // Set challenge reference and save new attachments
            attachments.forEach(att -> att.setChallenge(challenge));
            attachmentRepo.saveAll(attachments);
        }
    }
}
