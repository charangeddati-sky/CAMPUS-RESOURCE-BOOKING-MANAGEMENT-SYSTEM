package com.college.booking.repository;

import com.college.booking.entity.Examination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExaminationRepository extends JpaRepository<Examination, Long> {
    List<Examination> findAllByOrderByExamDateDesc();
}
