[CmdletBinding()]
param(
    [string]$BaseUrl = $env:SMOKE_BASE_URL,
    [string]$Email = $env:SMOKE_EMAIL,
    [string]$Password = $env:SMOKE_PASSWORD
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($BaseUrl) -or
    [string]::IsNullOrWhiteSpace($Email) -or
    [string]::IsNullOrWhiteSpace($Password)) {
    Write-Host 'SKIP: set SMOKE_BASE_URL, SMOKE_EMAIL, and SMOKE_PASSWORD to run the hosted smoke.'
    exit 2
}

$base = $BaseUrl.TrimEnd('/')
$requestHeaders = @{ 'X-Request-Id' = "smoke-$([guid]::NewGuid())" }

function Invoke-SmokeGet([string]$Path, [hashtable]$Headers) {
    $response = Invoke-WebRequest -Uri "$base$Path" -Method Get -Headers $Headers -UseBasicParsing
    if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 300) {
        throw "Smoke request failed: $Path ($($response.StatusCode))"
    }
}

Invoke-SmokeGet '/actuator/health' $requestHeaders

$login = Invoke-RestMethod -Uri "$base/api/auth/login" -Method Post -Headers $requestHeaders -ContentType 'application/json' -Body (@{
    email = $Email
    password = $Password
} | ConvertTo-Json)

if ([string]::IsNullOrWhiteSpace($login.accessToken)) {
    throw 'Smoke login did not return an access token.'
}

$authHeaders = @{
    Authorization = "Bearer $($login.accessToken)"
    'X-Request-Id' = "smoke-$([guid]::NewGuid())"
}

foreach ($path in @(
    '/api/dashboard',
    '/api/products?page=0&size=5&sort=createdAt,desc',
    '/api/risks?page=0&size=5&sort=createdAt,desc',
    '/api/imports?page=0&size=5',
    '/api/reports/daily-ops-brief',
    '/api/reports/inventory-risk',
    '/api/reports/supplier-sla',
    '/api/reports/order-delay',
    '/api/reports/product-margin'
)) {
    Invoke-SmokeGet $path $authHeaders
}

$brief = Invoke-RestMethod -Uri "$base/api/ai/recommendations/generate" -Method Post -Headers $authHeaders -ContentType 'application/json' -Body '{"topN":5}'
if ($brief.generatedBy -notin @('AI', 'RULE_BASED')) {
    throw 'Smoke brief generation returned an unexpected provenance.'
}

Write-Host 'PASS: health, login, dashboard, products, risks, brief, imports, and reports.'
exit 0
