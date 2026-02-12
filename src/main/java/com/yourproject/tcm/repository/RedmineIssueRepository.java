package com.yourproject.tcm.repository;

import com.yourproject.tcm.model.RedmineIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RedmineIssueRepository extends JpaRepository<RedmineIssue, Long> {
    
    List<RedmineIssue> findByExecutionId(Long executionId);
    
    void deleteByExecutionId(Long executionId);
}
