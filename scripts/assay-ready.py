#!/usr/bin/env python3
"""
The Assay-ready worklist for one set, split by where the work actually lands.

A card is *Assay-ready* for a set when both halves hold:

  - Argentum Assay reads it **whole** — its row in the baked verdict ledger
    (`game-server/src/main/resources/coverage/assay-verdicts.json`) carries no `kind`. A missing
    row is "unknown", never "no": those cards are reported separately as `absent`, because a
    multi-face card filed under a bare name never gets a verdict at all.
  - the set has no `card(...)`, `basicLand(...)` or `Printing(...)` for it yet.

That second half is why this script exists. `SetCoverageService.assayReady` counts a card as
authored when its name appears in the set's `cards` **+ `basicLands` + `printings`**, so a card
whose canonical has existed elsewhere in the corpus for years is badged ⚡ Assay-ready in this set
purely for want of a one-line reprint row — indistinguishable, in the badge, from a card nobody has
ever written. A sweep that authors canonicals and stops leaves half the badge behind; Jumpstart
reported "101 -> 0" and still had 79 rows outstanding the next day.

So the output is a four-way split, and each bucket is a different job:

  row-only      the canonical already exists in the corpus -> one `Printing(...)` row in THIS set
  elsewhere     no canonical yet, and the earliest real printing is a DIFFERENT scaffolded set
                -> author the canonical THERE, then a `Printing(...)` row here
  original      no canonical yet, and this set IS the earliest real printing -> author it here
  basic         a basic land -> a per-set `basicLand(...)` val plus the `basicLands` override.
                Never a `Printing(...)` row, and a Snow-Covered basic cannot use `basicLand()` at all

  unscaffolded  the earliest real printing is a set with no `definitions/<code>/` -> scaffold that
                set first, or the canonical lands somewhere it cannot live. A blocker, not a bucket

Earliest-printing selection reuses `missing-reprints.py`'s `expected_canonical`, so promo-first
printings are filtered the same way they are everywhere else (Ant Queen's first Scryfall printing is
a promo; taking `prints[0]` would file its canonical in an unscaffoldable set).

The `elsewhere` and `original` buckets have a tail worth budgeting in the same PR: authoring a
canonical is what lets `check-card-printing` see that the card's *other* scaffolded printings have
no rows either. `--tail` counts them.

Usage:
  scripts/assay-ready.py --set MH2              # the split, as a table
  scripts/assay-ready.py --set MH2 --names      # ... and every card name under its bucket
  scripts/assay-ready.py --set MH2 --tail       # ... and the reprint rows owed in OTHER sets
  scripts/assay-ready.py --set MH2 --json       # machine-readable, for generating agent briefs
  scripts/assay-ready.py --set MH2 --no-fetch   # cache only; uncached cards are reported, not hidden

Printing data comes from the shared `~/.cache/scryfall/printings/` cache. Entries past the 30-day
TTL are re-fetched by default (~0.15s each); `--no-fetch` skips the network and lists what it could
not classify under `unknown` rather than dropping it silently.
"""

from __future__ import annotations

import argparse
import importlib.machinery
import importlib.util
import json
import sys
from collections import defaultdict
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPTS_DIR.parent
sys.path.insert(0, str(SCRIPTS_DIR))

from set_dirs import dir_for_codes, scaffolded_set_codes  # noqa: E402

LEDGER_PATH = REPO_ROOT / "game-server/src/main/resources/coverage/assay-verdicts.json"

# Basic lands are not `Printing(...)` rows and not `card(...)` declarations, so they cannot be
# classified by the same rules as everything else. The canonical list is closed (CR 305.6 names the
# five, plus Wastes), and each has a Snow-Covered counterpart that needs a hand-written `card()`
# rather than `basicLand()` — `basicLand()` hardcodes "Basic Land — <type>".
BASIC_LAND_TYPES = ("Plains", "Island", "Swamp", "Mountain", "Forest", "Wastes")
BASIC_LAND_NAMES = frozenset(
    [*BASIC_LAND_TYPES, *(f"Snow-Covered {t}" for t in BASIC_LAND_TYPES)]
)


def _load_module(name: str, filename: str):
    """Import a sibling script that isn't a legal module name (`card-status`, `missing-reprints.py`).

    Registered in `sys.modules` *before* it executes, because `@dataclass` resolves a class's own
    module out of that table while decorating it and fails on a module that isn't there yet.
    """
    loader = importlib.machinery.SourceFileLoader(name, str(SCRIPTS_DIR / filename))
    spec = importlib.util.spec_from_loader(name, loader)
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    loader.exec_module(module)
    return module


