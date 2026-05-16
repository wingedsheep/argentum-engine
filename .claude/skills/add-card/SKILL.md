---
name: add-card
description: Implements new Magic: The Gathering cards for the Argentum Engine. Use when adding a new card, the user provides a card name to implement, or asks to implement a specific MTG card.
argument-hint: <card-name> [--set <set-code>]
---

# Add MTG Card

Implement the card specified in `$ARGUMENTS`. The card name is the main argument; `--set <set-code>` is optional (defaults to `por` for Portal).

## Step 1: Look Up Card Data on Scryfall

**REQUIRED**: Always fetch exact card data before implementing. Never guess card text or stats.

1. WebFetch: `https://api.scryfall.com/cards/named?exact=<card-name-url-encoded>&set=<set-code>`
   - **ALWAYS include `&set=<code>`** — each printing has different rarity, collector_number, artist, flavor_text, image_uris
   - Extract: name, mana_cost, type_line, oracle_text, power, toughness, colors, rarity, collector_number, artist, flavor_text, image_uris.normal
   - Common set codes: `ons` (Onslaught), `lgn` (Legions), `scg` (Scourge), `por` (Portal), `lea` (Alpha), `leb` (Beta), `2ed` (Unlimited), `ktk` (Khans of Tarkir)

2. **Check for Oracle Errata & Rulings** — `oracle_text` has current wording. Document significant errata (e.g., "Bury" → "Destroy + can't be regenerated", "Remove from game" → "Exile") in a comment above the card definition.
   - Fetch rulings: `https://api.scryfall.com/cards/named?exact=<card-name-url-encoded>&set=<set-code>` → use `rulings_uri` from response to fetch rulings list
   - Add mechanically significant rulings to the `metadata { }` block using `ruling(date, text)` — especially rules updates that changed card behavior (e.g., Champions of Kamigawa Wall/Defender errata, creature type updates, functional errata)
   - Skip trivial reminder-text rulings that just restate the rules (e.g., "Flying means...")

3. Parse oracle text: card type, abilities (spell/triggered/activated/static/replacement), targeting, keywords, conditions.

