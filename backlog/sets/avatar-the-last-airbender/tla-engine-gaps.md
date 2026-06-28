# Avatar: The Last Airbender — Engine Gap Analysis

Cross-reference of the **242 remaining (unimplemented) TLA cards** against the engine's actual
capabilities (SDK reference + source verification, June 2026). Generated to scope what must be
built before the set can be completed.

**Status:** 44 / 286 implemented (15%). Card list from `scripts/card-status --cards TLA`. Oracle text
pulled from Scryfall (`set:tla`, 286 unique cards; full payload in `bolav/tla_full.json`).
The full per-card checklist lives alongside this file in [`cards.md`](cards.md).

Per signature mechanic (implemented / total): **Earthbend 5/27**, **Firebending 2/21**,
**Waterbend 1/24**, **Airbend 0/10**, **Exhaust 0/8**, **Vigilance counter 3/15**. The handful of
Firebending/Waterbend cards already shipped lean on hand-wired approximations or avoid the gated cost —
the keyword primitives below are still the real work.

## Bottom line

TLA is built around **four "bending" keyword families** (Earthbend, Waterbend, Firebending, Airbend)
plus a returning **Exhaust** keyword and a pervasive **"second card drawn each turn"** sub-theme.
Of the four bending mechanics, **only Earthbend is already built** — it landed earlier as a composed
facade (`Effects.Earthbend`) and is proven across spell / ETB / activated-ability shapes. The other
three bending keywords, Exhaust, and the draw-count theme are the real work. Once those Tier-1/Tier-2
primitives land, the large majority of the remaining cards (standard creatures, dual lands, sieges,
sagas, lords, modal removal, cycling cards, Food/token makers) are buildable today.

### Already supported — no new engine work

Verified against source (file:line in each section below). These keywords/effects appear in TLA and
are fully expressible now:

- **Earthbend N** — `Effects.Earthbend(amount, target)` (`mtg-sdk/.../dsl/Effects.kt:2269`). Composes
  `AnimateLandEffect` (0/0 creature-land) + haste + N `+1/+1` counters + a single self-trigger that
  returns the land tapped when it dies **or** is exiled. Already proven as a spell (Earthbending
  Lesson), an ETB trigger (Badgermole Cub), and a sorcery-speed activated ability (Ba Sing Se). The
  full 27-card cycle (earthbend 1–4) reuses this directly. *(Caveats under Tier 3 §F.)*
- **Transform / Modal DFC** (8 Transform cards) — `frontFace`/`backFace` + `TransformEffect` +
  `DoubleFacedComponent`; `CardLayout.MODAL_DFC` + `modalBack(...)` for split modal DFCs.
- **Crew / Vehicles** (5), **Equip** (5) — `KeywordAbility.Numeric(Keyword.CREW, n)`, Vehicle type,
  `equipAbility(...)`.
- **Cycling / Landcycling / Typecycling / basic-land-cycling** (≈16 cards across all forms) —
  `KeywordAbility.Cycling`, `typecycling`, `basicLandcycling`.
- **Food** (5) — first-class `Subtype.FOOD` + `Effects.CreateFood()`; sacrifice/mana plumbing +
  `FOOD_SACRIFICED` tracker.
- **Raid** (3), **Kicker** (4), **Prowess** (7), **Flashback** (3), **Surveil** (1 effect), **Scry**
  (10), **Mill** (8), **Affinity** (1), **Fight** (2), **Landfall** (2), **Menace** (6 printed),
  **Ward** (3) — all have SDK facades / conditions in use today.
- **Keyword counters** — deathtouch, trample, reach, first strike, lifelink exist as
  `CounterType.*` mapped through `StateProjector.KEYWORD_COUNTER_MAP` (`StateProjector.kt:49`).
