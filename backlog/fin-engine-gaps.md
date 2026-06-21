# Final Fantasy — Engine Gap Analysis

Cross-reference of the **228 remaining (unimplemented, non-basic) FIN cards** against the engine's actual
capabilities (SDK reference + source verification, June 2026). Generated to scope what must be built before
the set can be completed.

**Status:** 75 / 300 implemented (25%). Card list + oracle text from Scryfall (`set:fin`, 300 unique cards),
diffed against `scripts/card-status --cards FIN`. The six basic lands (Plains, Island, Swamp, Mountain, Forest,
Wastes) are trivial reprints and excluded from the gap counts below. Sibling products (`fic` Commander, `fca`
Through the Ages) are out of scope.

## Bottom line

FIN is a **legends-matters / Equipment-matters / graveyard-recursion** set whose remaining work is dominated by
**four headline mechanics**, three of which are genuine engine gaps:

1. **Summon Sagas — "Enchantment Creature — Saga"** (≈15 cards + ≈8 transform/eikon backs). A permanent that is
   *simultaneously a creature and a Saga* — it sits on the battlefield as a creature (P/T, keywords, combat) while
   progressing through lore chapters. **The single biggest lift in the set** and a hard structural gap: `isSaga`
   is currently gated to enchantment-only.
2. **Job select** (≈16 Equipment). ETB: *"create a 1/1 colorless Hero creature token, then attach this to it."*
   Needs create-token-**then-attach-the-source-to-it** in one ETB, plus the keyword/reminder shell.
3. **Tiered** (6 spells). A brand-new keyword: *"Tiered (Choose one additional cost.)"* — pick exactly one of
   several escalating modes, each with its own additional mana cost paid at cast and its own (usually scaled) effect.
4. **Towns** (land subtype — **already supported**) plus a **new land // spell DFC layout** (≈6 lands) where the
   *land* is the permanent half and the spell half exiles itself so the land can be played from exile later — the
   inverse of Adventure.

Everything else is a long tail of standard material (cycling/landcycling, flashback, kicker, affinity, crew/vehicles,
modal removal, stun/finality/blight counters, surveil/scry, landfall, treasure/food) that the SDK already expresses,
plus a handful of one-off rares. The mana-spent spellslinger primitives that powered Strixhaven's Opus/Increment
([SOS gaps doc](sos-engine-gaps.md)) are reused heavily here — *"if at least four mana was spent to cast it"* is
already a first-class trigger condition.

### Already supported — no new engine work

Verified against source (file:line). These appear across the remaining FIN cards and are fully expressible today:

- **`Subtype("Town")` and counting Towns** — subtypes are free strings (`Subtype.kt:188`), so `Land — Town` parses
  and `GameObjectFilter.Land.withSubtype("Town")` + `DynamicAmounts.battlefield(...).count()` (`DynamicAmounts.kt:205`)
  already power "for each Town you control" / "five or more Towns" (Wandering Minstrel, PuPu UFO, Qiqirn Merchant,
  Balamb Garden cost reduction). *Implemented Town lands already ship (Starting Town, Capital City, …).*
- **"If at least N mana was spent to cast that spell" on a noncreature-cast trigger** — the Strixhaven Opus primitive
  `DynamicAmount.ContextProperty(MANA_SPENT_ON_TRIGGERING_SPELL)` (`DynamicAmount.kt:166`, `OpusDsl.kt`) directly
  covers Blazing Bomb, Sahagin, Prompto Argentum, Tellah, Ultros, The Prima Vista, The Emperor of Palamecia,
  Shantotto, Vivi (the whole "magic-counter" sub-theme).
- **Equip** — `equipAbility("{N}")` (`CardBuilder.kt:515`) + `AttachEquipmentExecutor`; **attach-to-target as a
  spell/triggered effect** — `Effects.AttachTargetEquipmentToCreature(...)` (`Effects.kt:3048`,
  `AttachTargetEquipmentToCreatureExecutor`), covering Raubahn, Weapons Vendor, Beatrix, Gilgamesh, Zack Fair.
  **Equip cost reduction** — `ActivatedAbility.genericCostReduction` (`ActivatedAbility.kt:58`). **`Filters.EquippedCreature`**
  for equipped-creature static buffs (`Filters.kt:137`).
- **Affinity for a subtype** — `KeywordAbility.AffinityForSubtype` (`KeywordAbility.kt:217`) → Bartz and Boko
  ("Affinity for Birds"); `Affinity(forType)` for the artifact form.
