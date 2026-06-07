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

Usage:
  scripts/generate-reprints.py            # generate all; print a summary
  scripts/generate-reprints.py --set DMU  # only the given target set
  scripts/generate-reprints.py --dry-run  # report what would be written
"""

from __future__ import annotations

import argparse
import json
import re
import time
from collections import defaultdict
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFINITIONS_ROOT = REPO_ROOT / "mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions"
PRINTINGS_CACHE = Path.home() / ".cache" / "scryfall" / "printings"
SET_CACHE = Path.home() / ".cache" / "scryfall"
CACHE_TTL_DAYS = 30

CARD_DSL_RE = re.compile(r'\bcard\(\s*"([^"]+)"')
PRINTING_NAME_RE = re.compile(r'\bname\s*=\s*"([^"]+)"')

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


def scaffolded_sets() -> set[str]:
    return {d.name for d in DEFINITIONS_ROOT.iterdir() if d.is_dir()}


def scan_definitions() -> tuple[dict[str, str], dict[str, set[str]]]:
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


def render(name: str, set_code: str, p: dict, set_entry: dict | None) -> str:
    val = f"{pascal(name)}Reprint"
    artist = set_entry.get("artist") if set_entry else None
    rarity = RARITY_MAP.get(p.get("rarity", ""), "COMMON")
    # Kotlin package segments can't start with a digit (e.g. 8ed, 5dn) — backtick-escape them.
    seg = f"`{set_code}`" if set_code[:1].isdigit() else set_code
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


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--set", help="only generate reprints for this target set code")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    only = args.set.lower() if args.set else None

    scaffolded = scaffolded_sets()
    canonical, reprints = scan_definitions()

    written = 0
    skipped_exists = 0
    skipped_nocache = 0
    by_set: dict[str, int] = defaultdict(int)

    for name in sorted(canonical):
        printings = load_card_printings(name)
        if printings is None:
            continue
        canon_set = canonical[name]
        expected = expected_canonical(printings)
        rows = reprints.get(name, set())
        seen_sets: set[str] = set()
        for p in printings:
            sc = p.get("set_code")
            if not sc or sc in seen_sets:
                continue
            seen_sets.add(sc)
            if sc not in scaffolded or sc == canon_set or sc in rows:
                continue
            if expected and sc == expected.get("set_code"):
                continue  # canonical-to-be slot (relocate, not a reprint)
            if only and sc != only:
                continue
            primary = primary_printing(printings, sc)
            if not primary or not primary.get("scryfall_id") or not primary.get("oracle_id"):
                skipped_nocache += 1
                continue
            out_dir = DEFINITIONS_ROOT / sc / "cards"
            if not out_dir.is_dir():
                continue
            out_file = out_dir / f"{pascal(name)}Reprint.kt"
            if out_file.exists():
                skipped_exists += 1
                continue
            content = render(name, sc, primary, set_cache_entry(sc, name))
            if args.dry_run:
                written += 1
                by_set[sc] += 1
                continue
            out_file.write_text(content, encoding="utf-8")
            written += 1
            by_set[sc] += 1

    print(f"{'(dry-run) would write' if args.dry_run else 'wrote'} {written} reprint files "
          f"across {len(by_set)} sets")
    print(f"skipped: {skipped_exists} already exist, {skipped_nocache} missing ids in cache")
    for sc in sorted(by_set, key=lambda s: (-by_set[s], s)):
        print(f"  {sc.upper():<6} {by_set[sc]}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
