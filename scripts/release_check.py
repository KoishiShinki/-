"""Inspect releasable source only; never print a matched credential value."""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SKIP_DIRS = {'.git', 'node_modules', 'target', 'dist', 'coverage', 'test-results',
             'playwright-report', '__pycache__', '.venv'}
PRIVATE_DIRS = {'data', 'uploads', 'logs', '.idea', '.vscode', '.agents', '.codex', 'work'}
# New raster/font assets require deliberate review before adding exact paths here.
REVIEWED_BINARY_PATHS: set[str] = set()
SENSITIVE_EXTENSIONS = {'.pem', '.key', '.p12', '.pfx', '.jks', '.db', '.dump',
                        '.doc', '.docx', '.pdf', '.log', '.zip', '.7z', '.rar',
                        '.csv', '.tsv', '.sqlite', '.sqlite3', '.bak'}
PATTERNS = {
    'private key': re.compile(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),
    'provider credential': re.compile(r'\b(?:sk-(?:proj-)?[A-Za-z0-9_-]{20,}|gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}|AKIA[A-Z0-9]{16})\b'),
    'credential in URL': re.compile(r'(?i)https?://[^\s/:]+:[^\s/@]+@'),
    'absolute personal path': re.compile(r'(?i)(?:[A-Z]:[\\/](?:Users|工作表格)[\\/]|/Us' r'ers/[^/\s]+/|/ho' r'me/[^/\s]+/)'),
}
CONFIG_SECRET = re.compile(r'''(?im)^[ \t]*(?:[\w.-]*(?:password|api[-_]?key|secret|token))[ \t]*[:=][ \t]*["']?([^\r\n#"']+)''')
ROOT_FILES = {'.gitignore', '.gitattributes', '.editorconfig', '.env.example',
              'README.md', 'README.en.md', 'LICENSE', 'SECURITY.md', 'CONTRIBUTING.md',
              'THIRD_PARTY_NOTICES.md', 'compose.yml', 'docker-compose.yml'}
SOURCE_PREFIXES = ('backend/src/', 'frontend/src/', 'frontend/public/', 'docs/',
                   'scripts/', 'licenses/', '.github/workflows/')
MODULE_FILES = {'backend/pom.xml', 'backend/Dockerfile', 'backend/.dockerignore',
                'frontend/package.json', 'frontend/package-lock.json',
                'frontend/index.html', 'frontend/vite.config.ts', 'frontend/vite.config.js',
                'frontend/tsconfig.json', 'frontend/tsconfig.app.json', 'frontend/tsconfig.node.json',
                'frontend/eslint.config.js', 'frontend/.env.example', 'frontend/.gitignore',
                'frontend/Dockerfile', 'frontend/.dockerignore', 'frontend/nginx.conf'}

def expected_source(rel: Path):
    name = rel.as_posix()
    return name in ROOT_FILES or name in MODULE_FILES or name.startswith(SOURCE_PREFIXES)

def source_files(root: Path):
    for path in sorted(root.rglob('*')):
        rel = path.relative_to(root)
        if any(part in SKIP_DIRS for part in rel.parts):
            continue
        # Include symlinked directories too: Path.rglob does not descend into
        # them, and a directory symlink must be reported as an unsafe entry.
        if path.is_file() or path.is_symlink():
            yield path, rel

def inspect(root: Path):
    findings = []
    count = 0
    for path, rel in source_files(root):
        count += 1
        if path.is_symlink() or not path.resolve().is_relative_to(root):
            findings.append((str(rel), 0, 'symlink or source outside project'))
            continue
        if not expected_source(rel):
            findings.append((str(rel), 0, 'file outside release allowlist'))
        if any(part in PRIVATE_DIRS for part in rel.parts):
            findings.append((str(rel), 0, 'private/runtime directory'))
        if path.name.startswith('.env') and path.name != '.env.example':
            findings.append((str(rel), 0, 'local environment file'))
        if path.suffix.lower() in SENSITIVE_EXTENSIONS:
            findings.append((str(rel), 0, 'non-source/private file type'))
        if rel.as_posix() in REVIEWED_BINARY_PATHS:
            continue
        try:
            raw = path.read_bytes()
            if b'\x00' in raw:
                findings.append((str(rel), 0, 'unreviewed binary'))
                continue
            content = raw.decode('utf-8')
        except UnicodeError:
            findings.append((str(rel), 0, 'unreviewed binary'))
            continue
        if path.suffix.lower() == '.sql' and '/src/test/' not in '/' + rel.as_posix():
            if re.search(r'(?im)^\s*(?:INSERT\s+INTO|REPLACE\s+INTO|COPY\s+\w+\s+FROM)\b', content):
                findings.append((str(rel), 0, 'SQL contains data; release requires schema only'))
        for label, pattern in PATTERNS.items():
            for match in pattern.finditer(content):
                findings.append((str(rel), content.count('\n', 0, match.start()) + 1, label))
        if path.suffix.lower() in {'.yml', '.yaml', '.properties', '.toml', '.env'} or path.name == '.env.example':
            for match in CONFIG_SECRET.finditer(content):
                value = match.group(1).strip()
                fallback = re.search(r'\$\{[^}:]+:([?+\-]?)([^}]*)\}', value)
                if fallback and fallback.group(1) != '?' and fallback.group(2).strip():
                    findings.append((str(rel), content.count('\n', 0, match.start()) + 1, 'literal secret fallback'))
                if value and not value.startswith(('${', '$', '<', 'CHANGE_', 'REPLACE_', 'your-')):
                    findings.append((str(rel), content.count('\n', 0, match.start()) + 1, 'literal secret configuration'))
    return count, findings

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=ROOT)
    args = parser.parse_args()
    count, findings = inspect(args.root.resolve())
    for path, line, label in findings:
        print(f'{path}:{line}: {label}')
    print(f'Checked {count} source files; {len(findings)} finding(s). Values are intentionally hidden.')
    return 1 if findings else 0

if __name__ == '__main__':
    sys.exit(main())