- **In-place transform DFC** — `TransformEffect` + `CardDefinition.doubleFacedCreature(...)`; proven in FIN by
  **Cecil, Dark Knight // Cecil, Redeemed Paladin** (`fin/cards/CecilDarkKnight.kt`). Covers creature↔creature
  transforms (Vincent Valentine, Sephiroth Fabled SOLDIER, Zenos, Emet-Selch, Exdeath, The Emperor).
- **Counters** — `CounterType` has `STUN`, `FINALITY`, `BLIGHT`, `CHARGE`, `LORE`, `INDESTRUCTIBLE`
  (`CounterType.kt`), with stun handled in `UntapHelpers` and finality "exile instead of die" in place. Covers the
  pervasive stun/finality riders (Ice Flan, Omega, Shiva, Relentless X-ATM092, Noctis, Yuna, Rydia's Return).
- **Cycling / landcycling / typecycling** (`KeywordAbility.Cycling`, basic-land-cycling) — Airship Crash, Balamb
  T-Rexaur, Cloudbound Moogle, Hill Gigas, Ice Flan, Malboro, Cid (cycling {W}{U}).
- **Flashback** (`Flashback` keyword) — Esper Origins, Laughing Mad, Memories Returning, Nibelheim Aflame, Random
  Encounter, Retrieve the Esper, Sorceress's Schemes, The Final Days, From Father to Son.
- **Kicker** (`KeywordAbility.Kicker`) — Chocobo Kick, Vayne's Treachery. **Crew / Vehicles** — Cargo Ship, The
  Regalia, The Lunar Whale, Sidequest backs. **Standard building blocks**: modal "choose one/one-or-more"
  (`ModalEffect`), fight, mill, surveil/scry, landfall, treasure/food tokens, copy-token-of-target
  (`Relm's Sketching`), "exile then meld" aside, devotion *predicate* (`CardPredicate`), additional combat phase
  (`AddCombatPhaseEffect`), play-top-of-library / cast-from-graveyard grants (`MiscStaticAbilities`,
  `SpellStaticAbilities.MayCastFromGraveyard`), `AddAdditionalUpkeepStepsEffect`.

What follows are the **genuine gaps** — elements no current SDK primitive expresses.

---

## Tier 1 — Headline mechanics (highest leverage)

### 1. Summon Sagas — "Enchantment Creature — Saga" (≈15 + ≈8 backs) — ❌ **GAP** (the big lift)

FIN's "Summon: X" cards (and the eikon back faces of the Dominants) are **creatures that are also Sagas**: they
enter as a creature with power/toughness and keywords (e.g. *Menace*, *Flying*, *Ward {2}*, *Indestructible*), sit
on the battlefield in combat, and **simultaneously progress through lore chapters** (lore counter on enter + after
each draw step). Most read *"Sacrifice after IV"*; some have **no sacrifice clause and simply remain as a creature**
once chapters stop; some chapter effects say *"This creature deals damage…"* referring to the saga-creature itself.

**The structural gap:** `TypeLine.isSaga = isEnchantment && hasSubtype(Subtype.SAGA)` (`TypeLine.kt:23`) can never be
true on a creature. Saga lore-counter accrual (`BeginningPhaseManager.addLoreCountersToSagas`,
`BeginningPhaseManager.kt:296`), chapter trigger detection, and the sacrifice SBA (`SagaSacrificeCheck.kt:19`) all
key off that enchantment-only predicate. So a permanent cannot today be both a combat creature and a chapter-ticking
Saga.

**Needs (full `add-feature` treatment — design for the whole "Summon" cycle, not one card):**
- Allow `isSaga` to hold on an **Enchantment Creature — Saga** (decouple the saga machinery from "enchantment, not
  creature"); make lore accrual + chapter triggers + the final-chapter SBA fire on a permanent that is also a creature.
- A **per-card "sacrifice after final chapter" flag** — `SagaSacrificeCheck` currently *always* sacrifices at
  `loreCount >= finalChapter`. Summon Sagas that stay as creatures need to opt out (and the Dominant backs transform
  back instead of sacrificing — see §2).
- Chapter abilities that reference **the saga-creature itself** (`This creature deals damage…`, P/T-derived amounts)
  must resolve `Self` to the saga permanent.
- The reminder line *"(As this Saga enters and after your draw step, add a lore counter.)"* + a `summon { }` /
  saga-creature DSL shell.

→ Summon: Anima, Bahamut, Brynhildr, Choco/Mog, Esper Ramuh, Fat Chocobo, Fenrir, G.F. Cerberus, G.F. Ifrit,
  Knights of Round, Leviathan, Primal Garuda, Primal Odin, Shiva, Titan (15), plus the eikon backs in §2.

Two sub-gaps surface inside this cycle:
- **"Play a card during any turn you put a lore counter on this Saga"** (Summon: Brynhildr I) — exile-and-play exists
  (`GrantMayPlayFromExileEffect`) but there is **no trigger keyed to a lore counter being added** to a specific Saga;
  the permission must renew on each lore-counter event. Small new trigger + linked-exile play grant.
- **"When you next cast a creature/instant spell this turn, …"** delayed riders (Brynhildr II, Fenrir II, G.F.
  Cerberus II/III copy-next-spell) — the "next spell you cast" one-shot rider pattern exists (next-spell-uncounterable,
  pending copy); verify the copy-next-instant variant composes, else extend.

### 2. Dominant / Eikon transform — creature front → **Saga-creature** back via exile-and-return (≈8) — ❌ **GAP**

The Dominants (Clive→Ifrit, Jill→Shiva, Dion→Bahamut, Joshua→Phoenix, Terra→Esper Terra, Jecht, Crystal Fragments,
Esper Origins) are double-faced cards whose **back face is a Summon Saga (§1)** and whose front transforms via an
activated ability worded *"Exile [card], then return it to the battlefield transformed under its owner's control"* —
a **leave-and-re-enter** transform (a new object), not an in-place `TransformEffect` flip like Cecil. The eikon then
ticks its chapters as a creature and, on its final chapter, *"exile it, then return it to the battlefield (front face
up)"* — re-entering as the original front-face legend.

**Needs:** (a) §1 (saga-creature back faces) as a hard prerequisite; (b) an **exile-and-return-transformed** effect
that re-enters the permanent as a *new object* on the chosen face (resetting the saga), as opposed to the in-place
`TransformEffect`. No `ExileAndReturnTransformed` effect exists today (grep: none). Compose from
exile + return-to-battlefield-transformed, or add a dedicated effect.
→ Clive, Jill, Dion, Joshua (5-color dominants); Terra, Magical Adept; Jecht; Crystal Fragments (Equipment→Saga);
  Esper Origins (sorcery cast-from-graveyard → enters transformed as a Saga).

### 3. Job select (≈16 Equipment) — ❌ **GAP** (create-token-then-attach-self)

*"Job select (When this Equipment enters, create a 1/1 colorless Hero creature token, then attach this to it.)"* The
two halves exist separately — `CreateTokenEffect` and `AttachTargetEquipmentToCreatureEffect` — but **the created
token's id is not published into the pipeline**, so the source Equipment cannot attach to the token it just made in a
single ETB. (Several Job-select cards also have a *named* equip cost, e.g. "Diana — Equip {2}", which is cosmetic.)

**Needs:** publish `CreateTokenEffect`'s new token id into a pipeline slot (the corpus already wants this — see the
SOS Job-select-shaped notes), then `AttachEquipment(self → that token)`; wrap as a `jobSelect()` keyword/DSL with
reminder text. Once built, all 16 are pure authoring.
→ Astrologian's Planisphere, Bard's Bow, Black Mage's Rod, Dark Knight's Greatsword, Dragoon's Lance, Machinist's
  Arsenal, Monk's Fist, Ninja's Blades, Paladin's Arms, Red Mage's Rapier, Sage's Nouliths, Samurai's Katana,
  Summoner's Grimoire, Thief's Knife, Warrior's Sword, White Mage's Staff.

