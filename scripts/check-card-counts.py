#!/usr/bin/env python3
"""
Verify (and optionally fix) the `**Implemented:** N / M` header in every
`backlog/sets/*/cards.md` file against the actual `[x]` and `[ ]` checkbox counts.

Two assertions per file:
  1. Header N must equal the count of `- [x] ...` lines.
  2. Header M must equal `[x] + [ ]` total (the full checklist size).

Usage:
  scripts/check-card-counts.py            # equivalent to --check
  scripts/check-card-counts.py --check    # report drift, exit 1 if any
  scripts/check-card-counts.py --fix      # rewrite headers in place
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
BACKLOG_GLOB = "backlog/sets/*/cards.md"

HEADER_RE = re.compile(r"^\*\*Implemented:\*\*\s*(\d+)\s*/\s*(\d+)\s*$", re.MULTILINE)
CHECKED_RE = re.compile(r"^- \[x\] ")
UNCHECKED_RE = re.compile(r"^- \[ \] ")


@dataclass
class Report:
    path: Path
    header_implemented: int | None
    header_total: int | None
    checked: int
    unchecked: int

    @property
    def actual_total(self) -> int:
        return self.checked + self.unchecked

    @property
    def has_header(self) -> bool:
        return self.header_implemented is not None

    @property
    def implemented_drift(self) -> bool:
        return self.has_header and self.header_implemented != self.checked

    @property
    def total_drift(self) -> bool:
        return self.has_header and self.header_total != self.actual_total

    @property
    def drifts(self) -> bool:
        return self.implemented_drift or self.total_drift


def scan(path: Path) -> Report:
    header_impl: int | None = None
    header_total: int | None = None
    checked = 0
    unchecked = 0
    for line in path.read_text(encoding="utf-8").splitlines():
        if header_impl is None:
            m = HEADER_RE.match(line)
            if m:
                header_impl = int(m.group(1))
                header_total = int(m.group(2))
                continue
        if CHECKED_RE.match(line):
            checked += 1
        elif UNCHECKED_RE.match(line):
            unchecked += 1
    return Report(path, header_impl, header_total, checked, unchecked)


def fix(report: Report) -> None:
    """Rewrite the header line in place. Caller must have verified `report.drifts`."""
    text = report.path.read_text(encoding="utf-8")
    new_header = f"**Implemented:** {report.checked} / {report.actual_total}"
    new_text, count = HEADER_RE.subn(new_header, text, count=1)
    if count != 1:
        raise RuntimeError(f"Could not locate header to rewrite in {report.path}")
    report.path.write_text(new_text, encoding="utf-8")


def format_drift(report: Report) -> str:
    rel = report.path.relative_to(REPO_ROOT)
    parts = []
    if report.implemented_drift:
        parts.append(
            f"implemented: header={report.header_implemented} actual=[x]={report.checked}"
        )
    if report.total_drift:
        parts.append(
            f"total: header={report.header_total} actual=[x]+[ ]={report.actual_total}"
        )
    return f"{rel}: " + "; ".join(parts)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group()
    group.add_argument("--check", action="store_true", help="report drift, exit 1 if any (default)")
    group.add_argument("--fix", action="store_true", help="rewrite drifting headers in place")
    args = parser.parse_args()

    files = sorted(REPO_ROOT.glob(BACKLOG_GLOB))
    if not files:
        print(f"No files matched {BACKLOG_GLOB}", file=sys.stderr)
        return 1

    reports = [scan(p) for p in files]

    missing_header = [r for r in reports if not r.has_header]
    drifting = [r for r in reports if r.drifts]

    if args.fix:
        for r in drifting:
            fix(r)
            print(f"fixed: {r.path.relative_to(REPO_ROOT)} → {r.checked} / {r.actual_total}")
        if missing_header:
            for r in missing_header:
                print(
                    f"skipped (no header): {r.path.relative_to(REPO_ROOT)} (would need manual init)",
                    file=sys.stderr,
                )
        if not drifting and not missing_header:
            print("all card-count headers in sync; nothing to fix")
        return 0

    # --check (default)
    if drifting:
        for r in drifting:
            print(format_drift(r))
        print(
            f"\n{len(drifting)} file(s) drift. Run `just fix-backlog` to sync.",
            file=sys.stderr,
        )
        return 1
    if missing_header:
        for r in missing_header:
            print(f"warning: no `**Implemented:**` header in {r.path.relative_to(REPO_ROOT)}")
    print(f"all {len(reports)} card-count headers in sync")
    return 0


if __name__ == "__main__":
    sys.exit(main())