4. **Check for earlier printings — decide canonical set before writing any code.**
   Many cards in newer sets are reprints. The canonical `CardDefinition` must live in the
   card's **earliest real-expansion printing** (per Scryfall, skipping promo / token /
   art_series). The asked-for set gets only a `Printing` row. Decision flow:

   a. **Is the card already implemented somewhere?**
      `grep -rn "name = \"<Card Name>\"" mtg-sets/src/main/kotlin/` — if a
      `CardDefinition` exists in another set, skip to Step 10b (add a reprint in the
      asked-for set; do **not** duplicate the script). Still run the verifier in 4d to
      confirm the existing canonical is in the right set.

   b. **Otherwise, list all printings on Scryfall:** follow `prints_search_uri` from the
      Step 1 response (or `https://api.scryfall.com/cards/search?q=%21%22<Card+Name>%22&unique=prints&order=released&dir=asc`).
      Take the *first* real-expansion-type printing (skip `promo`, `token`,
      `art_series`, `memorabilia`). Call that the **earliest real set**.

   c. **Place the canonical CardDefinition.** Three cases:

      - **Earliest real set == asked-for set** → proceed with Steps 2–10 normally in
        that set.
      - **Earliest real set is already scaffolded** under
        `mtg-sets/.../definitions/<setcode>/` → implement the full `CardDefinition`
        in *that* set (re-fetch Scryfall data with `&set=<earliest-code>` for
        authoritative metadata), and add a `Printing` row in the asked-for set via
        Step 10b.
      - **Earliest real set is NOT scaffolded** → scaffold it. Create a minimal
        `MtgSet` object under
        `mtg-sets/.../definitions/<earliest-code>/<EarliestSet>Set.kt` following the
        same pattern as existing sets (e.g. Tempest's
        `mtg-sets/.../definitions/tmp/TempestSet.kt`), wire it into
        `META-INF/services/com.wingedsheep.sdk.MtgSet`, then implement the canonical
        `CardDefinition` there. The asked-for set still gets a `Printing` row via
        Step 10b.

      If scaffolding the earliest set is out of scope for this PR (e.g. it would
      balloon the change set), document the deviation in the commit message and ask
      the user before falling back to a later set as canonical.

   d. **Backlog bookkeeping.** If the canonical set differs from the asked-for set, check
      off the card in **both** backlogs that list it (e.g. canonical set's `cards.md`
      and the reprinting set's deck/booster checklist), so neither tracker shows it as
      missing.

   e. **Verify after finishing.** Once the canonical file (and any reprint row) is in
      place, run `just check-card-printing "<Card Name>"`. The script lists every
      Scryfall printing alongside which sets are scaffolded in the repo, and exits
      non-zero if the canonical isn't in the earliest real printing's set, the earliest
      real set isn't scaffolded, or a scaffolded printing is missing a `Printing(...)`
      row. Fix any drift it reports before committing.

## Step 2: Check Existing Effects

**Always prefer atomic effects over monolithic effects** for reusability.

1. **Read [docs/architecture-principles.md](../../../docs/architecture-principles.md)** — understand the application architecture, design decisions, and how effects/executors/continuations fit together before implementing anything.
2. **Read [docs/card-sdk-language-reference.md](../../../docs/card-sdk-language-reference.md)** — complete inventory of all DSL facades (`Effects.*`, `Targets.*`, `Triggers.*`, `Filters.*`, `Costs.*`, `Conditions.*`, `DynamicAmount.*`, `EffectPatterns.*`), raw effect types, keywords, static abilities, replacement effects, and more.
3. **Search existing cards** with similar mechanics: `grep -r "<keyword-or-effect>" mtg-sets/src/main/kotlin/`

## Step 3: Model the Card

**File**: `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/{set}/cards/{CardName}.kt`

1. Write the card definition using DSL facades (`Effects.*`, `Targets.*`, `Triggers.*`, `Filters.*`, etc.) over raw constructors. See [examples.md](examples.md) for all patterns.

2. **Image URI — CRITICAL**:
   - **ALWAYS use the exact `image_uris.normal` URL from the Scryfall API response**
   - **NEVER generate, guess, or hallucinate an image URI** — they have specific hash-based paths
   - **Include the query parameter** (e.g., `?1562911270`) — it's part of the URL
   - **Verify with HEAD request**: `curl -sI "<image-url>" | head -1` — must return HTTP 200
   - If not 200, re-fetch the correct image URL from Scryfall for the target set

3. **Register** in `mtg-sets/.../definitions/{set}/{Set}Set.kt` — add to `allCards` list (alphabetically or by collector number)

4. **Auras**: Must use `typeLine = "Enchantment — Aura"` (modern Oracle errata) and need `auraTarget` in card script.

5. **Token Image URIs** — When a card creates creature tokens, look up the token's image on Scryfall and pass it via `imageUri`:
   - Search: `https://api.scryfall.com/cards/search?q=t%3Atoken+name%3A%22<TokenName>%22+set%3At<set-code>` (token sets use `t` prefix, e.g., `tblb` for BLB tokens)
   - If the token exists in the card's set, use its `image_uris.normal`
   - If not found in the set, search without the set filter: `https://api.scryfall.com/cards/search?q=t%3Atoken+name%3A%22<TokenName>%22&order=released&dir=desc` and use the most recent printing
   - Pass to `Effects.CreateToken(..., imageUri = "<url>")` or `CreateTokenEffect(..., imageUri = "<url>")`
   - If no matching token exists on Scryfall at all, omit `imageUri`

## Step 4: Implement Missing Effects

If the card needs effects, keywords, triggers, conditions, or static abilities that don't exist yet:

**4.0 Always try atomic composition first** — Express behavior as `CompositeEffect` of existing effects or `EffectPatterns.*` helper. For library manipulation, use `GatherCards → SelectFromCollection → MoveCollection` pipeline. Only create new effect types when no composition works.

**If a new effect type is truly needed:**

**Design for reusability** — New effects, static abilities, triggers, conditions, and components must be **general-purpose and parameterized**, not card-specific. Apply these principles:

1. **Target generality** — Effects should work on any valid entity type, not just one. The executor handles each entity type differently.
   - BAD: `GrantPlayerShroudEffect` — only works on players
   - GOOD: `GrantShroudEffect(target: EffectTarget, duration: Duration)` — works for players, creatures, and planeswalkers. Executor checks if target is a player (adds `PlayerShroudComponent`) or permanent (creates floating effect with `GrantKeyword(SHROUD)`).

2. **Duration/removal generality** — Don't bake timing into type names. Both effects and components should have a configurable duration or removal condition. The cleanup system reads the field to decide when to remove.
   - BAD: `GrantShroudUntilEndOfTurnEffect` — hardcodes "until end of turn" in the type name
   - GOOD: `GrantShroudEffect(duration: Duration)` — one type, any duration (EndOfTurn, EndOfCombat, Permanent, etc.)
   - BAD: `PlayerShroudUntilEndOfTurnComponent` (data object) — a new type for each duration
   - GOOD: `PlayerShroudComponent(removeOn: PlayerEffectRemoval)` with enum `{ EndOfTurn, Permanent }` — one type, multiple durations. `TurnManager.cleanupEndOfTurn()` checks `removeOn == EndOfTurn` to remove; `Permanent` instances survive.

3. **Parameterized filters and amounts** — Use `GameObjectFilter`, `DynamicAmount`, and configurable parameters so the same effect works across many cards.
   - BAD: `ReduceCostOfGoblinsEffect` — only reduces Goblin costs
   - GOOD: `ReduceSpellCostEffect(filter: GameObjectFilter, amount: DynamicAmount)` — works for any creature type or card property
   - BAD: `GainLifeWhenGoblinEntersEffect` — card-specific trigger
   - GOOD: general trigger `OnCreatureEntersBattlefield(filter)` paired with `Effects.GainLife(amount)`

4. **Name the mechanic, not the card** — The effect name should describe the *mechanic*, not the specific card it was built for.

- **4.1 Add Effect Type** in `mtg-sdk/.../scripting/effect/` (`DamageEffects.kt`, `LifeEffects.kt`, `DrawingEffects.kt`, `RemovalEffects.kt`, `PermanentEffects.kt`, `LibraryEffects.kt`, `ManaEffects.kt`, `TokenEffects.kt`, `CompositeEffects.kt`, `CombatEffects.kt`, `PlayerEffects.kt`, `StackEffects.kt`)
- **4.2 Create Executor** in `rules-engine/.../handlers/effects/{category}/`
- **4.3 Register Executor** in `{Category}Executors.kt`
- **4.4 Add DSL Facade** in `mtg-sdk/.../dsl/Effects.kt`
- **4.5 Add Keyword** (if needed) in `mtg-sdk/.../core/Keyword.kt` + `web-client/src/types/enums.ts` (enum + KeywordDisplayNames) + `web-client/src/assets/icons/keywords/index.ts` if icon needed
- **4.5b Add Counter Type** (if needed) — counter types span **5 layers** and ALL must be updated:
  1. `mtg-sdk/.../core/CounterType.kt` — add enum value + `Counters` string constant
  2. `rules-engine/.../mechanics/layers/StateProjector.kt` — add to `KEYWORD_COUNTER_MAP` if it's a keyword counter (flying, indestructible, trample, etc.) so it grants the keyword via projected state
  3. `web-client/src/types/enums.ts` — add to `CounterType` enum + `CounterTypeDisplayNames`
  4. `web-client/src/assets/icons/keywords/index.ts` — add to `counterManaClass` (use `ability-<keyword>` for keyword counters, `counter-<style>` for others). Note: mana-font has specific counter icons like `counter-flood`, `counter-lore`, `counter-bolt`, `counter-charge`, etc.
  5. **Frontend counter badge visualization** — each counter type needs explicit wiring in 3 files (there is NO generic counter renderer):
     - `web-client/src/components/game/board/shared.ts` — add `getXxxCounters(card)` helper function
     - `web-client/src/components/game/board/styles.ts` — add `xxxCounterBadge` style (position absolute, top/right, themed colors)
     - `web-client/src/components/game/card/GameCard.tsx` — import the helper, add a `{/* Xxx counter badge */}` JSX block (follow the pattern of existing counter badges like `blightCounterBadge`)
- **4.6 Add Static Ability** (if needed) in `mtg-sdk/.../scripting/StaticAbility.kt`
- **4.7 Add Replacement Effect** (if needed) in `mtg-sdk/.../scripting/ReplacementEffect.kt`
- **4.8 Add Trigger** (if needed) in `mtg-sdk/.../scripting/trigger/` + facade in `mtg-sdk/.../dsl/Triggers.kt`
- **4.9 Add Condition** (if needed) in `mtg-sdk/.../scripting/condition/` + facade in `mtg-sdk/.../dsl/Conditions.kt`
- **4.10 Update [docs/card-sdk-language-reference.md](../../../docs/card-sdk-language-reference.md)** with every new
  building block — effect, trigger, condition, filter, cost, keyword, dynamic amount, modal shape, replacement
  effect, etc. This document is the canonical SDK catalog; **any SDK addition or change must update it in the same
  change**, in the appropriate section (§4 Effects, §5 Effect patterns, §7 Filters, §8 Triggers, §9 Static
  abilities, §11 Keywords, §12 Conditions, §13 Dynamic amounts, §14 Modal & choice, §15 Replacement effects, etc.).

For code templates, see [examples.md](examples.md).
Refer back to [architecture-principles.md](../../../docs/architecture-principles.md) and [card-sdk-language-reference.md](../../../docs/card-sdk-language-reference.md) from Step 2 as needed.

## Step 5: Write Scenario Tests for New Effects

**Only if the card uses NEW effects/keywords/triggers/conditions/static abilities** from Step 4.

**File**: `game-server/src/test/kotlin/com/wingedsheep/gameserver/scenarios/{CardName}ScenarioTest.kt`

- Set up minimal board state, exercise new effect in isolation, verify state changes, cover edge cases
- See [examples.md](examples.md) and [card-sdk-language-reference.md](../../../docs/card-sdk-language-reference.md) for test templates and helpers

## Step 6: Player Experience Review

**Always do this step.** Walk through the card from the player's perspective — what will they see and click?

### 6.1 Trace the player flow

For each ability/mode on the card, answer:
1. **How does the player activate it?** Action menu button? Auto-triggered? What does the button text say — is it clear?
2. **What decisions does the player make?** Targeting, selecting cards, choosing options, ordering?
3. **Which UI component handles each decision?** Map each decision to a specific component in `web-client/src/components/decisions/`:
   - Selecting permanents on battlefield → `BattlefieldSelectionUI` / `BattlefieldTargetingUI` (player clicks cards in-place, seeing counters/effects/duplicates clearly)
   - Selecting cards in graveyard → `GraveyardTargetingUI` (zone overlay)
   - Selecting cards in hand → hand card click flow
   - Selecting from library (search) → `LibrarySearchUI`
   - Choosing from multiple zones → `MultiZoneSelectionUI` (clear zone labels)
   - Budget/pawprint modes → `BudgetModalDecisionUI`
   - Yes/no → `YesNoDecisionUI`
   - Choose color/number/option → respective decision UIs
4. **Is the routing correct?** Check that the decision type emitted by the engine executor maps to the right UI component in `DecisionUI.tsx`.

### 6.2 UX anti-patterns to watch for

- **Overlay when battlefield selection is better:** If the player is choosing among permanents already in play, prefer on-battlefield selection over a card list overlay. Overlays hide board context (counters, effects, which duplicate is which).
- **Flat card list when zones differ:** If a card says "choose from your hand or graveyard", the UI must show zone labels — not dump all cards into one flat row. Use `MultiZoneSelectionUI` or separate decision steps.
- **Overly complex action menus:** If a modal spell enumerates all mode combinations as separate action buttons, that's bad UX. Budget modes should use `BudgetModalDecisionUI`, modal spells should use the mode selection overlay.
- **Unclear ability descriptions:** The `description` field on `BudgetMode`, `Mode`, and activated abilities becomes button/label text. Write these from the player's perspective — concise but unambiguous.
- **Missing targeting context:** When targeting in a zone overlay, players can't see the full board. If the card targets "a creature you control", on-battlefield selection is almost always better than an overlay list.

### 6.3 Implement frontend changes if needed

If the existing components don't handle the card's UX well:
1. Check existing components in `web-client/src/components/decisions/`
2. Prefer extending an existing component over creating a new one
3. If a new decision type or component is truly needed, implement it

## Step 7: E2E Tests for New Visual/UX Effects

**Only if Step 6 introduced new visual or UX mechanics.**

**File**: `e2e-scenarios/tests/{set}/{card-name}.spec.ts`

- Use `createGame` fixture + `GamePage` helpers. See existing tests for patterns.
- **Tip**: Give both players at least one library card to prevent draw-from-empty losses.

## Step 8: Run All Tests

```bash
just build   # Simple cards (existing effects only)
just test    # Cards with new effects
```

Fix only failures caused by your changes. If a pre-existing test fails, report it and stop.

## Step 9: Walkthrough New Engine Mechanics

**Only if Step 4 introduced new effects, keywords, triggers, conditions, static abilities, or replacement effects.**

When new engine mechanics are introduced, mentally trace through the full execution path to verify nothing is missing.
Walk through **at least 2 scenarios** (the happy path + one alternative flow). For each scenario, trace every layer:

### 9.1 Pick Scenarios

Choose scenarios that exercise the new mechanic in distinct ways:

- **Happy path**: The most common use case (e.g., "Player casts the spell, targets a creature, effect resolves")
- **Alternative flow(s)**: At least one of:
  - Target becomes illegal before resolution (fizzle)
  - "May" ability — player declines
  - Triggered ability fires during another spell's resolution
  - Effect interacts with replacement effects (e.g., "if a creature would die" + indestructible)
  - Multiple instances of the same trigger firing simultaneously
  - The card or source leaves the battlefield before the effect/trigger resolves
  - Edge case specific to the mechanic (e.g., X=0, empty library, no valid targets)

### 9.2 Trace Each Layer

For each scenario, walk through these layers and verify the implementation handles it:

| Layer | What to Check |
|-------|---------------|
| **SDK (data model)** | Is the effect/trigger/condition correctly modeled as pure data? Are all parameters present (target, duration, filter, amount)? Can it be serialized? |
| **Engine (ActionProcessor / Handlers)** | When this action is processed, does the correct handler pick it up? Does it produce the right `GameEvent`s? Does it return the correct new `GameState`? |
| **Engine (TriggerDetector)** | If a new trigger type was added: does `TriggerDetector` detect it from the emitted events? Is the trigger registered in `TriggerIndex`? Does it match the correct `GameEvent` type? |
| **Engine (StateProjector)** | If a new continuous effect or static ability: is it applied in the correct Rule 613 layer? Does the projected state reflect it? |
| **Engine (Continuations)** | If the effect requires player input (targeting, selection, yes/no): does it pause correctly with a `PendingDecision`? Does the continuation resume with the player's choice? |
| **Engine (Cleanup)** | If the effect has a duration (end of turn, end of combat): is it cleaned up at the right time? |
| **Game Server (DTO/Masking)** | If a new event type was added: is it handled in `ClientEvent.kt`? Is any private information masked correctly by `StateMasker`? |
| **Frontend (Decisions)** | If a new decision type or UX flow is needed: is there a component in `web-client/src/components/decisions/` that handles it? |
| **Frontend (Display)** | If a new keyword/icon/visual indicator: is it in `enums.ts`, `KeywordDisplayNames`, and icon index? |

### 9.3 Report Findings

For each scenario, write a brief trace like:

> **Scenario: Player casts X, targeting Y**
> 1. SDK: `MyEffect(target, duration)` — ✅ all params present
> 2. Engine: `MyEffectExecutor.execute()` → emits `MyEvent` → ✅
> 3. Triggers: No new triggers needed — ✅
> 4. Continuations: No player input needed — ✅
> 5. Server DTO: `MyEvent` added to `ClientEvent.kt` `when` branch — ✅
> 6. Frontend: No new decision UI needed — ✅

If any layer has a gap (missing event mapping, missing continuation handling, missing frontend component), **fix it before proceeding**.

## Step 10: Verify Implementation Against Scryfall

Re-fetch the Scryfall card data and systematically compare it against your implementation to catch mistakes.

1. **Re-fetch card data**: WebFetch `https://api.scryfall.com/cards/named?exact=<card-name>&set=<set-code>` — extract `name`, `mana_cost`, `type_line`, `oracle_text`, `power`, `toughness`, `colors`, `keywords`, `rarity`, `collector_number`

2. **Read the card definition file** you wrote, then verify each field:

   | Scryfall Field | Check Against |
   |---|---|
   | `name` | `cardDef("...")` name matches exactly |
   | `mana_cost` | `manaCost` string matches (e.g., `{2}{R}{R}`) |
   | `type_line` | `typeLine` matches (use modern Oracle errata, e.g., "Creature — Human Soldier") |
   | `power` / `toughness` | `power` and `toughness` values match |
   | `oracle_text` | Every ability/keyword/effect in the oracle text is represented in the card script |
   | `keywords` | All keywords from the `keywords` array are present (flying, trample, etc.) |
   | `colors` | Consistent with mana cost (no need to set explicitly unless colorless/special) |
   | `rarity` | `rarity` in metadata matches |
   | `collector_number` | `collectorNumber` in metadata matches |

3. **Oracle text deep check** — Go through the oracle text line by line:
   - Each keyword ability → corresponding `keyword(Keyword.X)` or static ability
   - Each activated ability → corresponding `activatedAbility { }` block with correct costs and effects
   - Each triggered ability → corresponding `triggeredAbility { }` block with correct trigger and effects
   - Each spell effect → corresponding `spell { }` block with correct targeting and effects
   - Each static ability → corresponding `staticAbility { }` block
   - Conditional clauses ("if ~", "as long as") → appropriate `Conditions.*`
   - "Target" in oracle text → matching `target = ...` or `TargetRequirement`

4. **Report findings**: If any discrepancies are found, fix them before proceeding. Common mistakes to catch:
   - Wrong P/T values
   - Missing keyword abilities
   - Incorrect mana cost
   - Missing "target" requirement when oracle text says "target"
   - Wrong trigger condition (e.g., "when" vs "whenever", "enters the battlefield" vs "dies")
   - Missing ability entirely (multi-ability cards)
   - Wrong duration (e.g., "until end of turn" mapped to permanent effect)

## Step 10b: Adding Another Printing of an Existing Card (reprints)

**Use this when** either (a) the card is already implemented in another set and you're
adding a reprint, or (b) Step 1.4 routed the canonical `CardDefinition` to an earlier set
and the asked-for set needs only a `Printing` row. Skip otherwise.

The engine treats cards by name (oracle identity), not by printing — the canonical
`CardDefinition` (script, types, P/T) lives in **one** set's package and is registered
once. Reprints contribute only per-printing metadata: `setCode`, `collectorNumber`, art,
artist, scryfall id. They live in the **reprinting set's** package, not in the canonical
card's file.

### When to add a reprint vs. a new card

- **Same oracle text → reprint.** Add a `Printing` row to the new set's package.
- **Different oracle text (functional reprint, errata, name change) → new card.** Follow
  Steps 1–10 normally and pick whichever set should hold the canonical `CardDefinition`.

### Workflow

1. **Confirm the canonical card exists.** `grep -r "name = \"<Card Name>\"" mtg-sets/src/main/kotlin/`
   should find a `CardDefinition` in some other set's package. If not, this is a new card,
   not a reprint — go back to Step 1.

2. **Fetch printing-only data from Scryfall** for the new set:
   `https://api.scryfall.com/cards/named?exact=<card-name>&set=<new-set-code>`
   Extract: `set`, `collector_number`, `artist`, `image_uris.normal`, `rarity`,
   `released_at`, `id` (scryfallId), and `image_uris.normal` of any back face.

3. **Add the printing as a top-level val in the set's `cards/` package**:
   `mtg-sets/.../definitions/{set}/cards/<CardName>Reprint.kt`

   ```kotlin
   package com.wingedsheep.mtg.sets.definitions.{set}.cards

   import com.wingedsheep.sdk.model.Printing
   import com.wingedsheep.sdk.model.Rarity

   /**
    * <Card Name> reprint in <Set Name>. Canonical [com.wingedsheep.sdk.model.CardDefinition]
    * lives in another set's `cards/` package; this file contributes only presentation data.
    */
   val <CardName>Reprint = Printing(
       oracleId = "<Scryfall oracle_id, e.g. 'abc12345-...'>",
       name = "<Card Name>",
       setCode = "<NEW_SET_CODE>",
       collectorNumber = "<COLLECTOR_NUMBER>",
       scryfallId = "<Scryfall id>",
       artist = "<Artist Name>",
       imageUri = "<image_uris.normal — verify with HEAD request, must return 200>",
       releaseDate = "<YYYY-MM-DD>",
       rarity = Rarity.COMMON,
   )
   ```

   `CardDiscovery` scans the `cards/` package for top-level `Printing` vals automatically,
   so no other registration is needed.

4. **Wire `printings` in the set object** if not already done. In the same package's
   `{Set}Set.kt`:

   ```kotlin
   object MySet : MtgSet {
       override val code = "MYS"
       override val cards = ...
       override val printings: List<Printing> by lazy {
           CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
       }
   }
   ```

   `MtgSet.printings` defaults to an empty list, so a set with no reprint files
   automatically returns an empty list. `GameBeansConfig` registers `set.printings`
   alongside synthesised defaults — explicit reprints win when they share
   `(setCode, collectorNumber)` with a synthesised entry.

5. **Per-printing fields** — what to fill in:
   - `oracleId` — the canonical Scryfall `oracle_id` (same value across every printing of
     the card). Pull it from the API response. Use a placeholder if it's missing — it's
     informational; lookups go through `(setCode, collectorNumber)`.
   - `setCode` / `collectorNumber` — uniquely identify this row.
   - `imageUri` — must match exactly what Scryfall returns; verify with `curl -sI`.
   - `backFaceImageUri` — only for DFC reprints; pull from the back face's `image_uris.normal`.
   - `rarity` / `artist` / `releaseDate` / `scryfallId` — straight from the API response.
   - `isPromo`, `isFullArt`, `frameEffects` — fill from `promo`, `full_art`, `frame_effects`
     when relevant; safe to omit otherwise.

6. **No new test required** for a reprint by itself — the seam is already covered by
   `MultiPrintingGameTest`. Only add a test if you also touched the card's behavior.

7. **Backlog and commit message.**
   - If the *new* set has a `backlog/sets/{set-name}/cards.md`, mark the card if listed.
   - Commit: `Add {Card Name} reprint to {New Set Name}`.

### Example: Lightning Bolt reprinted from M10 → 2X2

Lightning Bolt's canonical `CardDefinition` (its spell script) lives in
`mtg-sets/.../definitions/m10/cards/LightningBolt.kt`. To add the 2X2 reprint:

```kotlin
// mtg-sets/.../definitions/2x2/cards/LightningBoltReprint.kt
package com.wingedsheep.mtg.sets.definitions.m2x2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

val LightningBoltReprint = Printing(
    oracleId = "4457ed35-7c10-48c8-9776-456485fdf070",
    name = "Lightning Bolt",
    setCode = "2X2",
    collectorNumber = "117",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/.../bolt-2x2.jpg?...",
    releaseDate = "2022-04-22",
    rarity = Rarity.UNCOMMON,
    scryfallId = "...",
)
```

```kotlin
// mtg-sets/.../definitions/2x2/M2X2Set.kt
object M2X2Set : MtgSet {
    override val code = "2X2"
    override val cards by lazy { CardDiscovery.findIn(CARDS_PACKAGE) }
    override val printings by lazy { CardDiscovery.findPrintingsIn(CARDS_PACKAGE) }
    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.m2x2.cards"
}
```

The engine continues to look up "Lightning Bolt" in `CardRegistry` and finds the M10
script. When a deck pins `PrintingRef("2X2", "117")`, `GameInitializer` resolves the
`Printing` from `PrintingRegistry`, stamps the 2X2 art onto the per-entity
`CardComponent.imageUri`, and the client renders the 2X2 print.

## Step 11: Update Set Backlog

If `backlog/sets/{set-name}/cards.md` exists:
- Mark card as implemented: `- [ ] Card Name` → `- [x] Card Name`
- Run `just fix-backlog` to resync the `**Implemented:** N / M` header (the script
  rewrites the header to match the actual `[x]` count and full checklist size).
- `just check-backlog` is the read-only verification mode used in CI / manual review.

## Step 12: Commit Changes

1. Stage all relevant files (card definition, set registration, new effects, tests, backlog)
2. Commit message: `Add {Card Name} to {Set Name}` (or `Add {Card Name} with {new mechanic} support`)
3. Do NOT push

## Important Rules

1. **Always fetch from Scryfall first** — Never guess card text or stats
2. **ALWAYS use set-specific API URL** — `&set=<code>` for correct metadata per printing
3. **NEVER hallucinate image URIs** — Use exact `image_uris.normal` from API, verified with HEAD request
4. **Prefer atomic effects** — Compose from GatherCards/SelectFromCollection/MoveCollection via `EffectPatterns.*`
5. **Check DSL facades first** — `Effects.*`, `Targets.*`, `Triggers.*`, `EffectPatterns.*` before raw types
6. **Check existing effects** — Most common effects already exist; compose before creating new ones
7. **Use immutable patterns** — Never modify state in place
8. **Test new mechanics** — All new effects/keywords/triggers/conditions need scenario tests
9. **Follow naming conventions** — CardName matches file name
10. **Keep effects data-only** — Logic goes in executors, not effect data classes
11. **Verify against Scryfall before committing** — Re-check every field against the API data (Step 10)
12. **Walkthrough new mechanics** — If new effects/engine changes, trace through all layers (Step 9)
13. **Build/test before committing** — `just build` (simple) or `just test` (new effects)
