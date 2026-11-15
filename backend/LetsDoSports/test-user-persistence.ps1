param(
    [string]$JdkHome = $env:JAVA_HOME
)

$ErrorActionPreference = 'Stop'
$backendPath = $PSScriptRoot
$workspacePath = Split-Path (Split-Path $backendPath -Parent) -Parent
$composePath = Join-Path $workspacePath 'db/compose.test.yml'
$seedPath = Join-Path $workspacePath 'db/seed/dev_seed.sql'
# Svako pokretanje dobiva vlastiti projekt, volumen i nasumičan lokalni port.
$testProject = 'lds-user-tests-' + [guid]::NewGuid().ToString('N').Substring(0, 12)
$savedEnvironment = @{}
foreach ($variable in @('JAVA_HOME', 'PATH', 'LDS_DB_URL', 'LDS_DB_USER', 'LDS_DB_PASSWORD', 'LDS_INTEGRATION_TESTS')) {
    $savedEnvironment[$variable] = [Environment]::GetEnvironmentVariable($variable, 'Process')
}
$savedOutputEncoding = $OutputEncoding

function Invoke-TestCompose([string[]]$Arguments) {
    & docker compose -p $testProject -f $composePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        & docker compose -p $testProject -f $composePath logs db
        throw "Docker Compose nije uspio: $Arguments"
    }
}

function Invoke-TestSql([string]$Sql) {
    $result = $Sql | & docker compose -p $testProject -f $composePath exec -T db `
        psql -X -U lds_admin -d lds_user_test -v ON_ERROR_STOP=1 -t -A
    if ($LASTEXITCODE -ne 0) { throw 'SQL provjera nije uspjela.' }
    return $result
}

function Assert-TestSql([string]$Sql, [string]$Expected) {
    $actual = (Invoke-TestSql $Sql | Out-String).Trim()
    if ($actual -ne $Expected) { throw "SQL: očekivano '$Expected', dobiveno '$actual'." }
}

try {
    if (-not $JdkHome -or -not (Test-Path -LiteralPath (Join-Path $JdkHome 'bin/java.exe'))) {
        throw 'Navedi -JdkHome s putanjom do JDK-a 23 ili postavi JAVA_HOME.'
    }
    $env:JAVA_HOME = $JdkHome
    $env:PATH = (Join-Path $JdkHome 'bin') + ';' + $env:PATH
    $OutputEncoding = [System.Text.UTF8Encoding]::new($false)
    $env:LDS_DB_USER = 'lds_app'
    $env:LDS_DB_PASSWORD = 'test_app_password'
    $env:LDS_INTEGRATION_TESTS = 'true'

    foreach ($scenario in @('upgrade', 'fresh')) {
        Write-Host "Provjera baze: $scenario"
        Invoke-TestCompose @('up', '-d', '--wait', 'db')
        if ($scenario -eq 'upgrade') {
            Invoke-TestCompose @('run', '--rm', 'migrate', '-target=3', 'migrate')
        }
        # Nove migracije očekivano su pending; postojeće checksumove i dalje provjeravamo.
        Invoke-TestCompose @('run', '--rm', 'migrate', '-ignoreMigrationPatterns=*:pending', 'validate')
        Invoke-TestCompose @('run', '--rm', 'migrate')
        Invoke-TestCompose @('run', '--rm', 'migrate', 'validate')

        $seed = Get-Content -LiteralPath $seedPath -Raw -Encoding utf8
        Invoke-TestSql ("SET ROLE lds_app;`n" + $seed) | Out-Host
        Invoke-TestSql ("SET ROLE lds_app;`n" + $seed) | Out-Host
        Assert-TestSql 'SELECT count(*) FROM users;' '4'
        Assert-TestSql 'SELECT count(*) FROM oauth_accounts;' '4'

        $binding = & docker compose -p $testProject -f $composePath port db 5432
        if ($LASTEXITCODE -ne 0) { throw 'Ne mogu dohvatiti testni port.' }
        $env:LDS_DB_URL = 'jdbc:postgresql://' + $binding.Trim() + '/lds_user_test'
        Push-Location $backendPath
        try {
            & mvn -B -Pintegration verify
            if ($LASTEXITCODE -ne 0) { throw 'Maven provjere nisu prošle.' }
        } finally {
            Pop-Location
        }

        Invoke-TestSql "BEGIN; INSERT INTO users (username, name, gender, date_of_birth, city_id) VALUES ('persistence-check', 'Trajna pohrana', 'tekst', '2000-01-01', (SELECT min(id) FROM sif_city)); INSERT INTO user_sports (user_id, sport_id, skill_level_id) SELECT id, (SELECT min(id) FROM sports), (SELECT min(id) FROM sif_skill_level) FROM users WHERE username = 'persistence-check'; INSERT INTO oauth_accounts (user_id, provider, provider_user_id) SELECT id, 'google', 'test-persistence-check' FROM users WHERE username = 'persistence-check'; COMMIT;" | Out-Host
        Invoke-TestCompose @('restart', 'db')
        Invoke-TestCompose @('up', '-d', '--wait', 'db')
        Assert-TestSql "SELECT count(*) FROM users WHERE username = 'persistence-check';" '1'
        Invoke-TestCompose @('down', '--volumes', '--remove-orphans')
    }
} finally {
    # Uklanja se samo volumen nasumično imenovanog testnog projekta.
    & docker compose -p $testProject -f $composePath down --volumes --remove-orphans
    foreach ($variable in $savedEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($variable, $savedEnvironment[$variable], 'Process')
    }
    $OutputEncoding = $savedOutputEncoding
}
