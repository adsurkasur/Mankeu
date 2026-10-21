#!/usr/bin/env python3
"""Fails when a Markdown file links to a repository file that does not exist.

Checks relative links `[text](path)` and image links in every tracked .md file (anchors and
external URLs are ignored). Run from anywhere: `py -I scripts/check_doc_links.py`.
"""
import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LINK = re.compile(r'!?\[[^\]]*\]\(([^)\s]+)(?:\s+"[^"]*")?\)')
FENCE = re.compile(r'```.*?```', re.S)


def main() -> int:
    files = subprocess.run(['git', 'ls-files', '*.md'], cwd=ROOT, capture_output=True, text=True, check=True).stdout.split()
    broken = []
    for rel in files:
        text = open(os.path.join(ROOT, rel), encoding='utf-8-sig').read()
        text = FENCE.sub('', text)
        for target in LINK.findall(text):
            if re.match(r'^[a-z]+:', target) or target.startswith('#'):
                continue
            path = target.split('#')[0]
            if not path:
                continue
            resolved = os.path.normpath(os.path.join(ROOT, os.path.dirname(rel), path.replace('%20', ' ')))
            if not os.path.exists(resolved):
                broken.append(f'{rel}: {target}')
    for line in broken:
        print(line)
    print(f'{len(files)} Markdown files, {len(broken)} broken links')
    return 1 if broken else 0


if __name__ == '__main__':
    sys.exit(main())