card_status = _load_module("card_status", "card-status")
missing_reprints = _load_module("missing_reprints", "missing-reprints.py")


def front_face(name: str) -> str:
    """`"Front // Back"` -> `"Front"`, matching how the ledger and the canonical lists are joined.

    Mirrors `AssayVerdictService.verdict`: the two sides are generated from different Scryfall
    products and only the front face is reliably equal between them.
    """
    return name.split(" // ", 1)[0].strip()


def load_verdicts() -> dict[str, str | None]:
    """Front-face name -> decline `kind`, or None when Assay reads the card whole.

    Last row wins on a duplicate front face, the same way the server resolves it: two full names
    sharing a front face carry the same reading, so which survives cannot matter.
    """
    if not LEDGER_PATH.is_file():
        sys.exit(
            f"no verdict ledger at {LEDGER_PATH.relative_to(REPO_ROOT)} — run `just assay-bake` first"
        )
    ledger = json.loads(LEDGER_PATH.read_text(encoding="utf-8"))
    return {front_face(row["name"]): row.get("kind") for row in ledger["cards"]}


def set_report(code: str, *, refresh: bool):
    """card-status's own view of the set: what Scryfall prints in it, what the repo implements."""
    for info in card_status.discover_sets():
        if info.code.upper() == code.upper():
            report = card_status.build_report(info, force_refresh=refresh)
            if report is None:
                sys.exit(f"could not load Scryfall data for {code}")
            return report
    scaffolded = ", ".join(sorted(c.upper() for c in scaffolded_set_codes()))
    sys.exit(f"no scaffolded set with code {code.upper()}\nscaffolded: {scaffolded}")


def printings_for(name: str, *, fetch: bool) -> list | None:
    """Every printing of a card, oldest first. None when we have no fresh data and may not fetch."""
    cached = missing_reprints.load_cached_printings(name)
    if cached is not None:
        return cached
    if not fetch:
        return None
    try:
        return missing_reprints.fetch_printings(name)
    except Exception as e:  # a Scryfall blip must name the card it lost, not abort the sweep
        print(f"warning: could not fetch printings for {name}: {e}", file=sys.stderr)
        return None


# --- classification -------------------------------------------------------------------------

BUCKET_ORDER = ("original", "elsewhere", "row-only", "basic", "unscaffolded", "unknown")

BUCKET_HELP = {
    "original": "author the canonical in this set",
    "elsewhere": "author the canonical in the earlier set, then a Printing row here",
    "row-only": "canonical already in the corpus — only a Printing row here",
    "basic": "a basicLand(...) val + the basicLands override — never a Printing row",
    "unscaffolded": "BLOCKED: earliest printing's set has no definitions/<code>/ — scaffold it first",
    "unknown": "no fresh printing data — re-run without --no-fetch to classify",
}


def classify(code: str, *, refresh: bool, fetch: bool) -> dict:
    """The whole worklist for one set: the split, the declines it isn't, and the reprint tail."""
    report = set_report(code, refresh=refresh)
    verdicts = load_verdicts()
    canonical, existing_rows = missing_reprints.scan_definitions()
    scaffolded = scaffolded_set_codes()
    set_code = report.info.code.lower()

    missing = sorted(report.draft_missing | report.extra_missing)
    buckets: dict[str, list[dict]] = defaultdict(list)
    declines: dict[str, int] = defaultdict(int)
    absent: list[str] = []

    for name in missing:
        if name not in verdicts:
            # Not in the ledger at all — a third state alongside the decline kinds, and the one
            # that catches multi-face cards filed under a bare name.
            absent.append(name)
            continue
        kind = verdicts[name]
        if kind is not None:
            declines[kind] += 1
            continue

        entry: dict = {"name": name}
        if name in BASIC_LAND_NAMES:
            entry["snow"] = name.startswith("Snow-Covered")
            buckets["basic"].append(entry)
            continue
        if name in canonical:
            entry["canonical_set"] = canonical[name]
            buckets["row-only"].append(entry)
            continue

        prints = printings_for(name, fetch=fetch)
        if prints is None:
            buckets["unknown"].append(entry)
            continue
        earliest = missing_reprints.expected_canonical(prints)
        if earliest is None:
            buckets["unknown"].append(entry)
            continue

        entry["earliest_set"] = earliest.set_code
        entry["earliest_released"] = earliest.released_at
        # The rows this card will owe in OTHER scaffolded sets once its canonical exists. They are
        # invisible to check-card-printing until then, which is exactly why they get budgeted here.
        entry["tail_sets"] = sorted(
            {p.set_code for p in prints if p.set_code in scaffolded}
            - {earliest.set_code, set_code}
            - existing_rows.get(name, set())
        )
        if earliest.set_code == set_code:
            buckets["original"].append(entry)
        elif earliest.set_code in scaffolded:
            buckets["elsewhere"].append(entry)
        else:
            buckets["unscaffolded"].append(entry)

    ready = sum(len(buckets[b]) for b in BUCKET_ORDER)
    return {
        "set": report.info.code.upper(),
        "display_name": report.info.display_name,
        "missing": len(missing),
        "assay_ready": ready,
        "declined": dict(sorted(declines.items(), key=lambda kv: -kv[1])),
        "absent": absent,
        "buckets": {b: buckets[b] for b in BUCKET_ORDER if buckets[b]},
    }


