#!/usr/bin/env python3
"""
mtgish -> Argentum cardDef EMITTER (the generation half of the bridge).

Given one mtgish card IR + its Scryfall metadata, render a COMPLETE, compiling Kotlin
`card("...") { }` definition. This module is the single source of truth for "what would the
generator emit": `fidelity.py` tiers cards on whether `render_card()` completes, `autogen.py`
writes the files, and the Kotlin gate (`verifyGeneratedCards`) compiles the output and diffs the
serialized tree against the golden snapshot. AUTO therefore means "the emitter renders the whole
card" — never a flag flip.

What it guarantees: the emitted card *compiles* and its *capabilities* match the golden tree.
What it does NOT guarantee: behavioural exactness (a best-effort filter/count can be subtly wrong,
e.g. an over-broad target) — that's review + scenario-test territory, exactly as FINDINGS states.

Imports are resolved from a live scan of the SDK source (symbol -> package), so they can't rot.
"""
from __future__ import annotations

import functools
import json
import re
from collections import Counter, namedtuple

import probe  # registry scan + mtgish extractor/index

RenderResult = namedtuple("RenderResult", "text complete reasons")


# ---------------------------------------------------------------------------
# Import resolution — scan the SDK for top-level declarations (anti-rot).
# ---------------------------------------------------------------------------
_DECL = re.compile(
    r"^(?:public\s+|internal\s+)?(?:sealed\s+|abstract\s+|open\s+|data\s+|value\s+)?"
    r"(?:class|object|interface|enum class|fun|val)\s+(?:<[^>]*>\s+)?([A-Za-z_][A-Za-z0-9_]*)")
# packages the generator legitimately draws from (disambiguates rare name clashes)
_PKG_PREF = ("com.wingedsheep.sdk.dsl", "com.wingedsheep.sdk.scripting.effects",
             "com.wingedsheep.sdk.scripting.targets", "com.wingedsheep.sdk.scripting.filters.unified",
             "com.wingedsheep.sdk.scripting", "com.wingedsheep.sdk.scripting.values",
             "com.wingedsheep.sdk.core", "com.wingedsheep.sdk.model")


@functools.lru_cache(maxsize=1)
def symbol_index() -> dict[str, str]:
    idx: dict[str, set] = {}
    sdk = probe.REPO_ROOT / "mtg-sdk/src/main/kotlin"
    for kt in sdk.rglob("*.kt"):
        pkg = None
        for line in kt.read_text().splitlines():
            m = re.match(r"^package\s+([\w.]+)", line)
            if m:
                pkg = m.group(1)
                continue
            if pkg and line[:1] and not line[:1].isspace():
                d = _DECL.match(line)
                if d:
                    idx.setdefault(d.group(1), set()).add(pkg)
    out = {}
    for sym, pkgs in idx.items():
        out[sym] = sorted(pkgs, key=lambda p: (_PKG_PREF.index(p) if p in _PKG_PREF else 99, p))[0]
    return out


def import_for(sym: str) -> str | None:
    pkg = symbol_index().get(sym)
    return f"{pkg}.{sym}" if pkg else None


# ---------------------------------------------------------------------------
# Shells: mana / typeline / metadata (complete, Scryfall-sourced — no TODOs).
# ---------------------------------------------------------------------------
MANA = {"ManaCostW": "{W}", "ManaCostU": "{U}", "ManaCostB": "{B}", "ManaCostR": "{R}",
        "ManaCostG": "{G}", "ManaCostC": "{C}", "ManaCostX": "{X}"}
COLOR_LETTERS = ["W", "U", "B", "R", "G"]
RARITY_DSL = {"common": "COMMON", "uncommon": "UNCOMMON", "rare": "RARE",
              "mythic": "MYTHIC", "special": "SPECIAL", "bonus": "BONUS"}


def render_mana(cost) -> str:
    out = []
    for s in cost or []:
        sym = s.get("_ManaSymbol")
        out.append("{%s}" % s.get("args", 0) if sym == "ManaCostGeneric" else MANA.get(sym, "{?}"))
    return "".join(out)


def render_typeline(tl) -> str:
    left = (" ".join(tl.get("Supertypes", [])) + " " + " ".join(tl.get("Cardtypes", []))).strip()
    subs = " ".join(tl.get("Subtypes", []))
    return f"{left} — {subs}" if subs else left


def _kt_str(s: str) -> str:
    return (s.replace("\\", "\\\\").replace('"', '\\"').replace("\t", "\\t").replace("\n", "\\n"))


def color_identity_dsl(meta) -> str | None:
    if not meta or meta.get("color_identity") is None:
        return None
    present = {c.upper() for c in meta["color_identity"]}
    return "".join(c for c in COLOR_LETTERS if c in present)


def metadata_lines(meta, indent="    ") -> list[str]:
    """A COMPLETE `metadata { }` block from Scryfall data — matches the hand-authored idiom and
    never emits a TODO. Degrades field-by-field; bare COMMON block only when no cache data exists."""
    if not meta:
        return [f"{indent}metadata {{ rarity = Rarity.COMMON }}  // no Scryfall cache for this set"]
    out = [f"{indent}metadata {{",
           f"{indent}    rarity = Rarity.{RARITY_DSL.get((meta.get('rarity') or '').lower(), 'COMMON')}"]
    if meta.get("collector_number"):
        out.append(f'{indent}    collectorNumber = "{_kt_str(str(meta["collector_number"]))}"')
    if meta.get("artist"):
        out.append(f'{indent}    artist = "{_kt_str(meta["artist"])}"')
    if meta.get("flavor_text"):
        out.append(f'{indent}    flavorText = "{_kt_str(meta["flavor_text"].replace("*", ""))}"')
    if meta.get("image_uri"):
        out.append(f'{indent}    imageUri = "{_kt_str(meta["image_uri"])}"')
    out.append(f"{indent}}}")
    return out


def doc_comment_lines(card, scryfall) -> list[str]:
    """A KDoc header echoing the printed card (name / cost / type / P-T / oracle text), matching the
    hand-authored idiom so a reviewer can diff the emitted DSL against the real rules text. The
    oracle text is the authoritative card wording from Scryfall's cache (None if no cache)."""
    head = [card["Name"]]
    mana = render_mana(card.get("ManaCost"))
    if mana:
        head.append(mana)
    head.append(render_typeline(card.get("Typeline", {})))
    pt = card.get("CardPT")
    if pt:
        head.append(f'{pt.get("Power")}/{pt.get("Toughness")}')
    oracle = (scryfall or {}).get("oracle_text")
    if oracle:  # split multi-line / multi-face text; never let "*/" close the KDoc early
        head += oracle.replace("*/", "* /").split("\n")
    return ["/**"] + [f" * {line}" if line else " *" for line in head] + [" */"]


# ---------------------------------------------------------------------------
# mtgish numeric / structure helpers
# ---------------------------------------------------------------------------
def _find_integer(node):
    if isinstance(node, dict):
        if node.get("_GameNumber") == "Integer":
            return node.get("args")
        if node.get("_GameNumber") in ("XValue", "X", "ValueX"):
            return "X"
        for v in node.values():
            r = _find_integer(v)
            if r is not None:
                return r
    elif isinstance(node, list):
        for v in node:
            r = _find_integer(v)
            if r is not None:
                return r
    return None


