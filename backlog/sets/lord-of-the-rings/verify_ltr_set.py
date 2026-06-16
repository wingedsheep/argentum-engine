#!/usr/bin/env python3
"""Verify every card and every required field in ltr_set.json against authoritative Scryfall data.

Guarantees coverage by:
  1. Matching every card in our dump by Scryfall id (with collector_number fallback).
  2. For each required field, asserting it is either present in our file OR legitimately
     empty in BOTH our file and authoritative Scryfall (e.g. lands have no mana_cost,
     noncreatures have no power/toughness, only planeswalkers have loyalty).
  3. Flagging any value mismatch against authoritative Scryfall.

This checks the local ltr_set.json dump against the live Scryfall API; the compiled
CardDefinitions are checked separately by LtrCardFieldVerificationTest.

Run:  python3 backlog/sets/lord-of-the-rings/verify_ltr_set.py
Exit code 0 = clean, 1 = discrepancies found.
"""
import json, os, sys, time, urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
SET_FILE = os.path.join(HERE, "ltr_set.json")

# user-requested fields -> Scryfall key
REQUIRED = [
    "name", "mana_cost", "color_identity", "type_line", "oracle_text",
    "power", "toughness", "loyalty",
    "rarity", "collector_number", "artist", "flavor_text", "image_uris",
]


def fetch_authoritative():
    cards, url = [], "https://api.scryfall.com/cards/search?order=set&unique=prints&q=e%3Altr"
    headers = {"User-Agent": "ArgentumEngine/1.0 (card data verification)", "Accept": "application/json"}
    while url:
        with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=30) as r:
            d = json.load(r)
        cards.extend(d["data"])
        url = d.get("next_page")
        time.sleep(0.1)
    return cards


def present(card, field):
    v = card.get(field)
    if field == "image_uris":
        return isinstance(v, dict) and bool(v.get("normal"))
    return field in card and v not in (None, "", [], {})


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
        for f in REQUIRED:
            po, pa = present(c, f), present(a, f)
            if not po and pa:
                problems.append(f"{name}.{f}: MISSING (Scryfall has it)")
            elif po and pa and f != "image_uris" and c.get(f) != a.get(f):
                problems.append(f"{name}.{f}: MISMATCH ours={c.get(f)!r} auth={a.get(f)!r}")

    if problems:
        print(f"FAIL: {len(problems)} issue(s)")
        for p in problems:
            print("  -", p)
        sys.exit(1)
    print(f"OK: all {len(ours)} cards verified; every required field present or legitimately empty.")


if __name__ == "__main__":
    main()
