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
  `AttachTargetEquipmentToCreatureExecutor`), covering Weapons Vendor, Beatrix, Gilgamesh.
  **Raubahn, Bull of Ala Mhigo is implemented** (attack trigger reuses that effect; "up to one target Equipment"
  is an optional target, declining/illegal attach is a no-op).
  **Zack Fair is implemented** — its "attach an Equipment that was attached to it" needed *more* than the plain
  attach effect: the source is sacrificed as a cost, so the Equipment is gathered via new last-known information
  (`CardSource.LastKnownEquipmentAttachedToSource` ← `EffectContext.lastKnownSourceAttachments`, captured before
  the cost), `chooseExactly(1)` picks one when several qualify, then `AttachTargetEquipmentToCreature` re-attaches
  it; `MoveAllLastKnownCounters` now also reads the cost-sacrifice counter map. Also fixed a CR 704.5n ordering
  bug where the leave-marker SBA would tear the re-attachment back off.
- **Dynamic ward cost ("Ward—Pay life equal to ~")** — `KeywordAbility.wardLife(DynamicAmount)` →
  `WardCost.DynamicLife`, resolved at ward-trigger resolution (CR 702.21b) with last-known power if the source
  has left (CR 112.7a). Implemented for Raubahn, Bull of Ala Mhigo.
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

### 2. Dominant / Eikon transform — creature front → **Saga-creature** back via exile-and-return (≈8) — ✅ **ENGINE DONE** (cards in progress)

The Dominants (Clive→Ifrit, Jill→Shiva, Dion→Bahamut, Joshua→Phoenix, Terra→Esper Terra, Jecht, Crystal Fragments,
Esper Origins) are double-faced cards whose **back face is a Summon Saga (§1)** and whose front transforms via an
activated ability worded *"Exile [card], then return it to the battlefield transformed under its owner's control"* —
a **leave-and-re-enter** transform (a new object), not an in-place `TransformEffect` flip like Cecil. The eikon then
ticks its chapters as a creature and, on its final chapter, *"exile it, then return it to the battlefield (front face
up)"* — re-entering as the original front-face legend.

