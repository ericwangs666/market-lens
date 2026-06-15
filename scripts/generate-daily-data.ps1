param(
  [string]$SeedPath = "data/market-seed.json",
  [string]$SiteDir = "market-site",
  [string]$OutputDir = "market-site/daily"
)

$ErrorActionPreference = "Stop"

function Get-ChinaNow {
  $zone = $null
  foreach ($id in @("China Standard Time", "Asia/Shanghai")) {
    try {
      $zone = [System.TimeZoneInfo]::FindSystemTimeZoneById($id)
      break
    } catch {}
  }

  if ($zone) {
    return [System.TimeZoneInfo]::ConvertTimeFromUtc([DateTime]::UtcNow, $zone)
  }

  return [DateTime]::UtcNow.AddHours(8)
}

function ConvertTo-OrderedMarketData {
  param(
    [object]$Seed,
    [string]$Date,
    [string]$LastRun
  )

  $markets = [ordered]@{}
  foreach ($marketName in $Seed.markets.PSObject.Properties.Name) {
    $market = $Seed.markets.$marketName
    $summary = if ($market.summaryTemplate) {
      [string]$market.summaryTemplate -replace "\{date\}", $Date
    } else {
      ""
    }

    $markets[$marketName] = [ordered]@{
      label = $market.label
      summary = $summary
      metrics = $market.metrics
      sectors = $market.sectors
      stocks = $market.stocks
    }
  }

  return [ordered]@{
    lastRun = $LastRun
    generatedDate = $Date
    source = "market-seed"
    sourceNote = "Generated from data/market-seed.json. Replace this seed or extend the script with licensed market APIs for production data."
    markets = $markets
    research = $Seed.research
    apiPlan = $Seed.apiPlan
  }
}

if (!(Test-Path $SeedPath)) {
  throw "Seed file not found: $SeedPath"
}

$now = Get-ChinaNow
$date = $now.ToString("yyyy-MM-dd")
$lastRun = $now.ToString("yyyy-MM-dd HH:mm 'CST'")
$seed = Get-Content -Raw -Encoding UTF8 $SeedPath | ConvertFrom-Json
$marketData = ConvertTo-OrderedMarketData -Seed $seed -Date $date -LastRun $lastRun
$json = $marketData | ConvertTo-Json -Depth 20

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
Set-Content -Path (Join-Path $OutputDir "$date.json") -Value $json -Encoding UTF8
Set-Content -Path (Join-Path $OutputDir "latest.json") -Value $json -Encoding UTF8

$dataJsPath = Join-Path $SiteDir "data.js"
$dataJs = "window.MARKET_DATA = $json;`n"
Set-Content -Path $dataJsPath -Value $dataJs -Encoding UTF8

$rootDataJsPath = "data.js"
if ((Test-Path $rootDataJsPath) -and ((Resolve-Path $rootDataJsPath).Path -ne (Resolve-Path $dataJsPath).Path)) {
  Set-Content -Path $rootDataJsPath -Value $dataJs -Encoding UTF8
}

Write-Host "Generated daily data for $date"
Write-Host "Updated $dataJsPath"