def _find_adjust_pt(node):
    if isinstance(node, dict):
        if node.get("_LayerEffect") == "AdjustPT":
            return node.get("args")
        for v in node.values():
            r = _find_adjust_pt(v)
            if r:
                return r
    elif isinstance(node, list):
        for v in node:
            r = _find_adjust_pt(v)
            if r:
                return r
    return None


def _contains(node, key, val) -> bool:
    """Does the subtree contain a {key: val} pair anywhere?"""
    if isinstance(node, dict):
        if node.get(key) == val:
            return True
        return any(_contains(v, key, val) for v in node.values())
    if isinstance(node, list):
        return any(_contains(v, key, val) for v in node)
    return False


def _subtypes(node) -> list[str]:
    """Collect IsLandType / subtype argument strings in a filter subtree."""
    out = []

    def walk(n):
        if isinstance(n, dict):
            if n.get("_Permanents") in ("IsLandType", "IsCardSubtype") and isinstance(n.get("args"), str):
                out.append(n["args"])
            if n.get("_CardsInLibrary") == "IsLandType" and isinstance(n.get("args"), str):
                out.append(n["args"])
            for v in n.values():
                walk(v)
        elif isinstance(n, list):
            for v in n:
                walk(v)
    walk(node)
    return out


# ---------------------------------------------------------------------------
# Keywords (incl. landwalk recovery)
# ---------------------------------------------------------------------------
def find_landwalk_keywords(node, keywords, out):
    if isinstance(node, dict):
        if node.get("_Rule") == "Landwalk":
            sub = node.get("args", {})
            name = sub.get("args") if isinstance(sub, dict) else None
            if isinstance(name, str) and (name.upper() + "WALK") in keywords:
                out.add(name.upper() + "WALK")
        for v in node.values():
            find_landwalk_keywords(v, keywords, out)
    elif isinstance(node, list):
        for v in node:
            find_landwalk_keywords(v, keywords, out)


def keyword_lines(card, effects, keywords, mapping) -> set[str]:
    out = set()
    find_landwalk_keywords(card.get("Rules", []), keywords, out)
    tags = Counter()
    probe.extract_tags(card.get("Rules", []), tags)
    for (disc, val) in tags:
        if val == "Landwalk":
            continue
        entry = mapping.get(f"{disc}:{val}", mapping.get(val))
        auto = probe.pascal_to_upper_snake(val) if isinstance(val, str) else ""
        if entry and entry.get("kind") == "keyword":
            out.add(entry["tag"])
        elif auto in keywords:
            out.add(auto)
    return out


# ---------------------------------------------------------------------------
# Target / filter recovery
# ---------------------------------------------------------------------------
def creature_filter_dsl(filter_node, used) -> str:
    used.add("TargetFilter")
    suffix = ""
    blob = json.dumps(filter_node)
    m = re.search(r'"IsNonColor".*?"_Color":\s*"(\w+)"', blob)
    if m:
        used.add("Color")
        suffix += f".notColor(Color.{m.group(1).upper()})"
    m = re.search(r'"IsColor".*?"_Color":\s*"(\w+)"', blob)
    if m:
        used.add("Color")
        suffix += f".color(Color.{m.group(1).upper()})"
    if _contains(filter_node, "_Permanents", "DoesntHaveAbility") and '"Flying"' in blob:
        used.add("Keyword")
        suffix += ".withoutKeyword(Keyword.FLYING)"
    return "TargetFilter.Creature" + suffix


def _target_types(args) -> set[str]:
    return set(re.findall(r'"IsCardtype",\s*"args":\s*"(\w+)"', json.dumps(args)))


def target_dsl(tnode, used) -> str | None:
    """Faithful Argentum target, or None if the filter can't be rendered (-> not AUTO)."""
    ttype = tnode.get("_Target")
    args = tnode.get("args")
    count_int = _find_integer(tnode)
    if ttype == "TargetPlayer":
        if _contains(tnode, "_Players", "Opponent"):
            used.add("TargetOpponent")
            return "TargetOpponent()"
        used.add("TargetPlayer")
        return "TargetPlayer()"
    if ttype in ("AnyTarget", "TargetPlayerOrPermanent"):
        used.add("AnyTarget")
        return "AnyTarget()"
    if ttype in ("TargetPermanent", "NumberTargetPermanents", "UptoNumberTargetPermanents",
                 "OneOrTwoTargetPermanents"):
        types = _target_types(args)
        if types == {"Creature"}:
            used.add("TargetCreature")
            parts = [f"filter = {creature_filter_dsl(args, used)}"]
            if ttype in ("NumberTargetPermanents", "UptoNumberTargetPermanents") and isinstance(count_int, int):
                parts.insert(0, f"count = {count_int}")
            if ttype in ("UptoNumberTargetPermanents", "OneOrTwoTargetPermanents"):
                parts.insert(0, "optional = true")
            return f"TargetCreature({', '.join(parts)})"
        single_type = {"Land": "TargetFilter.Land", "Artifact": "TargetFilter.Artifact",
                       "Enchantment": "TargetFilter.Enchantment"}
        if len(types) == 1 and next(iter(types)) in single_type:
            used.update(["TargetPermanent", "TargetFilter"])
            return f"TargetPermanent(filter = {single_type[next(iter(types))]})"
        if not types and "IsCardtype" not in json.dumps(args):
            used.add("TargetPermanent")
            return "TargetPermanent()"
        if types and types <= {"Creature", "Land", "Artifact", "Enchantment"}:
            used.add("TargetPermanent")  # multi-type Or (e.g. creature-or-land) — broad, review-flagged
            return "TargetPermanent()"
        return None  # unusual filters: not rendered yet -> SCAFFOLD
    if ttype == "TargetSpell":
        used.add("TargetSpell")
        return "TargetSpell()"
    if ttype == "TargetGraveyardCard":
        used.update(["TargetObject", "TargetFilter"])
        blob = json.dumps(args)
        if '"Creature"' in blob:
            filt = "TargetFilter.CreatureInYourGraveyard" if '"You"' in blob else "TargetFilter.CreatureInGraveyard"
        else:
            filt = "TargetFilter.CardInGraveyard"
        return f"TargetObject(filter = {filt})"
    return None  # spell / numeric targets need wiring we don't emit -> SCAFFOLD


def group_filter_dsl(filter_node, used) -> str:
    """Best-effort GroupFilter for mass effects (semantic exactness is review territory)."""
    used.add("GroupFilter")
    blob = json.dumps(filter_node)
    if '"Creature"' in blob:
        return "GroupFilter.AllCreatures"
    if '"Land"' in blob:
        return "GroupFilter.AllLands"
    return "GroupFilter.AllPermanents"


def land_search_filter_dsl(filter_node, used) -> str:
    used.add("GameObjectFilter")
    subs = _subtypes(filter_node)
    if subs:
        return f'GameObjectFilter.Land.withSubtype("{subs[0]}")'
    blob = json.dumps(filter_node)
    if '"Land"' in blob:
        return "GameObjectFilter.Land"
    if '"Creature"' in blob:
        return "GameObjectFilter.Creature"
    return "GameObjectFilter.Any"


