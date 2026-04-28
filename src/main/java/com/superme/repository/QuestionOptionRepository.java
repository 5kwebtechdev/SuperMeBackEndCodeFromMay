package com.superme.repository;

import com.superme.model.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    List<QuestionOption> findByQuestionId(Long questionId);

    List<QuestionOption> findByQuestionIdOrderByOptionOrder(Long questionId);

    void deleteByQuestionId(Long questionId);
}