package com.collabit.portfolio.repository;

import com.collabit.portfolio.domain.entity.Feedback;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {

}
