#!/usr/bin/env python3
"""
Generate `Printing(...)` reprint-row .kt files for the missing reprints found by
`missing-reprints.py`.

For every implemented `card("Name")` whose card was also printed in another
*scaffolded* set with no `Printing(...)` row yet, emit one file
`mtg-sets/.../definitions/<set>/cards/<CardName>Reprint.kt` holding a single
top-level `val <CardName>Reprint = Printing(...)`. Sets auto-discover these via
reflection (`findPrintingsIn`), so no registration is needed.

Metadata is read entirely from the local Scryfall caches written by
check-card-printing.py / card-status (no network):
  - per-card printings cache  ~/.cache/scryfall/printings/<slug>.json
        -> oracleId, scryfallId, collectorNumber, releaseDate, rarity, setCode
  - per-set cache             ~/.cache/scryfall/<set>.json
        -> artist, imageUri (matched by card name)

Cards with no fresh per-card cache entry are skipped (run missing-reprints.py
first to populate the cache). Files that already exist are left untouched.

Two things this can't fix are *named* at the end of a run instead of skipped
silently: cards with a stale cache, and cards whose canonical is filed in a later
set than their earliest real printing (those need the canonical relocated by a
human — a `Printing` row in the earlier set would be wrong).

Usage:
  scripts/generate-reprints.py            # generate all; print a summary
  scripts/generate-reprints.py --set DMU  # only rows written *into* DMU
  scripts/generate-reprints.py --dry-run  # report what would be written

Scoping a sweep to its own work
-------------------------------
`--set` filters the *target* set a row is written into; it does not limit which
canonicals are considered. A bare run therefore also emits every pre-existing
reprint gap in the corpus — an Assay-ready sweep of one set once found itself
holding 333 rows across 53 sets when only 94 belonged to the batch.

To generate only the rows owed by the canonicals *you* just authored:

  scripts/generate-reprints.py --cards-from names.txt   # one card name per line
  scripts/generate-reprints.py --card "Sift" --card "Isolate"
  scripts/generate-reprints.py --since main             # canonicals added vs a git ref

`--since` reads the working tree, not just commits, so it works mid-sweep while
the new card files are still uncommitted or untracked.
"""

from __future__ import annotations

import argparse
import importlib.machinery
import importlib.util
import json
import re
import sys
import time
from collections import defaultdict
from pathlib import Path

from set_dirs import (
    dir_for_codes,
    iter_card_files,
    root_for_set,
    scaffolded_set_codes,
    set_dir_codes,
)

REPO_ROOT = Path(__file__).resolve().parent.parent
SCRIPTS_DIR = Path(__file__).resolve().parent
PRINTINGS_CACHE = Path.home() / ".cache" / "scryfall" / "printings"
SET_CACHE = Path.home() / ".cache" / "scryfall"
CACHE_TTL_DAYS = 30


def _load_module(name: str, filename: str):
    """Import a sibling script that isn't a legal module name (`missing-reprints.py`).

    Registered in `sys.modules` *before* it executes, because `@dataclass` resolves a class's own
    module out of that table while decorating it and fails on a module that isn't there yet.
    Mirrors the loader in `assay-ready.py`.
    """
    loader = importlib.machinery.SourceFileLoader(name, str(SCRIPTS_DIR / filename))
    spec = importlib.util.spec_from_loader(name, loader)
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    loader.exec_module(module)
    return module


missing_reprints = _load_module("missing_reprints", "missing-reprints.py")

# Borrowed rather than re-declared: these three are one unit, and a local copy is exactly how this
# script used to truncate `card("Kongming, \"Sleeping Dragon\"")` to `Kongming, \` — a name that
# slugifies to a cache file that doesn't exist, so the card fell into the "no fresh cache" bucket
# and was reported as *stale* instead of as covered. See the comment above their definitions.
CARD_DSL_RE = missing_reprints.CARD_DSL_RE
PRINTING_NAME_RE = missing_reprints.PRINTING_NAME_RE
unescape_kotlin = missing_reprints.unescape_kotlin

# Guard against the naive bodies coming back: `[^"]+` cannot match a name with an escaped quote.
assert CARD_DSL_RE.search(r'card("Kongming, \"Sleeping Dragon\"")').group(1) == \
    r'Kongming, \"Sleeping Dragon\"', "CARD_DSL_RE no longer spans escaped quotes"
assert PRINTING_NAME_RE.search(r'name = "Pang Tong, \"Young Phoenix\"",').group(1) == \
    r'Pang Tong, \"Young Phoenix\"', "PRINTING_NAME_RE no longer spans escaped quotes"