# ---------------------------------------------------------------------------
# Action rendering — the pattern compiler. Returns a DSL string or None.
# ---------------------------------------------------------------------------
def _amount(node, used) -> str | None:
    """A DSL amount for a mtgish _GameNumber, or None if it isn't a plain int / X (-> SCAFFOLD,
    never a broken emit)."""
    n = _find_integer(node)
    if n is None:
        return None
    if n == "X":
        used.add("DynamicAmount")
        return "DynamicAmount.XValue"
    return str(n)


SELF_REFS = {"ThisPermanent", "Trigger_ThatCreature", "ThatEnteringPermanent",
             "Trigger_ThatPermanent", "ThatCreature", "ThatPermanent",
             "Trigger_ThatGraveyardCard", "ThatGraveyardCard"}


def _amount_node(args):
    """First subtree carrying a _GameNumber (the amount expression of a damage/draw action)."""
    found = [None]

    def walk(n):
        if isinstance(n, dict):
            if "_GameNumber" in n and found[0] is None:
                found[0] = n
            for v in n.values():
                walk(v)
        elif isinstance(n, list):
            for v in n:
                walk(v)
    walk(args)
    return found[0]


def _gainforeach_amount(args):
    """GainLifeForEach args = [perAmount, countExpr] -> a synthetic Multiply(count, per) node."""
    if isinstance(args, list) and len(args) == 2:
        return {"_GameNumber": "Multiply", "args": [args[0], args[1]]}
    return args


def _dynamic_amount(node, used) -> str | None:
    """A DynamicAmount DSL for a dynamic mtgish _GameNumber, or None if unrecognised.
    Best-effort (player/filter inexact) — semantic review territory; caps still match."""
    if not isinstance(node, dict):
        return None
    gn = node.get("_GameNumber")
    if gn == "Integer":
        used.add("DynamicAmount")
        return f"DynamicAmount.Fixed({node.get('args')})"
    if gn in ("XValue", "X", "ValueX"):
        used.add("DynamicAmount")
        return "DynamicAmount.XValue"
    if gn == "PowerOfTheSacrificedCreature":  # Final Strike: damage = sacrificed creature's power
        used.add("DynamicAmounts")
        return "DynamicAmounts.sacrificedPower()"
    if gn == "LifeTotalOfPlayer":
        used.update(["DynamicAmount", "Player"])
        player = "Player.Opponent" if _contains(node, "_Player", "Opponent") else "Player.You"
        return f"DynamicAmount.LifeTotal({player})"
    if gn in ("HalfRoundedUp", "HalfRoundedDown"):  # Cruel Bargain: lose half your life, rounded up
        inner = _dynamic_amount(node.get("args"), used)
        if inner is None:
            return None
        used.add("DynamicAmount")
        roundup = "true" if gn == "HalfRoundedUp" else "false"
        return f"DynamicAmount.Divide({inner}, DynamicAmount.Fixed(2), roundUp = {roundup})"
    if gn == "Multiply" and isinstance(node.get("args"), list) and len(node["args"]) == 2:
        a, b = node["args"]
        mult = _find_integer(a) if isinstance(_find_integer(a), int) else _find_integer(b)
        cnt = b if _find_integer(a) == mult else a
        inner = _dynamic_amount(cnt, used)
        if inner and isinstance(mult, int):
            used.add("DynamicAmount")
            return f"DynamicAmount.Multiply({inner}, {mult})"
        return None
    if gn and "NumberOf" in str(gn) or gn == "TheNumberOfPermanentsOnTheBattlefield":
        used.update(["DynamicAmount", "Player", "GameObjectFilter"])
        player = "Player.Opponent" if _contains(node, "_Player", "Opponent") else "Player.You"
        return f"DynamicAmount.AggregateBattlefield({player}, {land_search_filter_dsl(node, used)})"
    return None


def _find_ref(node) -> str | None:
    """First _Permanent / _Player reference string in a subtree (the action's subject)."""
    found = [None]

    def walk(n):
        if isinstance(n, dict):
            for k in ("_Permanent", "_Player", "_GraveyardCard"):
                if isinstance(n.get(k), str) and found[0] is None:
                    found[0] = n[k]
            for v in n.values():
                walk(v)
        elif isinstance(n, list):
            for v in n:
                walk(v)
    walk(node)
    return found[0]


def _ref_target(args, tvar, used) -> str | None:
    """Resolve an action's subject ref to an EffectTarget DSL. Ref_Target* -> the bound `tvar`;
    a self-reference -> EffectTarget.Self; otherwise fall back to `tvar`."""
    ref = _find_ref(args)
    if ref in ("Ref_TargetPermanent", "Ref_TargetPlayer", "Ref_TargetGraveyardCard"):
        return tvar
    if ref in SELF_REFS:
        used.add("EffectTarget")
        return "EffectTarget.Self"
    return tvar


def _keyword_of(node, keywords) -> str | None:
    for s in re.findall(r'"(\w+)"', json.dumps(node)):
        kw = probe.pascal_to_upper_snake(s)
        if kw in keywords:
            return kw
    return None


def _inner_action(node):
    """The nested _Action node inside an envelope action (PlayerAction / MayAction)."""
    args = node.get("args")
    if isinstance(args, list):
        for it in args:
            if isinstance(it, dict) and "_Action" in it:
                return it
    if isinstance(args, dict) and "_Action" in args:
        return args
    return None


