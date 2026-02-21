---
name: add-card
description: Implements new Magic: The Gathering cards for the Argentum Engine. Use when adding a new card, the user provides a card name to implement, or asks to implement a specific MTG card.
argument-hint: <card-name> [--set <set-code>]
disable-model-invocation: true
---

# Add MTG Card

Implement the card specified in `$ARGUMENTS`.

Parse the arguments: the card name is the main argument, and `--set <set-code>` is optional (defaults to `por` for Portal).

---

## Step 1: Look Up Card Data on Scryfall

**REQUIRED**: Always fetch the exact card data from Scryfall before implementing.

1. Use WebFetch to get card data:
   - URL: `https://api.scryfall.com/cards/named?exact=<card-name-url-encoded>&set=<set-code>`
   - **ALWAYS use the `&set=<set-code>` parameter** — each printing has different rarity, collector_number, artist, flavor_text, and image_uris
   - Extract: name, mana_cost, type_line, oracle_text, power, toughness, colors, rarity, collector_number, artist, flavor_text, image_uris.normal
   - Example: `WebFetch(https://api.scryfall.com/cards/named?exact=Lightning+Bolt&set=lea)`

   Common set codes: `ons` (Onslaught), `lgn` (Legions), `scg` (Scourge), `por` (Portal), `lea` (Alpha), `leb` (Beta), `2ed` (Unlimited), `ktk` (Khans of Tarkir)

2. **Check for Oracle Errata**:
   - The `oracle_text` from Scryfall contains the current Oracle wording with all errata applied
   - Document significant errata in a comment above the card definition
   - Common errata: "Bury" → "Destroy + can't be regenerated", "Remove from game" → "Exile", creature type updates

3. Parse the oracle text to understand:
   - Card type (creature, instant, sorcery, enchantment, artifact, planeswalker, etc.)
   - Abilities (spell effect, triggered, activated, static, replacement)
   - Targeting requirements
   - Keywords (simple and parameterized)
   - Conditions and restrictions

---

## Step 2: Check Existing Effects

Before implementing anything new, check what already exists. **Always prefer atomic effects over monolithic effects** for reusability.

1. **Consult [reference.md](reference.md)** — it contains the complete inventory of all DSL facades (`Effects.*`, `Targets.*`, `Triggers.*`, `Filters.*`, `Costs.*`, `Conditions.*`, `DynamicAmounts.*`, `EffectPatterns.*`), all raw effect types, keywords, static abilities, replacement effects, and more.

2. **Check existing card implementations** with similar mechanics:
   ```bash
   grep -r "<keyword-or-effect>" mtg-sets/src/main/kotlin/
   ```

---

## Step 3: Model the Card

Create the card definition using the `card` DSL, using only existing effects identified in Step 2.

**File**: `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/{set}/cards/{CardName}.kt`

1. Write the card definition using DSL facades (`Effects.*`, `Targets.*`, `Triggers.*`, `Filters.*`, etc.) over raw constructors.

2. **Image URI — CRITICAL**:
   - **ALWAYS use the exact `image_uris.normal` URL from the Scryfall API response**
   - **NEVER generate, guess, or hallucinate an image URI** — they have specific hash-based paths
   - Format: `https://cards.scryfall.io/normal/front/X/X/XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX.jpg?XXXXXXXXXX`
   - **Include the query parameter** (e.g., `?1562911270`) — it's part of the URL
   - **Verify the URL exists** by running a HEAD request:
     ```bash
     curl -sI "<image-url>" | head -1
     ```
     If the response is not `200`, find the correct image on Scryfall (always get the one with the correct set).

3. Register the card in the set file:
   - **File**: `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/{set}/{Set}Set.kt`
   - Add the card to the `allCards` list (alphabetically or by collector number)

For complete examples of creatures, spells, triggered abilities, activated abilities, static abilities, equipment, auras, modal spells, and X-cost spells, see [examples.md](examples.md).

---

## Step 4: Plan and Implement Missing Effects

If the card needs effects, keywords, triggers, conditions, or static abilities that don't exist yet:

