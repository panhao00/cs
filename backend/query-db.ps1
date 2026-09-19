$ErrorActionPreference = "Stop"

# 默认使用 registrar 账号（注册主任），可通过环境变量覆盖
$username = if ($env:CRS_USERNAME) { $env:CRS_USERNAME } else { "registrar" }
$password = if ($env:CRS_PASSWORD) { $env:CRS_PASSWORD } else { "registrar" }

Write-Host "使用账号: $username" -ForegroundColor Cyan

$loginBody = @{
    username = $username
    password = $password
} | ConvertTo-Json

$loginResp = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -ContentType "application/json" -Body $loginBody -UseBasicParsing
$token = ($loginResp.Content | ConvertFrom-Json).token
$headers = @{Authorization = "Bearer $token"}

echo "=== Users ==="
$users = Invoke-WebRequest -Uri "http://localhost:8080/api/users" -Method GET -Headers $headers -UseBasicParsing
$users.Content | ConvertFrom-Json | Select-Object id, username, name, role | Format-Table -AutoSize

echo "=== Courses ==="
$courses = Invoke-WebRequest -Uri "http://localhost:8080/api/courses" -Method GET -Headers $headers -UseBasicParsing
$courses.Content | ConvertFrom-Json | Select-Object id, code, name, credits | Format-Table -AutoSize

echo "=== Offerings ==="
$offerings = Invoke-WebRequest -Uri "http://localhost:8080/api/offerings" -Method GET -Headers $headers -UseBasicParsing
$offerings.Content | ConvertFrom-Json | Select-Object id, courseId, professorId, semester, year, capacity, enrolled | Format-Table -AutoSize

echo "=== Registrations ==="
$registrations = Invoke-WebRequest -Uri "http://localhost:8080/api/registrations" -Method GET -Headers $headers -UseBasicParsing
$registrations.Content | ConvertFrom-Json | Select-Object id, studentId, offeringId, status | Format-Table -AutoSize