def render_action(node, tvar, used, reasons, keywords=frozenset()) -> str | None:
    a = node.get("_Action")
    args = node.get("args")

    if a == "MayAction":  # "you may X" -> MayEffect wrapper (a real capability in the golden tree)
        inner = _inner_action(node)
        if inner is None:
            return None
        rendered = render_action(inner, tvar, used, reasons, keywords)
        if rendered is None:
            return None
        used.add("MayEffect")
        return f"MayEffect({rendered})"

    if a in ("SpellDealsDamage", "PermanentDealsDamage"):
        amt = _amount(args, used) or _dynamic_amount(_amount_node(args), used)
        if amt is None:
            return None
        if _contains(args, "_DamageRecipient", "EachPermanent"):  # mass: deal N to each creature
            used.update(["ForEachInGroupEffect", "DealDamageEffect", "EffectTarget"])
            return f"ForEachInGroupEffect({group_filter_dsl(args, used)}, DealDamageEffect({amt}, EffectTarget.Self))"
        tgt = _ref_target(args, tvar, used)
        if not tgt:
            return None
        used.add("DealDamageEffect")
        return f"DealDamageEffect({amt}, {tgt})"
    if a == "SpellDealsDistributedDamage":
        total = _find_integer(args)
        if not isinstance(total, int):
            return None
        used.add("DividedDamageEffect")
        return f"DividedDamageEffect(totalDamage = {total})"
    if a == "GainLifeForEach":  # gain (multiplier) per matching permanent
        dyn = _dynamic_amount(_gainforeach_amount(args), used)
        if dyn is None:
            return None
        used.add("GainLifeEffect")
        return f"GainLifeEffect({dyn})"
    if a == "PutEachPermanentIntoItsOwnersHand":  # bounce each chosen target (Command of Unsummoning)
        if _contains(node, "_Permanents", "Ref_TargetPermanents"):
            used.update(["ForEachTargetEffect", "MoveToZoneEffect", "Zone", "EffectTarget"])
            return ("ForEachTargetEffect(listOf(MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.HAND)))")
        return None
    if a == "DestroyPermanent":
        tgt = _ref_target(args, tvar, used)
        if not tgt:
            return None
        used.update(["MoveToZoneEffect", "Zone"])
        return f"MoveToZoneEffect({tgt}, Zone.GRAVEYARD, byDestruction = true)"
    if a in ("DestroyEachPermanent", "DestroyEachPermanentNoRegen"):
        used.update(["ForEachInGroupEffect", "MoveToZoneEffect", "Zone", "EffectTarget"])
        noregen = "true" if a == "DestroyEachPermanentNoRegen" else "false"
        return (f"ForEachInGroupEffect({group_filter_dsl(args, used)}, MoveToZoneEffect(EffectTarget.Self, "
                f"Zone.GRAVEYARD, byDestruction = true), noRegenerate = {noregen})")
    if a in ("PutPermanentIntoItsOwnersHand",):  # bounce
        tgt = _ref_target(args, tvar, used)
        if not tgt:
            return None
        used.update(["MoveToZoneEffect", "Zone"])
        return f"MoveToZoneEffect({tgt}, Zone.HAND)"
    if a in ("DrawNumberCards", "DrawACard"):
        used.add("DrawCardsEffect")
        amt = "1" if a == "DrawACard" else (_amount(args, used) or _dynamic_amount(_amount_node(args), used))
        return f"DrawCardsEffect({amt})" if amt is not None else None
    if a == "CounterSpell":
        used.add("CounterEffect")
        return "CounterEffect()"
    if a == "Shuffle":
        used.add("ShuffleLibraryEffect")
        return "ShuffleLibraryEffect()"
    if a == "ShuffleGraveyardCardIntoLibrary":  # e.g. Alabaster Dragon: dies -> shuffle self into lib
        tgt = _ref_target(args, tvar, used) or "EffectTarget.Self"
        used.update(["MoveToZoneEffect", "Zone", "ZonePlacement", "EffectTarget"])
        return f"MoveToZoneEffect({tgt}, Zone.LIBRARY, ZonePlacement.Shuffled)"
    if a == "GainLife":
        amt = _amount(args, used)
        if amt is None:
            return None
        used.add("GainLifeEffect")
        return f"GainLifeEffect({amt})"
    if a == "LoseLife":
        amt = _amount(args, used) or _dynamic_amount(_amount_node(args), used)
        if amt is None:
            return None
        used.update(["LoseLifeEffect", "EffectTarget"])
        return f"LoseLifeEffect({amt}, EffectTarget.Controller)"
    if a in ("DiscardACard", "DiscardNumberCards", "DiscardAnyNumberOfCards"):
        used.add("EffectPatterns")
        return f"EffectPatterns.discardCards({_find_integer(args) or 1})"
    if a == "DiscardACardAtRandom":
        used.add("EffectPatterns")
        return "EffectPatterns.discardRandom(1)"
    if a == "SearchLibrary":
        return _render_search(args, used)
    if a in ("LookAtTheTopNumberCardsOfLibrary", "LookAtTheTopNumberCardsOfPlayersLibrary"):
        return _render_look(node, used)
    if a in ("TapPermanent", "UntapPermanent"):
        tgt = _ref_target(args, tvar, used)
        if not tgt:
            return None
        used.update(["Effects", "EffectTarget"])
        return f"Effects.{'Tap' if a == 'TapPermanent' else 'Untap'}({tgt})"
    if a in ("TapEachPermanent", "UntapEachPermanent"):
        verb = "Tap" if a == "TapEachPermanent" else "Untap"
        if _contains(node, "_Permanents", "Ref_TargetPermanents"):  # Tidal Surge: each chosen target
            used.add("Effects")
            return f"Effects.{verb}EachTarget()"
        used.update(["ForEachInGroupEffect", "Effects", "EffectTarget"])  # mass: tap/untap a group
        return f"ForEachInGroupEffect({group_filter_dsl(args, used)}, Effects.{verb}(EffectTarget.Self))"
    if a in ("CreatePermanentLayerEffectUntil", "CreateEachPermanentLayerEffectUntil"):
        return _render_layer_effect(node, a, tvar, used, keywords)
    if a == "CreatePlayerEffectUntil":  # Summer Bloom: may play N additional lands
        n = _find_integer(node)
        if _contains(node, "_PlayerEffect", "MayPlayAdditionalLands") and isinstance(n, int):
            used.add("PlayAdditionalLandsEffect")
            return f"PlayAdditionalLandsEffect({n})"
        return None
    if a == "EachPermanentDoesntUntapDuringControllersNextUntap":
        used.add("SkipUntapEffect")
        return f"SkipUntapEffect({tvar})" if tvar else "SkipUntapEffect()"
    if a == "SkipAllCombatPhasesTheirNextTurn":
        used.add("SkipCombatPhasesEffect")
        return f"SkipCombatPhasesEffect({tvar})" if tvar else "SkipCombatPhasesEffect()"
    if a == "EachPlayerAction":
        return _render_each_player(node, used)
    if a == "PlayerAction":
        return _render_player_action(node, tvar, used, reasons, keywords)
    if a in ("PutGraveyardCardOntoBattlefield", "PutGraveyardCardIntoHand",
             "ReturnDeadGraveyardCardToTopOfLibrary", "PutPermanentOnTopOfOwnersLibrary"):
        # ReturnDead… ("return this card from the graveyard") often has no ref -> Self
        tgt = _ref_target(args, tvar, used)
        if not tgt:
            if a == "ReturnDeadGraveyardCardToTopOfLibrary":
                used.add("EffectTarget")
                tgt = "EffectTarget.Self"
            else:
                return None
        zone = {"PutGraveyardCardOntoBattlefield": "BATTLEFIELD", "PutGraveyardCardIntoHand": "HAND",
                "ReturnDeadGraveyardCardToTopOfLibrary": "LIBRARY",
                "PutPermanentOnTopOfOwnersLibrary": "LIBRARY"}[a]
        used.update(["MoveToZoneEffect", "Zone"])
        return f"MoveToZoneEffect({tgt}, Zone.{zone})"
    if a == "LookAtPlayersHand":
        tgt = _ref_target(args, tvar, used)
        used.add("LookAtTargetHandEffect")
        return f"LookAtTargetHandEffect({tgt})" if tgt else "LookAtTargetHandEffect()"
    if a == "TakeAnExtraTurn":
        used.add("TakeExtraTurnEffect")
        return "TakeExtraTurnEffect()"
    if a == "HavePlayerTakeAction":  # "target player does X" — same shape as PlayerAction
        return _render_player_action(node, tvar, used, reasons, keywords)
    if a == "CreateReplaceWouldDealDamageUntil":
        # "prevent all damage attacking creatures would deal to you this turn" (Deep Wood) ->
        # the PreventDamageShield singleton facade. Recognise the attacking-creature/PreventThatDamage shape.
        blob = json.dumps(node)
        if "IsAttacking" in blob and "PreventThatDamage" in blob and _contains(node, "_Player", "You"):
            used.add("Effects")
            return "Effects.PreventDamageFromAttackingCreatures()"
        return None
    if a == "CreateTriggerUntil":
        # "this turn, whenever an attacking creature deals combat damage to you, it deals that much to
        # its controller" (Harsh Justice) -> the ReflectCombatDamage singleton.
        blob = json.dumps(node)
        if ("WhenACreatureDealsCombatDamageToAPlayer" in blob and "ControllerOfPermanent" in blob
                and "Trigger_ThatMuch" in blob):
            used.add("ReflectCombatDamageEffect")
            return "ReflectCombatDamageEffect()"
        return None
    if a == "SpellDealsDistributedDamage":
        amt = _find_integer(args)
        if not isinstance(amt, int):
            return None
        used.add("DividedDamageEffect")
        return f"DividedDamageEffect(totalDamage = {amt})"
    if a == "CreateEachPermanentRuleEffectUntil":
        return _render_grant_to_group(node, tvar, used, reasons)
    return None  # GainLifeForEach etc. -> SCAFFOLD


