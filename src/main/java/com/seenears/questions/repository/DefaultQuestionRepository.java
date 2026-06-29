package com.seenears.questions.repository;

import com.seenears.global.domain.MoodType;
import com.seenears.questions.domain.DefaultQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DefaultQuestionRepository extends JpaRepository<DefaultQuestion, Long> {

    Optional<DefaultQuestion> findFirstByMoodTypeAndActiveTrueOrderByDisplayOrderAscIdAsc(MoodType moodType);
}