- Standard building blocks the gaps below compose with: tokens, `+1/+1` counters, distribute-counters,
  stun counters (`Crashing Wave`), draw/discard, destroy/exile, modal "choose one", quest counters,
  ETB/attack/dies triggers, take-extra-turn, copy-token-of-target, `MAX POWER` battlefield aggregate,
  `.named(...)` permanent counting, `ModifySpellCost`, give-control (`GiftGivenEffect`).

What follows are the **genuine gaps** — elements no current SDK primitive expresses.

---

## Tier 1 — Headline bending keywords (~55 cards, highest leverage)

The four bending families plus Exhaust gate the bulk of the set. **One of five is done (Earthbend).**

### 1. Waterbend {cost} — 🟡 PARTIAL (activated abilities done; ≈14 of ≈24 cards)

**Done (activated abilities):** `activatedAbility { cost = Costs.Mana("{N}"); hasWaterbend = true }` is
fully wired across engine payment (`AlternativePaymentHandler.applyWaterbendForAbility` +
`AlternativePaymentChoice.waterbendPermanents`), legal actions (`LegalAction.hasWaterbend`/
`waterbendPermanents`), client tap step (`waterbend` pipeline phase + `WaterbendSelector`), and the
mtgish bridge/emitter (`Waterbend` cost → `hasWaterbend = true`). Covers the ~14 activated-ability
waterbend cards (Aang's Iceberg, Flexible Waterbender, Geyser Leaper, "Waterbend {8}: Transform Aang",
"Waterbend {20}: Take an extra turn", …). **Still open** (reuse the same carrier): spell additional
cast cost (~6, e.g. Water Whip), in-resolution "may pay a waterbend cost" (Waterbending Lesson),
Ward — Waterbend, and the X variants (`WaterbendX`/`WaterbendCustomX`, mtgish-blocked for now).

Original analysis (for the spell/ability cost surface still to build) —
a convoke-style **alternative cost payment**: *"While paying a waterbend cost, you can tap your
artifacts **and** creatures to help. Each one pays for {1}."* This is **Convoke + Improvise combined**
(taps both creatures and artifacts). It appears as a cost on **abilities and spells**, e.g.
"Waterbend {8}: Transform Aang", "Exhaust — Waterbend {20}: Take an extra turn".

- The engine supports **Convoke** (`Keyword.CONVOKE`, `AlternativePaymentHandler.applyConvoke`,
  `AlternativePaymentHandler.kt:157`) but it **hard-rejects non-creatures** (`if (!…isCreature)
  continue`, line 179). **Improvise does not exist** (no enum, handler, or choice field).
- `AlternativePaymentChoice` (`AlternativePayment.kt:22`) models only `delvedCards`,
  `convokedCreatures`, `harmonizeCreature` — **no artifact-tap collection**.
- Spell-level alt payment is gated on the *spell carrying a keyword* (`CONVOKE`/`DELVE`); waterbend is
  **not a card keyword** but a cost on arbitrary abilities/spells, so the keyword-gated path doesn't fit.

**Needs:** a combined creature+artifact tap-to-pay variant (each pays {1} generic) — generalize the
convoke tap loop to accept "creature OR artifact" + an `AlternativePaymentChoice.waterbendPermanents`
set — **declarable as a cost on activated/loyalty/transform abilities and on spells**, plus the
legal-action/client surfacing that mirrors Convoke's tap step.
→ Aang (transform), Avatar Kuruk (extra turn), Secret of Bloodbending, ~21 others.

### 2. Firebending N — ❌ GAP (≈21 cards)

A static keyword on creatures: *"Whenever a creature with firebending N attacks, add {N}{R}. This
mana lasts until end of combat."* — an attack-triggered ritual that produces **combat-duration mana**.

- **No combat-duration mana.** The only mana-emptying point is end of turn
  (`CleanupPhaseManager.cleanupEndOfTurn`, line 294, called solely from `TurnManager.kt:427`). There is
  no per-step/per-phase or end-of-combat emptying. `AddManaEffect` (`ManaEffects.kt:35`) has only
  `color`/`amount`/`restriction`; `restriction` is a **spend** restriction (what you may spend it on),
  **not a temporal duration**. So firebending mana would over-persist to end of turn.
