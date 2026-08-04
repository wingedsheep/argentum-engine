#!/usr/bin/env python3
"""Verify every card and every required field in spm_set.json against authoritative Scryfall data.

Guarantees coverage by:
  1. Matching every card in our dump by Scryfall id (with collector_number fallback).
  2. For each required field, asserting it is either present in our file OR legitimately
     empty in BOTH our file and authoritative Scryfall (e.g. lands have no mana_cost,
     noncreatures have no power/toughness, only planeswalkers have loyalty).
  3. Flagging any value mismatch against authoritative Scryfall.

Double-faced cards (modal_dfc / transform) are checked face-by-face against Scryfall's
`card_faces`, for the per-face fields (name, mana_cost, type_line, oracle_text, power,
toughness, loyalty, flavor_text, artist, image_uris); color_identity / rarity /
collector_number are whole-card.

This checks the local spm_set.json dump against the live Scryfall API; the compiled
CardDefinitions are checked separately by SpmCardFieldVerificationTest.

Run:  python3 backlog/archived/sets/marvels-spider-man/verify_spm_set.py
Exit code 0 = clean, 1 = discrepancies found.
"""
import json, os, sys, time, urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
SET_FILE = os.path.join(HERE, "spm_set.json")

# user-requested fields -> Scryfall key
FACE_FIELDS = [
    "name", "mana_cost", "type_line", "oracle_text",
    "power", "toughness", "loyalty", "flavor_text", "artist", "image_uris",
]
WHOLE_FIELDS = ["color_identity", "rarity", "collector_number"]


def fetch_authoritative():
    cards, url = [], "https://api.scryfall.com/cards/search?order=set&unique=prints&q=e%3Aspm"
    headers = {"User-Agent": "ArgentumEngine/1.0 (card data verification)", "Accept": "application/json"}
    while url:
        with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=30) as r:
            d = json.load(r)
        cards.extend(d["data"])
        url = d.get("next_page")
        time.sleep(0.1)
    return cards


def present(obj, field):
    v = obj.get(field)
    if field == "image_uris":
        return isinstance(v, dict) and bool(v.get("normal"))
    return field in obj and v not in (None, "", [], {})


def norm_image(obj):
    """Normal image path without Scryfall's volatile ?<timestamp> cache-buster."""
    v = (obj.get("image_uris") or {}).get("normal")
    return v.split("?")[0] if v else None


def faces(card):
    """Return list of (label, our-face-obj) — one entry for a single-faced card, two for a DFC."""
    if "card_faces" in card and isinstance(card["card_faces"], list) and len(card["card_faces"]) >= 2:
        return list(enumerate(card["card_faces"]))
    return [(None, card)]


def cmp_field(problems, name, label, f, o, a):
    """Compare one face field of our object `o` against authoritative `a`."""
    tag = f"{name}" + (f"[{label}]" if label is not None else "") + f".{f}"
    po, pa = present(o, f), present(a, f)
    if not po and pa:
        problems.append(f"{tag}: MISSING (Scryfall has it: {a.get(f)!r})")
    elif po and not pa:
        problems.append(f"{tag}: EXTRA (Scryfall has none: {o.get(f)!r})")
    elif po and pa:
        if f == "image_uris":
            if norm_image(o) != norm_image(a):
                problems.append(f"{tag}: MISMATCH ours={norm_image(o)!r} auth={norm_image(a)!r}")
        elif o.get(f) != a.get(f):
            problems.append(f"{tag}: MISMATCH ours={o.get(f)!r} auth={a.get(f)!r}")


def main():
    ours = json.load(open(SET_FILE))["data"]
    auth = fetch_authoritative()
    auth_by_id = {c["id"]: c for c in auth}
    auth_by_cn = {c["collector_number"]: c for c in auth}

    problems = []
    for c in ours:
        name = c.get("name", "?")
        a = auth_by_id.get(c.get("id")) or auth_by_cn.get(c.get("collector_number"))
        if a is None:
            problems.append(f"{name}: no authoritative match")
            continue
        # whole-card fields
        for f in WHOLE_FIELDS:
            if c.get(f) != a.get(f):
                problems.append(f"{name}.{f}: MISMATCH ours={c.get(f)!r} auth={a.get(f)!r}")
        # per-face fields
        our_faces, auth_faces = faces(c), faces(a)
        if len(our_faces) != len(auth_faces):
            problems.append(f"{name}: face-count mismatch ours={len(our_faces)} auth={len(auth_faces)}")
            continue
        for (label, of), (_, af) in zip(our_faces, auth_faces):
            for f in FACE_FIELDS:
                cmp_field(problems, name, label, f, of, af)

    if problems:
        print(f"FAIL: {len(problems)} issue(s)")
        for p in problems:
            print("  -", p)
        sys.exit(1)
    print(f"OK: all {len(ours)} cards verified; every required field present or legitimately empty.")


if __name__ == "__main__":
    main()
