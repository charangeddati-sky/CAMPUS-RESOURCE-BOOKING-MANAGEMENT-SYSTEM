package com.college.booking.repository;

import com.college.booking.entity.Issue;
import com.college.booking.enums.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByResourceIdOrderByCreatedAtDesc(Long resourceId);
    List<Issue> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
    List<Issue> findByStatus(IssueStatus status);
    long countByStatusIn(Collection<IssueStatus> statuses);
    long countByResourceId(Long resourceId);
    void deleteByResourceId(Long resourceId);
}
