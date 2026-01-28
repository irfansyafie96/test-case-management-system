package com.yourproject.tcm.model.dto;

public class TestSubmoduleDTO {
    private Long id;
    private String name;
    private Long testModuleId;
    private String testModuleName;

    public TestSubmoduleDTO(Long id, String name, Long testModuleId, String testModuleName) {
        this.id = id;
        this.name = name;
        this.testModuleId = testModuleId;
        this.testModuleName = testModuleName;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getTestModuleId() { return testModuleId; }
    public void setTestModuleId(Long testModuleId) { this.testModuleId = testModuleId; }

    public String getTestModuleName() { return testModuleName; }
    public void setTestModuleName(String testModuleName) { this.testModuleName = testModuleName; }
}