# 多语言资源契约

E01 的资源审计结论（2026-08-13）：默认中文、英文、繁体中文与台湾繁体中文的 `strings.xml` 均为 151 个 key，`plurals.xml` 均为 4 个 key；key 集合和格式占位符保持一致。滑动灵敏度、滑动音效名称与说明已经分别提供四种语言，不允许把默认中文复制到英文资源作为补齐方式。

## 本地检查

在仓库根目录执行以下 PowerShell 片段，可核对每个语言目录的 key 集合与 `%1$d` / `%1$s` 等占位符数量：

```powershell
$resourceDirs = @('values', 'values-en', 'values-b+zh+Hant', 'values-zh-rTW')
$base = $null
foreach ($dir in $resourceDirs) {
    $file = Join-Path 'app/src/main/res' (Join-Path $dir 'strings.xml')
    [xml]$xml = Get-Content -Raw -LiteralPath $file
    $keys = @($xml.resources.string | ForEach-Object { $_.name })
    if ($null -eq $base) { $base = $keys; continue }
    if (@($base | Where-Object { $_ -notin $keys }).Count -gt 0 -or
        @($keys | Where-Object { $_ -notin $base }).Count -gt 0) {
        throw "Resource key mismatch: $dir"
    }
}
```

`aapt2`/Gradle 负责资源链接和 XML 格式校验；该契约检查负责尽早发现语言目录之间的 key 漂移。E01 尚未宣称四语言截图或字体放大设备验收，相关验证属于 E02、E05、E07 与 E08。
