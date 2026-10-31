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
    '/api/reports/daily-ops-brief',
    '/api/reports/inventory-risk',
    '/api/reports/supplier-sla',
    '/api/reports/order-delay',
    '/api/reports/product-margin',
    '/api/ai/recommendations?page=0&size=5&sort=createdAt,desc'
)) {
    Invoke-SmokeGet $path $authHeaders
}

$recommendations = Invoke-RestMethod -Uri "$base/api/ai/recommendations?page=0&size=5&sort=createdAt,desc" -Method Get -Headers $authHeaders
if ($recommendations.total -lt 1 -or $recommendations.items[0].generatedBy -ne 'RULE_BASED') {
    throw 'Hosted demo must expose at least one pre-generated RULE_BASED brief.'
}

function Assert-SmokeForbidden([string]$Method, [string]$Path, [string]$Body = $null) {
    try {
        $params = @{ Uri = "$base$Path"; Method = $Method; Headers = $authHeaders; UseBasicParsing = $true }
        if ($Method -notin @('GET', 'HEAD') -and -not [string]::IsNullOrEmpty($Body)) {
            $params.ContentType = 'application/json'
            $params.Body = $Body
        }
        Invoke-WebRequest @params | Out-Null
        throw "Expected VIEWER request to be forbidden: $Method $Path"
    } catch {
        $status = 0
        if ($null -ne $_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
        }
        if ($status -ne 403) { throw }
    }
}

Assert-SmokeForbidden 'GET' '/api/imports?page=0&size=5'
Assert-SmokeForbidden 'POST' '/api/ai/recommendations/generate' '{"topN":5}'

Write-Host 'PASS: health, VIEWER login/read paths, RULE_BASED brief, reports, and least-privilege 403 gates.'
exit 0
