package com.yourproject.tcm.model.dto;

import java.util.List;

/**
 * TestAnalyticsDTO - Data Transfer Object for test execution analytics
 * Used to send analytics data from backend to frontend
 */
public class TestAnalyticsDTO {

    // Overall KPIs
    private long totalTestCases;
    private long executedCount;
    private long passedCount;
    private long failedCount;
    private long notExecutedCount;
    private double passRate;
    private double failRate;

    // Project breakdown
    private List<ProjectAnalytics> byProject;

    // Module breakdown
    private List<ModuleAnalytics> byModule;

    public TestAnalyticsDTO() {}

    public TestAnalyticsDTO(long totalTestCases, long executedCount, long passedCount, long failedCount, long notExecutedCount, double passRate, double failRate, List<ProjectAnalytics> byProject, List<ModuleAnalytics> byModule) {
        this.totalTestCases = totalTestCases;
        this.executedCount = executedCount;
        this.passedCount = passedCount;
        this.failedCount = failedCount;
        this.notExecutedCount = notExecutedCount;
        this.passRate = passRate;
        this.failRate = failRate;
        this.byProject = byProject;
        this.byModule = byModule;
    }

    // Getters and Setters
    public long getTotalTestCases() { return totalTestCases; }
    public void setTotalTestCases(long totalTestCases) { this.totalTestCases = totalTestCases; }

    public long getExecutedCount() { return executedCount; }
    public void setExecutedCount(long executedCount) { this.executedCount = executedCount; }

    public long getPassedCount() { return passedCount; }
    public void setPassedCount(long passedCount) { this.passedCount = passedCount; }

    public long getFailedCount() { return failedCount; }
    public void setFailedCount(long failedCount) { this.failedCount = failedCount; }

    public long getNotExecutedCount() { return notExecutedCount; }
    public void setNotExecutedCount(long notExecutedCount) { this.notExecutedCount = notExecutedCount; }

    public double getPassRate() { return passRate; }
    public void setPassRate(double passRate) { this.passRate = passRate; }

    public double getFailRate() { return failRate; }
    public void setFailRate(double failRate) { this.failRate = failRate; }

    public List<ProjectAnalytics> getByProject() { return byProject; }
    public void setByProject(List<ProjectAnalytics> byProject) { this.byProject = byProject; }

    public List<ModuleAnalytics> getByModule() { return byModule; }
    public void setByModule(List<ModuleAnalytics> byModule) { this.byModule = byModule; }

    /**
     * ProjectAnalytics - Analytics data for a single project
     */
    public static class ProjectAnalytics {
        private Long projectId;
        private String projectName;
        private long totalTestCases;
        private long executedCount;
        private long passedCount;
        private long failedCount;
        private long notExecutedCount;

        public ProjectAnalytics() {}

        public ProjectAnalytics(Long projectId, String projectName, long totalTestCases, long executedCount, long passedCount, long failedCount, long notExecutedCount) {
            this.projectId = projectId;
            this.projectName = projectName;
            this.totalTestCases = totalTestCases;
            this.executedCount = executedCount;
            this.passedCount = passedCount;
            this.failedCount = failedCount;
            this.notExecutedCount = notExecutedCount;
        }

        // Getters and Setters
        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }

        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }

        public long getTotalTestCases() { return totalTestCases; }
        public void setTotalTestCases(long totalTestCases) { this.totalTestCases = totalTestCases; }

        public long getExecutedCount() { return executedCount; }
        public void setExecutedCount(long executedCount) { this.executedCount = executedCount; }

        public long getPassedCount() { return passedCount; }
        public void setPassedCount(long passedCount) { this.passedCount = passedCount; }

        public long getFailedCount() { return failedCount; }
        public void setFailedCount(long failedCount) { this.failedCount = failedCount; }

        public long getNotExecutedCount() { return notExecutedCount; }
        public void setNotExecutedCount(long notExecutedCount) { this.notExecutedCount = notExecutedCount; }
    }

    /**
     * ModuleAnalytics - Analytics data for a single module
     */
    public static class ModuleAnalytics {
        private Long moduleId;
        private String moduleName;
        private Long projectId;
        private String projectName;
        private long totalTestCases;
        private long executedCount;
        private long passedCount;
        private long failedCount;
        private long notExecutedCount;

        public ModuleAnalytics() {}

        public ModuleAnalytics(Long moduleId, String moduleName, Long projectId, String projectName, long totalTestCases, long executedCount, long passedCount, long failedCount, long notExecutedCount) {
            this.moduleId = moduleId;
            this.moduleName = moduleName;
            this.projectId = projectId;
            this.projectName = projectName;
            this.totalTestCases = totalTestCases;
            this.executedCount = executedCount;
            this.passedCount = passedCount;
            this.failedCount = failedCount;
            this.notExecutedCount = notExecutedCount;
        }

        // Getters and Setters
        public Long getModuleId() { return moduleId; }
        public void setModuleId(Long moduleId) { this.moduleId = moduleId; }

        public String getModuleName() { return moduleName; }
        public void setModuleName(String moduleName) { this.moduleName = moduleName; }

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }

        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }

        public long getTotalTestCases() { return totalTestCases; }
        public void setTotalTestCases(long totalTestCases) { this.totalTestCases = totalTestCases; }

        public long getExecutedCount() { return executedCount; }
        public void setExecutedCount(long executedCount) { this.executedCount = executedCount; }

        public long getPassedCount() { return passedCount; }
        public void setPassedCount(long passedCount) { this.passedCount = passedCount; }

        public long getFailedCount() { return failedCount; }
        public void setFailedCount(long failedCount) { this.failedCount = failedCount; }

        public long getNotExecutedCount() { return notExecutedCount; }
        public void setNotExecutedCount(long notExecutedCount) { this.notExecutedCount = notExecutedCount; }
    }
}