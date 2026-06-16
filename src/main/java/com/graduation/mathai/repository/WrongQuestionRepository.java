package com.graduation.mathai.repository;

import com.graduation.mathai.model.WrongQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WrongQuestionRepository extends JpaRepository<WrongQuestion, Long> {
    List<WrongQuestion> findAllByOrderByCreatedAtDesc();

    List<WrongQuestion> findBySubjectOrderByCreatedAtDesc(String subject);

    Optional<WrongQuestion> findById(long id);
}