- **No attack-triggered-mana keyword.** No `FIREBENDING` in `Keyword.kt`; the attack-trigger →
  `AddManaEffect` half is composable, but the mana it makes is wrong without combat-duration support.

**Needs:** (a) a combat-duration mana concept (`Duration`-tagged mana entries + an end-of-combat
emptying point), and (b) a `firebending(n)` keyword/static so it's declarable once rather than hand-wired
on 21 cards.
→ Avatar Aang (firebending 2), most mono-red and Fire Nation creatures.

### 3. Airbend [target] — ❌ GAP (≈10 cards)

*"Airbend target permanent"* = *"Exile it. While it's exiled, its owner may cast it for **{2}** rather
than its mana cost."* Forms include "target nonland permanent", "any number of", "all other creatures",
and **"target … or spell"** (exile a spell from the stack and let its owner recast it for {2}).

- The exile + may-play-from-exile machinery is robust (`MayPlayExpiry`,
  `GrantMayPlayFromExileEffect`, `MayPlayPermission`, `ZonePermissionComponents`), **but** it can only
  **waive** cost (`PlayWithoutPayingCostComponent`), add a non-mana additional cost
  (`PlayWithAdditionalCostComponent`), or **tax** by generic (`PlayWithCostIncreaseComponent`).
  **None models "pay a fixed {2} mana cost *instead of* the printed cost."**
- The "counter a spell, exile it, grant a recast" path
  (`CounterEffect` + `CounterDestination.Exile(grantFreeCast)`) grants a **free** cast to the
  **counter's controller** — airbend grants a **{2}** cast to the **spell's owner**.

**Needs:** (a) a **fixed-alternative-cost may-play** ("castable for {2} rather than its mana cost", by
its owner, for as long as it stays exiled) honored in the cast enumerators + cost calculator; and (b) a
**stack-spell airbend** branch (counter-and-exile granting the *owner* the same {2}-recast). Multi-target
/ "all other creatures" shapes then compose from existing targeting.
→ Aang the Last Airbender, Appa, Airbending Lesson, Avatar's Wrath, Glider Staff, Airbender Ascension, …

### 4. Exhaust — ❌ GAP (8 cards)

Returning keyword (Edge of Eternities): *"Exhaust — [activated ability]. Activate each exhaust ability
only once [per game]."*

- No `Exhaust` keyword and no **once-per-game** tracking. The closest primitive,
  `ActivationRestriction.Once` (`ActivationRestriction.kt:78`), tracks via
  `AbilityActivatedEverComponent` **on the permanent entity** — it resets when the permanent leaves and
  re-enters the battlefield, which is wrong for exhaust.
- *(Note: SDK doc line ~1461 mislabels `OnlyOnce` as "once per game" — the actual enum is per-permanent
  lifetime; update the doc when this lands.)*

**Needs:** an `Exhaust` keyword + a player/game-scoped activation tracker that survives zone changes,
plus the "Exhaust —" display tag.
→ Avatar Kuruk, Avatar Kyoshi, and 6 others (several also Waterbend-gated).

---

## Tier 2 — Small recurring primitives (cheap, scattered unlocks)

### 5. Vigilance keyword counter — 🟡 one-line fix (15 cards)

The keyword-counter system exists, but **`VIGILANCE` is absent** from both `CounterType` and
`StateProjector.KEYWORD_COUNTER_MAP` (`StateProjector.kt:49`). Vigilance is TLA's most-used keyword
counter. Add the enum value + map entry (and `MENACE` alongside if any menace-counter card appears).

### 6. "Nth card drawn this turn" trigger + draw-count condition — ❌ GAP (high frequency)

