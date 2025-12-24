
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
  description?: string;
  testSuites?: TestSuite[];
  createdDate?: string;
  updatedDate?: string;
}

// Test Suite Model
export interface TestSuite {
  id: number | string;
  name: string;
  moduleId: number | string;
  moduleName?: string;
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
  expectedResult?: string;
  suiteId: number | string;
  createdDate?: string;
  updatedDate?: string;
  prerequisites?: string;
  tags?: string[];
}

// Test Execution Model
export interface TestExecution {
  id: string;
  testCaseId: string;
  status: 'PASS' | 'FAIL' | 'BLOCKED' | 'NOT_EXECUTED';
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
}

// Test Step Result Model
export interface TestStepResult {
  id: string;
  executionId: string;
  stepNumber: number;
  stepDescription: string;
  status: 'PASS' | 'FAIL' | 'BLOCKED' | 'NOT_EXECUTED';
  actualResult?: string;
  notes?: string;
  screenshotFileName?: string;
  attachments?: string[]; // Array of attachment file names
  testStep?: any; // Reference to the test step details
}

// User Model
export interface User {
  id: string;
  username: string;
  email: string;
  roles: string[];
  enabled: boolean;
  createdDate?: string;
  assignedProjects?: Project[];
  assignedTestModules?: TestModule[];
}

// Assignment Request DTOs
export interface ProjectAssignmentRequest {
  userId: string;
  projectId: string;
}

export interface ModuleAssignmentRequest {
  userId: string;
  testModuleId: string;
}

