# Login as qaba
$loginBody = @{username='qaba';password='qaba123'} | ConvertTo-Json
$session = $null
$response = Invoke-RestMethod -Uri http://localhost:8080/api/auth/login -Method POST -Body $loginBody -ContentType 'application/json' -SessionVariable session
Write-Host "Login response:" $response
Write-Host "Session cookies count:" $session.Cookies.GetCookies([uri]"http://localhost:8080").Count

# Try to create project
$projectBody = @{
    name='QA Project Test'
    description='Should fail for QA user'
    startDate='2024-01-01'
    endDate='2024-12-31'
} | ConvertTo-Json
try {
    $projectResponse = Invoke-RestMethod -Uri http://localhost:8080/api/projects -Method POST -Body $projectBody -ContentType 'application/json' -WebSession $session
    Write-Host "PROJECT CREATION SUCCEEDED (should not happen):" $projectResponse
} catch {
    Write-Host "PROJECT CREATION FAILED (expected):"
    Write-Host "StatusCode:" $_.Exception.Response.StatusCode.value__
    Write-Host "StatusDescription:" $_.Exception.Response.StatusDescription
    Write-Host "Message:" $_.Exception.Message
}