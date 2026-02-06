---
name: add-card
description: Implements new Magic: The Gathering cards for the Argentum Engine. Use when adding a new card, the user provides a card name to implement, or asks to implement a specific MTG card.
argument-hint: <card-name> [--set <set-code>]
disable-model-invocation: true
---

# Add MTG Card

Implement the card specified in `$ARGUMENTS`.

Parse the arguments: the card name is the main argument, and `--set <set-code>` is optional (defaults to `por` for Portal).

## Phase 1: Fetch Card Data from Scryfall

**REQUIRED**: Always fetch the exact card data from Scryfall API before implementing.

1. Use WebFetch to get card data:
   - URL: `https://api.scryfall.com/cards/named?exact=<card-name-url-encoded>&set=<set-code>`
   - **ALWAYS use the `&set=<set-code>` parameter** - each printing has different rarity, collector_number, artist, flavor_text, and image_uris
   - Extract: name, mana_cost, type_line, oracle_text, power, toughness, colors, rarity, collector_number, artist, flavor_text, image_uris.normal

   Common set codes: `ons` (Onslaught), `por` (Portal), `lea` (Alpha), `leb` (Beta), `2ed` (Unlimited)

2. **CRITICAL - Image URI**:
   - **ALWAYS use the exact `image_uris.normal` URL from the Scryfall API response**
   - **NEVER generate, guess, or hallucinate an image URI** - they have specific hash-based paths
   - Format: `https://cards.scryfall.io/normal/front/X/X/XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX.jpg?XXXXXXXXXX`
   - Validate that the URL really exists with a head request, if it doesn't exist find it on scryfall (always get the one with the correct series)
   - **Include the query parameter** (e.g., `?1562911270`) - it's part of the URL

3. **Check for Oracle Errata**:
   - The `oracle_text` from Scryfall contains the current Oracle wording with all errata applied
   - Document significant errata in a comment above the card definition
   - Common errata: "Bury" -> "Destroy + can't be regenerated", "Remove from game" -> "Exile", creature type updates

4. Parse the oracle text to understand:
   - Card type (creature, instant, sorcery, enchantment, artifact, planeswalker, etc.)
   - Abilities (spell effect, triggered, activated, static, replacement)
   - Targeting requirements
   - Keywords (simple and parameterized)
   - Conditions and restrictions

## Phase 2: Check Existing SDK Components

Before implementing anything new, check what already exists using the DSL facades and raw types.

1. **Check DSL facades first** - these are the primary API:
   - `Effects.*` - effect construction (`mtg-sdk/.../dsl/Effects.kt`)
   - `Targets.*` - target requirements (`mtg-sdk/.../dsl/Targets.kt`)
   - `Triggers.*` - trigger types (`mtg-sdk/.../dsl/Triggers.kt`)
   - `Filters.*` - filter construction (`mtg-sdk/.../dsl/Filters.kt`)
   - `Costs.*` - ability costs (`mtg-sdk/.../dsl/Costs.kt`)
   - `Conditions.*` - conditions (`mtg-sdk/.../dsl/Conditions.kt`)
   - `DynamicAmounts.*` - dynamic values (`mtg-sdk/.../dsl/DynamicAmounts.kt`)
   - `EffectPatterns.*` - common patterns (`mtg-sdk/.../dsl/EffectPatterns.kt`)

2. **Check raw effect types** in `mtg-sdk/.../scripting/effect/`:
   - `DamageEffects.kt`, `LifeEffects.kt`, `DrawingEffects.kt`, `RemovalEffects.kt`
   - `PermanentEffects.kt`, `LibraryEffects.kt`, `ManaEffects.kt`, `TokenEffects.kt`
   - `CompositeEffects.kt`, `CombatEffects.kt`, `PlayerEffects.kt`, `StackEffects.kt`

3. **Check other SDK types**:
   - `Keyword.kt` - keywords
   - `KeywordAbility.kt` - parameterized keywords (Ward, Protection, Cycling, etc.)
   - `StaticAbility` types - for permanent/global effects
   - `ReplacementEffect.kt` - replacement effects
   - `AdditionalCost.kt` - additional spell costs
   - `ActivationRestriction.kt` - activation restrictions