# --- reporting ------------------------------------------------------------------------------


def print_report(result: dict, *, show_names: bool, show_tail: bool) -> None:
    dirs = dir_for_codes()
    print(f"{result['set']} — {result['display_name']}")
    declined_total = sum(result["declined"].values())
    print(
        f"  {result['missing']} missing: "
        f"{result['assay_ready']} Assay-ready, "
        f"{declined_total} declined, "
        f"{len(result['absent'])} absent from the ledger"
    )
    if result["declined"]:
        kinds = ", ".join(f"{k} {v}" for k, v in result["declined"].items())
        print(f"  declines: {kinds}")
    print()

    if not result["buckets"]:
        print("  nothing Assay-ready — this set's tail is grammar work, not authoring work.")
        return

    width = max(len(b) for b in result["buckets"])
    for bucket, entries in result["buckets"].items():
        print(f"  {bucket.ljust(width)}  {len(entries):>4}   {BUCKET_HELP[bucket]}")
    print()

    if show_names:
        for bucket, entries in result["buckets"].items():
            print(f"  {bucket} ({len(entries)}) — {BUCKET_HELP[bucket]}")
            for e in entries:
                suffix = ""
                if "canonical_set" in e:
                    suffix = f"  -> canonical in {e['canonical_set'].upper()}"
                elif "earliest_set" in e and bucket != "original":
                    code = e["earliest_set"]
                    where = dirs.get(code)
                    suffix = f"  -> author in {code.upper()}"
                    suffix += f" (definitions/{where}/)" if where else "  [NOT SCAFFOLDED]"
                elif e.get("snow"):
                    suffix = "  -> hand-written card(), not basicLand()"
                print(f"    {e['name']}{suffix}")
            print()

    if show_tail:
        owed: dict[str, list[str]] = defaultdict(list)
        for entries in result["buckets"].values():
            for e in entries:
                for code in e.get("tail_sets", []):
                    owed[code].append(e["name"])
        if not owed:
            print("  reprint tail: none — no other scaffolded set prints these cards.")
            return
        total = sum(len(v) for v in owed.values())
        print(f"  reprint tail: {total} Printing rows owed in {len(owed)} OTHER scaffolded sets")
        print("  (invisible to check-card-printing until the canonical exists — budget them here)")
        for code, names in sorted(owed.items(), key=lambda kv: (-len(kv[1]), kv[0])):
            print(f"    {code.upper():<6} {len(names):>3}  {', '.join(sorted(names)[:4])}"
                  + (" …" if len(names) > 4 else ""))


def main() -> int:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("--set", required=True, metavar="CODE", help="set code, e.g. MH2")
    parser.add_argument("--names", action="store_true", help="list every card under its bucket")
    parser.add_argument("--tail", action="store_true", help="count the Printing rows owed elsewhere")
    parser.add_argument("--json", action="store_true", help="machine-readable output")
    parser.add_argument("--refresh", action="store_true", help="re-fetch the set from Scryfall")
    parser.add_argument(
        "--no-fetch",
        action="store_true",
        help="cache only; cards with no fresh printing cache land in `unknown` rather than vanishing",
    )
    args = parser.parse_args()

    result = classify(args.set, refresh=args.refresh, fetch=not args.no_fetch)
    if args.json:
        print(json.dumps(result, indent=2))
    else:
        print_report(result, show_names=args.names, show_tail=args.tail)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
