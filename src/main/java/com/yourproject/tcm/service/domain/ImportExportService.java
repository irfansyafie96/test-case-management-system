package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.*;
import com.yourproject.tcm.repository.*;
import com.yourproject.tcm.service.SecurityHelper;
import com.yourproject.tcm.service.UserContextService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Domain service for import/export operations.
 * Extracted from TcmService for better separation of concerns.
 */
@Service
public class ImportExportService {

    private final TestModuleRepository testModuleRepository;
    private final SubmoduleRepository submoduleRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestCaseService testCaseService;
    private final UserContextService userContextService;
    private final SecurityHelper securityHelper;
    private final jakarta.persistence.EntityManager entityManager;

    @Autowired
    public ImportExportService(
            TestModuleRepository testModuleRepository,
            SubmoduleRepository submoduleRepository,
            TestCaseRepository testCaseRepository,
            TestCaseService testCaseService,
            UserContextService userContextService,
            SecurityHelper securityHelper,
            jakarta.persistence.EntityManager entityManager) {
        this.testModuleRepository = testModuleRepository;
        this.submoduleRepository = submoduleRepository;
        this.testCaseRepository = testCaseRepository;
        this.testCaseService = testCaseService;
        this.userContextService = userContextService;
        this.securityHelper = securityHelper;
        this.entityManager = entityManager;
    }

    /**
     * Check if user has permission to import into a module (for use by controller before transaction)
     * @param moduleId ID of the test module
     * @throws RuntimeException if user doesn't have permission
     */
    public void checkImportPermission(Long moduleId) {
        // Get module
        Optional<TestModule> moduleOpt = testModuleRepository.findById(moduleId);
        if (moduleOpt.isEmpty()) {
            throw new RuntimeException("Test module not found with id: " + moduleId);
        }

        TestModule module = moduleOpt.get();
        
        // Check user permissions
        User currentUser = userContextService.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("Access denied: User not authenticated");
        }
        
        // Verify organization boundary
        if (module.getProject() == null || module.getProject().getOrganization() == null) {
            throw new RuntimeException("Test module has no organization assigned");
        }
        securityHelper.requireSameOrganization(currentUser, module.getProject().getOrganization());
        
        // Check role permissions
        securityHelper.requireAdminQaOrBa(currentUser);
        
