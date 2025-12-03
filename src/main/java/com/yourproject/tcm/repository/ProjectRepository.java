package com.yourproject.tcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.yourproject.tcm.model.Project;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByName(String name);
}