assert unescape_kotlin(r'Kongming, \"Sleeping Dragon\"') == 'Kongming, "Sleeping Dragon"'

SCAFFOLDABLE_SET_TYPES = {
    "core", "expansion", "draft_innovation", "masters", "commander",
    "starter", "duel_deck", "from_the_vault", "premium_deck", "spellbook",
    "planechase", "archenemy", "vanguard", "treasure_chest", "alchemy",
    "funny", "remastered",
}
IGNORED_SET_CODES = {"om1"}
RARITY_MAP = {
    "common": "COMMON", "uncommon": "UNCOMMON", "rare": "RARE",
    "mythic": "MYTHIC", "special": "SPECIAL", "bonus": "BONUS",
}


def slugify(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")


def pascal(name: str) -> str:
    front = name.split(" // ", 1)[0]
    parts = re.split(r"[^A-Za-z0-9]+", front)
    pas = "".join(p[:1].upper() + p[1:] for p in parts if p)
    if pas and pas[0].isdigit():
        pas = "N" + pas
    return pas


def scan_definitions() -> tuple[dict[str, str], dict[str, set[str]]]:
    canonical: dict[str, str] = {}
    reprints: dict[str, set[str]] = defaultdict(set)
    codes = set_dir_codes()
    for kt in iter_card_files():
        text = kt.read_text(encoding="utf-8")
        set_code = codes.get(kt.parts[-3], kt.parts[-3])
        card_names = {unescape_kotlin(m.group(1)) for m in CARD_DSL_RE.finditer(text)}
        for name in card_names:
            canonical[name] = set_code
        if "Printing(" in text:
            for literal in PRINTING_NAME_RE.findall(text):
                name = unescape_kotlin(literal)
                if name not in card_names:
                    reprints[name].add(set_code)
    return canonical, reprints


def load_card_printings(name: str) -> list[dict] | None:
    path = PRINTINGS_CACHE / f"{slugify(name)}.json"
    if not path.is_file():
        return None
    if (time.time() - path.stat().st_mtime) / 86400 >= CACHE_TTL_DAYS:
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return None


_set_cache: dict[str, dict] = {}


def set_cache_entry(set_code: str, name: str) -> dict | None:
    if set_code not in _set_cache:
        path = SET_CACHE / f"{set_code}.json"
        if path.is_file():
            try:
                _set_cache[set_code] = json.loads(path.read_text(encoding="utf-8")).get("cards", {})
            except json.JSONDecodeError:
                _set_cache[set_code] = {}
        else:
            # Without the set cache every row gets `artist = null`; say so instead of writing it silently.
            print(
                f"warning: no set cache {path} — rows for {set_code} will have no artist; "
                f"run `scripts/card-status --set {set_code}` first",
                file=sys.stderr,
            )
            _set_cache[set_code] = {}
    cards = _set_cache[set_code]
    front = name.split(" // ", 1)[0]
    return cards.get(name) or cards.get(front)


def expected_canonical(printings: list[dict]) -> dict | None:
    for p in printings:
        if p.get("set_code") in IGNORED_SET_CODES:
            continue
        if p.get("set_type") in SCAFFOLDABLE_SET_TYPES:
            return p
    return printings[0] if printings else None


def primary_printing(printings: list[dict], set_code: str) -> dict | None:
    """The main printing of the card within `set_code` — prefer plain numeric
    collector numbers (skip showcase/variant suffixes), lowest number wins."""
    in_set = [p for p in printings if p.get("set_code") == set_code]
    if not in_set:
        return None
    numeric = [p for p in in_set if str(p.get("collector_number", "")).isdigit()]
    pool = numeric or in_set
    return min(pool, key=lambda p: int(p["collector_number"]) if str(p.get("collector_number", "")).isdigit() else 10**9)


def image_uri(p: dict, set_entry: dict | None) -> str | None:
    if set_entry and set_entry.get("image_uri"):
        return set_entry["image_uri"]
    sid = p.get("scryfall_id")
    if sid and len(sid) >= 2:
        return f"https://cards.scryfall.io/normal/front/{sid[0]}/{sid[1]}/{sid}.jpg"
    return None


def kt_str(s: str | None) -> str:
    if s is None:
        return "null"
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def render(name: str, set_code: str, p: dict, set_entry: dict | None, pkg_dir: str) -> str:
    val = f"{pascal(name)}Reprint"
    artist = set_entry.get("artist") if set_entry else None
    rarity = RARITY_MAP.get(p.get("rarity", ""), "COMMON")
    # Kotlin package segments can't start with a digit (e.g. 8ed, 5dn) — backtick-escape them.
    seg = f"`{pkg_dir}`" if pkg_dir[:1].isdigit() else pkg_dir
    pkg = f"com.wingedsheep.mtg.sets.definitions.{seg}.cards"
    lines = [
        f"package {pkg}",
        "",
        "import com.wingedsheep.sdk.model.Printing",
        "import com.wingedsheep.sdk.model.Rarity",
        "",
        f"/**",
        f" * {name} reprint in {set_code.upper()}. Canonical CardDefinition lives in its earliest set.",
        f" */",
        f"val {val} = Printing(",
        f'    oracleId = {kt_str(p.get("oracle_id"))},',
        f"    name = {kt_str(name)},",
        f'    setCode = "{set_code.upper()}",',
        f'    collectorNumber = {kt_str(str(p.get("collector_number", "")))},',
        f'    scryfallId = {kt_str(p.get("scryfall_id"))},',
        f"    artist = {kt_str(artist)},",
        f"    imageUri = {kt_str(image_uri(p, set_entry))},",
        f'    releaseDate = {kt_str(p.get("released_at"))},',
        f"    rarity = Rarity.{rarity},",
        ")",
        "",
    ]
    return "\n".join(lines)



def canonical_names_since(ref: str) -> set[str]:
    """Card names whose canonical `card("...")` lives in a file that differs from `ref`.

    Reads the *working tree*, not just committed history: a sweep runs this while its new
    card files are still uncommitted, and often untracked. Tracked-but-modified files come
    from `git diff --name-only <ref>`, brand-new ones from `git ls-files --others`.
    """
    import subprocess

    def git(*a: str) -> list[str]:
        out = subprocess.run(
            ["git", *a], cwd=REPO_ROOT, capture_output=True, text=True, check=True
        ).stdout
        return [ln for ln in out.splitlines() if ln.strip()]

    try:
        paths = set(git("diff", "--name-only", ref))
        paths |= set(git("ls-files", "--others", "--exclude-standard"))
    except subprocess.CalledProcessError as exc:
        sys.exit(f"--since {ref!r}: git failed ({exc.stderr.strip() or exc})")

    names: set[str] = set()
    for rel in paths:
        if "/definitions/" not in rel or not rel.endswith(".kt"):
            continue
        path = REPO_ROOT / rel
        if not path.is_file():
            continue  # deleted in the working tree
        text = path.read_text(encoding="utf-8")
        names |= {unescape_kotlin(m.group(1)) for m in CARD_DSL_RE.finditer(text)}
    return names


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--set", help="only write rows INTO this target set code "
                                  "(does not limit which canonicals are considered)")
    ap.add_argument("--card", action="append", metavar="NAME", default=[],
                    help="only rows owed by this canonical; repeatable")
    ap.add_argument("--cards-from", metavar="FILE",
                    help="only rows owed by the canonicals named in FILE, one per line "
                         "(blank lines and #-comments ignored)")
    ap.add_argument("--since", metavar="REF",
                    help="only rows owed by canonicals whose file differs from REF in the "
                         "working tree (includes untracked files, so it works mid-sweep)")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    only = args.set.lower() if args.set else None

    # Which canonicals to consider. Empty set == no restriction (the historical behaviour).
    wanted: set[str] = set(args.card)
    if args.cards_from:
        path = Path(args.cards_from)
        if not path.is_file():
            sys.exit(f"--cards-from: no such file: {path}")
        for line in path.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if line and not line.startswith("#"):
                wanted.add(line)
    if args.since:
        wanted |= canonical_names_since(args.since)
    scoped = bool(args.card or args.cards_from or args.since)
    if scoped and not wanted:
        print("no canonicals matched the scope — nothing to do")
        return 0

    scaffolded = scaffolded_set_codes()
    dir_for = dir_for_codes()
    canonical, reprints = scan_definitions()

    written = 0
    skipped_exists = 0
    skipped_nocache = 0
    stale: list[str] = []
    misplaced: list[tuple[str, str, str]] = []  # (card, canonical set today, set it belongs in)
    by_set: dict[str, int] = defaultdict(int)

    for name in sorted(canonical):
        if scoped and name not in wanted:
            continue
        printings = load_card_printings(name)
        if printings is None:
            # No per-card cache, or one past the 30-day TTL. Counted and named rather than skipped
            # quietly: a stale entry drops a card that genuinely owes rows, and a silent drop reads
            # as "this card needed nothing" — 40 of Jumpstart's 79 missing rows vanished this way.
            stale.append(name)
            continue
        canon_set = canonical[name]
        expected = expected_canonical(printings)
        expected_sc = expected.get("set_code") if expected else None
        rows = reprints.get(name, set())
        # A canonical filed in a later set than the card's earliest real printing produces *no*
        # output from the loop below: its `continue` is right (that set owes a relocated
        # `card(...)`, not a `Printing` row) but silent, so the run reads as "this card needs
        # nothing" — the same failure the `stale` list above was given a name for. Blossoming
        # Defense and Flame Lash were both first printed in KLD with their canonicals sitting in
        # ECL and BLB; `--set KLD` said "would write 4" with no hint those two were involved or
        # that a human had to decide anything. Collected once per card rather than inside the
        # per-printing loop, so a card whose earliest set already holds a stray `Printing` row (a
        # row where the canonical belongs) is reported too instead of being swallowed by the
        # `sc in rows` skip. With `--set X` the reader is working X, so keep the cards whose
        # canonical *belongs in* X — the relocations that command can act on.
        if expected_sc and expected_sc != canon_set and expected_sc in scaffolded:
            if only is None or expected_sc == only:
                misplaced.append((name, canon_set, expected_sc))
        seen_sets: set[str] = set()
        for p in printings:
            sc = p.get("set_code")
            if not sc or sc in seen_sets:
                continue
            seen_sets.add(sc)
            if sc not in scaffolded or sc == canon_set or sc in rows:
                continue
            if expected_sc and sc == expected_sc:
                continue  # canonical-to-be slot (relocate, not a reprint) — reported via `misplaced`
            if only and sc != only:
                continue
            primary = primary_printing(printings, sc)
            if not primary or not primary.get("scryfall_id") or not primary.get("oracle_id"):
                skipped_nocache += 1
                continue
            pkg_dir = dir_for.get(sc, sc)
            out_dir = root_for_set(pkg_dir) / pkg_dir / "cards"
            if not out_dir.is_dir():
                continue
            out_file = out_dir / f"{pascal(name)}Reprint.kt"
            if out_file.exists():
                skipped_exists += 1
                continue
            content = render(name, sc, primary, set_cache_entry(sc, name), pkg_dir)
            if args.dry_run:
                written += 1
                by_set[sc] += 1
                continue
            out_file.write_text(content, encoding="utf-8")
            written += 1
            by_set[sc] += 1

    if scoped:
        considered = sum(1 for n in canonical if n in wanted)
        print(f"scoped to {len(wanted)} canonical name(s); {considered} of them are "
              f"implemented in this checkout")
        unknown = sorted(n for n in wanted if n not in canonical)
        if unknown:
            shown = ", ".join(unknown[:8]) + (" …" if len(unknown) > 8 else "")
            print(f"NOTE: {len(unknown)} requested name(s) have no canonical here: {shown}")
    print(f"{'(dry-run) would write' if args.dry_run else 'wrote'} {written} reprint files "
          f"across {len(by_set)} sets")
    # Every bucket, so `written` reconciles without guessing where the rest of the corpus went.
    # The units differ and saying so beats implying they add up: the first three count *rows* (one
    # card can owe several), the last two count *cards* that produced no row at all.
    print(f"scanned {len(canonical)} canonical cards | rows: {written} to write, "
          f"{skipped_exists} already exist, {skipped_nocache} missing ids in cache "
          f"| cards needing a human: {len(misplaced)} misplaced canonical, {len(stale)} stale cache")
    if stale:
        shown = ", ".join(stale[:8]) + (" …" if len(stale) > 8 else "")
        print(
            f"WARNING: {len(stale)} cards had no fresh printing cache and were NOT considered: {shown}\n"
            f"         re-run scripts/missing-reprints.py (or fetch_printings per name) first"
        )
    if misplaced:
        print(
            f"WARNING: {len(misplaced)} canonicals are filed in a later set than the card's earliest\n"
            f"         real printing. Nothing is written for those: the earliest set needs the\n"
            f"         canonical `card(...)` RELOCATED into it, after which the set the canonical\n"
            f"         sits in today owes a `Printing` row instead. Each needs a human:"
        )
        for name, actual, belongs in sorted(misplaced):
            print(f"         {name}: canonical in {actual.upper()}, earliest printing is {belongs.upper()}")
    if by_set:
        print(f"per target set (sums to {written}):")
        for sc in sorted(by_set, key=lambda s: (-by_set[s], s)):
            print(f"  {sc.upper():<6} {by_set[sc]}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
