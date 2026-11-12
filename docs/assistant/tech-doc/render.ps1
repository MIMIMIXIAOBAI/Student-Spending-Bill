# render.ps1 — TECHNICAL-DOC.md -> TECHNICAL-DOC.pdf
#
# 从 docs/assistant/TECHNICAL-DOC.md 生成同目录 TECHNICAL-DOC.pdf：
#   Markdown -> HTML（含目录与页码占位）-> Edge headless 打印 PDF
#   -> 用 PyMuPDF 回读目录真实页码并二次打印 -> 盖页脚 -> pypdf 自校验。
#
# 依赖（Windows 自带 + Python 两个包）：
#   * Microsoft Edge（headless 打印，无需 LibreOffice / pandoc）
#   * Python 3.9+，pypdf、PyMuPDF（pip install pypdf pymupdf）
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File docs\assistant\tech-doc\render.ps1
#   powershell -ExecutionPolicy Bypass -File docs\assistant\tech-doc\render.ps1 -MinPages 20 -MaxPages 30
#
# 退出码：0 成功；非 0 失败（缺依赖、打印失败、页数越界、目录页码不稳定）。
param(
    [string]$Md  = (Join-Path $PSScriptRoot '..\TECHNICAL-DOC.md'),
    [string]$Pdf = (Join-Path $PSScriptRoot '..\TECHNICAL-DOC.pdf'),
    [int]$MinPages = 24,
    [int]$MaxPages = 30
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0

function Fail([string]$message) {
    Write-Host "[render][FAIL] $message" -ForegroundColor Red
    exit 1
}

$Md  = [System.IO.Path]::GetFullPath($Md)
$Pdf = [System.IO.Path]::GetFullPath($Pdf)
if (-not (Test-Path -LiteralPath $Md)) { Fail "Markdown not found: $Md" }

$python = (Get-Command python -ErrorAction SilentlyContinue)
if (-not $python) { Fail 'python not found on PATH' }

$edgeCandidates = @(
    'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe',
    'C:\Program Files\Microsoft\Edge\Application\msedge.exe'
)
$edge = $edgeCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $edge) { Fail 'Microsoft Edge not found (expected under Program Files)' }

