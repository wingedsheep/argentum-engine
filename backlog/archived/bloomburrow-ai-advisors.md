# Bloomburrow AI Advisor Checklist

Instructions and checklist for improving the AI's play with Bloomburrow cards using the
`CardAdvisor` system.

## How It Works

The `CardAdvisor` system (`rules-engine/.../ai/advisor/`) lets you override the generic AI's
behavior for specific cards. Advisors hook into three decision points:

1. **`evaluateCast(CastContext)`** — Should the AI cast this spell/activate this ability right now?
   Returns a score override (or null to use default simulation). The context includes:
   - `state` / `projected` — full game state
   - `passScore` — score of doing nothing
   - `defaultScore` — what the generic 1-ply simulation scored
   - `evaluator` / `simulator` — for custom evaluation

2. **`respondToDecision(AdvisorDecisionContext)`** — How should the AI respond to a decision during
   this card's resolution? (targets, modes, yes/no, etc.) Returns a `DecisionResponse` or null.

3. **`attackPenalty(state, projected, entityId, playerId)`** — Score penalty for attacking with this
   creature. Positive values disincentivize attacking (10.0 = almost never attacks).

## Advisor Patterns

### Timing (evaluateCast)

| Pattern | When to use | Implementation |
|---------|------------|----------------|
| **Hold for combat** | Combat tricks, pump spells | Penalize main phase (`passScore - 1.0`), allow combat (`return null`) |
| **Hold for opponent's turn** | Instant removal, counterspells, flash creatures | Penalize own main phase, allow opponent's turn |
| **Reactive play** | Protection spells (hexproof, indestructible) | When stack is non-empty, let simulation decide freely (`return null`) |
| **Cast when behind** | Board wipes | Check board value ratio, penalize when ahead |
| **Flash ambush** | Flash creatures | Boost during opponent's combat (`+3.0`), penalize own main phase |

### Decision Overrides (respondToDecision)

| Pattern | When to use | Implementation |
|---------|------------|----------------|
| **Gift mode choice** | All gift cards | Use `pickBestGiftMode()` — simulates both modes with hand-size-scaled penalty |
| **Joint target simulation** | Bite/fight spells | Try all (my creature × their creature) combos, pick best board state |
| **Graveyard retrieval** | "Return up to N" spells | Always select max targets, rank by mana value |
| **Specific card logic** | Unique cards | Card-specific heuristics (e.g., Nocturnal Hunger life check) |

### Combat (attackPenalty)

| Pattern | When to use | Implementation |
|---------|------------|----------------|
| **Don't attack** | Creatures with valuable tap abilities | Return high penalty (e.g., `10.0` for Goblin Sharpshooter) |

## How to Add an Advisor

1. Identify what the AI does wrong with the card (timing? targeting? mode choice?)
2. Pick the right pattern from above
3. Either add the card name to an existing advisor's `cardNames` set, or create a new
   `object MyAdvisor : CardAdvisor` in `BloomburrowAdvisorModule.kt`
4. Register it in `BloomburrowAdvisorModule.register()`
5. Run the benchmark to verify:
   ```bash
   ./gradlew :rules-engine:test --tests "*.AdvisorBenchmark" -Dbenchmark=true -DbenchmarkGames=100 --rerun
   ```

## Key State Queries for Advisors

```kotlin
// Phase/timing
state.step.phase == Phase.COMBAT           // in combat?
state.step.isMainPhase                     // main phase?
state.activePlayerId == playerId           // our turn?
state.stack.isNotEmpty()                   // something on the stack?

// Board state
creatureBoardValue(state, projected, playerId)   // total creature value
creatureCount(projected, playerId)               // creature count
lifeTotal(state, playerId)                       // life total
handSize(state, playerId)                        // cards in hand

// Opponent
state.getOpponent(playerId)                      // opponent's entity ID
giftPenalty(state, playerId)                     // context-sensitive gift cost

// Creature details (from projected state)
projected.isCreature(entityId)
projected.getPower(entityId)
projected.getToughness(entityId)
projected.getKeywords(entityId)
projected.getController(entityId)
projected.getBattlefieldControlledBy(playerId)
```

## Bloomburrow Card Checklist

Cards are grouped by type. Status:
- [x] = Has an advisor
- [ ] = Needs review (may or may not need an advisor)
- N/A = Vanilla/simple enough that generic AI handles it fine

### Instants

