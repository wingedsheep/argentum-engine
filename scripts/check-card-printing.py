#!/usr/bin/env python3
"""
Verify a single implemented card's printing layout against Scryfall:

  - find the file holding the canonical `card("<Name>") { ... }` CardDefinition,
  - list every Scryfall printing of that card (oldest -> newest),
  - confirm the canonical CardDefinition lives in the card's *earliest*
    real-expansion printing (strict — promo/token printings are skipped),
  - for every other scaffolded printing, confirm a `Printing(...)` row exists.

If the earliest printing's set isn't scaffolded under
`mtg-sets/.../definitions/<setcode>/`, that's drift: scaffold the set so the
canonical can live there.

Exit code:
  0 - canonical is in the earliest real printing and every scaffolded
      printing has a reprint row
  1 - drift (wrong canonical set, earliest set not scaffolded, missing
      reprint rows, or no CardDefinition)
  2 - input error (card not implemented in the repo, Scryfall lookup failed)

Usage:
  scripts/check-card-printing.py "Lightning Bolt"
  scripts/check-card-printing.py "Monastery Swiftspear" --refresh
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
from dataclasses import dataclass
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFINITIONS_ROOT = REPO_ROOT / "mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions"
CACHE_ROOT = Path.home() / ".cache" / "scryfall" / "printings"
SCRYFALL_BASE = "https://api.scryfall.com"
USER_AGENT = "argentum-engine-check-card-printing/1.0"
REQUEST_DELAY_SEC = 0.15  # Scryfall asks for 50–100ms between requests
CACHE_TTL_DAYS = 30

CARD_DSL_RE = re.compile(r'\b(?:card|basicLand)\(\s*"([^"]+)"')
PRINTING_NAME_RE = re.compile(r'\bname\s*=\s*"([^"]+)"')
PRINTING_SETCODE_RE = re.compile(r'\bsetCode\s*=\s*"([^"]+)"')

# Set types we expect to scaffold as a top-level set. Promo/token/memorabilia
# printings are noted but never count as "expected canonical".
SCAFFOLDABLE_SET_TYPES = {
    "core", "expansion", "draft_innovation", "masters", "commander",
    "starter", "duel_deck", "from_the_vault", "premium_deck", "spellbook",
    "planechase", "archenemy", "vanguard", "treasure_chest", "alchemy",
    "funny", "remastered",
}


@dataclass(frozen=True)
class Printing:
    set_code: str  # lowercase Scryfall set code
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
    """GET a Scryfall URL with polite pacing and 429-aware exponential backoff."""
    for attempt in range(max_retries):
        time.sleep(REQUEST_DELAY_SEC)
        req = urllib.request.Request(
            url,
            headers={"User-Agent": USER_AGENT, "Accept": "application/json"},
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


def fetch_printings(card_name: str, *, refresh: bool) -> list[Printing]:
    """Return all printings of a card from Scryfall, sorted oldest -> newest."""
    CACHE_ROOT.mkdir(parents=True, exist_ok=True)
    cache_path = CACHE_ROOT / f"{slugify(card_name)}.json"
    if not refresh and cache_path.is_file():
        age_days = (time.time() - cache_path.stat().st_mtime) / 86400
        if age_days < CACHE_TTL_DAYS:
            try:
                raw = json.loads(cache_path.read_text(encoding="utf-8"))
                return [Printing(**p) for p in raw]
            except (json.JSONDecodeError, TypeError):
                pass  # fall through and re-fetch

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

    cache_path.write_text(
        json.dumps([p.__dict__ for p in printings], indent=2),
        encoding="utf-8",
    )
    return printings


def scaffolded_sets() -> set[str]:
    if not DEFINITIONS_ROOT.is_dir():
        return set()
    return {d.name for d in DEFINITIONS_ROOT.iterdir() if d.is_dir()}


def find_canonical(card_name: str) -> tuple[str, Path] | None:
    """Return (set_code_lower, file_path) of the file with `card("Name") { ... }`."""
    front = card_name.split(" // ", 1)[0].strip()
    for kt in DEFINITIONS_ROOT.glob("*/cards/*.kt"):
        text = kt.read_text(encoding="utf-8")
        for m in CARD_DSL_RE.finditer(text):
            if m.group(1) in (card_name, front):
                # definitions/<setcode>/cards/<file>.kt
                set_code = kt.parts[-3]
                return (set_code, kt)
    return None


def find_reprint_rows(card_name: str) -> dict[str, Path]:
    """Return {scaffolded_set_code_lower: file_path} for `Printing(...)` rows
    that name this card. Skips files that hold a CardDefinition.
    """
    front = card_name.split(" // ", 1)[0].strip()
    result: dict[str, Path] = {}
    for kt in DEFINITIONS_ROOT.glob("*/cards/*.kt"):
        text = kt.read_text(encoding="utf-8")
        if "Printing(" not in text:
            continue
        if any(m.group(1) in (card_name, front) for m in CARD_DSL_RE.finditer(text)):
            continue  # this file holds the CardDefinition, not a reprint row
        if not any(n in (card_name, front) for n in PRINTING_NAME_RE.findall(text)):
            continue
        set_code = kt.parts[-3]
        result[set_code] = kt
    return result


def expected_canonical(printings: list[Printing]) -> Printing | None:
    """Earliest real-expansion printing of the card on Scryfall.

    Strict: we want the canonical to live in the *card's* original home, not
    just the earliest set we happen to have scaffolded today. Promo / token /
    art_series / memorabilia printings are skipped so e.g. a pre-release
    promo doesn't displace the actual expansion debut. If somehow no
    expansion-like printing exists, fall back to the very first printing.
    """
    for p in printings:
        if p.set_type in SCAFFOLDABLE_SET_TYPES:
            return p
    return printings[0] if printings else None


def format_row(label: str, value: str) -> str:
    return f"  {label:<26}{value}"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("card_name", help='exact card name, quoted (e.g. "Lightning Bolt")')
    parser.add_argument("--refresh", action="store_true", help="force re-fetch from Scryfall")
    args = parser.parse_args()

    name = args.card_name.strip()
    canonical = find_canonical(name)
    reprint_rows = find_reprint_rows(name)

    if canonical is None and not reprint_rows:
        print(f"error: '{name}' is not implemented in mtg-sets/", file=sys.stderr)
        print("       (no `card(\"...\")` declaration or `Printing(...)` row found)", file=sys.stderr)
        return 2

    try:
        printings = fetch_printings(name, refresh=args.refresh)
    except urllib.error.HTTPError as e:
        print(f"error: Scryfall lookup failed: {e}", file=sys.stderr)
        return 2
    if not printings:
        print(f"error: no Scryfall printings for '{name}'", file=sys.stderr)
        return 2

    scaffolded = scaffolded_sets()
    expected = expected_canonical(printings)
    expected_scaffolded = expected is not None and expected.set_code in scaffolded

    print(f"Card: {name}")
    print(format_row("oracle_id:", printings[0].oracle_id or "(missing)"))
    print(format_row("Scryfall printings:", str(len(printings))))
    print()

    if canonical:
        print(format_row(
            "Canonical (actual):",
            f"{canonical[0]}  ({canonical[1].relative_to(REPO_ROOT)})",
        ))
    else:
        print(format_row("Canonical (actual):", "(none — only reprint rows exist)"))

    if expected:
        suffix = "" if expected_scaffolded else "  [NOT SCAFFOLDED]"
        print(format_row(
            "Canonical (expected):",
            f"{expected.set_code}  ({expected.set_name}, released {expected.released_at}){suffix}",
        ))
    else:
        print(format_row("Canonical (expected):", "(Scryfall returned no printings)"))
    print()

    issues: list[str] = []

    if expected is None:
        issues.append("Scryfall returned no printings — cannot determine the expected canonical.")
    elif not expected_scaffolded:
        issues.append(
            f"Card's earliest real printing is '{expected.set_code}' "
            f"({expected.set_name}, {expected.released_at}), which is NOT scaffolded "
            f"under `mtg-sets/.../definitions/{expected.set_code}/`. Scaffold "
            f"{expected.set_code} and put the CardDefinition there; the current "
            f"canonical (if any) becomes a reprint row."
        )
    elif canonical is None:
        issues.append(
            f"No `card(...)` CardDefinition exists for this card. Add one in "
            f"`mtg-sets/.../definitions/{expected.set_code}/cards/`."
        )
    elif canonical[0] != expected.set_code:
        issues.append(
            f"Canonical lives in '{canonical[0]}' but the earliest real printing "
            f"is '{expected.set_code}'. Move the CardDefinition to "
            f"`mtg-sets/.../definitions/{expected.set_code}/cards/` and replace "
            f"`{canonical[1].relative_to(REPO_ROOT)}` with a `Printing(...)` row."
        )

    missing_reprints: list[Printing] = []
    for p in printings:
        if p.set_code not in scaffolded:
            continue
        if expected is not None and p.set_code == expected.set_code:
            continue  # canonical slot, not a reprint slot
        if canonical is not None and p.set_code == canonical[0]:
            continue  # current canonical (may be wrong; flagged above)
        if p.set_code not in reprint_rows:
            missing_reprints.append(p)

    if missing_reprints:
        issues.append(
            "Missing `Printing(...)` rows in scaffolded sets: "
            + ", ".join(p.set_code for p in missing_reprints)
        )

    # Per-printing table.
    print("  Printings (oldest -> newest):")
    print(f"    {'set':<6} {'released':<11} {'rarity':<10} {'in repo?':<10} {'row?'}")
    for p in printings:
        in_repo = "yes" if p.set_code in scaffolded else "no"
        row = ""
        if p.set_code in scaffolded:
            if canonical and canonical[0] == p.set_code:
                row = "canonical"
            elif p.set_code in reprint_rows:
                row = "reprint"
            else:
                row = "MISSING"
        marker = ""
        if expected and expected.set_code == p.set_code:
            marker = "  <-- expected canonical"
        print(f"    {p.set_code:<6} {p.released_at or '?':<11} {p.rarity or '?':<10} {in_repo:<10} {row}{marker}")
    print()

    if not issues:
        print("  ok: canonical is in the card's earliest real printing and all scaffolded reprints have rows.")
        return 0

    print("  drift detected:")
    for i, msg in enumerate(issues, start=1):
        print(f"    {i}. {msg}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