$work = Join-Path $env:TEMP ('techdoc-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work -Force | Out-Null
$pyFile     = Join-Path $work 'render.py'
$html1      = Join-Path $work 'doc-1.html'
$html2      = Join-Path $work 'doc-2.html'
$headings   = Join-Path $work 'headings.json'
$pdf1       = Join-Path $work 'raw-1.pdf'
$pdf2       = Join-Path $work 'raw-2.pdf'
$pdfFinal   = Join-Path $work 'final.pdf'

$pySource = @'
import json, os, re, sys

CSS = r"""
@page { size: A4; margin: 18.6mm 18.5mm 20mm 18.5mm; }
* { box-sizing: border-box; }
html { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
body { margin: 0; color: #1b1b1b; font-family: "Microsoft YaHei", "SimHei", "PingFang SC", sans-serif;
       font-size: 10.5pt; line-height: 1.62; }
p { margin: 0 0 6px; text-align: left; }
strong { color: #10243b; }
a { color: #1a4f8a; text-decoration: none; }
code { font-family: Consolas, "Courier New", monospace; font-size: 8.9pt; background: #f2f5f8;
       border: 1px solid #e2e8ee; border-radius: 2px; padding: 0 3px; }
pre { margin: 6px 0 9px; padding: 7px 10px; background: #f6f8fa; border-left: 3px solid #b7c6d6;
      white-space: pre-wrap; word-break: break-word; page-break-inside: avoid; }
pre code { background: none; border: none; padding: 0; font-size: 8.25pt; line-height: 1.45; }
blockquote { margin: 8px 0; padding: 7px 11px; background: #fbf8ee; border-left: 3px solid #d3b062;
             color: #3f3f3f; page-break-inside: avoid; }
blockquote p { margin: 0; }
hr { border: none; border-top: 1px solid #dbe3ea; margin: 12px 0; }
ul, ol { margin: 4px 0 8px; padding-left: 2em; }
li { margin: 2px 0; }
table { width: 100%; border-collapse: collapse; margin: 6px 0 10px; font-size: 8.85pt; line-height: 1.45;
        page-break-inside: auto; break-after: avoid; page-break-after: avoid; }
thead { display: table-header-group; }
tr { page-break-inside: avoid; }
th, td { border: 1px solid #c6d3e0; padding: 3px 5px; vertical-align: top; text-align: left; }
th { background: #eaf1f8; color: #10243b; font-weight: 600; }
table.nw1 th:first-child, table.nw1 td:first-child { white-space: nowrap; }
tbody tr:nth-child(even) td { background: #fafcfe; }
h1 { font-size: 16.5pt; color: #10243b; margin: 0 0 10px; padding: 0 0 6px;
     border-bottom: 2px solid #23486f; line-height: 1.3; }
h1.chapter { page-break-before: always; }
h2 { font-size: 12.4pt; color: #17395c; margin: 15px 0 6px; padding-left: 8px;
     border-left: 3px solid #4a7ab5; line-height: 1.35; page-break-after: avoid; }
h3 { font-size: 11pt; color: #1d3b55; margin: 10px 0 4px; page-break-after: avoid; }
/* cover */
section.cover { min-height: 240mm; padding-top: 46mm; page-break-after: always; }
section.cover h1 { font-size: 25pt; border-bottom: none; margin: 0 0 4px; letter-spacing: 1px; }
section.cover h2 { font-size: 13.5pt; color: #3c5a7a; border-left: none; padding: 0; margin: 0 0 26px; font-weight: 500; }
section.cover table { width: 100%; font-size: 9.2pt; }
section.cover td:first-child { width: 27%; color: #3c5a7a; background: #f4f7fa; }
section.cover blockquote { margin-top: 16px; font-size: 9.4pt; }
/* toc */
section.toc { page-break-before: always; }
section.toc h1 { border-bottom: 2px solid #23486f; }
.toc-row { display: flex; align-items: baseline; margin: 0; font-size: 9.4pt; }
.toc-row.lvl1 { font-weight: 600; margin-top: 3px; color: #10243b; }
.toc-row.lvl2 { padding-left: 1.6em; color: #2c3e50; }
.toc-row .dots { flex: 1; border-bottom: 1px dotted #9fb0c2; margin: 0 6px; transform: translateY(-3px); }
.toc-row .pg { min-width: 2ch; text-align: right; font-variant-numeric: tabular-nums; }
.toc-mark { margin-top: 10px; font-size: 8pt; color: #9aa8b5; text-align: right; }
.toc-break { page-break-after: always; }
/* footer space is reserved by @page margin; nothing else needed */
"""

def esc(s):
    return s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')

def inline(s):
    parts = []
    def keep(m):
        parts.append('<code>' + esc(m.group(1)) + '</code>')
        return '\x00%d\x00' % (len(parts) - 1)
    s = re.sub(r'`([^`]+)`', keep, s)
    s = esc(s)
    s = re.sub(r'\*\*([^*]+)\*\*', r'<strong>\1</strong>', s)
    s = re.sub(r'\[([^\]]+)\]\(([^)]+)\)', r'<a href="\2">\1</a>', s)
    s = re.sub(r'\x00(\d+)\x00', lambda m: parts[int(m.group(1))], s)
    return s

def join_para(lines):
    out = ''
    for ln in lines:
        ln = ln.strip()
        if not out:
            out = ln
            continue
        prev = out[-1]
        nxt = ln[0]
        if (prev.isascii() and prev.isalnum() or prev in ')]},.;:') and \
           (nxt.isascii() and nxt.isalnum() or nxt in '([{'):
            out += ' ' + ln
        else:
            out += ln
    return out

def is_table_start(lines, i):
    if i + 1 >= len(lines):
        return False
    a = lines[i].strip()
    b = lines[i + 1].strip()
    return a.startswith('|') and a.endswith('|') and re.match(r'^\|[\s:|-]+\|$', b) is not None

def cells(row):
    row = row.strip()
    if row.startswith('|'):
        row = row[1:]
    if row.endswith('|'):
        row = row[:-1]
    return [c.strip() for c in row.split('|')]

def convert(md_text):
    lines = md_text.replace('\r\n', '\n').replace('\r', '\n').split('\n')
    html = []
    headings = []
    ch = 0
    sec = 0
    in_cover = False
    i = 0
    while i < len(lines):
        line = lines[i]
        s = line.strip()

        if s == '<!-- cover:start -->':
            in_cover = True
            html.append('<section class="cover">')
            i += 1
            continue
        if s == '<!-- cover:end -->':
            in_cover = False
            html.append('</section>')
            i += 1
            continue
        if s == '<!-- toc -->':
            html.append('@@TOC@@')
            i += 1
            continue

        m = re.match(r'^(#{1,3})\s+(.*)$', s)
        if m:
            level = len(m.group(1))
            text = m.group(2).strip()
            if in_cover:
                html.append('<h%d>%s</h%d>' % (level, inline(text), level))
            elif level == 1:
                ch += 1
                sec = 0
                hid = 's%d' % ch
                headings.append({'id': hid, 'title': text, 'level': 1})
                html.append('<h1 class="chapter" id="%s">%s</h1>' % (hid, inline(text)))
            elif level == 2:
                sec += 1
                hid = 's%d-%d' % (ch, sec)
                headings.append({'id': hid, 'title': text, 'level': 2})
                html.append('<h2 id="%s">%s</h2>' % (hid, inline(text)))
            else:
                html.append('<h3>%s</h3>' % inline(text))
            i += 1
            continue

        if s.startswith('```'):
            i += 1
            buf = []
            while i < len(lines) and not lines[i].strip().startswith('```'):
                buf.append(lines[i])
                i += 1
            i += 1
            html.append('<pre><code>%s</code></pre>' % esc('\n'.join(buf)))
            continue

        if is_table_start(lines, i):
            head = cells(lines[i])
            i += 2
            rows = []
            while i < len(lines) and lines[i].strip().startswith('|'):
                rows.append(cells(lines[i]))
                i += 1
            first_max = max([len(re.sub(r'[`*]', '', c)) for c in [head[0]] + [r[0] for r in rows if r]] or [0])
            cls = ' class="nw1"' if first_max <= 16 else ''
            out = ['<table%s><thead><tr>' % cls]
            out.extend('<th>%s</th>' % inline(c) for c in head)
            out.append('</tr></thead><tbody>')
            for r in rows:
                out.append('<tr>' + ''.join('<td>%s</td>' % inline(c) for c in r) + '</tr>')
            out.append('</tbody></table>')
            html.append(''.join(out))
            continue

        if s.startswith('>'):
            buf = []
            while i < len(lines) and lines[i].strip().startswith('>'):
                buf.append(re.sub(r'^\s*>\s?', '', lines[i]))
                i += 1
            html.append('<blockquote>' + ''.join('<p>%s</p>' % inline(x) for x in buf if x.strip()) + '</blockquote>')
            continue

        if s.startswith('- '):
            buf = []
            while i < len(lines) and lines[i].strip().startswith('- '):
                buf.append(lines[i].strip()[2:])
                i += 1
            html.append('<ul>' + ''.join('<li>%s</li>' % inline(x) for x in buf) + '</ul>')
            continue

        if re.match(r'^\d+\.\s', s):
            buf = []
            while i < len(lines) and re.match(r'^\d+\.\s', lines[i].strip()):
                buf.append(re.sub(r'^\d+\.\s+', '', lines[i].strip()))
                i += 1
            html.append('<ol>' + ''.join('<li>%s</li>' % inline(x) for x in buf) + '</ol>')
            continue

        if s == '---':
            html.append('<hr>')
            i += 1
            continue

        if s == '':
            i += 1
            continue

        buf = []
        while i < len(lines):
            t = lines[i].strip()
            if t == '' or t.startswith('#') or t.startswith('```') or t.startswith('>') \
               or t.startswith('|') or t.startswith('- ') or re.match(r'^\d+\.\s', t) or t == '---':
                break
            buf.append(lines[i])
            i += 1
        html.append('<p>%s</p>' % inline(join_para(buf)))

    toc = ['<section class="toc"><h1 class="toc-title">目 录</h1>']
    for h in headings:
        lvl = 'lvl1' if h['level'] == 1 else 'lvl2'
        toc.append('<div class="toc-row %s"><span class="t">%s</span>'
                   '<span class="dots"></span>'
                   '<span class="pg" data-toc="%s">00</span></div>'
                   % (lvl, inline(h['title']), h['id']))
    toc.append('</section>')
    body = '\n'.join(html).replace('@@TOC@@', '\n'.join(toc))
    return body, headings

def page_html(body):
    return ('<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8">'
            '<title>中央学生助手技术文档</title><style>' + CSS + '</style></head><body>'
            + body + '</body></html>')

def norm(s):
    return re.sub(r'\s+', '', s)

def read_pages(pdf):
    import pymupdf as fitz
    doc = fitz.open(pdf)
    texts = [norm(p.get_text('text')) for p in doc]
    doc.close()
    return texts

def cmd_html(md_path, html_out, headings_out):
    with open(md_path, encoding='utf-8') as f:
        md = f.read()
    body, headings = convert(md)
    with open(html_out, 'w', encoding='utf-8') as f:
        f.write(page_html(body))
    with open(headings_out, 'w', encoding='utf-8') as f:
        json.dump(headings, f, ensure_ascii=False, indent=1)
    print('HTML written: %d headings' % len(headings))

def cmd_fill(html_in, pdf_in, html_out, headings_in):
    with open(headings_in, encoding='utf-8') as f:
        headings = json.load(f)
    texts = read_pages(pdf_in)
    sentinel = '目录'
    start = None
    for idx, t in enumerate(texts):
        if sentinel in t:
            start = idx + 1
            break
    if start is None:
        raise SystemExit('sentinel not found in PDF (TOC title missing)')
    with open(html_in, encoding='utf-8') as f:
        html = f.read()
    mapping = {}
    cursor = start
    for h in headings:
        target = norm(h['title'])
        found = None
        for p in range(cursor, len(texts)):
            if target in texts[p]:
                found = p
                break
        if found is None:
            raise SystemExit('heading not located in PDF: ' + h['title'])
        mapping[h['id']] = found + 1
        cursor = found
        pat = re.compile(r'(<span class="pg" data-toc="%s">)00(</span>)' % re.escape(h['id']))
        html, n = pat.subn(lambda m: m.group(1) + str(found + 1) + m.group(2), html)
        if n != 1:
            raise SystemExit('TOC placeholder not replaced exactly once: ' + h['id'])
    with open(html_out, 'w', encoding='utf-8') as f:
        f.write(html)
    print('TOC filled: %d entries, first chapter at page %s' % (len(mapping), mapping.get('s1')))

def cmd_stamp(pdf_in, pdf_out, raw1):
    import pymupdf as fitz
    doc = fitz.open(pdf_in)
    total = doc.page_count
    font_path = None
    for cand in (r'C:\Windows\Fonts\simhei.ttf', r'C:\Windows\Fonts\msyh.ttf',
                 r'C:\Windows\Fonts\msyhbd.ttf', r'C:\Windows\Fonts\simsun.ttc'):
        if os.path.exists(cand):
            font_path = cand
            break
    for idx in range(1, total):
        page = doc[idx]
        text = '第 %d 页 · 共 %d 页' % (idx + 1, total)
        placed = False
        if font_path:
            try:
                page.insert_font(fontname='docfoot', fontfile=font_path)
                font = fitz.Font(fontfile=font_path)
                width = font.text_length(text, fontsize=8.5)
                x = (page.rect.width - width) / 2.0
                page.insert_text((x, page.rect.height - 24), text,
                                 fontname='docfoot', fontsize=8.5, color=(0.42, 0.42, 0.42))
                placed = True
            except Exception as exc:
                print('footer font failed (%s), falling back to helv' % exc)
        if not placed:
            text = 'Page %d / %d' % (idx + 1, total)
            width = fitz.get_text_length(text, fontname='helv', fontsize=8.5)
            x = (page.rect.width - width) / 2.0
            page.insert_text((x, page.rect.height - 24), text,
                             fontname='helv', fontsize=8.5, color=(0.42, 0.42, 0.42))
    doc.set_metadata({
        'title': '中央学生助手（CampusBill）技术文档',
        'author': 'CampusBill',
        'subject': '意图控制、架构约束与可复现性论证',
        'keywords': 'CampusBill, DeepSeek, intent control, reproducibility',
        'creator': 'docs/assistant/tech-doc/render.ps1',
        'producer': 'render.ps1 (Edge --headless --print-to-pdf)',
        'creationDate': "D:20261112000000+08'00'",
        'modDate': "D:20261112000000+08'00'",
    })
    doc.save(pdf_out, garbage=4, deflate=True)
    doc.close()
    print('footer stamped on %d pages (font: %s)' % (total - 1, font_path or 'helv'))

def cmd_stats(pdf_path):
    from pypdf import PdfReader
    reader = PdfReader(pdf_path)
    pages = len(reader.pages)
    data = open(pdf_path, 'rb').read()
    import hashlib
    import pymupdf
    doc = pymupdf.open(pdf_path)
    text = ''.join(p.get_text('text') for p in doc)
    doc.close()
    print(json.dumps({
        'pdf': pdf_path,
        'pages': pages,
        'bytes': len(data),
        'sha256': hashlib.sha256(data).hexdigest().upper(),
        'text_sha256': hashlib.sha256(text.encode('utf-8')).hexdigest().upper(),
    }, ensure_ascii=False))

def main():
    mode = sys.argv[1]
    if mode == 'html':
        cmd_html(sys.argv[2], sys.argv[3], sys.argv[4])
    elif mode == 'fill':
        cmd_fill(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5])
    elif mode == 'stamp':
        cmd_stamp(sys.argv[2], sys.argv[3], sys.argv[4])
    elif mode == 'stats':
        cmd_stats(sys.argv[2])
    else:
        raise SystemExit('unknown mode: ' + mode)