- [x] **Blooming Blast** — GiftValueAdvisor (gift mode simulation)
- [x] **Cache Grab** — N/A (generic SelectCardsDecision handler picks best permanent from milled cards)
- [x] **Conduct Electricity** — InstantRemovalAdvisor (hold for opponent's turn)
- [x] **Consumed by Greed** — GiftRemovalAdvisor (gift mode with sacrifice)
- [x] **Crumb and Get It** — GiftCombatTrickAdvisor (hold for combat + gift mode)
- [x] **Dawn's Truce** — GiftProtectionAdvisor (hold for opponent's turn + gift mode)
- [x] **Dazzling Denial** — CounterspellAdvisor (hold for opponent's spells)
- [x] **Dire Downdraft** — InstantRemovalAdvisor (instant tuck removal, hold for opponent's turn)
- [x] **Early Winter** — InstantRemovalAdvisor (modal exile creature/enchantment)
- [x] **Feed the Cycle** — InstantRemovalAdvisor (instant destroy, hold for opponent's turn)
- [x] **Hazel's Nocturne** — GraveyardRetrievalAdvisor (always return max creatures)
- [x] **High Stride** — CombatTrickAdvisor (hold for combat)
- [x] **Into the Flood Maw** — GiftBounceAdvisor (gift mode simulation)
- [x] **Long River's Pull** — GiftCounterspellAdvisor (prefer non-gift for creatures)
- [x] **Mabel's Mettle** — CombatTrickAdvisor (hold for combat)
- [x] **Might of the Meek** — CombatTrickAdvisor (hold for combat)
- [x] **Nocturnal Hunger** — GiftRemovalAdvisor (life + hand-aware gift decision)
- [x] **Overprotect** — CombatTrickAdvisor (hold for combat)
- [x] **Parting Gust** — GiftRemovalAdvisor (permanent vs temporary exile)
- [x] **Pawpatch Formation** — ModalRemovalAdvisor (lighter hold: removal + draw/Food fallback)
- [x] **Peerless Recycling** — GiftValueAdvisor (gift mode simulation)
- [x] **Polliwallop** — BiteSpellAdvisor (joint target: creature × opponent creature)
- [x] **Rabbit Response** — CombatTrickAdvisor (team pump, hold for combat)
- [x] **Rabid Gnaw** — BiteSpellAdvisor (joint target: pump + bite)
- [x] **Repel Calamity** — InstantRemovalAdvisor (hold for opponent's turn)
- [ ] **Run Away Together** — Bounce both players' creatures. Review targeting
- [ ] **Savor** — Lifegain instant. Generic AI likely handles fine
- [x] **Sazacap's Brew** — GiftCardDrawAdvisor (gift mode simulation)
- [x] **Scales of Shale** — CombatTrickAdvisor (hold for combat)
- [x] **Shore Up** — CombatTrickAdvisor (hold for combat)
- [x] **Sonar Strike** — InstantRemovalAdvisor (hold for opponent's turn)
- [x] **Spellgyre** — SpellgyreAdvisor (counter or draw mode, context-dependent)
- [ ] **Take Out the Trash** — Damage + conditional draw. Review
- [x] **Valley Rally** — CombatTrickAdvisor (hold for combat)

### Sorceries

- [ ] **Agate Assault** — Modal: 4 damage creature / exile artifact
- [ ] **Calamitous Tide** — Bounce up to 2 + loot
- [x] **Coiling Rebirth** — GiftValueAdvisor (gift mode simulation)
- [x] **Cruelclaw's Heist** — GiftValueAdvisor (gift mode simulation)
- [x] **Dewdrop Cure** — GiftValueAdvisor (gift mode simulation)
- [ ] **Diresight** — Surveil 2, draw 2, lose 2 life. Generic AI likely fine
- [ ] **Fell** — Simple destroy creature. Sorcery speed, no advisor needed
- [ ] **For the Common Good** — Token creation. Generic AI likely fine
- [ ] **Hop to It** — Token creation. Generic AI likely fine
- [x] **Longstalk Brawl** — BiteSpellAdvisor (joint target + gift mode)
- [x] **Mind Spiral** — GiftCardDrawAdvisor (gift mode simulation)
- [ ] **Otterball Antics** — Token creation + pump. Generic AI likely fine
- [ ] **Pearl of Wisdom** — Draw 2. Generic AI likely fine
- [ ] **Playful Shove** — 1 damage + draw. Generic AI likely fine
- [ ] **Portent of Calamity** — Complex card selection. May need help
- [ ] **Psychic Whorl** — Discard spell. Generic AI likely fine
- [ ] **Ruthless Negotiation** — Control magic sorcery. Review targeting
- [x] **Season of Gathering** — Budget modal (5 pawprint). Generic: `DecisionResponder` simulates every budget spend
- [x] **Season of Loss** — Budget modal (5 pawprint). Generic: `DecisionResponder` simulates every budget spend
- [x] **Season of the Bold** — Budget modal (5 pawprint). Generic: `DecisionResponder` simulates every budget spend
- [x] **Season of the Burrow** — Budget modal (5 pawprint). Generic: `DecisionResponder` simulates every budget spend
- [x] **Season of Weaving** — Budget modal (5 pawprint). Generic: `DecisionResponder` simulates every budget spend
- [ ] **Splash Portal** — Token + bounce. Generic AI likely fine
- [x] **Starfall Invocation** — BoardWipeAdvisor + GiftBoardWipeAdvisor
- [ ] **Stargaze** — Look at 2X, keep X. Complex selection, may need help
- [x] **Wear Down** — GiftValueAdvisor (gift mode simulation)
- [x] **Wildfire Howl** — BoardWipeAdvisor + GiftBoardWipeAdvisor

### Enchantments

- [ ] **Artist's Talent** — Class: discard-draw / cost reduction / damage boost
- [ ] **Bandit's Talent** — Class: discard / life loss / draw
- [x] **Banishing Light** — N/A (simple exile, generic AI handles targeting)
- [ ] **Blacksmith's Talent** — Class: equipment / auto-attach / double strike
- [ ] **Builder's Talent** — Class: wall token / +1/+1 counters / reanimate
- [ ] **Caretaker's Talent** — Class: token draw / copy token / token buff
- [x] **Feather of Flight** — CombatTrickAdvisor (flash aura, hold for combat)
- [ ] **Festival of Embers** — Cast from graveyard engine. Complex value assessment
- [ ] **Gossip's Talent** — Class: surveil / unblockable / exile-flicker
- [ ] **Hoarder's Overflow** — Stash counter engine. Complex trigger management
- [x] **Hunter's Talent** — BiteSpellAdvisor (bite ETB, joint target simulation)
- [ ] **Innkeeper's Talent** — Class: +1/+1 counter / ward / double counters
- [ ] **Kitnap** — Aura tap/stun with gift decision
- [ ] **Lunar Convocation** — Enchantment with triggered abilities
- [ ] **Scavenger's Talent** — Class: food / mill / reanimate
- [ ] **Stocking the Pantry** — Draw engine with supply counters
- [ ] **Stormchaser's Talent** — Class: token / return spell / token on cast
- [ ] **Sugar Coat** — Transform creature into Food artifact
- [ ] **War Squeak** — Aura: can't block + haste

### Creatures (only those with complex abilities needing advisors)

- [x] **Galewind Moose** — FlashCreatureAdvisor (ambush block during opponent's combat)
- [ ] **Dragonhawk, Fate's Tempest** — Complex triggered abilities
- [ ] **Mabel, Heir to Cragflame** — Equipment synergy triggers
- [ ] **Vren, the Relentless** — Death trigger value engine
- [ ] **Ygra, Eater of All** — Food sacrifice synergy
- [ ] Other creatures with activated abilities — Review for `attackPenalty` if tap ability > attacking

### Artifacts

- [ ] **Bumbleflower's Sharepot** — Card draw artifact
- [ ] **Carrot Cake** — Food token + draw triggers
- [ ] **Fountainport Bell** — Scry + draw
- [ ] **Heirloom Epic** — Equipment with adventure
- [ ] **Patchwork Banner** — Tribal anthem + mana
- [ ] **Short Bow** — Equipment: reach + ping
- [ ] **Sinister Monolith** — Mana rock with downside
- [ ] **Starforged Sword** — Equipment buff
- [ ] **Wishing Well** — Scry artifact

### Lands

- N/A — Lands don't need advisors (play logic is handled generically)

## Priority Cards for Next Pass

High impact cards that likely need advisors but don't have one yet:

1. **Season cycle** (5 cards) — Budget modal spells with complex mode optimization. The generic
   `BudgetModalDecision` handler picks cheapest modes greedily, but optimal play requires evaluating
   which combination of modes produces the best board state.
2. **Class enchantments** (10 cards) — When to level up vs spend mana on creatures is a key
   strategic decision the generic AI can't evaluate.
3. **Pawpatch Formation** — Three-mode removal/utility spell.
4. **Stargaze** / **Portent of Calamity** — Complex card selection where generic AI may undervalue.
5. **Kitnap** — Gift aura with control effect.

## Running the Benchmark

```bash
# Quick validation (5 pairs = 10 games, ~5 seconds)
./gradlew :rules-engine:test --tests "*.AdvisorBenchmark" -Dbenchmark=true -DbenchmarkGames=5 --rerun

# Standard benchmark (100 pairs = 200 games, ~30-60 seconds)
./gradlew :rules-engine:test --tests "*.AdvisorBenchmark" -Dbenchmark=true -DbenchmarkGames=100 --rerun

# Large benchmark for statistical significance (500 pairs = 1000 games)
./gradlew :rules-engine:test --tests "*.AdvisorBenchmark" -Dbenchmark=true -DbenchmarkGames=500 --rerun
```

Each pair plays the same random Bloomburrow sealed deck twice (swapping who goes first) to reduce
first-player bias. Look for:
- **Advised win % > 50%** (excluding draws) = improvement
- **Balanced P1/P2 wins** = not just first-player bias
- **Run multiple times** — there's high variance between runs due to random decks
