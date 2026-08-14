"""Shared reader for `coverage/card-exclusions.json` — cards we won't implement.

A handful of cards need mechanics the engine will never carry (ante, subgames,
physical dexterity). Counting them as "missing" forever means a set like Antiquities
can never read complete even once every card we intend to build is built, so coverage
reporting treats them as *resolved-by-decision*: they drop out of the denominator
while they are still missing, and are surfaced separately as "not planned".

Names are unique across Magic, so the manifest keys on name alone and an entry
applies to every set that prints the card.

Readers: `scripts/card-status` (CLI report) and `scripts/gen-set-totals` (bakes the
flag into the Set Completion resource). The exclusion never *hides* a card — it only
moves it out of the "still to do" bucket.
"""

from __future__ import annotations

import json
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
MANIFEST = REPO_ROOT / "coverage/card-exclusions.json"


def _load() -> dict:
    if not MANIFEST.is_file():
        return {}
    return json.loads(MANIFEST.read_text(encoding="utf-8"))


def load_exclusions() -> dict[str, str]:
    """Map card name -> reason key (`ante`, `subgame`, …). Empty if the manifest is absent.

    A reason is mandatory and must resolve to a sentence in the manifest's `reasons` block:
    "we're not doing this one" is only a defensible answer to "why is this set complete?" if it
    comes with the why, and every consumer down to the tooltip renders that sentence. An entry
    with a missing or unknown reason is a hard error rather than a silently skipped card.
    """
    payload = _load()
    reasons = payload.get("reasons", {})
    out: dict[str, str] = {}
    for entry in payload.get("cards", []):
        name = entry.get("name")
        reason = entry.get("reason")
        if not name:
            raise ValueError(f"{MANIFEST.name}: exclusion entry without a name: {entry!r}")
        if not reason:
            raise ValueError(f"{MANIFEST.name}: exclusion for {name!r} has no reason")
        if reason not in reasons:
            raise ValueError(
                f"{MANIFEST.name}: exclusion for {name!r} cites unknown reason {reason!r} — "
                f"add it to the `reasons` block (have: {', '.join(sorted(reasons)) or 'none'})"
            )
        out[name] = reason
    return out


def load_reasons() -> dict[str, str]:
    """Map reason key -> human-readable sentence, for tooltips and CLI legends."""
    return dict(_load().get("reasons", {}))