if __name__ == '__main__':
    main()
'@

Set-Content -LiteralPath $pyFile -Value $pySource -Encoding UTF8
$env:PYTHONIOENCODING = 'utf-8'

function Run-Py([string[]]$pyArgs) {
    & $python $pyFile @pyArgs
    if ($LASTEXITCODE -ne 0) { Fail ("python step failed: " + ($pyArgs -join ' ')) }
}

Write-Host "[render] markdown : $Md"
Run-Py @('html', $Md, $html1, $headings)

$userData = Join-Path $work 'edge-profile'
function Print-Pdf([string]$htmlFile, [string]$outPdf) {
    if (Test-Path -LiteralPath $outPdf) { Remove-Item -LiteralPath $outPdf -Force }
    $uri = ([System.Uri]$htmlFile).AbsoluteUri
    $edgeArgs = @(
        '--headless=new',
        '--disable-gpu',
        '--no-first-run',
        '--no-default-browser-check',
        '--no-pdf-header-footer',
        '--virtual-time-budget=8000',
        "--user-data-dir=$userData",
        "--print-to-pdf=$outPdf",
        $uri
    )
    $proc = Start-Process -FilePath $edge -ArgumentList $edgeArgs -PassThru -Wait
    if (-not (Test-Path -LiteralPath $outPdf)) {
        Fail "Edge did not produce PDF (exit $($proc.ExitCode)): $outPdf"
    }
    $size = (Get-Item -LiteralPath $outPdf).Length
    if ($size -lt 10000) { Fail "PDF too small ($size bytes): $outPdf" }
    Write-Host ("[render] printed  : {0} ({1:N0} bytes)" -f (Split-Path -Leaf $outPdf), $size)
}

