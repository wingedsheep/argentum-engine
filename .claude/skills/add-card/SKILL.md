---
name: add-card
description: Implements new Magic: The Gathering cards for the Argentum Engine. Use when adding a new card, the user provides a card name to implement, or asks to implement a specific MTG card.
argument-hint: <card-name> [--set <set-code>]
disable-model-invocation: true
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

## Step 2: Check Existing Effects

**Always prefer atomic effects over monolithic effects** for reusability.

1. **Consult [reference.md](reference.md)** — complete inventory of all DSL facades (`Effects.*`, `Targets.*`, `Triggers.*`, `Filters.*`, `Costs.*`, `Conditions.*`, `DynamicAmounts.*`, `EffectPatterns.*`), raw effect types, keywords, static abilities, replacement effects, and more.
2. **Search existing cards** with similar mechanics: `grep -r "<keyword-or-effect>" mtg-sets/src/main/kotlin/`

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
- **4.6 Add Static Ability** (if needed) in `mtg-sdk/.../scripting/StaticAbility.kt`
- **4.7 Add Replacement Effect** (if needed) in `mtg-sdk/.../scripting/ReplacementEffect.kt`
- **4.8 Add Trigger** (if needed) in `mtg-sdk/.../scripting/trigger/` + facade in `mtg-sdk/.../dsl/Triggers.kt`
- **4.9 Add Condition** (if needed) in `mtg-sdk/.../scripting/condition/` + facade in `mtg-sdk/.../dsl/Conditions.kt`
- **4.10 Update [reference.md](reference.md)** with all new types added

For code templates, see [examples.md](examples.md).

## Step 5: Write Scenario Tests for New Effects

**Only if the card uses NEW effects/keywords/triggers/conditions/static abilities** from Step 4.

**File**: `game-server/src/test/kotlin/com/wingedsheep/gameserver/scenarios/{CardName}ScenarioTest.kt`

- Set up minimal board state, exercise new effect in isolation, verify state changes, cover edge cases
- See [examples.md](examples.md) and [reference.md](reference.md) for test templates and helpers

## Step 6: Frontend Changes for New Effects

**Only if new effects require UX changes** (new decision type, overlay, targeting flow, zone interaction, player prompt).

1. Check existing components in `web-client/src/components/decisions/`
2. Plan and implement minimal UI changes
3. Skip if no new frontend mechanics needed

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

## Step 9: Verify Implementation Against Scryfall

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

## Step 10: Update Set Backlog

If `backlog/sets/{set-name}/cards.md` exists:
- Mark card as implemented: `- [ ] Card Name` → `- [x] Card Name`
- Update implementation count in header if present

## Step 11: Commit Changes

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
11. **Verify against Scryfall before committing** — Re-check every field against the API data (Step 9)
12. **Build/test before committing** — `just build` (simple) or `just test` (new effects)