4. **Check existing card implementations** with similar mechanics:
   ```bash
   grep -r "<keyword-or-effect>" mtg-sets/src/main/kotlin/
   ```

For a full inventory, see [reference.md](reference.md).

## Phase 3: Identify Missing Components

Compare the card's needs against existing components:

1. **Everything exists** -> Skip to Phase 5
2. **New effect needed** -> Phase 4 (implement new effect + executor)
3. **New keyword needed** -> Phase 4 (add keyword)
4. **New static ability needed** -> Phase 4
5. **New replacement effect needed** -> Phase 4
6. **New trigger needed** -> Phase 4
7. **New condition needed** -> Phase 4

## Phase 4: Implement Missing Backend Components

If new components are needed:

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

For code templates, see [examples.md](examples.md).

## Phase 5: Create Card Definition

**File**: `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/{set}/cards/{CardName}.kt`

Use the `card` DSL. Prefer DSL facades (`Effects.*`, `Targets.*`, `Triggers.*`, `Filters.*`, etc.) over raw constructors. For complete examples of creatures, spells, triggered abilities, activated abilities, static abilities, equipment, auras, modal spells, and X-cost spells, see [examples.md](examples.md).

## Phase 6: Register Card in Set

**File**: `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/{set}/{Set}Set.kt`

Add the card to the `allCards` list (alphabetically or by collector number).

## Phase 7: Create Scenario Test (Required for New Effects/Keywords)

If this card uses a NEW effect, keyword, trigger, condition, or static ability, create a scenario test.

**File**: `game-server/src/test/kotlin/com/wingedsheep/gameserver/scenarios/{CardName}ScenarioTest.kt`

For test template and available helper methods, see [examples.md](examples.md) and [reference.md](reference.md).

## Phase 8: Run Tests and Verify

```bash
# Run specific test
just test-class MyCardScenarioTest

# Run all game-server tests
just test-server

# Verify the build
just build
```

## Checklists

**Simple Card (existing effects only)**:
- [ ] Fetch card data from Scryfall with **set-specific URL** (e.g., `&set=ons`)
- [ ] **Verify metadata matches the target set** (rarity, collector number, artist, flavor text)
- [ ] **Verify image URI is from Scryfall response** (never hallucinate, include query param)
- [ ] Check for errata (oracle text vs printed text)
- [ ] Create card file in `mtg-sets/.../cards/`
- [ ] Add to set's `allCards` list

**Card with New Effect**:
- [ ] All of the above, plus:
- [ ] Add effect type to appropriate file in `mtg-sdk/.../scripting/effect/`
- [ ] Add DSL facade in `Effects.kt` (recommended)
- [ ] Create executor in `rules-engine/.../handlers/effects/{category}/`
- [ ] Register in appropriate `{Category}Executors.kt`
- [ ] Write scenario test
- [ ] Run tests

**Card with New Keyword**:
- [ ] All of the simple card steps, plus:
- [ ] Add keyword to `mtg-sdk/.../core/Keyword.kt`
- [ ] Update `web-client/src/types/enums.ts` (enum + display names)
- [ ] Update keyword handlers if needed
- [ ] Write scenario test
- [ ] Run tests

**Card with Static/Replacement/Trigger/Condition**:
- [ ] All of the simple card steps, plus:
- [ ] Add type to appropriate SDK file
- [ ] Add DSL facade entry if applicable
- [ ] Implement handler/executor if needed
- [ ] Write scenario test
- [ ] Run tests

## Important Rules

1. **Always fetch from Scryfall first** - Never guess card text or stats
2. **ALWAYS use set-specific API URL** - Use `&set=<code>` to get correct metadata for the specific printing
3. **NEVER hallucinate image URIs** - Always use the exact `image_uris.normal` from Scryfall API response
4. **Check DSL facades first** - Use `Effects.*`, `Targets.*`, `Triggers.*`, etc. before checking raw types
5. **Check existing effects** - Most common effects already exist
6. **Use immutable patterns** - Never modify state in place
7. **Test new mechanics** - All new effects/keywords/triggers/conditions need tests
8. **Follow naming conventions** - CardName should match file name
9. **Keep effects data-only** - Logic goes in executors, not effect data classes