def _render_grant_to_group(node, tvar, used, reasons) -> str | None:
    """A spell that grants a combat rule to a group / target (Alluring Scent, Taunt, Dread Charge)."""
    if _contains(node, "_PermanentRule", "MustBlockAttacker"):  # "all creatures must block target"
        tgt = _ref_target(node.get("args"), tvar, used)
        if not tgt:
            return None
        used.add("MustBeBlockedEffect")
        return f"MustBeBlockedEffect({tgt})"
    if _contains(node, "_PermanentRule", "MustAttackPlayer"):  # Taunt: that player's creatures attack you
        used.add("TauntEffect")
        return f"TauntEffect({tvar})" if tvar else "TauntEffect()"
    blob = json.dumps(node)
    if ("CantBeBlockedExceptByColor" in blob or "CantBeBlockedExceptByDefenders" in blob) \
            and '"_Color"' in blob:  # Dread Charge: black creatures can't be blocked except by black
        used.update(["GrantCantBeBlockedExceptByColorEffect", "GroupFilter", "Color"])
        m = re.search(r'"_Color":\s*"(\w+)"', blob)
        color = f"Color.{m.group(1).upper()}" if m else "Color.BLACK"
        return (f"GrantCantBeBlockedExceptByColorEffect(filter = {group_filter_dsl(node.get('args'), used)}, "
                f"canOnlyBeBlockedByColor = {color})")
    return None


def _render_player_action(node, tvar, used, reasons, keywords) -> str | None:
    """`target player does X` — render X scoped to the referenced player."""
    args = node.get("args")
    if _contains(node, "_Player", "OwnerOfPermanent") and _contains(node, "_Action", "GainLife"):
        used.add("OwnerGainsLifeEffect")  # Path of Peace: destroyed permanent's owner gains N
        return f"OwnerGainsLifeEffect({_find_integer(args)})"
    inner = _inner_action(node)
    if inner is None:
        return None
    ptv = _ref_target(args, tvar, used)  # the player the action applies to
    ia = inner.get("_Action")
    if ia in ("DiscardACard", "DiscardNumberCards", "DiscardAnyNumberOfCards"):
        used.add("EffectPatterns")
        n = _find_integer(inner.get("args")) or 1
        return f"EffectPatterns.discardCards({n}, {ptv})" if ptv else f"EffectPatterns.discardCards({n})"
    if ia in ("DrawNumberCards", "DrawACard"):
        used.add("DrawCardsEffect")
        amt = "1" if ia == "DrawACard" else _amount(inner.get("args"), used)
        if amt is None:
            return None
        return f"DrawCardsEffect({amt}, {ptv})" if ptv else f"DrawCardsEffect({amt})"
    if ia == "GainLife":
        amt = _amount(inner.get("args"), used)
        used.add("GainLifeEffect")
        return f"GainLifeEffect({amt}, {ptv})" if (amt and ptv) else (f"GainLifeEffect({amt})" if amt else None)
    if ia == "LoseLife":
        amt = _amount(inner.get("args"), used)
        if amt is None or not ptv:
            return None
        used.add("LoseLifeEffect")
        return f"LoseLifeEffect({amt}, {ptv})"
    if ia == "DiscardACardAtRandom":
        used.add("EffectPatterns")
        return f"EffectPatterns.discardRandom(1, {ptv})" if ptv else "EffectPatterns.discardRandom(1)"
    if ia == "SkipAllCombatPhasesTheirNextTurn":
        used.add("SkipCombatPhasesEffect")
        return f"SkipCombatPhasesEffect({ptv})" if ptv else "SkipCombatPhasesEffect()"
    if ia == "RevealHand":  # "target player reveals their hand" — its own effect
        used.add("RevealHandEffect")
        return f"RevealHandEffect({ptv})" if ptv else "RevealHandEffect()"
    return None


def _render_layer_effect(node, action, tvar, used, keywords) -> str | None:
    """CreatePermanentLayerEffectUntil / its each-permanent form -> ModifyStats / GrantKeyword,
    optionally over a group (ForEachInGroup)."""
    mass = action == "CreateEachPermanentLayerEffectUntil"
    target = "EffectTarget.Self" if mass else _ref_target(node.get("args"), tvar, used)
    if not target:
        return None
    used.add("EffectTarget")
    inner = []
    pt = _find_adjust_pt(node)
    if pt and isinstance(pt, list) and len(pt) == 2:
        used.add("ModifyStatsEffect")
        inner.append(f"ModifyStatsEffect(powerModifier = {pt[0]}, toughnessModifier = {pt[1]}, target = {target})")
    if _contains(node, "_LayerEffect", "AddAbility"):
        kw = None
        if _contains(node, "_Rule", "Landwalk"):  # AddAbility{Landwalk{Forest}} -> FORESTWALK
            subs = _subtypes(node)
            if subs and (subs[0].upper() + "WALK") in keywords:
                kw = subs[0].upper() + "WALK"
        kw = kw or _keyword_of(node, keywords)
        if kw:
            used.update(["GrantKeywordEffect", "Keyword"])
            inner.append(f"GrantKeywordEffect(Keyword.{kw}, {target})")
        else:
            return None
    if not inner:
        return None
    effect = inner[0] if len(inner) == 1 else None
    if effect is None:
        used.add("CompositeEffect")
        effect = "CompositeEffect(listOf(" + ", ".join(inner) + "))"
    if mass:
        used.add("ForEachInGroupEffect")
        # the group's filter lives in the first arg of the each-permanent layer effect
        gf = group_filter_dsl(node.get("args", [{}])[0] if isinstance(node.get("args"), list) else {}, used)
        return f"ForEachInGroupEffect({gf}, {effect})"
    return effect