A pervasive sub-theme: *"Whenever you draw your second card each turn …"*, *"Whenever an opponent draws
their second card each turn …"*, and static gates like *"as long as you've drawn two or more cards this
turn."* The engine **already tracks** `CardsDrawnThisTurnComponent` (`PlayerComponents.kt:449`,
incremented in `DrawCardPrimitive.kt:106`), **but nothing surfaces it to the SDK** — there is no
`Triggers.youDrawNthCard(n, player)` and no `Conditions.CardsDrawnThisTurn(atLeast)` /
`DynamicAmount` reading it. `NthSpellCast` (`GameEvent.kt`) is the exact analogy to mirror.

**Needs:** a draw-ordinal trigger (both "you" and "an opponent") + a cards-drawn-this-turn condition /
dynamic amount. Plumbing is already in place — this is a surface-it task, not new state.
→ The Unagi of Kyoshi Island, Raven Eagle, Otter-Penguin, Tiger-Seal, Wolfbat, Messenger Hawk,
  Obsessive Pursuit, and more.

### 7. Surveil trigger — ❌ GAP (≥1 card)

`Triggers.WheneverYouScry` exists but there is **no `WheneverYouSurveil`** (surveil is a distinct
keyword). Needs a surveil trigger (mirror of the scry one), ideally a combined "whenever you scry or
surveil".
→ Planetarium of Wan Shi Tong ("whenever you scry **or** surveil … once each turn, free-cast top card").

### 8. "Permanents sacrificed this turn" count tracker — ❌ GAP (≥1 card)

`TurnTracker` has `FOOD_SACRIFICED` (boolean) and `CREATURES_DIED`, but **no running count of all
permanents sacrificed this turn** as a `DynamicAmount`. "Whenever you sacrifice another permanent"
*triggers* already exist (`YouSacrificeOneOrMore`); the gap is the **count**.
→ Obsessive Pursuit ("X counters, X = permanents you've sacrificed this turn"); a boolean
  "sacrificed a permanent this turn" sibling would also serve Phoenix Fleet Airship.

---

## Tier 3 — One-off complex cards (each needs unique new functionality)

### A. Player hijack (Mindslaver) + combat-phase-scoped variant — ❌ GAP

