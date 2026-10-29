#!/usr/bin/env python3
"""Regenerate the vendored Draftsim tables under ai/src/main/resources/draftai/.

Input is the directory ``extract_from_bundle.py`` wrote (``tables/<CODE>.txt``,
``removal/<CODE>.json``). For every set it produces what the draft app's own
loaders produce, in the shape ``DraftsimData`` reads:

* ``ratings/<CODE>.json`` -- ``nameKey -> rating``, compact, in table row order.
  Mirrors the bundle's ratings loader: drop the first line (header), key column 0
  and, when present, column 1 (the other face) by nameKey, rating from column 6;
  rows whose rating doesn't parse are skipped. A table that yields no ratings at
  all is reported and left alone (AFR's upstream table is headerless with the
  rating in column 5, so Draftsim's own app reads nothing from it either).
* ``archetypes/<CODE>.json`` -- only for tables whose header has >= 9 columns, and
  only rows carrying at least one tag, fixing colour or splashable flag.
  Column 8 holds ``[Archetype Name enabler|payoff]`` / ``[Archetype Name]`` tags:
  the archetype name keeps its case and only the role is lowercased; a bare tag is
  role ``member`` unless it repeats an enabler/payoff archetype. Column 9 is the
  fixing colours (WUBRGC letters). Column 10 is splashable, and the marker is
  per-table: TMT/SOS/FDN write ``yes``, MSH writes ``X`` -- both count.
* ``removal/<CODE>.json`` -- the bundle's curated list verbatim (card name ->
  tags). Name resolution (lowercase, split-card front-face alias) and the merge of
  ``removal/_overrides.json`` happen at load time in ``DraftsimData``, so the
  overrides file is hand-maintained and never generated.
* ``ratings/_manifest.json`` -- every ratings file except the cubes, sorted.

Never written, because they are first-party (hand-curated, see DraftsimData's
class doc): ratings for ATQ SCG LGN POR DOM MRD ONS INV BIG, ``archetypes/FDN.json``
and ``removal/_overrides.json``.

Usage::

    import_draftsim_sets.py <extract-dir> [--out DIR] [--check]

``--check`` writes nothing and reports, per file, whether it is identical,
"reordered only" (same content, different key order -- upstream rewrote the
table without changing a value), or changed, itemizing changed values and
added/removed names. Exit status 1 under ``--check`` when anything differs.

Stdlib only.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import unicodedata
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
DEFAULT_OUT = REPO / "ai" / "src" / "main" / "resources" / "draftai"

FIRST_PARTY_RATINGS = {"ATQ", "SCG", "LGN", "POR", "DOM", "MRD", "ONS", "INV", "BIG"}
FIRST_PARTY_ARCHETYPES = {"FDN"}
SPLASHABLE_MARKERS = {"yes", "x"}

DIACRITICS = re.compile("[̀-ͯ]")
ROLE_TAG = re.compile(r"\[([\w][\w\s-]*?)\s+(enabler|payoff)\]", re.IGNORECASE)
BARE_TAG = re.compile(r"\[([\w][\w\s-]*?)\]")


def name_key(name: str) -> str:
    """DraftsimData.nameKey: front face, NFD, strip diacritics, '_'->' ', trim, lowercase."""
    front = name.split("//")[0]
    return DIACRITICS.sub("", unicodedata.normalize("NFD", front)).replace("_", " ").strip().lower()


def js_number(v: float) -> str:
    """JSON.stringify's number form: 3 not 3.0."""
    return str(int(v)) if v == int(v) else repr(v)


def js_float(text: str | None) -> float | None:
    """parseFloat: leading numeric prefix, NaN (None) otherwise."""
    if text is None:
        return None
    m = re.match(r"\s*[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?", text)
    return float(m.group(0)) if m else None


def rows(text: str) -> list[list[str]]:
    return [line.split("\t") for line in text.strip().split("\n")]


def cell(row: list[str], i: int) -> str:
    return row[i].strip() if i < len(row) else ""


def parse_ratings(text: str) -> dict[str, float]:
    out: dict[str, float] = {}
    for row in rows(text)[1:]:
        name, other = cell(row, 0), cell(row, 1)
        rating = js_float(row[6]) if len(row) > 6 else None
        if rating is None or not name:
            continue
        out[name_key(name)] = rating
        if other:
            out[name_key(other)] = rating
    return out


def parse_tags(text: str) -> list[dict]:
    tags = [{"archetype": m.group(1), "role": m.group(2).lower()} for m in ROLE_TAG.finditer(text)]
    for m in BARE_TAG.finditer(text):
        name = m.group(1).strip()
        if name in ("enabler", "payoff") or any(name.startswith(t["archetype"]) for t in tags):
            continue
        tags.append({"archetype": name, "role": "member"})
    return tags