# --- static abilities (PermanentRuleEffect) -> flags() / staticAbility { ability = ... } ---------
# These classes live outside the effects registry, so the capability gate is vacuous for them;
# the generated static is best-effort and (like every draft) flagged for rules-text review.
def _static_ability_dsl(rule_name, rule_node, used, keywords) -> str | None:
    if rule_name == "CantBlock":
        used.add("CantBlock")
        return "CantBlock()"
    if rule_name == "CantBeBlockedByMoreThanOne":
        used.add("CantBeBlockedByMoreThan")
        return "CantBeBlockedByMoreThan(maxBlockers = 1)"
    if rule_name == "CanBlockOnly":
        used.update(["CanOnlyBlockCreaturesWith", "GameObjectFilter"])
        kw = _keyword_of(rule_node, keywords)
        bf = f"GameObjectFilter.Creature.withKeyword(Keyword.{kw})" if kw else "GameObjectFilter.Creature"
        if kw:
            used.add("Keyword")
        return f"CanOnlyBlockCreaturesWith(blockerFilter = {bf})"
    if rule_name in ("CantBeBlockedExceptByDefenders", "CantBeBlockedByDefenders"):
        used.update(["CantBeBlockedExceptBy", "GameObjectFilter", "Keyword"])
        return "CantBeBlockedExceptBy(blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.DEFENDER))"
    if rule_name == "CantAttackUnlessDefendingPlayer":  # Deep-Sea Serpent: defender must control an Island
        subs = _subtypes(rule_node)
        if not subs:
            return None
        used.update(["CantAttackUnless", "Conditions"])
        return f'CantAttackUnless(Conditions.OpponentControlsLandType("{subs[0]}"))'
    if rule_name in ("MustBlockAttacker",):
        used.add("MustBlock")
        return "MustBlock()"
    if rule_name in ("MustAttackPlayer",):
        used.add("MustAttack")
        return "MustAttack()"
    return None


def static_block(rule, used, reasons, keywords) -> list[str] | None:
    """A PermanentRuleEffect rule -> flag / staticAbility body lines (best-effort, review-flagged)."""
    rules = []

    def collect(n):
        if isinstance(n, dict):
            if isinstance(n.get("_PermanentRule"), str):
                rules.append(n)
            for v in n.values():
                collect(v)
        elif isinstance(n, list):
            for v in n:
                collect(v)
    collect(rule)
    if not rules:
        reasons.add("PermanentRuleEffect")
        return None
    lines = []
    for r in rules:
        name = r["_PermanentRule"]
        if name == "CantBeBlocked":
            used.add("AbilityFlag")
            lines.append("    flags(AbilityFlag.CANT_BE_BLOCKED)")
            continue
        dsl = _static_ability_dsl(name, r, used, keywords)
        if dsl is None:
            reasons.add(name)
            return None
        lines += ["    staticAbility {", f"        ability = {dsl}", "    }"]
    return lines


# ---------------------------------------------------------------------------
# Spell / trigger / whole-card structure
# ---------------------------------------------------------------------------
def extract_envelope(node):
    """(targets|None, actions|None) from the first Targeted / ActionList envelope in a subtree.
    Shared by spells (SpellActions) and triggered abilities (TriggerA)."""
    found = [None, None]

    def walk(n):
        if isinstance(n, dict):
            if n.get("_Actions") == "Targeted" and isinstance(n.get("args"), list) and len(n["args"]) >= 2:
                if found[1] is None:
                    found[0] = n["args"][0] if isinstance(n["args"][0], list) else []
                    al = n["args"][1]
                    found[1] = al.get("args", []) if isinstance(al, dict) else []
            elif n.get("_Actions") == "ActionList" and isinstance(n.get("args"), list) and found[1] is None:
                found[1] = n["args"]
            for v in n.values():
                walk(v)
        elif isinstance(n, list):
            for v in n:
                walk(v)
    walk(node)
    return found[0], found[1]


def _eachplayer_maydraw(card, used) -> str | None:
    """EachPlayerActions[DrawUpto{n}, GainLifeForEach...] -> eachPlayerMayDraw (Temporary Truce)."""
    rules = card.get("Rules", [])
    if not _contains(rules, "_Action", "EachPlayerActions") or not _contains(rules, "_Action", "DrawUptoNumberCards"):
        return None
    blob = json.dumps(rules)
    mx = re.search(r'"DrawUptoNumberCards".*?"args":\s*(\d+)', blob)
    life = re.search(r'"GainLifeForEach".*?"args":\s*(\d+)', blob)
    if not mx:
        return None
    used.add("EffectPatterns")
    lpc = f", lifePerCardNotDrawn = {life.group(1)}" if life else ""
    return f"EffectPatterns.eachPlayerMayDraw(maxCards = {mx.group(1)}{lpc})"


def _spell_target(targets, used, reasons):
    """(tdsl, tvar) for a spell/trigger envelope's targets, or (None, None) if unrenderable."""
    if not targets:
        return "", None
    if len(targets) > 1:
        reasons.add("multi-target")
        return None, None
    tdsl = target_dsl(targets[0], used)
    if tdsl is None:
        reasons.add(f"target:{targets[0].get('_Target')}")
        return None, None
    return tdsl, "t"


def _distributed_spell(card, used):
    """Forked-Lightning shape: TargetedDistributed -> TargetCreature(count) + DividedDamageEffect."""
    blob = json.dumps(card.get("Rules", []), separators=(",", ":"))
    if '"TargetedDistributed"' not in blob:
        return None
    total = re.search(r'"DistributeNumberAmongTargets","args":\{"_GameNumber":"Integer","args":(\d+)', blob)
    mx = re.search(r'"BetweenOneAndNumberTargetPermanents","args":\[\{"_GameNumber":"Integer","args":(\d+)', blob)
    if not total or not mx:
        return None
    used.update(["TargetCreature", "DividedDamageEffect"])
    m = mx.group(1)
    return ['    spell {',
            f'        target = TargetCreature(count = {m}, minCount = 1)',
            f'        effect = DividedDamageEffect(totalDamage = {total.group(1)}, minTargets = 1, maxTargets = {m})',
            '    }']


def _flux_effect(card, used) -> str | None:
    """Each player discards any number, then draws that many; you draw 1 (Flux)."""
    blob = json.dumps(card.get("Rules", []), separators=(",", ":"))
    if "TheNumberOfCardsDiscardedByPlayerThisWay" in blob and "DiscardAnyNumberOfCards" in blob:
        used.add("EffectPatterns")
        bonus = 1 if '"DrawACard"' in blob else 0
        return f"EffectPatterns.eachPlayerDiscardsDraws(controllerBonusDraw = {bonus})"
    return None


def _winds_effect(card, used) -> str | None:
    """Each player shuffles their hand into their library, then draws that many (Winds of Change)."""
    blob = json.dumps(card.get("Rules", []), separators=(",", ":"))
    if "ShuffleHandIntoLibrary" in blob and "NumCardsShuffledIntoLibraryThisWay" in blob:
        used.update(["EffectPatterns", "Player"])
        return "EffectPatterns.wheelEffect(Player.Each)"
    return None


def _balance_effect(card, used) -> list[str] | None:
    """Draw the difference between target opponent's hand and yours (Balance of Power)."""
    blob = json.dumps(card.get("Rules", []), separators=(",", ":"))
    if "NumCardsInHandIs" in blob and '"Minus"' in blob and "TheNumberOfCardsInPlayersHand" in blob:
        used.update(["DrawCardsEffect", "DynamicAmounts", "TargetOpponent"])
        return ["    spell {",
                "        target = TargetOpponent()",
                "        effect = DrawCardsEffect(DynamicAmounts.handSizeDifferenceFromTargetOpponent())",
                "    }"]
    return None


