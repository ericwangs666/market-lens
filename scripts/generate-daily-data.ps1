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

function Get-HistoryReports {
  param(
    [string]$OutputDir,
    [string]$CurrentDate,
    [string]$CurrentLastRun
  )

  $items = @{}
  if (Test-Path $OutputDir) {
    Get-ChildItem -Path $OutputDir -Filter "*.json" -File |
      Where-Object { $_.Name -notin @("latest.json", "index.json") } |
      ForEach-Object {
        $dateValue = $_.BaseName
        $lastRunValue = $dateValue
        try {
          $report = Get-Content -Raw -Encoding UTF8 $_.FullName | ConvertFrom-Json
          if ($report.lastRun) { $lastRunValue = $report.lastRun }
        } catch {}

        $items[$dateValue] = [ordered]@{
          date = $dateValue
          file = $_.Name
          lastRun = $lastRunValue
        }
      }
  }

  $items[$CurrentDate] = [ordered]@{
    date = $CurrentDate
    file = "$CurrentDate.json"
    lastRun = $CurrentLastRun
  }

  return $items.Values | Sort-Object -Property date -Descending
}

if (!(Test-Path $SeedPath)) {
  throw "Seed file not found: $SeedPath"
}

$now = Get-ChinaNow
$date = $now.ToString("yyyy-MM-dd")
$lastRun = $now.ToString("yyyy-MM-dd HH:mm 'CST'")
$seed = Get-Content -Raw -Encoding UTF8 $SeedPath | ConvertFrom-Json
$marketData = ConvertTo-OrderedMarketData -Seed $seed -Date $date -LastRun $lastRun
$historyReports = @(Get-HistoryReports -OutputDir $OutputDir -CurrentDate $date -CurrentLastRun $lastRun)
$marketData["historyReports"] = $historyReports
$json = $marketData | ConvertTo-Json -Depth 20
$historyJson = "[" + (($historyReports | ForEach-Object { $_ | ConvertTo-Json -Depth 8 }) -join ",`n") + "]"

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
Set-Content -Path (Join-Path $OutputDir "$date.json") -Value $json -Encoding UTF8
Set-Content -Path (Join-Path $OutputDir "latest.json") -Value $json -Encoding UTF8
Set-Content -Path (Join-Path $OutputDir "index.json") -Value $historyJson -Encoding UTF8

$rootDailyDir = "daily"
New-Item -ItemType Directory -Force -Path $rootDailyDir | Out-Null
Set-Content -Path (Join-Path $rootDailyDir "$date.json") -Value $json -Encoding UTF8
Set-Content -Path (Join-Path $rootDailyDir "latest.json") -Value $json -Encoding UTF8
Set-Content -Path (Join-Path $rootDailyDir "index.json") -Value $historyJson -Encoding UTF8

$dataJsPath = Join-Path $SiteDir "data.js"
$dataJs = "window.MARKET_DATA = $json;`n"
Set-Content -Path $dataJsPath -Value $dataJs -Encoding UTF8

$rootDataJsPath = "data.js"
if ((Test-Path $rootDataJsPath) -and ((Resolve-Path $rootDataJsPath).Path -ne (Resolve-Path $dataJsPath).Path)) {
  Set-Content -Path $rootDataJsPath -Value $dataJs -Encoding UTF8
}

Write-Host "Generated daily data for $date"
Write-Host "Updated $dataJsPath"