`HijackNextTurnEffect` exists across SDK + engine but the source documents it as a **stub** ("ships as a
no-op that emits a `TurnHijackedEvent` only — full input/visibility routing in a follow-up"). There is
also **no "control only during their next combat phase"** scope.
→ **Secret of Bloodbending** (control an opponent during their next combat phase; if kicked/waterbent,
  their whole next turn). Implementing this means finishing the player-hijack machinery **and** adding a
  combat-phase-scoped variant.

### B. Damage-amplification replacement (scaled by counters) — ❌ GAP

*"If a source you control would deal damage to an opponent or a permanent an opponent controls, it deals
that much damage plus [N] instead."* The replacement catalog has `PreventDamage`, `CapDamage`,
`RedirectDamage`, life-gain/loss multipliers — but **no "increase outgoing damage by N"** replacement
scoped to your sources hitting opponents, scaled by a counter type.
→ **Fated Firepower** (plus fire counters).

### C. Extra combat phase with a restricted attacker set — ❌ GAP

`AddCombatPhaseEffect` exists (`PlayerEffects.kt`) but is attacker-agnostic with **no per-phase attacker
restriction** and no untap rider. Needs an extra-combat variant that constrains *which* creatures may
attack that phase (e.g. "only land creatures").
→ **Bumi, Unleashed** / **Aang, Destined Savior** / **Bumi, King of Three Trials** ("additional combat
  phase; only land creatures can attack during it").

### D. Player-scoped cast-**zone** restriction — ❌ GAP

*"Until your next turn, your opponents can't cast spells from anywhere other than their hands."*
`CantCastSpells` / `PlayersCantCastSpells` blank or filter by *spell*, but there is **no knob to restrict
legal cast *zones***. Needs a duration-bounded zone restriction checked in `CastPermissionUtils`.
→ **Avatar's Wrath**.

### E. Grant a permanent the abilities of a chosen exiled card — ❌ GAP

*"Koh has all activated and triggered abilities of the last creature card chosen with Koh."* The SDK can
grant a *fixed, named* triggered/activated ability, but nothing copies **an arbitrary referenced card's
whole ability list** onto a permanent and **re-points it when a linked choice changes**. Needs an
ability-grant-from-chosen-exiled-card static + a per-source "chosen card" pointer.
→ **Koh, the Face Stealer**.

### F. Mana-emptying **color conversion** replacement — ❌ GAP

`PreventManaPoolEmptying` (Upwelling) keeps unspent mana from emptying, but there is **no primitive that
converts** emptying mana to a color. *"If you would lose unspent mana, that mana becomes red instead"* is
a replacement on the mana-emptying step, not a prevention.
→ **Ozai, the Phoenix King**. (The Last Agni Kai's "don't lose unspent **red** mana" can lean on a
  red-filtered `PreventManaPoolEmptying`; Ozai's conversion is the new piece.)

### G. Foretell — ❌ GAP (1 card)

*"During your turn, pay {2} and exile this card face down; cast it later for its foretell cost."* Zero
matches anywhere in the codebase. Needs a Foretell keyword + the face-down-exile + cast-for-foretell-cost
flow (the existing Plot mechanic is a structural template, but distinct).

### H. Rename a permanent to a fixed name — ❌ GAP

*"… is a 1/1 Citizen with '{T}: Add {C}' **named Humble Merchant** …"* The
RemoveAllAbilities + SetStats + SetCreatureSubtypes + grant-mana-ability composite is expressible, but
**setting a permanent's name** has no SDK primitive.
→ **Honest Work**.

### I. Smaller dynamic-amount / static additions (verify-then-build)

- **Own-creature-type count with a cap** — "+1/+1 for each of its creature types, max 10" (per-creature,
  self-referential count + clamp). → **Diligent Zookeeper** *(already implemented — confirm the dynamic
  amount it used is reusable)*.
- **"Permanents you own that an opponent controls" dynamic amount** (owner ≠ controller metric).
  → **Iroh, Tea Master**.
- **"As though it had flash" rider on a may-play-from-exile grant** — `GrantMayPlayFromExileEffect` has
  `withAnyManaType` + `condition` but no instant-speed timing rider. → **Azula, Cunning Usurper**.
- **Turn-gated whole-graveyard flashback static** — every matching instant/sorcery in your graveyard
  gains flashback continuously (not a single-card grant). → **Iroh, Grand Lotus**.
- **Continuous "all nonbasic lands are Mountains" group land-type static** (Layer 4, overwrite types +
  abilities). → **Zhao, the Moon Slayer** (also a trivial new `conqueror` string counter).
- **Take-damage-from-a-named-source as a "pay-or-suffer" avoidance cost** — defender chooses to take N
  damage from the source instead of losing a permanent. → **Combustion Man**.

---

## Recommended build order

1. **Earthbend is done.** Start with **Vigilance keyword counter** (§5, one line) and the
   **Nth-card-drawn trigger + draw-count condition** (§6) — both are cheap surface-it tasks that unlock
   ~20 scattered cards.
2. **Waterbend** (§1) and **Firebending** (§2) — the two biggest lifts; together they gate ~45 cards.
   Waterbend = a combined convoke+improvise alt-payment declarable as an ability/spell cost; Firebending
   = combat-duration mana + attack-trigger keyword.
3. **Airbend** (§3) — fixed-alternative-cost may-play + stack-spell exile branch (~10 cards).
4. **Exhaust** (§4) — once-per-game activation tracker (8 cards, several also Waterbend-gated).
5. **Surveil trigger** (§7) + **sacrificed-this-turn count** (§8) — small.
6. **Tier-3 one-offs** (§A–I) as the relevant legendaries / rares come up.

The four bending keywords (Tier 1) cover the bulk of remaining cards; Earthbend already being built means
the green earth-matters cards are largely ready once the shared supporting effects are filled in.
