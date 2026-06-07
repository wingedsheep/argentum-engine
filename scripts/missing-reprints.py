#!/usr/bin/env python3
"""
Corpus-wide reprint audit. For every implemented card, check Scryfall for
other *scaffolded* sets that printed it but lack a `Printing(...)` row.

This is the batch version of `check-card-printing.py`. It:
  - scans `mtg-sets/.../definitions/<set>/cards/*.kt` for every canonical
    `card("Name") { ... }` declaration and every `Printing(...)` row,
  - for each canonical card, reads its Scryfall printing list (oldest ->
    newest) from the shared `~/.cache/scryfall/printings/` cache used by
    check-card-printing.py (fetching only cards that are missing/stale, unless
    --no-fetch),
  - reports, grouped by set, every scaffolded set that reprinted the card but
    has no reprint row yet ("reprints to add").

It also separately flags canonical drift (canonical not in the card's earliest
real printing) — that's a `coverage-relocate` job, not a reprint, so it's
listed apart and not counted as a missing reprint.

Usage:
  scripts/missing-reprints.py                 # full sweep, fetch missing
  scripts/missing-reprints.py --no-fetch      # cache-only (fast, may skip uncached)
  scripts/missing-reprints.py --set DSK       # only cards whose canonical lives in DSK
  scripts/missing-reprints.py --limit 50      # cap fetches (for a quick look)
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFINITIONS_ROOT = REPO_ROOT / "mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions"
CACHE_ROOT = Path.home() / ".cache" / "scryfall" / "printings"
SCRYFALL_BASE = "https://api.scryfall.com"
USER_AGENT = "argentum-engine-missing-reprints/1.0"
REQUEST_DELAY_SEC = 0.15
CACHE_TTL_DAYS = 30

# Only `card(...)` declarations participate in the Printing-row reprint system.
# Basic lands are defined per-set via `basicLand(...)` (each set scaffolds its
# own art variants), never as shared `Printing(...)` rows — exclude them.
CARD_DSL_RE = re.compile(r'\bcard\(\s*"([^"]+)"')
PRINTING_NAME_RE = re.compile(r'\bname\s*=\s*"([^"]+)"')

SCAFFOLDABLE_SET_TYPES = {
    "core", "expansion", "draft_innovation", "masters", "commander",
    "starter", "duel_deck", "from_the_vault", "premium_deck", "spellbook",
    "planechase", "archenemy", "vanguard", "treasure_chest", "alchemy",
    "funny", "remastered",
}
IGNORED_SET_CODES = {"om1"}


@dataclass(frozen=True)
class Printing:
    set_code: str
    set_name: str
    set_type: str
    collector_number: str
    released_at: str
    rarity: str
    oracle_id: str | None
    scryfall_id: str | None


def slugify(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")


def scryfall_get(url: str, *, max_retries: int = 5) -> dict:
    for attempt in range(max_retries):
        time.sleep(REQUEST_DELAY_SEC)
        req = urllib.request.Request(
            url, headers={"User-Agent": USER_AGENT, "Accept": "application/json"}
        )
        try:
            with urllib.request.urlopen(req) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except urllib.error.HTTPError as e:
            if e.code != 429 or attempt == max_retries - 1:
                raise
            retry_after = e.headers.get("Retry-After") if e.headers else None
            wait = float(retry_after) if retry_after and retry_after.isdigit() else 2 ** attempt
            time.sleep(wait)
    raise RuntimeError("unreachable")


def load_cached_printings(card_name: str) -> list[Printing] | None:
    cache_path = CACHE_ROOT / f"{slugify(card_name)}.json"
    if not cache_path.is_file():
        return None
    age_days = (time.time() - cache_path.stat().st_mtime) / 86400
    if age_days >= CACHE_TTL_DAYS:
        return None
    try:
        raw = json.loads(cache_path.read_text(encoding="utf-8"))
        return [Printing(**p) for p in raw]
    except (json.JSONDecodeError, TypeError):
        return None


def fetch_printings(card_name: str) -> list[Printing]:
    CACHE_ROOT.mkdir(parents=True, exist_ok=True)
    q = f'!"{card_name}"'
    url = (
        f"{SCRYFALL_BASE}/cards/search"
        f"?q={urllib.parse.quote(q)}&unique=prints&order=released&dir=asc"
    )
    printings: list[Printing] = []
    while url:
        data = scryfall_get(url)
        for card in data.get("data", []):
            printings.append(
                Printing(
                    set_code=card.get("set", ""),
                    set_name=card.get("set_name", ""),
                    set_type=card.get("set_type", ""),
                    collector_number=card.get("collector_number", ""),
                    released_at=card.get("released_at", ""),
                    rarity=card.get("rarity", ""),
                    oracle_id=card.get("oracle_id"),
                    scryfall_id=card.get("id"),
                )
            )
        url = data.get("next_page") if data.get("has_more") else None
    (CACHE_ROOT / f"{slugify(card_name)}.json").write_text(
        json.dumps([p.__dict__ for p in printings], indent=2), encoding="utf-8"
    )
    return printings


def scaffolded_sets() -> set[str]:
    if not DEFINITIONS_ROOT.is_dir():
        return set()
    return {d.name for d in DEFINITIONS_ROOT.iterdir() if d.is_dir()}


def scan_definitions() -> tuple[dict[str, str], dict[str, set[str]]]:
    """Return (canonical[name]->set_code, reprints[name]->{set_codes})."""
    canonical: dict[str, str] = {}
    reprints: dict[str, set[str]] = defaultdict(set)
    for kt in DEFINITIONS_ROOT.glob("*/cards/*.kt"):
        text = kt.read_text(encoding="utf-8")
        set_code = kt.parts[-3]
        card_names = {m.group(1) for m in CARD_DSL_RE.finditer(text)}
        for name in card_names:
            canonical[name] = set_code
        if "Printing(" in text:
            for name in PRINTING_NAME_RE.findall(text):
                if name not in card_names:  # reprint row, not the canonical file
                    reprints[name].add(set_code)
    return canonical, reprints


def expected_canonical(printings: list[Printing]) -> Printing | None:
    for p in printings:
        if p.set_code in IGNORED_SET_CODES:
            continue
        if p.set_type in SCAFFOLDABLE_SET_TYPES:
            return p
    return printings[0] if printings else None


def main() -> int:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("--no-fetch", action="store_true", help="cache-only; skip uncached cards")
    parser.add_argument("--set", help="only cards whose canonical lives in this set code")
    parser.add_argument("--limit", type=int, default=0, help="cap number of Scryfall fetches")
    args = parser.parse_args()

    scaffolded = scaffolded_sets()
    canonical, reprints = scan_definitions()

    only_set = args.set.lower() if args.set else None
    names = sorted(
        n for n, sc in canonical.items() if only_set is None or sc == only_set
    )

    missing_by_set: dict[str, list[tuple[str, str]]] = defaultdict(list)  # set -> [(card, canonical_set)]
    drift: list[tuple[str, str, str]] = []  # (card, actual_canonical, expected_set)
    fetched = uncovered = 0

    for i, name in enumerate(names):
        printings = load_cached_printings(name)
        if printings is None:
            if args.no_fetch or (args.limit and fetched >= args.limit):
                uncovered += 1
                continue
            try:
                printings = fetch_printings(name)
                fetched += 1
            except urllib.error.HTTPError:
                uncovered += 1
                continue
            if fetched % 25 == 0:
                print(f"  ...fetched {fetched} (at {name})", file=sys.stderr)
        if not printings:
            uncovered += 1
            continue

        canon_set = canonical[name]
        expected = expected_canonical(printings)
        if expected and expected.set_code != canon_set and expected.set_code in scaffolded:
            drift.append((name, canon_set, expected.set_code))

        rows = reprints.get(name, set())
        for p in printings:
            sc = p.set_code
            if sc not in scaffolded:
                continue
            if sc == canon_set or sc in rows:
                continue
            if expected and sc == expected.set_code:
                continue  # this is the expected-canonical slot (drift, handled above)
            missing_by_set[sc].append((name, canon_set))

    total_missing = sum(len(v) for v in missing_by_set.values())
    print(f"\n=== Missing reprint rows: {total_missing} across {len(missing_by_set)} sets ===")
    for sc in sorted(missing_by_set, key=lambda s: (-len(missing_by_set[s]), s)):
        cards = sorted(missing_by_set[sc])
        print(f"\n{sc.upper()}  ({len(cards)} reprints to add)")
        for name, canon_set in cards:
            print(f"    {name}  (canonical: {canon_set.upper()})")

    if drift:
        print(f"\n=== Canonical drift (relocate, NOT a reprint): {len(drift)} ===")
        for name, actual, expected in sorted(drift):
            print(f"    {name}: canonical in {actual.upper()}, earliest printing is {expected.upper()}")

    print(f"\nScanned {len(names)} cards | fetched {fetched} | uncovered {uncovered}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
