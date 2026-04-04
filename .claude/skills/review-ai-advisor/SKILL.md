---
name: review-ai-advisor
description: Pick the next unchecked Bloomburrow card from the AI advisor checklist, evaluate if it needs an advisor, create one if needed, and check it off.
---

# Review Next AI Advisor Card

Pick the next unchecked card from `backlog/bloomburrow-ai-advisors.md`, evaluate whether it needs
a custom AI advisor, implement one if necessary, and check it off the list.

## Step 1: Pick the Next Card

1. Read `backlog/bloomburrow-ai-advisors.md`
2. Find the first line matching `- [ ] **Card Name**` (unchecked, not N/A)
3. Note the card name and any existing comments about it

## Step 2: Read the Card Definition

1. Find the card's definition file in `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/bloomburrow/cards/`
2. Read it fully. Understand what the card does: its type, effects, targets, triggers, keywords.

## Step 3: Read the Current Advisor Module

1. Read `rules-engine/src/main/kotlin/com/wingedsheep/engine/ai/advisor/modules/BloomburrowAdvisorModule.kt`
2. Understand what advisor patterns already exist and which cards are already covered.

## Step 4: Evaluate Whether an Advisor Is Needed

Ask yourself these questions about the card:

### Timing Questions (evaluateCast)
- **Is it an instant?** Does the generic AI cast it at the wrong time?
  - Combat tricks (pump/protection) should be held for combat
  - Removal should be held for opponent's turn or combat
  - Counterspells should be held for opponent's spells
  - Flash creatures should be held for opponent's combat
- **Is it a sorcery/creature?** Sorceries can only be cast at sorcery speed, so timing advisors
  rarely help. Creatures without flash are the same. Mark these as N/A for timing.

### Decision Questions (respondToDecision)
- **Does it have a gift mode?** All gift cards should use `pickBestGiftMode()` with context-sensitive
  penalty scaling. Check if it's already covered by an existing gift advisor.
- **Does it have coupled multi-targets?** (e.g., bite/fight: "your creature deals damage to their
  creature"). These need joint target simulation — add to `BiteSpellAdvisor` or create a new one.
- **Does it have "up to N" optional targets on graveyard/library?** The generic AI often selects 0.
  These need an advisor that selects the maximum.
- **Does it have complex modes?** (e.g., Season budget modals, choose 1 of 3+). The generic AI's
  mode simulation might be adequate — test before adding an advisor.

### Combat Questions (attackPenalty)
- **Is it a creature with a valuable tap ability?** If the tap ability is worth more than attacking,
  add an `attackPenalty`.

### When NO Advisor Is Needed
- Vanilla creatures (just stats + keywords)
- Simple sorcery-speed removal (Fell — just destroys a creature)
- Card draw with no choices (Pearl of Wisdom — just draws 2)
- Creatures without activated abilities or flash
- Cards where the 1-ply simulation correctly evaluates the outcome

## Step 5: Determine Category

If an advisor IS needed, check if it fits an **existing** category:

| Category | Advisor Object | When to add here |
|----------|---------------|-----------------|
| Pump/protection instants | `CombatTrickAdvisor` | Pure stat buff or keyword grant instant |
| Instant-speed removal | `InstantRemovalAdvisor` | Instant that destroys/exiles/damages creatures |
| Counterspells | `CounterspellAdvisor` | Pure counter effect |
| Board wipes | `BoardWipeAdvisor` | Destroys/damages all creatures |
| Gift removal | `GiftRemovalAdvisor` | Gift card that removes a creature |
| Gift combat tricks | `GiftCombatTrickAdvisor` | Gift card that pumps creatures |
| Gift card draw | `GiftCardDrawAdvisor` | Gift card that draws cards |
| Gift value | `GiftValueAdvisor` | Gift card where gift mode gives more targets/effects |
| Gift bounce | `GiftBounceAdvisor` | Gift card that bounces permanents |
| Gift protection | `GiftProtectionAdvisor` | Gift card that protects creatures |
| Gift counterspell | `GiftCounterspellAdvisor` | Gift card that counters spells |
| Gift board wipe | `GiftBoardWipeAdvisor` | Gift card that wipes the board |
| Bite/fight | `BiteSpellAdvisor` | Your creature damages/fights their creature |
| Graveyard retrieval | `GraveyardRetrievalAdvisor` | "Return up to N" from graveyard |
| Flash creatures | `FlashCreatureAdvisor` | Big flash creature that should ambush block |
| Counter/draw modal | `SpellgyreAdvisor` | Modal: counter OR card advantage |

If the card needs a **new category** (doesn't fit any existing advisor):
1. Create a new `object MyNewAdvisor : CardAdvisor` in `BloomburrowAdvisorModule.kt`
2. Register it in `BloomburrowAdvisorModule.register()`
3. Document the pattern in a comment above the advisor

## Step 6: Implement (if needed)

If the card fits an existing advisor:
1. Add the card name to the advisor's `cardNames` set
2. Verify no conflicts (a card can only be in one advisor's `cardNames`)

If the card needs a new advisor:
1. Create the advisor object following the patterns in the file
2. Register it in `BloomburrowAdvisorModule.register()`

If no advisor is needed:
- Skip this step

## Step 7: Compile and Test

1. Run `./gradlew :rules-engine:compileKotlin` to verify compilation
2. Run `./gradlew :rules-engine:test` to verify no tests break
3. Optionally run a quick benchmark:
   ```bash
   ./gradlew :rules-engine:test --tests "*.AdvisorBenchmark" -Dbenchmark=true -DbenchmarkGames=5 --rerun
   ```

## Step 8: Update the Checklist

Edit `backlog/bloomburrow-ai-advisors.md`:

**If an advisor was added or the card was added to an existing advisor:**
- Change `- [ ] **Card Name**` to `- [x] **Card Name** — AdvisorName (brief reason)`

**If no advisor is needed (generic AI handles it fine):**
- Change `- [ ] **Card Name**` to `- [x] **Card Name** — N/A (brief reason, e.g., "simple sorcery removal")`

## Step 9: Summary

Report what you did:
- Card name and what it does
- Whether an advisor was added (and which one) or marked N/A
- If a new advisor category was created, explain the pattern
