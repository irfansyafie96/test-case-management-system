
// Project Model - Used across multiple components
export interface Project {
  id: number | string;
  name: string;
  description?: string;
  createdDate?: string;
  updatedDate?: string;
  createdBy?: string;
  modules?: TestModule[]; // For when we need to load project with modules
}

// Module Model - Associated with projects
export interface TestModule {
  id: number | string;
  name: string;
  projectId: number | string;
  projectName?: string; // Added dynamically for display
  project?: Project; // Full project information for display
  description?: string;
  submodules?: Submodule[];
  submodulesCount?: number; // Number of submodules in this module
  testCasesCount?: number; // Total number of test cases across all submodules
  createdDate?: string;
  updatedDate?: string;
  isEditable?: boolean; // UI flag: true if current user can edit this module
}

// Submodule Model
export interface Submodule {
  id: number | string;
  name: string;
  moduleId: number | string;
  moduleName?: string;
  testModule?: TestModule; // Full module information for display
  testCases?: TestCase[];
  createdDate?: string;
  updatedDate?: string;
}

// Test Step Model
export interface TestStep {
  id: number | string;
  stepNumber: number;
  action: string;
  expectedResult: string;
  testCaseId?: number | string;
}

// Test Case Model
export interface TestCase {
  id: number | string;
  testCaseId: string; // This is the business ID (TC-001), usually string
  title: string;
  description?: string;
  status?: string;
  steps?: string[]; // Legacy/Simplified steps
  testSteps?: TestStep[]; // Detailed steps from backend
  submoduleId: number | string;
  submodule?: Submodule; // Full submodule information for display
  createdDate?: string;
  updatedDate?: string;
  prerequisites?: string;
  tags?: string[];
  isEditable?: boolean; // UI flag: true if current user can edit this test case
  
  // Flattened hierarchy fields from backend
  projectName?: string;
  moduleName?: string;
  submoduleName?: string;
}

// Test Execution Model
export interface TestExecution {
  id: string;
  testCaseId: string;
  status: 'PASSED' | 'FAILED' | 'BLOCKED' | 'PENDING';
  executedBy?: string;
  executionDate?: string;
  notes?: string;
  stepResults?: TestStepResult[];
  duration?: number;
  environment?: string;
  overallResult?: string;
  testCase?: TestCase; // Added reference to the associated test case
  title?: string; // Added for display purposes
  assignedToUser?: User; // Added for assignment functionality
  
  // Flattened fields from backend DTO
  assignedToUserId?: number;
  assignedToUsername?: string;
  
  // Flattened hierarchy fields for easier grouping
  projectId?: number | string;
  projectName?: string;
  moduleId?: number | string;
  moduleName?: string;
  submoduleId?: number | string;
  submoduleName?: string;

  // Redmine Integration Fields
  redmineIssueId?: string;
  redmineIssueUrl?: string;
  bugReportSubject?: string;
  bugReportDescription?: string;
  redmineIssueCreatedAt?: string;
  redmineIssueUpdatedAt?: string;
  
  // Convenience fields for display
  testCaseTitle?: string; // Alias for title (test case title)
  
  // Test Cycle/Phase
  testCycle?: TestCycle;
}

// Completion Summary Model
export interface CompletionSummary {
  total: number;
  passed: number;
  failed: number;
  blocked: number;
  pending: number;
}

// Test Step Result Model
export interface TestStepResult {
  id: string;
  testStepId?: string; // ID of the test step (needed for updating step results)
  executionId: string;
  stepNumber: number;
  stepDescription: string;
  status: 'PASSED' | 'FAILED' | 'BLOCKED' | 'PENDING';
  actualResult?: string;
  notes?: string;
  screenshotFileName?: string;
  attachments?: string[]; // Array of attachment file names
  testStep?: any; // Reference to the test step details (deprecated, use action and expectedResult)
  action?: string; // Action from test step (from backend DTO)
  expectedResult?: string; // Expected result from test step (from backend DTO)
}

// User Model
export interface User {
  id: number | string;
  username: string;
  email: string;
  roles?: string[];
  enabled: boolean;
  isExternal?: boolean;
  createdDate?: string;
  assignedProjects?: Project[];
  assignedTestModules?: TestModule[]; // Legacy - kept for backward compatibility
  assignedModulesForEditing?: TestModule[]; // Module editors (QA/BA only)
  assignedModulesForExecution?: TestModule[]; // Execution assignees (QA/BA/TESTER)
}

// Invitation Model
export interface Invitation {
  id?: number | string;
  email: string;
  role: string;
  token: string;
  organizationName: string;
  isExternal: boolean;
  projectId?: number | string;
  accepted: boolean;
  expiryDate: string;
}

// Assignment Request DTOs
export interface ProjectAssignmentRequest {
  userId: number | string;
  projectId: number | string;
}

export interface ModuleAssignmentRequest {
  userId: number | string;
  testModuleId: number | string;
}

export interface ExecutionAssignmentRequest {
  userId: number | string;
  testModuleId: number | string;
}

// Redmine Issue Interface - for tracking multiple issues per execution
export interface RedmineIssue {
  id: number | string;
  redmineIssueId?: string;
  redmineIssueUrl: string;
  bugReportSubject?: string;
  bugReportDescription?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
}

// Test Cycle Model - represents a testing phase within a project
export interface TestCycle {
  id: number | string;
  name: string;
  description?: string;
  redmineProjectUrl?: string;
  redmineProjectIdentifier?: string;
  projectId: number | string;
  projectName?: string;
  startDate?: string;
  endDate?: string;
  isActive: boolean;
  sortOrder: number;
  createdDate?: string;
  createdBy?: string;
}

// Ticket Audit Log - tracks history of ticket changes
export interface TicketAuditLog {
  id: number | string;
  action: string;
  oldValue?: string;
  newValue?: string;
  changedBy: string;
  changedAt: string;
  notes?: string;
}

// Ticket DTO - for ticket management page
export interface Ticket {
  id: number | string;
  redmineIssueId?: string;
  redmineIssueUrl: string;
  bugReportSubject?: string;
  bugReportDescription?: string;
  status: 'OPEN' | 'CLOSED';
  executionId?: number | string;
  testCaseTitle?: string;
  testCaseId?: string;
  projectName?: string;
  projectId?: number | string;
  cycleName?: string;
  cycleId?: number | string;
  createdAt?: string;
  updatedAt?: string;
  auditLogs?: TicketAuditLog[];
}