def parse_archetypes(text: str) -> dict[str, dict] | None:
    table = rows(text)
    if len(table[0]) < 9:
        return None
    out: dict[str, dict] = {}
    for row in table[1:]:
        name, other = cell(row, 0), cell(row, 1)
        if not name:
            continue
        record = {
            "archetypes": parse_tags(cell(row, 8)),
            "fixing": [c for c in cell(row, 9) if c in "WUBRGC"],
            "splashable": cell(row, 10).lower() in SPLASHABLE_MARKERS,
        }
        if not (record["archetypes"] or record["fixing"] or record["splashable"]):
            continue  # untagged rows carry nothing the scorer can use; the vendored files omit them
        out[name_key(name)] = record
        if other:
            out[name_key(other)] = record
    return out


def dump_ratings(table: dict[str, float]) -> str:
    body = ",".join(json.dumps(k, ensure_ascii=False) + ":" + js_number(v) for k, v in table.items())
    return "{" + body + "}\n"


def dump_pretty(obj) -> str:
    return json.dumps(obj, ensure_ascii=False, indent=2) + "\n"


def describe(kind: str, old_text: str, new_text: str) -> str:
    old, new = json.loads(old_text), json.loads(new_text)
    if old == new:
        return "reordered only"
    changed = sorted(k for k in new if k in old and old[k] != new[k])
    added = sorted(set(new) - set(old))
    removed = sorted(set(old) - set(new))
    parts = []
    if changed:
        sample = ", ".join(
            f"{k} {old[k]}->{new[k]}" if kind == "ratings" else k for k in changed[:6])
        parts.append(f"{len(changed)} changed ({sample}{', ...' if len(changed) > 6 else ''})")
    if added:
        parts.append(f"+{len(added)} ({', '.join(added[:6])}{', ...' if len(added) > 6 else ''})")
    if removed:
        parts.append(f"-{len(removed)} ({', '.join(removed[:6])}{', ...' if len(removed) > 6 else ''})")
    return "; ".join(parts)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("extract_dir", type=Path)
    ap.add_argument("--out", type=Path, default=DEFAULT_OUT)
    ap.add_argument("--check", action="store_true", help="diff against --out instead of writing")
    args = ap.parse_args()

    src, out = args.extract_dir, args.out
    planned: dict[str, str] = {}  # relative path -> content
    notes: list[str] = []

    for table_path in sorted((src / "tables").glob("*.txt")):
        code = table_path.stem
        with open(table_path, encoding="utf-8", newline="") as fh:
            text = fh.read()
        ratings = parse_ratings(text)
        if code in FIRST_PARTY_RATINGS:
            notes.append(f"{code}: upstream now has a table, but ours is first-party -- not written")
        elif not ratings:
            notes.append(f"{code}: no ratings parse from the upstream table (Draftsim's app reads none either) -- left alone")
        else:
            planned[f"ratings/{code}.json"] = dump_ratings(ratings)
        archetypes = parse_archetypes(text)
        if archetypes is not None:
            if code in FIRST_PARTY_ARCHETYPES:
                current = out / "archetypes" / f"{code}.json"
                same = current.exists() and current.read_text(encoding="utf-8") == dump_pretty(archetypes)
                notes.append(f"{code}: archetypes/{code}.json is first-party -- not written "
                             f"(upstream's columns {'match it byte-for-byte' if same else 'differ'})")
            else:
                planned[f"archetypes/{code}.json"] = dump_pretty(archetypes)

    for removal_path in sorted((src / "removal").glob("*.json")):
        code = removal_path.stem
        if code.startswith("_"):
            continue
        planned[f"removal/{code}.json"] = dump_pretty(json.loads(removal_path.read_text(encoding="utf-8")))

    rated = {p.stem for p in (out / "ratings").glob("*.json") if not p.stem.startswith("_")}
    rated |= {Path(p).stem for p in planned if p.startswith("ratings/")}
    manifest = sorted(c for c in rated if not c.endswith("_cube"))
    planned["ratings/_manifest.json"] = json.dumps(manifest, indent=2) + "\n"

    differs = 0
    for rel, content in sorted(planned.items()):
        target = out / rel
        if not target.exists():
            status = "NEW"
        else:
            old = target.read_text(encoding="utf-8")
            if old == content:
                status = None
            else:
                kind = rel.split("/")[0]
                status = "manifest changed" if rel.endswith("_manifest.json") else describe(kind, old, content)
        if status is not None:
            differs += 1
            print(f"{rel}: {status}")
        if not args.check and status is not None:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(content, encoding="utf-8")
    identical = len(planned) - differs
    for n in notes:
        print(f"note: {n}")
    print(f"{len(planned)} files generated: {identical} identical, {differs} differ"
          + (" (check only, nothing written)" if args.check else " (written)"))
    return 1 if args.check and differs else 0


if __name__ == "__main__":
    sys.exit(main())