        // ADMIN users can import into any module
        if (!userContextService.isAdmin(currentUser)) {
            // Re-fetch user with assignedTestModules loaded to avoid lazy loading issues
            User userWithModules = userContextService.getCurrentUserWithModules();
            
            // Check if user has assigned test modules
            if (userWithModules.getAssignedTestModules() == null || userWithModules.getAssignedTestModules().isEmpty()) {
                throw new RuntimeException("Access denied: You are not assigned to any test modules");
            }
            
            // Check assignment by ID
            boolean isAssigned = userWithModules.getAssignedTestModules().stream()
                .anyMatch(m -> m.getId().equals(module.getId()));
            if (!isAssigned) {
                throw new RuntimeException("Access denied: You are not assigned to this test module");
            }
        }
    }

    /**
     * Import test cases from Excel file into a test module
     * @param moduleId ID of the test module to import into
     * @param file Excel file to import (.xlsx format)
     * @return Import result with statistics
     */
    @Transactional
    public Map<String, Object> importTestCasesFromExcel(Long moduleId, MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int suitesCreated = 0;
        int testCasesCreated = 0;
        int testCasesSkipped = 0;

        try {
            // Validate file format
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("File is empty or null");
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
                throw new RuntimeException("Invalid file format. Only .xlsx files are supported.");
            }

            // Get module
            Optional<TestModule> moduleOpt = testModuleRepository.findById(moduleId);
            if (moduleOpt.isEmpty()) {
                throw new RuntimeException("Test module not found with id: " + moduleId);
            }

            TestModule module = moduleOpt.get();

            // Parse Excel file
            Workbook workbook = WorkbookFactory.create(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0); // First sheet

            // Validate headers
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("Excel file has no header row");
            }

            List<String> expectedHeaders = Arrays.asList("Submodule Name", "Test Case ID", "Title", "Description", "Step Number", "Action", "Expected Result");
            for (int i = 0; i < expectedHeaders.size(); i++) {
                Cell cell = headerRow.getCell(i);
                String headerValue = cell != null ? cell.getStringCellValue().trim() : "";
                if (!headerValue.equals(expectedHeaders.get(i))) {
                    throw new RuntimeException("Invalid header at column " + (i + 1) + ". Expected: " + expectedHeaders.get(i) + ", Found: " + headerValue);
                }
            }

            // Get existing test case IDs in this module to check for duplicates
            Set<String> existingTestCaseIds = new HashSet<>();
            if (module.getSubmodules() != null) {
                for (Submodule submodule : module.getSubmodules()) {
                    if (submodule.getTestCases() != null) {
                        for (TestCase testCase : submodule.getTestCases()) {
                            existingTestCaseIds.add(testCase.getTestCaseId());
                        }
                    }
                }
            }

            // Parse data rows
            Map<String, List<Map<String, Object>>> testCaseDataMap = new LinkedHashMap<>(); // Maintains insertion order
            int rowNum = 1; // Start from row 1 (after header)

            while (true) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    break; // End of data
                }

                try {
                    // Extract data from row
                    String submoduleName = getCellValueAsString(row.getCell(0));
                    String testCaseId = getCellValueAsString(row.getCell(1));
                    String title = getCellValueAsString(row.getCell(2));
                    String description = getCellValueAsString(row.getCell(3));
                    String stepNumberStr = getCellValueAsString(row.getCell(4));
                    String action = getCellValueAsString(row.getCell(5));
                    String expectedResult = getCellValueAsString(row.getCell(6));

                    // Validate required fields
                    if (submoduleName == null || submoduleName.trim().isEmpty()) {
                        errors.add("Row " + (rowNum + 1) + ": Submodule Name is required");
                        rowNum++;
                        continue;
                    }
                    if (testCaseId == null || testCaseId.trim().isEmpty()) {
                        errors.add("Row " + (rowNum + 1) + ": Test Case ID is required");
                        rowNum++;
                        continue;
                    }
                    if (title == null || title.trim().isEmpty()) {
                        errors.add("Row " + (rowNum + 1) + ": Title is required");
                        rowNum++;
                        continue;
                    }
                    if (stepNumberStr == null || stepNumberStr.trim().isEmpty()) {
                        errors.add("Row " + (rowNum + 1) + ": Step Number is required");
                        rowNum++;
                        continue;
                    }
                    if (action == null || action.trim().isEmpty()) {
                        errors.add("Row " + (rowNum + 1) + ": Action is required");
                        rowNum++;
                        continue;
                    }
                    if (expectedResult == null || expectedResult.trim().isEmpty()) {
                        errors.add("Row " + (rowNum + 1) + ": Expected Result is required");
                        rowNum++;
                        continue;
                    }

                    // Parse step number
                    int stepNumber;
                    try {
                        stepNumber = Integer.parseInt(stepNumberStr.trim());
                        if (stepNumber < 1) {
                            errors.add("Row " + (rowNum + 1) + ": Step Number must be positive (found: " + stepNumber + ")");
                            rowNum++;
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        errors.add("Row " + (rowNum + 1) + ": Step Number must be a valid integer (found: " + stepNumberStr + ")");
                        rowNum++;
                        continue;
                    }

                    // Check for duplicate test case ID in module
                    String testCaseKey = testCaseId.trim();
                    if (existingTestCaseIds.contains(testCaseKey)) {
                        testCasesSkipped++;
                        rowNum++;
                        continue; // Skip this row
                    }

                    // Group test case data by test case ID
                    String key = submoduleName.trim() + "|" + testCaseKey;
                    if (!testCaseDataMap.containsKey(key)) {
                        testCaseDataMap.put(key, new ArrayList<>());
                    }

                    Map<String, Object> stepData = new HashMap<>();
                    stepData.put("submoduleName", submoduleName.trim());
                    stepData.put("testCaseId", testCaseKey);
                    stepData.put("title", title.trim());
                    stepData.put("description", description != null ? description.trim() : "");
                    stepData.put("stepNumber", stepNumber);
                    stepData.put("action", action.trim());
                    stepData.put("expectedResult", expectedResult.trim());

                    testCaseDataMap.get(key).add(stepData);

                    rowNum++;
                } catch (Exception e) {
                    errors.add("Row " + (rowNum + 1) + ": " + e.getMessage());
                    rowNum++;
                }
            }

            workbook.close();

            // If there are validation errors, throw exception to rollback
            if (!errors.isEmpty()) {
                throw new RuntimeException("Validation failed: " + String.join("; ", errors));
            }

            // Create submodules and test cases
            Map<String, Submodule> submoduleMap = new HashMap<>();
            for (String key : testCaseDataMap.keySet()) {
                List<Map<String, Object>> steps = testCaseDataMap.get(key);
                if (steps.isEmpty()) {
                    continue;
                }

                String submoduleName = steps.get(0).get("submoduleName").toString();
                String testCaseId = steps.get(0).get("testCaseId").toString();
                String title = steps.get(0).get("title").toString();
                String description = steps.get(0).get("description").toString();

                // Find or create submodule
                Submodule submodule;
                if (submoduleMap.containsKey(submoduleName)) {
                    submodule = submoduleMap.get(submoduleName);
                } else {
                    // Check if submodule already exists in module
                    submodule = module.getSubmodules().stream()
                        .filter(s -> s.getName().equals(submoduleName))
                        .findFirst()
                        .orElse(null);

                    if (submodule == null) {
                        submodule = new Submodule();
                        submodule.setName(submoduleName);
                        submodule.setTestModule(module);
                        submodule = submoduleRepository.save(submodule);
                        suitesCreated++;
                    }
                    submoduleMap.put(submoduleName, submodule);
                }

                // Create test case
                TestCase testCase = new TestCase();
                testCase.setTestCaseId(testCaseId);
                testCase.setTitle(title);
                testCase.setDescription(description);
                testCase.setSubmodule(submodule);

                // Create test steps
                List<TestStep> testSteps = new ArrayList<>();
                for (Map<String, Object> stepData : steps) {
                    TestStep step = new TestStep();
                    step.setStepNumber((Integer) stepData.get("stepNumber"));
                    step.setAction(stepData.get("action").toString());
                    step.setExpectedResult(stepData.get("expectedResult").toString());
                    step.setTestCase(testCase);
                    testSteps.add(step);
                }

                // Sort steps by step number
                testSteps.sort((a, b) -> a.getStepNumber().compareTo(b.getStepNumber()));
                testCase.setTestSteps(testSteps);

                testCase = testCaseRepository.save(testCase);
                entityManager.flush(); // Ensure steps are persisted to database
                testCasesCreated++;

                // Auto-generate executions for assigned users
                if (module.getAssignedUsers() != null) {
                    for (User user : module.getAssignedUsers()) {
                        try {
                            testCaseService.autoGenerateTestExecution(testCase.getId(), user.getId());
                        } catch (Exception e) {
                            // Log error but continue
                        }
                    }
                }
            }

            // Build result
            result.put("success", true);
            result.put("message", "Import completed successfully");
            result.put("suitesCreated", suitesCreated);
            result.put("testCasesCreated", testCasesCreated);
            result.put("testCasesSkipped", testCasesSkipped);
            result.put("errors", errors);

        } catch (RuntimeException e) {
            // Re-throw to trigger rollback
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Helper method to get cell value as string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return null;
            default:
                return null;
        }
    }

    /**
     * Download Excel template for test case import
     * @return Excel template file as byte array
     */
    public byte[] downloadExcelTemplate() throws IOException {
        Resource resource = new ClassPathResource("templates/test-case-import-template.xlsx");
        InputStream inputStream = resource.getInputStream();
        return inputStream.readAllBytes();
    }
}