### 4. Tiered (6 spells) — ✅ **DONE** (choose-one escalating additional cost)

*"Tiered (Choose one additional cost.)"* then 2-3 modes, **each with its own additional mana cost** paid at cast and
its own (usually scaled) effect — e.g. Fire Magic: `Fire — {0} — 1 dmg to each creature / Fira — {2} — 2 dmg / Firaga
— {5} — 3 dmg`. Verified against CR 702.183a (*"Choose one. As an additional cost to cast this spell, pay the cost
associated with that mode."*) that **no engine change was needed**: the modal/Spree cast pipeline already charges a
single chosen mode's `additionalManaCost` on the choose-1 path — `CastSpellEnumerator.computeModeEnumeration` folds
the chosen tier's cost into each `CastSpellMode` legal action (only affordable tiers are offered) and
`CastSpellHandler` adds it on execute. Shipped a `spell { tiered { } }` authoring builder (`CardBuilder.kt`,
`TieredBuilder` → a `ModalEffect` with `chooseCount = 1` and per-tier `additionalManaCost`) + a `TIERED_REMINDER`
constant; no `Keyword.TIERED` (Tiered adds no behavior beyond the modal-with-additional-cost shape, mirroring Spree).
All six cards implemented and covered by `TieredScenarioTest` (per-tier cost charging, choose-one rejection,
affordability gating, scaled effects, targeting, double/triple P/T, Vincent's dies→return-tapped).
→ Fire Magic, Ice Magic, Thunder Magic, Restoration Magic, Tifa's Limit Break, Vincent's Limit Break.

### 5. Town land // spell DFC — "play the land from exile later" (≈6) — ❌ **GAP** (new layout)

Several Town lands are double-faced: front = `Land — Town` (enters tapped, taps for mana), back = a one-shot
sorcery, with reminder *"(Then exile this card. You may play the land later from exile.)"* You either play the land
**or** cast the spell; casting the spell exiles the card and lets you **play the land half from exile** later. This
is the **inverse of Adventure** (Adventure exiles after the spell and lets you cast the *creature*; here the
permanent half is a *land*). No `CardLayout` variant models "spell-then-exile, replay the land from exile"
(`CardDefinition.kt` `CardLayout`). Adventure's machinery (`CastSpell.faceIndex`, exile-with-play-permission) is the
structural template; needs a sibling layout + loader so the land becomes the playable-from-exile half.
→ Ishgard the Holy See, Jidoor Aristocratic Capital, Lindblum Industrial Regency, Midgar City of Mako, Zanarkand
  Ancient Metropolis (and Balamb Garden, which is a land→Vehicle *transform* land — closer to a DFC land that flips).

---

## Tier 2 — Small recurring primitives (cheap, scattered unlocks)

6. **Equipment / equipped-creature count as a `DynamicAmount`** — ✅ DONE. Added
   `DynamicAmounts.equipmentYouControl()` (count over `Any.withSubtype(Equipment)`) and
   `equippedCreaturesYouControl()` (count over `Creature.equipped()`) — pure composition, no new SDK type.
   → Adelbert Steiner (+1/+1 per Equipment), Barret Wallace, Slash of Light, Judgment Bolt.

7. **Devotion as a `DynamicAmount`** — ✅ DONE. Added `DynamicAmount.DevotionTo(colors, player)` (CR 700.5) +
   `DynamicAmounts.devotionTo(vararg colors)`; the evaluator counts colored mana symbols (incl. hybrid / monocolored
   hybrid / Phyrexian) on controlled permanents, read via projected controller. → Clive, Ifrit's Dominant (ETB).

8. **"First combat phase of the turn" condition + additional-combat rider** — ❌ GAP. `AddCombatPhaseEffect` exists,
   but nothing tests *"if it's the first combat phase of the turn"* to gate the extra phase (and untap-the-attacker
   rider). → Genji Glove, Balthier and Fran, Sidequest: Play Blitzball. Add an `is-first-combat-phase` condition +
   the untap-and-add-phase composition.

9. **Additional end step** — ❌ GAP. No `AddEndStepEffect` analogous to `AddCombatPhaseEffect` /
   `AddAdditionalUpkeepStepsEffect`. → Y'shtola Rhul ("there is an additional end step after this step"). One new
   player effect + turn-sequence hook.

10. **"You win coin flips / coins come up heads" replacement** — ❌ GAP. `FlipCoinEffect`/`FlipCoinsEffect` exist, but
    no replacement makes flips you make come up your way. → Edgar, King of Figaro ("the first time you flip one or
    more coins each turn, those coins come up heads and you win those flips"); The Gold Saucer is a plain flip (no
    replacement). New coin-flip replacement keyed once-per-turn.

11. **"Whenever you scry or surveil" trigger** — ✅ DONE. Added `SurveiledEvent` (mirroring `ScriedEvent`) emitted by
    `Patterns.Library.surveil(N)` via `EmitSurveiledEventEffect`, plus `Triggers.WheneverYouSurveil` and the combined
    `Triggers.WheneverYouScryOrSurveil` (indexed under both categories). Surveil reuses `TRIGGER_SCRY_COUNT` for
    "cards looked at." → Matoya, Archon Elder; Golbez (surveil trigger).

12. **`SubtypeCount` on a single creature with a cap** — verify. "+2/+2 for each of its creature types" patterns and
    self-type counts use `EntityNumericProperty.SubtypeCount` (`EntityNumericProperty.kt:93`); confirm it's reusable
    for the FIN cards that scale off their own types.

---

## Tier 3 — One-off complex cards (each needs unique new functionality)

- **Meld** (Vanille, Cheerful l'Cie + Fang, Fearless l'Cie → Ragnarok, Divine Deliverance) — ❌ GAP. No `MELD`
  layout or paired-card exile-and-combine logic. Fang is already implemented as a normal creature; meld is a distinct
  DFC-combine mechanic. Needs a meld layout + the "exile both, return the combined back face" flow. *(Ragnarok itself
  is castable/returnable today; only the meld path is missing.)*
- **"Damage you'd deal is doubled" / stagger** (Lightning, Army of One; Kuja's Flare Star; The Earth Crystal counter
  doubling; The Wind Crystal lifegain doubling; The Water Crystal mill doubling) — replacement effects that **scale
  outgoing damage / counters / life / mill by ×2**. The counter-doubling and damage-doubling one-shots exist; confirm
  a *continuous* "your sources deal double damage to a tagged player until your next turn" replacement (Lightning's
  Stagger) and the various ×2 static replacements compose, else add the missing replacement variants. *(Mirrors the
  TLA "damage-amplification replacement" gap.)*
- **Y'shtola / Gogo copy-an-ability** — Gogo, Master of Mimicry ("Copy target activated or triggered ability you
  control X times") needs ability-on-stack copying with X. Verify `CopyAbility` support; likely a gap for the
  targeted-ability-copy-X-times shape.
- **"This ability triggers an additional time"** (Cloud, Midgar Mercenary; The Masamune) — a static that makes a
  permanent's/equipment's triggered abilities trigger one extra time. Check for an existing "trigger doubling"
  primitive (Panharmonicon-style); likely a gap scoped to "triggers of this creature and Equipment attached to it."
- **Half-rounded-down sacrifice / mill** (Zodiark "sacrifices half … rounded down"; Jidoor "mills half their
  library") — `DynamicAmount.Divide(..., roundUp=false)` exists (used by Cecil); confirm it feeds a
  "each player sacrifices N of their choice" and mill-half. Likely supported once wired.
- **Blight counter that strips land types/abilities + replaces with "{T}: Add {C}"** (Ultima, Origin of Oblivion) —
  `BLIGHT` counter exists; the **continuous type/ability-stripping driven by a blight counter on a land** is the new
  piece (Layer 4/6 overwrite gated on a counter). Plus "whenever you tap a land for {C}, add an additional {C}"
  (a mana-doubling replacement on colorless taps).
- **Win/lose-the-game riders** (Zenos → Shinryu "when the chosen player loses the game, you win"; Summon: Primal Odin
  II grants "deals combat damage → that player loses the game") — confirm `Effects.WinTheGame` / `LoseTheGame` and a
  "chosen player loses → you win" linked trigger exist.
- **"Cast a spell you don't own" matters** (Vaan, Street Thief) — needs a trigger/condition on casting a spell whose
  owner isn't you (exiled-from-opponent cast). Verify ownership-vs-controller is tracked on cast for the trigger.
- **Two-permanent "this creature gets all abilities of a chosen card"** — not in FIN at the Koh scale, but Relm's
  Sketching (copy-token-of-target artifact/creature/**land**) — confirm copy-token supports copying a *land*.

---

## Recommended build order

1. ✅ **Warm-ups (Tier 2, cheap) — DONE:** Equipment/equipped-creature count `DynamicAmount` (§6), devotion dynamic amount
   (§7), scry-or-surveil trigger (§11). These unlock ~10 scattered cards with trivial engine work, and the bulk of
   the standard cycling/flashback/kicker/affinity/crew cards are already buildable today via the `add-card` skill.
2. ✅ **Job select (§3)** — publish the created-token id into the pipeline, then `jobSelect()` shell. Unlocks all 16
   Equipment at once; isolated and high-yield.
3. ✅ **Tiered (§4) — DONE:** the modal/Spree cast pipeline already charged the chosen mode's additional cost on the
   choose-1 path (no engine change); shipped the `tiered { }` builder + all 6 spells.
4. **Town land // spell DFC (§5)** — new inverse-Adventure layout. ~6 lands.
5. **Summon Sagas (§1)** — the headline `add-feature`: make Sagas co-exist with creatures + an opt-out-of-sacrifice
   flag + self-referential chapter resolution. Largest lift; gates ~15 cards.
6. **Dominant / Eikon transform (§2)** — depends on §1; add exile-and-return-transformed (new object). ~8 cards.
7. **Tier-2 turn-structure + Tier-3 one-offs** (additional end step §9, first-combat condition §8, coin-flip-win §10,
   meld, ×2 replacements, ability-copy) as the relevant legendaries/rares come up.

The four headline mechanics (Summon Sagas + their eikon backs, Job select, Tiered, the Town DFC) cover the bulk of
the genuinely-blocked cards; once they land, the remaining ~120 are standard material built on today's SDK.
