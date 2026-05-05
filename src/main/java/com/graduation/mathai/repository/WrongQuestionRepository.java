package com.graduation.mathai.repository;

import com.graduation.mathai.model.WrongQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WrongQuestionRepository extends JpaRepository<WrongQuestion, Long> {
    List<WrongQuestion> findByUserIdOrderByCreatedAtDesc(long userId);

    Optional<WrongQuestion> findByIdAndUserId(long id, long userId);
}