def _extra_turn_effect(card, used) -> str | None:
    """Take an extra turn, then lose the game at that turn's end step (Last Chance / Final Fortune):
    [TakeAnExtraTurn, CreateFutureTrigger{... LoseTheGame}] collapses to the loseAtEndStep flag."""
    _targets, actions = extract_envelope(card.get("Rules", []))
    if not actions:
        return None
    has_extra = any(a.get("_Action") == "TakeAnExtraTurn" for a in actions)
    lose_after = any(a.get("_Action") == "CreateFutureTrigger" and _contains(a, "_Action", "LoseTheGame")
                     for a in actions)
    if has_extra and lose_after:
        used.add("TakeExtraTurnEffect")
        return "TakeExtraTurnEffect(loseAtEndStep = true)"
    return None


def _condition_dsl(if_node, used) -> str | None:
    """Map a mtgish If-condition to a Conditions.* facade (best-effort; only known shapes)."""
    blob = json.dumps(if_node, separators=(",", ":"))
    if "ControlsMorePermanentThanPlayer" in blob and '"Land"' in blob:
        used.add("Conditions")
        return "Conditions.OpponentControlsMoreLands"
    if "ControlsMorePermanentThanPlayer" in blob and '"Creature"' in blob:
        used.add("Conditions")
        return "Conditions.OpponentControlsMoreCreatures"
    return None


def _conditional_spell(card, used, reasons, keywords) -> list[str] | None:
    """Top-level `If{cond}[effect]` -> spell `condition =` gate + the inner effect (Gift of Estates)."""
    _targets, actions = extract_envelope(card.get("Rules", []))
    if not actions or len(actions) != 1 or actions[0].get("_Action") != "If":
        return None
    if_node = actions[0]
    cond = _condition_dsl(if_node, used)
    if cond is None:
        return None
    body = if_node.get("args")
    inner = body[1] if isinstance(body, list) and len(body) > 1 and isinstance(body[1], list) else None
    if not inner:
        return None
    edsl = render_effect_list(inner, None, used, reasons, keywords)
    if edsl is None:
        return None
    return ["    spell {", f"        condition = {cond}", f"        effect = {edsl}", "    }"]


def spell_block(card, used, reasons, keywords) -> list[str] | None:
    for shortcut in (_eachplayer_maydraw, _flux_effect, _winds_effect, _extra_turn_effect):
        e = shortcut(card, used)
        if e is not None:
            return ["    spell {", f"        effect = {e}", "    }"]
    for whole in (_distributed_spell, _balance_effect):
        block = whole(card, used)
        if block is not None:
            return block
    cond = _conditional_spell(card, used, reasons, keywords)
    if cond is not None:
        return cond
    targets, actions = extract_envelope(card.get("Rules", []))
    if actions is None:
        return None
    tdsl, tvar = _spell_target(targets, used, reasons)
    if tdsl is None:
        return None
    edsl = render_effect_list(actions, tvar, used, reasons, keywords)
    if edsl is None:
        return None
    inner = [f'        val t = target("target", {tdsl})'] if tvar else []
    return ["    spell {", *inner, f"        effect = {edsl}", "    }"]


TRIGGER_SPEC = {
    "WhenAPermanentEntersTheBattlefield": "Triggers.EntersBattlefield",
    "WhenACreatureOrPlaneswalkerDies": "Triggers.Dies",
    "WhenACreatureAttacks": "Triggers.Attacks",
    "WhenACreatureDealsCombatDamageToAPlayer": "Triggers.DealsCombatDamageToPlayer",
}


def trigger_block(rule, used, reasons, keywords) -> list[str] | None:
    """A TriggerA rule (self-triggered) -> triggeredAbility { trigger; [optional]; [target]; effect }."""
    spec = None
    for mt_trigger, dsl in TRIGGER_SPEC.items():
        if _contains(rule, "_Trigger", mt_trigger) and _contains(rule, "_Permanent", "ThisPermanent"):
            spec = dsl
            break
    if spec is None:
        reasons.add("trigger-shape")
        return None
    used.add("Triggers")
    targets, actions = extract_envelope(rule)
    if not actions:
        reasons.add("trigger-actions")
        return None
    tdsl, tvar = _spell_target(targets, used, reasons)
    if tdsl is None:
        return None
    edsl = render_effect_list(actions, tvar, used, reasons, keywords)  # MayAction -> MayEffect wrapper
    if edsl is None:
        return None
    lines = ["    triggeredAbility {", f"        trigger = {spec}"]
    if tvar:
        lines.append(f'        val t = target("target", {tdsl})')
    lines += [f"        effect = {edsl}", "    }"]
    return lines


def activated_block(rule, used, reasons, keywords) -> list[str] | None:
    """An Activated / ActivatedWithModifiers rule -> activatedAbility { cost; [target]; effect }.
    Activation restrictions/modifiers are dropped (review territory)."""
    args = rule.get("args")
    cost = "AbilityCost.Tap"  # default; refine from the _Cost node
    cost_node = args[0] if isinstance(args, list) and args else None
    if cost_node and cost_node.get("_Cost") == "TapPermanent":
        cost = "AbilityCost.Tap"
    elif cost_node and cost_node.get("_Cost") == "PayMana":
        cost = None  # mana costs need the symbols; leave to review -> SCAFFOLD
    if cost is None:
        reasons.add("activated-cost")
        return None
    used.add("AbilityCost")
    targets, actions = extract_envelope(rule)
    if not actions:
        reasons.add("activated-actions")
        return None
    tdsl, tvar = _spell_target(targets, used, reasons)
    if tdsl is None:
        return None
    edsl = render_effect_list(actions, tvar, used, reasons, keywords)
    if edsl is None:
        return None
    lines = ["    activatedAbility {", f"        cost = {cost}"]
    if tvar:
        lines.append(f'        val t = target("target", {tdsl})')
    lines += [f"        effect = {edsl}", "    }"]
    return lines


PERMANENT_TYPES = {"Creature", "Artifact", "Enchantment", "Land", "Planeswalker"}


def is_permanent(card) -> bool:
    return any(t in PERMANENT_TYPES for t in card.get("Typeline", {}).get("Cardtypes", []))


def render_card(card, scryfall, effects, keywords, mapping,
                package="com.wingedsheep.mtg.sets.generated.demo.cards") -> RenderResult:
    """Render a whole card. complete=True iff every rule (spell / trigger / static) is emitted."""
    used = {"card", "Rarity"}
    reasons: set[str] = set()
    name = card["Name"]
    ident = re.sub(r"[^A-Za-z0-9]", "", name)
    pt = card.get("CardPT")

    kw = keyword_lines(card, effects, keywords, mapping)
    if kw:
        used.add("Keyword")

    body = [*doc_comment_lines(card, scryfall),
            f'val {ident} = card("{_kt_str(name)}") {{',
            f'    manaCost = "{render_mana(card.get("ManaCost"))}"']
    ci = color_identity_dsl(scryfall)
    if ci is not None:
        body.append(f'    colorIdentity = "{ci}"')
    body.append(f'    typeLine = "{render_typeline(card.get("Typeline", {}))}"')
    if pt:
        body += [f'    power = {pt.get("Power")}', f'    toughness = {pt.get("Toughness")}']
    if kw:
        body.append(f'    keywords({", ".join("Keyword." + k for k in sorted(kw))})')

    handled_rules = {"SpellActions", "TriggerA", "PermanentRuleEffect", "Flying", "Haste",
                     "Vigilance", "Reach", "Defender", "Landwalk", "FirstStrike", "Trample",
                     "CastEffect"}  # CastEffect = cast restriction / additional cost (review territory)
    for rule in card.get("Rules", []):
        if not isinstance(rule, dict):
            continue
        rname = rule.get("_Rule")
        if rname == "SpellActions":
            block = spell_block(card, used, reasons, keywords)
        elif rname == "TriggerA":
            block = trigger_block(rule, used, reasons, keywords)
        elif rname == "PermanentRuleEffect":
            block = static_block(rule, used, reasons, keywords)
        elif rname in ("Activated", "ActivatedWithModifiers"):
            block = activated_block(rule, used, reasons, keywords)
        elif rname in handled_rules or (isinstance(rname, str) and rname in mapping
                                        and mapping[rname].get("kind") == "keyword"):
            continue  # keyword rule already rendered via keyword_lines
        elif isinstance(rname, str) and probe.pascal_to_upper_snake(rname) in keywords:
            continue  # auto-keyword rule
        else:
            reasons.add(rname or "unknown-rule")
            return _incomplete(body, used, reasons, scryfall, package)
        if block is None:
            return _incomplete(body, used, reasons, scryfall, package)
        body += block

    if not is_permanent(card) and not _contains(card.get("Rules", []), "_Rule", "SpellActions"):
        reasons.add("no-renderable-effect")
        return _incomplete(body, used, reasons, scryfall, package)

    body += metadata_lines(scryfall)
    body.append("}")
    return RenderResult(_assemble(body, used, package), True, reasons)


