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
   - **Include the query parameter** (e.g., `?1562911270`) - it's part of the URL

3. **Check for Oracle Errata**:
   - The `oracle_text` from Scryfall contains the current Oracle wording with all errata applied
   - Document significant errata in a comment above the card definition
   - Common errata: "Bury" -> "Destroy + can't be regenerated", "Remove from game" -> "Exile", creature type updates

4. Parse the oracle text to understand:
   - Card type (creature, instant, sorcery, enchantment, etc.)
   - Abilities (spell effect, triggered, activated, static)
   - Targeting requirements
   - Keywords

## Phase 2: Check Existing Effects

Before implementing anything new, search for existing effects that can be reused.

1. Search for effects in mtg-sdk:
   ```bash
   grep -r "data class.*Effect\|data object.*Effect" mtg-sdk/src/main/kotlin/
   ```

2. Check for existing executors:
   ```bash
   ls rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/
   ```

3. Review existing card implementations with similar mechanics:
   ```bash
   grep -r "<keyword-or-effect>" mtg-sets/src/main/kotlin/
   ```

For a full list of existing effects and keywords, see [reference.md](reference.md).

## Phase 3: Identify Missing Components

Compare the card's needs against existing components:

1. **Effect exists and can be used as-is** -> Skip to Phase 5
2. **Effect exists but needs parameters** -> Skip to Phase 5
3. **Effect does NOT exist** -> Phase 4 (implement new effect)
4. **Keyword does NOT exist** -> Phase 4 (add keyword)

## Phase 4: Implement Missing Backend Components

If new effects or keywords are needed:

**4.1 Add Effect Type** in `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/Effect.kt`

**4.2 Create Effect Executor** in `rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/{category}/`

**4.3 Register Executor** in `rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/{Category}Executors.kt`

**4.4 Add New Keyword** (if needed) in `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt`

For code templates, see [examples.md](examples.md).

## Phase 5: Create Card Definition

**File**: `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/{set}/cards/{CardName}.kt`

Use the `card` DSL. For complete examples of creatures, spells, triggered abilities, and X-cost spells, see [examples.md](examples.md).

## Phase 6: Register Card in Set

**File**: `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/{set}/{Set}Set.kt`

Add the card to the `allCards` list (alphabetically or by collector number).

## Phase 7: Create Scenario Test (Required for New Effects/Keywords)

If this card uses a NEW effect or keyword, create a scenario test.

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
- [ ] Add effect type to `mtg-sdk/.../Effect.kt`
- [ ] Create executor in `rules-engine/.../handlers/effects/`
- [ ] Register in appropriate `*Executors.kt`
- [ ] Write scenario test
- [ ] Run tests

**Card with New Keyword**:
- [ ] All of the simple card steps, plus:
- [ ] Add keyword to `mtg-sdk/.../Keyword.kt`
- [ ] Update keyword handlers if needed
- [ ] Write scenario test
- [ ] Run tests

## Important Rules

1. **Always fetch from Scryfall first** - Never guess card text or stats
2. **ALWAYS use set-specific API URL** - Use `&set=<code>` to get correct metadata for the specific printing
3. **NEVER hallucinate image URIs** - Always use the exact `image_uris.normal` from Scryfall API response
4. **Check existing effects** - Most common effects already exist
5. **Use immutable patterns** - Never modify state in place
6. **Test new mechanics** - All new effects/keywords need tests
7. **Follow naming conventions** - CardName should match file name
8. **Keep effects data-only** - Logic goes in executors, not effect data classes