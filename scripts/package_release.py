"""Create a clean archive after source inspection, excluding build artifacts/history."""
import argparse
import sys
import zipfile
from pathlib import Path
from release_check import ROOT, inspect, source_files

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--output', required=True, type=Path)
args = parser.parse_args()
output = args.output.resolve()
if output.is_relative_to(ROOT):
    sys.exit('Choose an output path outside the project.')
count, findings = inspect(ROOT)
if findings:
    for path, line, label in findings:
        print(f'{path}:{line}: {label}')
    sys.exit('Release blocked: inspect the listed files first.')
output.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
    for path, relative in source_files(ROOT):
        archive.write(path, Path('chronicle-studio') / relative)
print(f'Packaged {count} reviewed source files into {output.name}.')