**Shipped:** the engine gap is closed by `Effects.ExileAndReturnTransformed(target, returnAs)` +
`ExileAndReturnTransformedExecutor` (`ReturnFace.TRANSFORMED` for front→back, `FRONT` for the eikon final
chapter's "front face up"). It exiles a DFC and re-enters it as a *new object* via the standard
`ZoneTransitionService.moveToZone` path, so a Saga back re-enters with one lore counter (CR 714.2b) and ETB/LTB
triggers fire (not transform triggers) — distinct from in-place `TransformEffect`. The Craft return
(`ReturnSelfFromExileTransformed`) was refactored to share the same flip-and-return-from-exile helper. The eikon
backs need **no** sacrifice opt-out flag: their final chapter exile-returns them front face up *before* the
CR 714.4 sacrifice SBA applies (chapter on the stack → no sac; resolves → no longer a Saga). Covered by
`DominantEikonTransformScenarioTest` (front→back new object + fresh lore, eikon final-chapter return-to-front
instead of sacrifice, sorcery-speed gating, second card).
→ ✅ Clive // Ifrit, Jill // Shiva, Jecht // Braska's Final Aeon (front = "may" combat-damage trigger; back = a
  plain sacrifice-after-III Summon Saga, no return). The remaining cards reuse the **same** transform effect; they
  are blocked only on **unrelated chapter/effect primitives**, not on §2:
  - Dion // Bahamut — needs the "during your turn, Knights you control have flying" conditional static grant.
  - Joshua // Phoenix — needs "return any number of target creature cards with total mana value ≤ 6 from your
    graveyard to the battlefield" (any-number-with-total-MV-budget reanimation).
  - Terra // Esper Terra — needs "create a token copy of target nonlegendary enchantment, if it's a Saga put up
    to three lore counters on it, sacrifice at the next end step".
  - Crystal Fragments // Summon: Alexander — Equipment front works via `CardDefinition.doubleFacedPermanent`, but
    the Alexander back needs a **group damage-prevention shield** ("prevent all damage that would be dealt to
    creatures you control this turn" — `PreventDamageEffect` only targets a single entity today).
  - Esper Origins — a different template (a *sorcery* that, when cast from a graveyard, exiles itself off the stack
    and *puts itself onto the battlefield transformed with a finality counter*) — a spell-becomes-permanent shape,
    not the permanent exile-and-return this effect models.

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

### 5. Town land // spell DFC — "play the land from exile later" (5) — ✅ **DONE** (reused ADVENTURE layout)

Several Town lands are double-faced: front = `Land — Town` (enters tapped, taps for mana), back = a one-shot
instant/sorcery, with reminder *"(Then exile this card. You may play the land later from exile.)"* You either play
the land **or** cast the spell; casting the spell exiles the card and lets you **play the land half from exile**
later. These cards are literally typed `— Adventure` (CR 715) on Scryfall, and it turned out **no new `CardLayout`
was needed**: `CardLayout.ADVENTURE` already models "spell resolves → exile → replay the main face from exile", and
the resolution grant (`StackResolver` `adventureFaceExile` → generic `MayPlayPermission`) plus the exile-replay
(`CastFromZoneEnumerator` / `PlayLandHandler`) are blind to whether the primary face is a creature or a land. The
only engine gap was `CastSpellEnumerator` skipping land-primary cards before its secondary-face enumeration — fixed
so a land // spell Adventure offers *both* "play the land" (PlayLandEnumerator) and "cast the Adventure spell"
(`CastSpell.faceIndex = 0`). All five lands implemented + `TownLandSpellAdventureEnumerationTest` /
`TownLandSpellAdventureScenarioTest` (play-from-hand, cast → exile → replay-from-exile, land-drop gating).
→ Ishgard the Holy See, Jidoor Aristocratic Capital, Lindblum Industrial Regency, Midgar City of Mako, Zanarkand
  Ancient Metropolis. (Balamb Garden is a land→Vehicle *transform* land — a separate "DFC land that flips" mechanic,
  still a gap.)

---

## Tier 2 — Small recurring primitives (cheap, scattered unlocks)

6. **Equipment / equipped-creature count as a `DynamicAmount`** — ✅ DONE. Added
   `DynamicAmounts.equipmentYouControl()` (count over `Any.withSubtype(Equipment)`) and
   `equippedCreaturesYouControl()` (count over `Creature.equipped()`) — pure composition, no new SDK type.
   → Adelbert Steiner (+1/+1 per Equipment), Barret Wallace, Slash of Light, Judgment Bolt.

7. **Devotion as a `DynamicAmount`** — ✅ DONE. Added `DynamicAmount.DevotionTo(colors, player)` (CR 700.5) +
   `DynamicAmounts.devotionTo(vararg colors)`; the evaluator counts colored mana symbols (incl. hybrid / monocolored
   hybrid / Phyrexian) on controlled permanents, read via projected controller. → Clive, Ifrit's Dominant (ETB).

8. **"First combat phase of the turn" condition + additional-combat rider** — ⚠️ PARTIAL. `AddCombatPhaseEffect`
   exists, but nothing tests *"if it's the first combat phase of the turn"* to gate the extra phase. Genji Glove ships
   using the accepted `oncePerTurn = true` anti-infinite-loop approximation (same precedent as Raph & Leo / Éomer —
   composes `Triggers.attacks(binding = ATTACHED)` + untap the equipped creature + `Effects.AddCombatPhase`); the two
   diverge only when another source of additional combat exists *and* the equipped creature first attacks in combat #2.
   Still desirable: a faithful `Conditions.IsFirstCombatPhase` primitive to swap in for `oncePerTurn`. → Genji Glove
   (DONE via approximation), Balthier and Fran, Sidequest: Play Blitzball.

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
  layout or paired-card exile-and-combine logic. All three cards are now implemented as normal creatures with their
  non-meld abilities (Fang's graveyard-leave trigger, Vanille's mill-and-return ETB, Ragnarok's dies trigger);
  following the Brisela precedent, each omits its meld linkage and documents it. Meld remains a distinct DFC-combine
  mechanic: still needs a meld layout + the "exile both, return the combined back face" flow to wire the trio together.
- **"Damage you'd deal is doubled" / stagger** (Lightning, Army of One; Kuja's Flare Star; The Earth Crystal counter
  doubling; The Wind Crystal lifegain doubling) — replacement effects that **scale
  outgoing damage / counters / life / mill**. The counter-doubling and damage-doubling one-shots exist; confirm
  a *continuous* "your sources deal double damage to a tagged player until your next turn" replacement (Lightning's
  Stagger) and the various ×2 static replacements compose, else add the missing replacement variants. *(Mirrors the
  TLA "damage-amplification replacement" gap.)* **The Water Crystal is now implemented** — its "an opponent
  mills that many cards plus four instead" is the additive `ModifyMillAmount` replacement (twin of `ModifyDrawAmount`),
  applied at the mill announcement in `GatherCardsExecutor` via the new `CardSource.TopOfLibrary.isMill` flag +
  `EventPattern.MillEvent`.
- **Y'shtola / Gogo copy-an-ability** — ✅ **DONE.** Gogo, Master of Mimicry ("Copy target activated or triggered
  ability you control X times") implemented by generalizing `CopyTargetSpellOrAbilityEffect` with a
  `copies: DynamicAmount` (pass `DynamicAmount.XValue`); the executor makes N independent copies of the chosen
  ability, pausing per copy that has targets for CR 707.10c retargeting, and copies no-target abilities too.
  Added `Targets.ActivatedOrTriggeredAbilityYouControl`, `ActivatedAbility.minimumXValue` ("X can't be 0") and
  `ActivatedAbility.cantBeCopied` ("This ability can't be copied", reusing the `CantBeCopiedComponent` marker).
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
- ~~**"Cast a spell you don't own" matters** (Vaan, Street Thief)~~ — ✅ IMPLEMENTED. `SpellCastPredicate.NotOwnedByController`
  (owner ≠ caster) already existed; Vaan composes it with the per-damaged-player `OneOrMoreDealCombatDamageToPlayerEvent`
  batch (now exposes `Player.TriggeringPlayer` = the damaged player) + `MayEffect(CastFromCollection(payManaCost), otherwise =
  CreateTreasure)`. Also fixed a nested-cast double-trigger: cast-during-resolution now propagates
  `triggersAlreadyProcessed` through `EffectResult` so "whenever you cast a spell" fires once.
- **Two-permanent "this creature gets all abilities of a chosen card"** — not in FIN at the Koh scale, but Relm's
  Sketching (copy-token-of-target artifact/creature/**land**) — confirm copy-token supports copying a *land*.
- **"Destroy up to one Equipment attached to that creature"** (Light of Judgment) — ❌ GAP. The damage half is
  trivial (`Effects.DealDamage(6, target)`), but the rider needs an **optional, up-to-one** select-and-destroy of
  the Equipment attached to the *damaged* creature. `Effects.DestroyAllEquipmentOnTarget` is mandatory and destroys
  *all* of them; there is no "attached to that creature" target/group filter (only `IsAttachedToBySource` /
  `AttachedToCardType`) and no up-to-N optional select-attached-and-destroy primitive. Needs either a
  `DestroyEquipmentOnTarget(target, max = 1, optional = true)` effect with a selection continuation, or an
  "attached-to-context-target" `StatePredicate` usable in the gather→select→destroy pipeline.

---

## Recommended build order

1. ✅ **Warm-ups (Tier 2, cheap) — DONE:** Equipment/equipped-creature count `DynamicAmount` (§6), devotion dynamic amount
   (§7), scry-or-surveil trigger (§11). These unlock ~10 scattered cards with trivial engine work, and the bulk of
   the standard cycling/flashback/kicker/affinity/crew cards are already buildable today via the `add-card` skill.
2. ✅ **Job select (§3)** — publish the created-token id into the pipeline, then `jobSelect()` shell. Unlocks all 16
   Equipment at once; isolated and high-yield.
3. ✅ **Tiered (§4) — DONE:** the modal/Spree cast pipeline already charged the chosen mode's additional cost on the
   choose-1 path (no engine change); shipped the `tiered { }` builder + all 6 spells.
4. ✅ **Town land // spell DFC (§5) — DONE:** reused `CardLayout.ADVENTURE` (these cards are typed `— Adventure`);
   the only engine change was enumerating the Adventure spell face for a land-primary card. 5 lands shipped.
5. ✅ **Summon Sagas (§1)** — the headline `add-feature`: make Sagas co-exist with creatures + an opt-out-of-sacrifice
   flag + self-referential chapter resolution. Largest lift; gates ~15 cards.
6. ✅ **Dominant / Eikon transform (§2) — ENGINE DONE:** `Effects.ExileAndReturnTransformed(target, returnAs)`
   (new object, both directions) + shared flip-and-return helper with the Craft return. Clive//Ifrit, Jill//Shiva,
   Jecht//Braska shipped; the remaining ~5 reuse the same effect and are blocked only on unrelated chapter
   primitives (see §2).
7. **Tier-2 turn-structure + Tier-3 one-offs** (additional end step §9, first-combat condition §8, coin-flip-win §10,
   meld, ×2 replacements, ability-copy) as the relevant legendaries/rares come up.

The four headline mechanics (Summon Sagas + their eikon backs, Job select, Tiered, the Town DFC) cover the bulk of
the genuinely-blocked cards; once they land, the remaining ~120 are standard material built on today's SDK.

---

## Implementation pass — 2026-06-26 (add-card batch)

Implemented:
- **Sage's Nouliths** (#70) — Job-select Equipment; +1/+0, grants the equipped creature
  "Whenever this creature attacks, untap target attacking creature" (Web-Shooters–style
  `GrantTriggeredAbility` with an attacking-creature target), Cleric subtype, `Hagneia — Equip {3}`
  ("Hagneia" is a rules-inert ability word). Pure authoring, no engine change.

Deferred (need `add-feature`, not pure authoring):
- **Crystal Fragments** (#13) — transforming Equipment // `Summon: Alexander` Saga creature DFC.
  Falls under the headline §1/§2 work: the back is an Enchantment Creature — Saga whose chapters need
  "prevent all damage to creatures you control this turn" (a damage-prevention chapter primitive the
  engine lacks), and the front is an *Equipment* that exiles-and-returns-transformed (the
  `ExileAndReturnTransformed` helper today targets creature // creature flips). Not straightforward.
- **Stolen Uniform** (#75) — "Gain control of target Equipment until end of turn, attach it to a
  chosen creature you control; when you lose control of it this turn, unattach it." Needs a
  *temporary control change of an Equipment* + a forced *attach* effect + a brand-new
  "**when you lose control of …**" delayed trigger (no such trigger exists today).
- **Blazing Bomb** (#130) — the cast trigger ("noncreature spell, ≥4 mana spent → +1/+1 counter") is
  fully buildable today via `Conditions.TriggeringSpellManaSpentAtLeast(4)`. The **Blow Up** half
  ("{T}, Sacrifice this creature: it deals damage equal to its power") is blocked: `AbilityCost.SacrificeSelf`
  does **not** populate `EffectContext.sacrificedPermanents`, so `DynamicAmounts.sacrificedPower()`
  (and `sourcePower()`) resolve to **0** at resolution — the self-sacrificed source's last-known
  power is never snapshotted. Fix is a small reusable engine addition mirroring the existing
  `lastKnownSourceCounters` capture (snapshot the self-sacrificed source's P/T, or add a
  `DynamicAmount.LastKnownSourcePower`). Note: **Cinder Shade (INV)** and **Ghitu Fire-Eater (ULG)**
  share this latent gap.

## Implementation pass — 2026-06-28 (add-card batch)

Implemented (all pure authoring, no engine change):
- **The Prima Vista** (#64) — Vehicle; Flying + Crew 2 + "whenever you cast a noncreature spell, if
  at least four mana was spent, this becomes an artifact creature until end of turn"
  (`Triggers.YouCastNoncreature` + `Conditions.TriggeringSpellManaSpentAtLeast(4)` intervening-if →
  `Effects.BecomeCreature(Self, 5/3, EndOfTurn)`).
- **The Gold Saucer** (#279) — `Land — Town`; taps for {C}; "{2},{T}: flip a coin, on a win create a
  Treasure" (`FlipCoinEffect(wonEffect = Effects.CreateTreasure(1))`); "{3},{T}, Sacrifice two
  artifacts: Draw a card" (`Costs.SacrificeMultiple(2, GameObjectFilter.Artifact)`).
- **Eden, Seat of the Sanctum** (#277) — `Land — Town`; taps for {C}; "{5},{T}: Mill two, then you may
  sacrifice Eden. When you do, return another target permanent card from your graveyard to your hand"
  (`ReflexiveTriggerEffect` with `optional = true`; the reflexive target is chosen after the sacrifice,
  so `excludeSelf = true` models "another").
- **Jenova, Ancient Calamity** (#228) — combat trigger puts +1/+1 counters equal to its power on up to
  one other target creature, which becomes a Mutant (`AddDynamicCountersEffect` + `Effects.AddCreatureType`);
  "whenever a Mutant you control dies during your turn, draw cards equal to its power"
  (`Triggers.leavesBattlefield` filtered to Mutants + `Conditions.IsYourTurn` + `DynamicAmounts.triggeringPower`).

Resolved (was deferred):
- **The Lunar Whale** (#60) — ✅ implemented. Vehicle; Flying/Crew 1 + `LookAtTopOfLibrary` +
  `ConditionalStaticAbility(PlayLandsAndCastFilteredFromTopOfLibrary(GameObjectFilter.Any),
  Conditions.SourceAttackedThisTurn)`. The non-revealing "play the top card" (any type) is modeled as
  the filtered play-top permission with an unrestricted `GameObjectFilter.Any` spell filter. The engine
  change that unblocked it: the four play/cast-from-top readers — `CastPermissionUtils.hasPlayLandsFromTopOfLibrary`
  / `getCastFilteredFromTopOfLibraryFilter`, `PlayLandHandler.hasPlayFromTopOfLibrary`, and
  `CastZoneResolver.hasCastFromTopOfLibraryPermission` — now unwrap `ConditionalStaticAbility` and
  evaluate the gate against the granting permanent (mirroring the existing graveyard-play / equip
  conditional handling), so the land-play and spell-cast paths both honor the "attacked this turn"
  gate. Scenario test: `TheLunarWhaleScenarioTest`.

## Implementation pass — 2026-07-03 (add-card batch)

Implemented (pure authoring, no engine change):
- **Sidequest: Catch a Fish** (#31) — `Enchantment // Land` transform DFC. Front upkeep ability is a
  look-top → may-take → reflexive pipeline: `Effects.Pipeline { gather(TopOfLibrary(1)); chooseUpTo(1,
  filter = Artifact or Creature); ifNotEmpty(kept) { toHand(kept); run(CreateFood()); run(TransformEffect(Self)) } }`
  — the `ifNotEmpty` gate reproduces "if you put a card into your hand this way" exactly (a declined or
  non-matching top card leaves the card on top and the enchantment un-transformed, per the 2025-06-06
  ruling). The back **Cooking Campsite** is a colorless `Land`: `{T}: Add {W}` mana ability + a
  sorcery-speed `{3},{T}, Sacrifice an artifact` activated ability that puts a +1/+1 counter on each
  creature you control (`ForEachInGroup(Creature.youControl()) { AddCounters(PLUS_ONE_PLUS_ONE) }`).
  Scenario test: `SidequestCatchAFishScenarioTest` (take → Food + transform; land top → no take, no Food,
  no transform).

Screened but deferred (each needs `add-feature`, not pure authoring — confirmed against source):
- **Kain, Traitorous Dragoon** — "that player (the one dealt combat damage) gains control of Kain … you
  draw/treasure/lose that many": no EffectTarget/Player for the damaged player as a `newController` on the
  non-batch `DealsCombatDamageToPlayer` trigger, and no triggering-combat-damage-amount `DynamicAmount`.
- **Firion, Wild Rose Warrior** — "create a token copy of the entering Equipment, except it has [equip-cost
  reduction static]": `CreateTokenCopyOfTargetEffect` grants added *keywords/activated/triggered* abilities
  but has no `addedStaticAbilities`, and equip-cost-reduction isn't a grantable static on a token copy.
- **Emet-Selch, Unsundered** — back "During your turn, you may play cards from your graveyard" needs a
  conditional (your-turn) wrapper the graveyard-play readers don't unwrap (only the top-of-library readers
  do, via The Lunar Whale's fix), plus a controller-scoped "cards → exile instead of graveyard" replacement
  (Rest in Peace's is all-players).
- **Sidequest: Play Blitzball** — end-of-combat "if a player was dealt 6+ combat damage this turn"
  condition doesn't exist (only a legendary-creature-specific variant), and there's no "transform this,
  then attach it to a creature you control" effect.
- The remaining missing FIN cards are the documented Tier-3 one-offs (Ultima → *End the turn*; Ultima,
  Origin of Oblivion → blight land-strip; The Masamune → trigger-doubling; Gogo → copy-ability-N-times;
  Joshua → any-number total-MV reanimation; Edgar → coin-flip-win replacement; Esper Origins →
  spell-becomes-permanent; Kefka/Sephiroth/Zenos/Terra transform backs → per-card chapter/emblem/win-rider
  primitives). All are `add-feature` scope.

## Implementation pass — 2026-07-03 (Sephiroth + stale-gap corrections)

Two of this doc's "gaps" had already closed on `main` by the time of this pass — the doc above is
stale on both:
- **Ultima** (*End the turn*) — **already implemented and shipped**. `EndTheTurnEffect`
  (`Effects.EndTheTurn`) + `TurnManager.performEndTheTurn` (CR 722) exist; `Ultima.kt` is a live FIN card.
- **Sephiroth, Fabled SOLDIER** — **NOT `add-feature` scope after all; pure authoring, now implemented.**
  The transform back is a plain creature//creature in-place `TransformEffect` (Cecil precedent), *not* a
  Saga back, so it needs no chapter primitives. Every piece already exists:
  - "enters or attacks" → two sibling triggered abilities (Gilgamesh/Frodo shape).
  - "you may sacrifice another creature. If you do, draw" → `OptionalCostEffect(cost = SacrificeEffect(
    excludeSource), ifPaid = DrawCards(1))`; the back's "sacrifice any number of other creatures, draw
    that many" → `SacrificeEffect(any = true, excludeSource = true)` + `DrawCards(permanentsSacrificedThisWay)`.
  - "whenever another creature dies, target opponent loses 1 / you gain 1" → `leavesBattlefield(Creature,
    to = GRAVEYARD, binding = OTHER)` + drain (Al Bhed Salvagers precedent).
  - "if this is the fourth time this ability has resolved this turn, transform" →
    `IncrementAbilityResolutionCountEffect` + `Conditions.SourceAbilityResolvedNTimes(4)` +
    `TransformEffect(Self)` (Harvestrite Host precedent). A simultaneous death of Sephiroth + others
    still counts each other creature but no-ops the Self transform — matching the ruling.
  - "Super Nova" back emblem → transform-to-back trigger → `CreateGlobalTriggeredAbility(duration =
    Permanent, ability = "whenever a creature dies, target opponent loses 1 / you gain 1")` (Death Frenzy
    precedent). Covered by `SephirothFabledSoldierScenarioTest` (drain, fourth-resolution transform,
    emblem still draining post-transform).
- **Edgar, King of Figaro** — implemented; needed one small, reusable engine addition (closes the §10
  coin-flip-win gap). The ETB "draw a card for each artifact you control" is pure authoring; the
  Two-Headed Coin static is a new `WinCoinFlips(firstFlipEachTurn)` static ability — a coin-flip
  **result replacement** (CR 705.3, not a Rule 613 layer effect). The three coin-flip executors query
  it through a shared `CoinFlipModifiers` utility (mirroring `LifeGainModifiers`); a per-player
  `FlippedCoinsThisTurnComponent` (cleared at cleanup) implements the "first time each turn" gate. The
  primitive is general: `firstFlipEachTurn = false` gives a plain "you win all coin flips". Covered by
  `EdgarKingOfFigaroScenarioTest` (forced first-flip win via The Gold Saucer, the once-per-turn gate,
  and the artifact-count ETB draw).

## Implementation pass — 2026-07-05 (Terra, Magical Adept + two engine additions)

- **Terra, Magical Adept // Esper Terra** — implemented (295/300). The front ETB (mill five, put up to
  one enchantment milled this way into hand) and the Trance `ExileAndReturnTransformed` into the Esper
  Terra Summon-Saga back are pure authoring (CacheGrab / Clive-Ifrit precedents). Chapter IV (`AddMana`
  ×5 colors + `ExileAndReturnTransformed(FRONT)`) is authoring too; it exile-returns front face up before
  the CR 714.4 final-chapter sacrifice applies, like the Dominant eikons. Two small, reusable engine
  additions unblocked chapters I–III:
  - **`AddCountersUpTo(counterType, max, target)`** (`Effects.AddCountersUpTo`) — the additive,
    single-kind, player-chosen mirror of `RemoveAnyNumberOfCounters`: one `ChooseNumberDecision` (0..max),
    then placement through the normal `AddCounters` chokepoint (honors placement replacements + Saga
    chapter triggers). Chapter I–III's "if it's a Saga, put up to three lore counters on it" composes as
    `ConditionalEffect(CollectionContainsMatch(CREATED_TOKENS, Enchantment.withSubtype(SAGA)),
    AddCountersUpTo(LORE, 3, PipelineTarget(CREATED_TOKENS)))`. mtgish bridge: `UptoNumberCountersOfTypeOnPermanent`
    → `AddCountersUpTo` (capability-only; player-choice shape stays SCAFFOLD). Tested by
    `AddCountersUpToScenarioTest`.
  - **Token copies of a Saga now enter as Sagas (CR 714.2b/714.3a)** — the `CreateTokenCopyOf*` executors
    (target/source/chosen/equipped) route through the shared `ZoneMovementUtils.applySagaEntryIfNeeded`
    hook (`BattlefieldEntry.place`, the ad-hoc insertion path, skips enters-with-counters setup), so a
    token copy of a Saga gains a `SagaComponent`, its on-enter lore counter (chapter I triggers), and
    accrues lore each turn. Previously such tokens were inert. Tested by `TokenCopyOfSagaEntryScenarioTest`
    (generally) and `TerraMagicalAdeptScenarioTest` (via the card). Covered by `TerraMagicalAdeptScenarioTest`
    (ETB mill-take, Trance transform, chapter copy with/without the Saga lore prompt).
- **Zenos yae Galvus // Shinryu, Transcendent Rival — DONE.** Front's "My First Friend" ETB is an
  optional `target(CreatureOpponentControls, optional = true)` + `ForEachInGroup(AllCreatures
  .other().otherThanTarget(), ModifyStats(-2,-2))` (the -2/-2 still resolves when no creature is
  chosen, per the ruling), plus a persistent reflexive delayed trigger (`Triggers.LeavesBattlefield`
  + `watchedTarget` + new `DelayedTriggerExpiry.Never`) that transforms Zenos when the chosen
  creature leaves. Shinryu's win-rider uses the new `Triggers.AnyPlayerLosesGame`
  (`EventPattern.PlayerLostGameEvent` → engine `PlayerLostEvent`) gated by the new
  `Conditions.TriggeringPlayerIs(Player.ChosenOpponent)` + `Effects.WinGame()`. Also fixed
  `TriggerMatcher.filterByTriggerCondition` to thread `triggeringPlayerId` into the intervening-if
  context (previously null). Covered by `ZenosYaeGalvusScenarioTest` (incl. a 3-player win-con pod).
- Remaining missing FIN cards (2): Esper Origins; Ultima, Origin of Oblivion — both still `add-feature`
  scope (see Tier-3 above). (Emet-Selch, Unsundered and Gogo, Master of Mimicry are now implemented.)