Write-Host "[render] edge     : $edge"
Print-Pdf $html1 $pdf1

Run-Py @('fill', $html1, $pdf1, $html2, $headings)
Print-Pdf $html2 $pdf2

# 目录回填后页数必须稳定，否则页码不可信
$pages1 = (& $python -c "import sys, pypdf;print(len(pypdf.PdfReader(sys.argv[1]).pages))" $pdf1)
$pages2 = (& $python -c "import sys, pypdf;print(len(pypdf.PdfReader(sys.argv[1]).pages))" $pdf2)
if ($LASTEXITCODE -ne 0) { Fail 'pypdf page count failed' }
if ([int]$pages1 -ne [int]$pages2) {
    Fail "page count changed after TOC fill ($pages1 -> $pages2); TOC page numbers would be wrong"
}

Run-Py @('stamp', $pdf2, $pdfFinal, $pdf1)
$statsLine = (& $python $pyFile 'stats' $pdfFinal)
if ($LASTEXITCODE -ne 0) { Fail 'stats failed' }
$stats = $statsLine | ConvertFrom-Json
if ($stats.pages -lt $MinPages -or $stats.pages -gt $MaxPages) {
    Fail ("page count {0} out of range [{1}, {2}]" -f $stats.pages, $MinPages, $MaxPages)
}

Copy-Item -LiteralPath $pdfFinal -Destination $Pdf -Force
$finalSize = (Get-Item -LiteralPath $Pdf).Length
$finalHash = (Get-FileHash -LiteralPath $Pdf -Algorithm SHA256).Hash

Write-Host ''
Write-Host "[render] OK"
Write-Host ("  markdown : {0}" -f $Md)
Write-Host ("  pdf      : {0}" -f $Pdf)
Write-Host ("  pages    : {0} (allowed {1}-{2})" -f $stats.pages, $MinPages, $MaxPages)
Write-Host ("  bytes    : {0}" -f $finalSize)
Write-Host ("  sha256   : {0}" -f $finalHash)
Write-Host ("  text_sha : {0} (stable across re-runs; binary sha varies with PDF /ID)" -f $stats.text_sha256)
Write-Host ("  toc      : {0} entries with real page numbers" -f ((& $python -c "import json,sys;print(len(json.load(open(sys.argv[1],encoding='utf-8'))))" $headings) -join ''))

Remove-Item -LiteralPath $work -Recurse -Force
exit 0
