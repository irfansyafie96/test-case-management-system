package com.yourproject.tcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.yourproject.tcm.model.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> { }