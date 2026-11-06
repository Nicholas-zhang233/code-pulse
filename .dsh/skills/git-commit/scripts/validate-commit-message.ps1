<#
.SYNOPSIS
    校验 git commit message 是否符合 code-pulse 项目规范。

.DESCRIPTION
    检查首行 type(scope): 中文摘要 格式、长度、空行、正文条目、trailer 等。
    格式/结构问题计入 FAIL 并以退出码 1 结束；建议类问题只输出 WARN，不影响退出码。
    仅使用 PowerShell 内置 cmdlet 与运算符，兼容 ConstrainedLanguage。

.PARAMETER Path
    commit message 文件路径（UTF-8）。与 -Message 二选一，同时提供时 Path 优先。

.PARAMETER Message
    直接传入 message 文本（可含换行，方便管道或脚本调用）。

.PARAMETER Quiet
    只输出最终结论，不逐条打印错误与警告。

.EXAMPLE
    pwsh -File scripts/validate-commit-message.ps1 -Path temp/commit-msg.txt

.EXAMPLE
    $msg = "feat(app): 添加专家模式支持`n`n- 添加工作流支持"
    pwsh -File scripts/validate-commit-message.ps1 -Message $msg
#>
param(
    [string]$Path,
    [string]$Message,
    [switch]$Quiet
)

$ErrorActionPreference = 'Stop'

if ($Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        Write-Host "ERROR: 找不到消息文件：$Path" -ForegroundColor Red
        exit 2
    }
    $text = Get-Content -LiteralPath $Path -Raw -Encoding UTF8
}
elseif ($Message) {
    $text = $Message
}
else {
    Write-Host "ERROR: 请提供 -Path <文件> 或 -Message <文本>" -ForegroundColor Red
    exit 2
}

if ((-not $text) -or ($text -match '^\s*$')) {
    Write-Host "FAIL: 提交信息为空" -ForegroundColor Red
    exit 1
}

# 统一换行并去掉尾部空行
$text = $text -replace "`r`n", "`n" -replace "`r", "`n"
$lines = $text -split "`n"
while ($lines.Count -gt 1 -and $lines[$lines.Count - 1] -match '^\s*$') {
    $lines = $lines[0..($lines.Count - 2)]
}

$failures = @()
$warnings = @()

# ---- 首行：type(scope): 中文摘要 ----
$headerPattern = '^(feat|fix|refactor|docs|style)\(([a-z0-9][a-z0-9-]*)\): (.+)$'
if ($lines[0] -cnotmatch $headerPattern) {
    $failures += "首行不符合 <type>(<scope>): 中文摘要 格式（type 仅限 feat/fix/refactor/docs/style，scope 小写）：$($lines[0])"
}
else {
    $desc = $Matches[3].Trim()

    if ($lines[0].Length -gt 50) {
        $failures += "首行超过 50 字符（当前 $($lines[0].Length) 字符）"
    }
    if ($lines[0] -match '[。..!！?？]$') {
        $failures += '首行不得以句号、问号或感叹号结尾'
    }
    if ($desc -notmatch '[\u4e00-\u9fa5]') {
        $failures += "摘要必须以中文书写：$desc"
    }

    $hanText = $desc -replace '[^\u4e00-\u9fa5]', ''
    $hanCount = $hanText.Length
    if ($hanCount -lt 8 -or $hanCount -gt 25) {
        $warnings += "摘要汉字数为 $hanCount，建议 8–25 个汉字"
    }

    $whole = $desc -replace '[^\u4e00-\u9fa5]', ''
    if ($whole -match '(若干|相关|部分|一些)') {
        $warnings += '摘要含空泛词（若干/相关/部分/一些），建议写具体对象'
    }
    if ($desc -match '[\u4e00-\u9fa5] +[A-Za-z0-9]|[A-Za-z0-9] +[\u4e00-\u9fa5]') {
        $warnings += '摘要中英文之间存在空格（历史两种写法并存，保持一致即可）'
    }
}

# ---- 空行与正文 ----
$bodyStart = -1
if ($lines.Count -eq 1) {
    $failures += '正文至少需要 1 条以 "- " 开头的要点'
}
elseif ($lines[1] -notmatch '^\s*$') {
    $failures += '首行之后必须空一行，再开始写正文条目'
}
else {
    $bodyStart = 2
}

$bullets = @()
if ($bodyStart -ge 2) {
    if ($lines.Count -le $bodyStart) {
        $failures += '正文至少需要 1 条以 "- " 开头的要点'
    }
    else {
        for ($i = $bodyStart; $i -lt $lines.Count; $i++) {
            $line = $lines[$i]
            $lineNo = $i - $bodyStart + 1

            if ($line -match '^\s*$') {
                $failures += "正文第 $lineNo 行为空行，条目之间不要空行"
                continue
            }
            if ($line -notmatch '^- .+') {
                $failures += "正文第 $lineNo 行必须以 `- ` 开头：$line"
                continue
            }

            $bullets += $line
            if ($line -match '[。.]$') {
                $failures += "正文第 $lineNo 行以句号结尾，请去掉：$line"
            }
            if ($line.Length -gt 100) {
                $warnings += "正文第 $lineNo 行较长（$($line.Length) 字符），建议精简"
            }
        }

        $count = $bullets.Count
        if ($count -gt 0 -and $count -gt 12) {
            $failures += "正文条目过多（$count 条，最多 12 条）"
        }
        elseif ($count -gt 0 -and ($count -lt 5 -or $count -gt 8)) {
            $warnings += "正文 $count 条，建议 5–8 条"
        }
        if ($count -eq 0) {
            $failures += '正文至少需要 1 条以 "- " 开头的要点'
        }
    }
}

# ---- trailer ----
if ($text -match '(?m)^(Co-authored-by|Signed-off-by|Reviewed-by|BREAKING CHANGE|Closes #)') {
    $warnings += '消息包含 trailer；本仓库历史提交不使用 trailer，非必要请删除'
}

# ---- 输出 ----
if (-not $Quiet) {
    foreach ($f in $failures) { Write-Host "FAIL: $f" -ForegroundColor Red }
    foreach ($w in $warnings) { Write-Host "WARN: $w" -ForegroundColor Yellow }
}

if ($failures.Count -gt 0) {
    if (-not $Quiet) {
        Write-Host ("FAILED: {0} 个错误，{1} 个警告" -f $failures.Count, $warnings.Count) -ForegroundColor Red
    }
    exit 1
}

if (-not $Quiet) {
    Write-Host ("PASS: 提交信息符合 code-pulse 规范（正文 {0} 条）" -f $bullets.Count) -ForegroundColor Green
}
exit 0
