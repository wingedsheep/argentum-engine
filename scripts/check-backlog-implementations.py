#!/usr/bin/env python3
"""
Verify that every `- [ ]` entry in `backlog/sets/*/cards.md` is genuinely
unimplemented, by cross-referencing card names against the corresponding
`mtg-sets/.../<set>/cards/` Kotlin sources.

A card counts as implemented if its display name matches either:
  - a `card("Name") { ... }` DSL declaration (`CardDefinition`)
  - a `name = "Name",` field on a `Printing(...)` row (reprints)

DFC / Adventure entries in the backlog use `Front // Back` — only the front
face is matched against Kotlin names, since that is what the engine registers.

Usage:
  scripts/check-backlog-implementations.py             # report drift, exit 1 if any
  scripts/check-backlog-implementations.py --check     # same as default
  scripts/check-backlog-implementations.py --fix       # tick `[x]` for implemented cards
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
BACKLOG_GLOB = "backlog/sets/*/cards.md"
DEFINITIONS_ROOT = REPO_ROOT / "mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions"

SET_CODE_RE = re.compile(r"^#\s+.*\(([A-Z]+)\)\s+-\s+Card Checklist", re.MULTILINE)
UNCHECKED_LINE_RE = re.compile(r"^- \[ \] (.+)$")
CARD_DSL_RE = re.compile(r'\bcard\(\s*"([^"]+)"')
PRINTING_NAME_RE = re.compile(r'\bname\s*=\s*"([^"]+)"')


@dataclass
class SetReport:
    backlog_path: Path
    set_code: str | None
    cards_dir: Path | None
    unchecked: list[tuple[int, str]] = field(default_factory=list)  # (line_no, raw_name)
    implemented_names: set[str] = field(default_factory=set)
    drifters: list[tuple[int, str]] = field(default_factory=list)  # (line_no, raw_name)


def front_face(name: str) -> str:
    """Strip ` // back` suffix from DFC/adventure names."""
    return name.split(" // ", 1)[0].strip()


def scan_backlog(path: Path) -> tuple[str | None, list[tuple[int, str]]]:
    text = path.read_text(encoding="utf-8")
    set_code_match = SET_CODE_RE.search(text)
    set_code = set_code_match.group(1) if set_code_match else None
    unchecked: list[tuple[int, str]] = []
    for line_no, line in enumerate(text.splitlines(), start=1):
        m = UNCHECKED_LINE_RE.match(line)
        if m:
            unchecked.append((line_no, m.group(1).strip()))
    return set_code, unchecked


def scan_implementations(cards_dir: Path) -> set[str]:
    names: set[str] = set()
    if not cards_dir.is_dir():
        return names
    for kt in cards_dir.glob("*.kt"):
        text = kt.read_text(encoding="utf-8")
        names.update(CARD_DSL_RE.findall(text))
        # `Printing(...)` rows use `name = "..."`; CardDefinitions also sometimes
        # carry a `name = ` field. Both are safe to admit — false positives would
        # require a quoted name field on something that isn't a card, which the
        # codebase doesn't do.
        names.update(PRINTING_NAME_RE.findall(text))
    return names


def build_report(backlog_path: Path) -> SetReport:
    set_code, unchecked = scan_backlog(backlog_path)
    cards_dir = DEFINITIONS_ROOT / set_code.lower() / "cards" if set_code else None
    implemented = scan_implementations(cards_dir) if cards_dir else set()
    drifters = [
        (line_no, raw)
        for line_no, raw in unchecked
        if front_face(raw) in implemented
    ]
    return SetReport(
        backlog_path=backlog_path,
        set_code=set_code,
        cards_dir=cards_dir,
        unchecked=unchecked,
        implemented_names=implemented,
        drifters=drifters,
    )


def fix(report: SetReport) -> int:
    """Tick `[x]` for each drifting line. Returns count of edits made."""
    if not report.drifters:
        return 0
    lines = report.backlog_path.read_text(encoding="utf-8").splitlines(keepends=True)
    drifter_line_nos = {line_no for line_no, _ in report.drifters}
    edited = 0
    for idx in range(len(lines)):
        line_no = idx + 1
        if line_no in drifter_line_nos and lines[idx].startswith("- [ ] "):
            lines[idx] = "- [x] " + lines[idx][len("- [ ] "):]
            edited += 1
    report.backlog_path.write_text("".join(lines), encoding="utf-8")
    return edited


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group()
    group.add_argument("--check", action="store_true", help="report drift, exit 1 if any (default)")
    group.add_argument("--fix", action="store_true", help="tick `[x]` for implemented entries in place")
    args = parser.parse_args()

    files = sorted(REPO_ROOT.glob(BACKLOG_GLOB))
    if not files:
        print(f"No files matched {BACKLOG_GLOB}", file=sys.stderr)
        return 1

    reports = [build_report(p) for p in files]

    missing_set_code = [r for r in reports if r.set_code is None]
    drifting = [r for r in reports if r.drifters]

    if args.fix:
        total = 0
        for r in drifting:
            n = fix(r)
            total += n
            print(f"fixed {n} entr{'y' if n == 1 else 'ies'}: {r.backlog_path.relative_to(REPO_ROOT)}")
        for r in missing_set_code:
            print(
                f"skipped (no set code in header): {r.backlog_path.relative_to(REPO_ROOT)}",
                file=sys.stderr,
            )
        if total == 0:
            print("no implemented-but-unchecked entries found")
        else:
            print(f"\nticked {total} entr{'y' if total == 1 else 'ies'} across {len(drifting)} file(s)")
            print("run `just fix-backlog` to resync the `**Implemented:** N / M` headers")
        return 0

    # --check (default)
    if drifting:
        for r in drifting:
            rel = r.backlog_path.relative_to(REPO_ROOT)
            print(f"{rel} ({r.set_code}): {len(r.drifters)} implemented but unchecked")
            for line_no, raw in r.drifters:
                print(f"  L{line_no}: {raw}")
        total = sum(len(r.drifters) for r in drifting)
        print(
            f"\n{total} entr{'y' if total == 1 else 'ies'} across {len(drifting)} file(s) "
            f"should be `[x]`. Run `just fix-backlog-implementations` to tick them.",
            file=sys.stderr,
        )
        return 1
    for r in missing_set_code:
        print(f"warning: no set code in header: {r.backlog_path.relative_to(REPO_ROOT)}")
    print(f"all {len(reports)} backlog file(s) checked; no implemented-but-unchecked entries")
    return 0


if __name__ == "__main__":
    sys.exit(main())
