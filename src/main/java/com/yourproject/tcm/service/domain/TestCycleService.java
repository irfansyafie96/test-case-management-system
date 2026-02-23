package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.Project;
import com.yourproject.tcm.model.TestCycle;
import com.yourproject.tcm.model.dto.TestCycleDTO;
import com.yourproject.tcm.repository.ProjectRepository;
import com.yourproject.tcm.repository.TestCycleRepository;
import com.yourproject.tcm.service.UserContextService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TestCycleService {
    
    private final TestCycleRepository testCycleRepository;
    private final ProjectRepository projectRepository;
    private final UserContextService userContextService;
    
    public TestCycleService(
            TestCycleRepository testCycleRepository,
            ProjectRepository projectRepository,
            UserContextService userContextService) {
        this.testCycleRepository = testCycleRepository;
        this.projectRepository = projectRepository;
        this.userContextService = userContextService;
    }
    
    public TestCycleDTO createCycle(Long projectId, TestCycleDTO dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
        
        TestCycle cycle = new TestCycle();
        cycle.setName(dto.getName());
        cycle.setDescription(dto.getDescription());
        cycle.setRedmineProjectUrl(dto.getRedmineProjectUrl());
        cycle.setProject(project);
        cycle.setStartDate(dto.getStartDate());
        cycle.setEndDate(dto.getEndDate());
        cycle.setActive(dto.isActive());
        cycle.setSortOrder(testCycleRepository.findMaxSortOrderByProjectId(projectId) + 1);
        
        TestCycle saved = testCycleRepository.save(cycle);
        return toDTO(saved);
    }
    
    public TestCycleDTO updateCycle(Long cycleId, TestCycleDTO dto) {
        TestCycle cycle = testCycleRepository.findById(cycleId)
                .orElseThrow(() -> new RuntimeException("Test cycle not found: " + cycleId));
        
        cycle.setName(dto.getName());
        cycle.setDescription(dto.getDescription());
        cycle.setRedmineProjectUrl(dto.getRedmineProjectUrl());
        cycle.setStartDate(dto.getStartDate());
        cycle.setEndDate(dto.getEndDate());
        cycle.setActive(dto.isActive());
        
        TestCycle saved = testCycleRepository.save(cycle);
        return toDTO(saved);
    }
    
    public void deleteCycle(Long cycleId) {
        TestCycle cycle = testCycleRepository.findById(cycleId)
                .orElseThrow(() -> new RuntimeException("Test cycle not found: " + cycleId));
        testCycleRepository.delete(cycle);
    }
    
    @Transactional(readOnly = true)
    public List<TestCycleDTO> getCyclesByProject(Long projectId) {
        return testCycleRepository.findByProjectIdOrderBySortOrderAsc(projectId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<TestCycleDTO> getActiveCyclesByProject(Long projectId) {
        return testCycleRepository.findByProjectIdAndIsActiveTrueOrderBySortOrderAsc(projectId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public TestCycleDTO getCycleById(Long cycleId) {
        TestCycle cycle = testCycleRepository.findById(cycleId)
                .orElseThrow(() -> new RuntimeException("Test cycle not found: " + cycleId));
        return toDTO(cycle);
    }
    
    @Transactional(readOnly = true)
    public TestCycle getCycleEntityById(Long cycleId) {
        return testCycleRepository.findById(cycleId)
                .orElseThrow(() -> new RuntimeException("Test cycle not found: " + cycleId));
    }
    
    private TestCycleDTO toDTO(TestCycle cycle) {
        TestCycleDTO dto = new TestCycleDTO();
        dto.setId(cycle.getId());
        dto.setName(cycle.getName());
        dto.setDescription(cycle.getDescription());
        dto.setRedmineProjectUrl(cycle.getRedmineProjectUrl());
        dto.setRedmineProjectIdentifier(cycle.getRedmineProjectIdentifier());
        dto.setProjectId(cycle.getProject().getId());
        dto.setProjectName(cycle.getProject().getName());
        dto.setStartDate(cycle.getStartDate());
        dto.setEndDate(cycle.getEndDate());
        dto.setActive(cycle.isActive());
        dto.setSortOrder(cycle.getSortOrder());
        dto.setCreatedDate(cycle.getCreatedDate());
        dto.setCreatedBy(cycle.getCreatedBy());
        return dto;
    }
}