**4.0 Always try atomic composition first** — Before adding a new effect type, try to express the behavior as a `CompositeEffect` of existing atomic effects or an `EffectPatterns.*` helper. For library manipulation, use the `GatherCards → SelectFromCollection → MoveCollection` pipeline. Only create a new effect type when no composition of existing effects works.

**If a new effect type is truly needed:**

**4.1 Add Effect Type** in the appropriate file under `mtg-sdk/.../scripting/effect/`:
- `DamageEffects.kt`, `LifeEffects.kt`, `DrawingEffects.kt`, `RemovalEffects.kt`
- `PermanentEffects.kt`, `LibraryEffects.kt`, `ManaEffects.kt`, `TokenEffects.kt`
- `CompositeEffects.kt`, `CombatEffects.kt`, `PlayerEffects.kt`, `StackEffects.kt`

**4.2 Create Effect Executor** in `rules-engine/.../handlers/effects/{category}/`
- Choose the matching category directory (combat/, damage/, drawing/, library/, life/, mana/, permanent/, player/, removal/, stack/, token/, composite/, information/)

**4.3 Register Executor** in the appropriate `{Category}Executors.kt` module file

**4.4 Add DSL Facade** (recommended) in `mtg-sdk/.../dsl/Effects.kt` for new effects

**4.5 Add New Keyword** (if needed) in `mtg-sdk/.../core/Keyword.kt`
- Also update `web-client/src/types/enums.ts` (Keyword enum + KeywordDisplayNames)
- Also update `web-client/src/assets/icons/keywords/index.ts` if it should have an icon

**4.6 Add Static Ability** (if needed) in `mtg-sdk/.../scripting/StaticAbility.kt`

**4.7 Add Replacement Effect** (if needed) in `mtg-sdk/.../scripting/ReplacementEffect.kt`

**4.8 Add Trigger** (if needed) in the appropriate file under `mtg-sdk/.../scripting/trigger/`
- Also add a facade entry in `mtg-sdk/.../dsl/Triggers.kt`

**4.9 Add Condition** (if needed) in the appropriate file under `mtg-sdk/.../scripting/condition/`
- Also add a facade entry in `mtg-sdk/.../dsl/Conditions.kt`

**4.10 Update [reference.md](reference.md)** — After adding any new effect, keyword, trigger, condition, static ability, replacement effect, cost, or DSL facade entry, update the reference file so it stays in sync with the codebase.

For code templates, see [examples.md](examples.md).

---

## Step 5: Write Scenario Tests for New Effects

**Only if the card uses NEW effects, keywords, triggers, conditions, or static abilities** created in Step 4.

**File**: `game-server/src/test/kotlin/com/wingedsheep/gameserver/scenarios/{CardName}ScenarioTest.kt`

Write tests that:
- Set up a minimal board state with the card
- Exercise the new effect in isolation
- Verify the expected game state changes
- Cover edge cases (e.g., no valid targets, multiple triggers)

For test template and available helper methods, see [examples.md](examples.md) and [reference.md](reference.md).

---

## Step 6: Plan Frontend Changes for New Effects