def _render_search(args, used) -> str | None:
    used.add("EffectPatterns")
    blob = json.dumps(args)
    if "PutFoundCardsOntoBattlefield" in blob:
        dest = "BATTLEFIELD"
    elif "PutFoundCardsIntoHand" in blob:
        dest = "HAND"
    elif "PutSetAsideCardsOnTopOfLibrary" in blob or "OnTopOfLibrary" in blob:
        dest = "TOP_OF_LIBRARY"
    else:
        dest = "HAND"
    used.add("SearchDestination")
    filt = land_search_filter_dsl(args, used)
    count = _find_integer(args)
    parts = [f"filter = {filt}"]
    if isinstance(count, int) and count != 1:
        parts.append(f"count = {count}")
    parts.append(f"destination = SearchDestination.{dest}")
    if "RevealFoundCards" in blob:
        parts.append("reveal = true")
    return f"EffectPatterns.searchLibrary({', '.join(parts)})"


def _render_look(node, used) -> str | None:
    used.add("EffectPatterns")
    look = _find_integer(node)
    if look is None:
        return None
    blob = json.dumps(node)
    keep = None
    for m in re.finditer(r'"PutNumber\w*IntoHand".*?"args":\s*(\d+)', blob):
        keep = int(m.group(1))
    if keep is not None:
        return f"EffectPatterns.lookAtTopAndKeep(count = {look}, keepCount = {keep})"
    if "PutTheRemainingCardsOnTopOfLibraryInAnyOrder" in blob:  # Omen: look + reorder
        return f"EffectPatterns.lookAtTopAndReorder(count = {look})"
    return None


def _render_each_player(node, used) -> str | None:
    used.add("EffectPatterns")
    blob = json.dumps(node, separators=(",", ":"))
    if _contains(node, "_Players", "Opponent") and "Discard" in blob:
        return "EffectPatterns.eachOpponentDiscards(1)"  # Noxious Toad
    if _contains(node, "_Action", "DrawNumberCards") or _contains(node, "_GameNumber", "ValueX"):
        return "EffectPatterns.eachPlayerDrawsX(includeController = true, includeOpponents = true)"
    return None


def _paycost_dsl(cost_node, used) -> str | None:
    blob = json.dumps(cost_node)
    used.add("PayCost")
    # Sacrifice-as-cost: "...unless you sacrifice a Forest" / "...three Forests". The Unless branch's
    # SacrificePermanent (sac self) is captured by _echo_effect's `suffer = SacrificeSelfEffect`, so
    # the cost is purely PayCost.Sacrifice(filter[, count]) — matching the golden's {PayOrSuffer,
    # Sacrifice} tree (Plant Elemental, Primeval Force, Thing from the Deep).
    kind = cost_node.get("_Cost") if isinstance(cost_node, dict) else None
    if kind in ("SacrificeAPermanent", "SacrificeNumberPermanents"):
        filt = land_search_filter_dsl(cost_node, used)  # IsLandType -> Land.withSubtype, etc.
        args = [filt]
        count = _find_integer(cost_node) if kind == "SacrificeNumberPermanents" else 1
        if isinstance(count, int) and count != 1:
            args.append(f"count = {count}")
        return f"PayCost.Sacrifice({', '.join(args)})"
    if "DiscardACardAtRandom" in blob:
        return "PayCost.Discard(random = true)"
    if "Discard" in blob:
        return "PayCost.Discard()"
    if "Mana" in blob:
        return "PayCost.OwnManaCost"
    return None


def _echo_effect(actions, used) -> str | None:
    """[MayCost(cost), Unless(CostWasPaid, [Sacrifice...])] -> PayOrSufferEffect (echo / upkeep cost)."""
    if len(actions) != 2:
        return None
    a0, a1 = actions
    if a0.get("_Action") != "MayCost" or a1.get("_Action") != "Unless":
        return None
    if not _contains(a1, "_Condition", "CostWasPaid") or not _contains(a1, "_Action", "SacrificePermanent"):
        return None
    cost = _paycost_dsl(a0.get("args"), used)
    if cost is None:
        return None
    used.update(["PayOrSufferEffect", "SacrificeSelfEffect"])
    return f"PayOrSufferEffect(cost = {cost}, suffer = SacrificeSelfEffect)"


def render_effect_list(actions, tvar, used, reasons, keywords=frozenset()) -> str | None:
    """Render a list of mtgish actions to one Effect (Composite if >1). None if any can't render."""
    echo = _echo_effect(actions, used)
    if echo is not None:
        return echo
    rendered = []
    for act in actions:
        r = render_action(act, tvar, used, reasons, keywords)
        if r is None:
            reasons.add(act.get("_Action") or act.get("_Rule") or "unknown-action")
            return None
        rendered.append(r)
    if not rendered:
        return None
    if len(rendered) == 1:
        return rendered[0]
    used.add("CompositeEffect")
    inner = ",\n            ".join(rendered)
    return f"CompositeEffect(\n        listOf(\n            {inner}\n        )\n    )"


def _incomplete(body, used, reasons, scryfall, package) -> RenderResult:
    body = list(body)
    body.append(f"    // STRUCTURE needs human wiring: {', '.join(sorted(reasons))}")
    body += metadata_lines(scryfall)
    body.append("}")
    return RenderResult(_assemble(body, used, package), False, reasons)


_FILE_HEADER = [
    "// === GENERATED DRAFT — do NOT merge as-is. ===",
    "// Source: mtgish IR via the coverage bridge (predictive, approximate).",
    "// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.",
    "// Then move into the set's cards/ package (auto-registers via classpath scan).",
]


def _assemble(body, used, package) -> str:
    imports = sorted({imp for s in used if (imp := import_for(s))})
    header = (_FILE_HEADER + ["", f"package {package}", ""]
              + [f"import {i}" for i in imports] + ["", ""])
    return "\n".join(header + body) + "\n"