**Only if the new effects require UX/frontend changes** (e.g., a new decision type, overlay, targeting flow, zone interaction, or player prompt that didn't exist before).

If frontend changes are needed:
1. Identify which web-client components need modification
2. Check existing decision/overlay components in `web-client/src/components/decisions/`
3. Plan the minimal UI changes needed to support the new mechanic
4. Implement the frontend changes

If no new frontend mechanics are needed, skip this step.

---

## Step 7: Write E2E Tests for New Visual/UX Effects

**Only if Step 6 introduced new visual or UX mechanics.**

**File**: `e2e-scenarios/tests/{set}/{card-name}.spec.ts`

- Mirror the scenario test from Step 5 but run against the full stack (server + client)
- Use the `createGame` fixture to set up the board state
- Use `GamePage` helpers (`clickCard`, `selectAction`, `answerYes`, `resolveStack`, `expectOnBattlefield`, etc.)
- See existing tests in `e2e-scenarios/tests/` for patterns
- **Tip**: Give both players at least one card in their library to prevent draw-from-empty-library losses

---

## Step 8: Run All Tests

Verify nothing is broken:

```bash
# Build the entire project
just build

# Run all tests
just test
```

If tests fail, fix the issues before proceeding. Focus only on failures caused by your changes — if a pre-existing test fails, report it to the user and stop.

---

## Step 9: Update Set Backlog

If the card belongs to a set that has a backlog file in `backlog/sets/`, update it:

1. Check if `backlog/sets/{set-name}/cards.md` exists
2. If it does, mark the card as implemented: change `- [ ] Card Name` to `- [x] Card Name`
3. Update the implementation count in the header if present (e.g., `**Implemented:** 334 / 335` → `**Implemented:** 335 / 335`)

If there's no backlog file for this set, skip this step.

---

## Step 10: Commit Changes

Create a git commit with all the changes:

1. Stage all relevant files (card definition, set registration, new effects, tests, backlog)
2. Write a clear commit message describing what was added:
   - For simple cards: `Add {Card Name} to {Set Name}`
   - For cards with new effects: `Add {Card Name} with {new mechanic} support`
3. Do NOT push — just commit locally

---

## Checklists

**Simple Card (existing effects only)**:
- [ ] Fetch card data from Scryfall with **set-specific URL** (e.g., `&set=ons`)
- [ ] **Verify metadata matches the target set** (rarity, collector number, artist, flavor text)
- [ ] **Verify image URI** with HEAD request — confirm HTTP 200
- [ ] Check for errata (oracle text vs printed text)
- [ ] Create card file in `mtg-sets/.../cards/`
- [ ] Add to set's `allCards` list
- [ ] Run `just build` — verify it compiles
- [ ] Update set backlog if applicable
- [ ] Commit

**Card with New Effect**:
- [ ] All of the above, plus:
- [ ] Compose from atomic effects if possible (prefer `EffectPatterns.*` / `CompositeEffect`)
- [ ] If truly new: add effect type to `mtg-sdk/.../scripting/effect/`
- [ ] Add DSL facade in `Effects.kt` (recommended)
- [ ] Create executor in `rules-engine/.../handlers/effects/{category}/`
- [ ] Register in appropriate `{Category}Executors.kt`
- [ ] Write scenario test
- [ ] Plan frontend changes if needed
- [ ] Write E2E test if new frontend/UI mechanic was introduced
- [ ] **Update [reference.md](reference.md)** with all new effects/types added
- [ ] Run `just test` — all tests pass
- [ ] Update set backlog if applicable
- [ ] Commit

**Card with New Keyword**:
- [ ] All of the simple card steps, plus:
- [ ] Add keyword to `mtg-sdk/.../core/Keyword.kt`
- [ ] Update `web-client/src/types/enums.ts` (enum + display names)
- [ ] Update keyword handlers if needed
- [ ] Write scenario test
- [ ] **Update [reference.md](reference.md)** with the new keyword
- [ ] Run `just test` — all tests pass
- [ ] Update set backlog if applicable
- [ ] Commit

## Important Rules

1. **Always fetch from Scryfall first** — Never guess card text or stats
2. **ALWAYS use set-specific API URL** — Use `&set=<code>` to get correct metadata for the specific printing
3. **NEVER hallucinate image URIs** — Always use the exact `image_uris.normal` from Scryfall API response, verified with a HEAD request
4. **Prefer atomic effects over monolithic effects** — Compose behavior from small reusable effects (GatherCards, SelectFromCollection, MoveCollection, etc.) instead of creating large single-purpose effects. Use `EffectPatterns.*` and `CompositeEffect` to combine them.
5. **Check DSL facades first** — Use `Effects.*`, `Targets.*`, `Triggers.*`, `EffectPatterns.*`, etc. before checking raw types
6. **Check existing effects** — Most common effects already exist; compose them before creating new ones
7. **Use immutable patterns** — Never modify state in place
8. **Test new mechanics** — All new effects/keywords/triggers/conditions need scenario tests
9. **Follow naming conventions** — CardName should match file name
10. **Keep effects data-only** — Logic goes in executors, not effect data classes
11. **Auras must use `Enchantment — Aura` type line** — Cards with "Enchant creature/permanent/land/artifact" use `typeLine = "Enchantment — Aura"` (modern Oracle errata). They need `auraTarget` in the card script.
12. **Verify before committing** — Always run `just build` (simple cards) or `just test` (new effects) before committing
