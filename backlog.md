# Rules Engine Backlog

This backlog outlines the steps to build the MTG rules engine, starting with the Portal set as a demonstration.

Portal is an ideal starting set because it's simplified: limited keywords and straightforward mechanics. While Portal itself doesn't contain instants, the engine supports the full rules including instants, flash, enchantments, and artifacts for future set expansion.

---

## Phase 1: Foundation - Core Domain Models

### 1.1 Create the `rules-engine` module
- [x] Add new Gradle module `rules-engine` with Kotlin 2.2 setup
- [x] Configure testing framework (JUnit 5 + Kotest assertions)
- [x] Set up module dependencies

### 1.2 Card Identity and Basic Types
- [x] `CardId` - unique identifier for each card instance (value class)
- [x] `Color` enum (White, Blue, Black, Red, Green, Colorless)
- [x] `ManaSymbol` sealed class hierarchy ({W}, {U}, {B}, {R}, {G}, {C}, {X}, generic {1}, {2}, etc.)
- [x] `ManaCost` - collection of mana symbols with CMC calculation
- [x] `CardType` enum (Creature, Sorcery, Land, Enchantment, Artifact)
- [x] `Supertype` enum (Basic, Legendary)
- [x] `Subtype` - creature types (Dragon, Goblin, etc.), land types (Plains, Island, etc.)
- [x] Write unit tests for mana cost parsing and CMC calculation

### 1.3 Card Definition Model
- [x] `CardDefinition` - the template/blueprint for a card (name, cost, types, text, P/T)
- [x] `CardInstance` - a specific instance of a card in the game (has CardId, references CardDefinition)
- [x] `CreatureStats` - power/toughness (base values and current values)
- [x] Write tests for card creation and stat tracking

### 1.4 Game Zones
- [x] `Zone` sealed class (Library, Hand, Battlefield, Graveyard, Exile, Stack, Command)
- [x] `ZoneType` enum for zone identification
- [x] `ZonedCard` - card instance with its current zone
- [x] Zone transition tracking (for "when enters/leaves" triggers)
- [x] Write tests for zone operations

---

## Phase 2: Game State Management

### 2.1 Player Model
- [x] `PlayerId` - unique player identifier
- [x] `Player` - life total, mana pool, poison counters
- [x] `ManaPool` - tracks available mana by color
- [x] Write tests for life/mana operations

### 2.2 Game State
- [x] `GameState` - immutable snapshot of entire game
  - Players and their zones (hand, library, graveyard)
  - Battlefield (shared zone)
  - Stack
  - Exile
  - Turn number, active player, priority holder
  - Phase/step tracking
- [x] Implement copy/update mechanics for state transitions
- [x] Write tests for state immutability

### 2.3 Turn Structure
- [x] `Phase` enum (Beginning, PrecombatMain, Combat, PostcombatMain, Ending)
- [x] `Step` enum (Untap, Upkeep, Draw, BeginCombat, DeclareAttackers, DeclareBlockers, CombatDamage, EndCombat, End, Cleanup)
- [x] `TurnState` - handles phase/step progression and priority
- [x] Write tests for turn progression

---

## Phase 3: Core Game Actions

### 3.1 Basic Actions
- [x] `Action` sealed class hierarchy for all game actions
- [x] `DrawCard` action
- [x] `ShuffleLibrary` action
- [x] `GainLife` / `LoseLife` actions
- [x] `AddMana` / `SpendMana` actions
- [x] Write tests for each action

### 3.2 Card Movement
- [x] `MoveCard` action (generic zone-to-zone movement)
- [x] Library → Hand (draw)
- [x] Hand → Stack (cast)
- [x] Stack → Battlefield (resolve permanent)
- [x] Stack → Graveyard (resolve sorcery)
- [x] Battlefield → Graveyard (destroy/sacrifice)
- [x] Any → Exile
- [x] Write tests for zone transitions

### 3.3 Tapping
- [x] `TapStatus` on permanents
- [x] `Tap` / `Untap` actions
- [x] "Tap for mana" for lands
- [x] "Tap to attack" for creatures
- [x] Write tests for tap mechanics

---

## Phase 4: Casting and Resolution

### 4.1 Mana Payment
- [x] `CanPayCost` - check if player can pay a mana cost
- [x] `PayManaCost` - deduct mana from pool
- [x] Handle generic mana payment (player choice)
- [x] Write tests for cost payment

### 4.2 Casting Spells
- [x] `CastSpell` action
- [x] Timing restrictions:
  - Sorcery speed (main phase, empty stack, active player has priority)
  - Instant speed (any time player has priority)
  - Flash (permanents that can be cast at instant speed)
- [x] Put spell on stack
- [ ] Targeting (for spells that target)
- [x] Write tests for casting

### 4.3 Stack and Resolution
- [x] `Stack` implementation (LIFO)
- [x] `ResolveTopOfStack` action
- [x] Permanent resolution (enters battlefield)
- [x] Sorcery/Instant resolution (effect then graveyard)
- [x] Write tests for stack operations

### 4.4 Priority System
- [x] `Priority` - tracks who can act
- [x] `PassPriority` action
- [x] Round of priority passing leads to resolution
- [x] Active player receives priority after each spell/ability resolves
- [x] Responding to spells/abilities (instant-speed interaction)
- [x] Write tests for priority

---

## Phase 5: Combat System

### 5.1 Combat Setup
- [x] `DeclareAttacker` action
- [x] Attacker requirements (untapped, no summoning sickness unless haste)
- [x] Tap attackers (vigilance creatures don't tap)
- [x] Write tests for attack declaration

### 5.2 Blocking
- [x] `DeclareBlocker` action
- [x] Blocker requirements (untapped creature)
- [x] Multiple blockers on one attacker (damage assignment order)
- [x] Flying restriction (can only be blocked by fliers/reach)
- [x] Write tests for blocking

### 5.3 Combat Damage
- [x] `ResolveCombatDamage` action
- [x] Simultaneous damage (first strike is not in Portal)
- [x] Trample damage assignment
- [x] Lethal damage calculation (including deathtouch)
- [x] Damage to players (unblocked attackers)
- [x] Write tests for damage calculation

### 5.4 Creature Death
- [x] State-based action: creature with lethal damage dies
- [x] State-based action: creature with 0 or less toughness dies
- [x] Move to graveyard
- [x] Player loses at 0 life / 10+ poison
- [x] Write tests for creature death and state-based actions

---

## Phase 6: Keywords and Abilities (Portal Subset)

### 6.1 Keyword Abilities
- [x] `Keyword` enum (Flying, Trample, Haste, Vigilance, First Strike, Flash, Lifelink, Deathtouch, Reach, Defender)
- [x] `HasKeyword` check on permanents
- [x] Write tests for keyword detection

### 6.2 Flying
- [x] Implement flying evasion in blocking rules
- [x] "Can only be blocked by creatures with flying or reach"
- [x] Write tests for flying

### 6.3 Trample
- [x] Implement trample damage assignment
- [x] Excess damage goes to defending player
- [x] Write tests for trample

### 6.4 Haste
- [x] Remove summoning sickness check for creatures with haste
- [x] Write tests for haste

### 6.5 Vigilance
- [x] Attacking doesn't cause tap for vigilant creatures
- [x] Write tests for vigilance

### 6.6 Flash
- [x] Permanents with flash can be cast at instant speed
- [x] Write tests for flash

### 6.7 First Strike
- [ ] First strike damage step before regular damage
- [ ] Double strike deals damage in both steps
- [ ] Write tests for first strike

### 6.8 Lifelink
- [ ] Damage dealt by creature with lifelink causes controller to gain life
- [ ] Write tests for lifelink

### 6.9 Deathtouch
- [x] Any amount of damage from deathtouch source is lethal
- [x] Write tests for deathtouch

### 6.10 Reach
- [x] Creatures with reach can block fliers
- [x] Write tests for reach

### 6.11 Defender
- [x] Creatures with defender cannot attack
- [x] Write tests for defender

---

## Phase 7: Triggered Abilities

### 7.1 Trigger System
- [ ] `Trigger` sealed class (OnEnterBattlefield, OnDeath, OnDraw, etc.)
- [ ] `TriggeredAbility` - condition + effect
- [ ] Trigger detection during state transitions
- [ ] Write tests for trigger detection

### 7.2 Common Triggers (Portal)
- [ ] "When this creature enters the battlefield..."
- [ ] "When this creature dies..."
- [ ] "At the beginning of your upkeep..."
- [ ] Write tests for each trigger type

### 7.3 Trigger Resolution
- [ ] Triggers go on the stack
- [ ] APNAP order (Active Player, Non-Active Player)
- [ ] Write tests for trigger stacking

---

## Phase 8: Enchantments and Artifacts

### 8.1 Enchantments
- [ ] Non-Aura enchantments (global effects)
- [ ] Aura enchantments (attached to permanents/players)
- [ ] Aura targeting on cast
- [ ] Aura attachment rules (falls off if target invalid)
- [ ] Static abilities from enchantments
- [ ] Write tests for enchantments

### 8.2 Artifacts
- [ ] Artifact permanents
- [ ] Artifact creatures
- [ ] Equipment (attach to creatures)
- [ ] Equip cost and timing (sorcery speed)
- [ ] Equipment falls off when creature leaves
- [ ] Activated abilities on artifacts
- [ ] Write tests for artifacts

---

## Phase 9: Targeting System

### 9.1 Target Definition
- [ ] `TargetRequirement` - what can be targeted (creature, player, etc.)
- [ ] `Target` - selected target(s)
- [ ] Legal target validation
- [ ] Write tests for target validation

### 9.2 Target Selection
- [ ] Player choice for targets
- [ ] "Target creature", "Target player", "Target creature or player"
- [ ] "Any target" handling
- [ ] Write tests for target selection

### 9.3 Target Validation on Resolution
- [ ] Check targets still legal when spell/ability resolves
- [ ] Fizzle if all targets illegal
- [ ] Write tests for fizzling

---

## Phase 10: Game Flow

### 10.1 Game Setup
- [ ] `StartGame` - initialize game state
- [ ] Set starting life totals (20)
- [ ] Determine starting player (coin flip / die roll)
- [ ] Shuffle libraries
- [ ] Draw opening hands (7 cards)
- [ ] Write tests for game setup

### 10.2 Mulligan
- [ ] `Mulligan` action (London mulligan: draw 7, put X on bottom)
- [ ] Mulligan decision per player
- [ ] Write tests for mulligan

### 10.3 Win/Lose Conditions
- [ ] State-based action: player at 0 or less life loses
- [ ] State-based action: player draws from empty library loses
- [ ] State-based action: player with 10+ poison loses (not in Portal)
- [ ] Declare winner when one player remains
- [ ] Write tests for win conditions

### 10.4 State-Based Actions
- [ ] `CheckStateBasedActions` - runs between every action
- [ ] Creature with 0 or less toughness dies
- [ ] Creature with lethal damage marked dies
- [ ] Player with 0 or less life loses
- [ ] Legendary rule (not common in Portal, but include)
- [ ] Write tests for SBAs

---

## Phase 11: Player Interaction Interface

### 11.1 Decision Interface
- [ ] `PlayerDecision` sealed class (choose target, choose attacker, etc.)
- [ ] `PlayerInterface` - abstraction for player input
- [ ] Synchronous decision model (request → response)
- [ ] Write tests with mock player

### 11.2 Choice Types
- [ ] Choose targets
- [ ] Choose attackers/blockers
- [ ] Choose mana payment
- [ ] Yes/no decisions
- [ ] Choose order (damage assignment)
- [ ] Choose cards (mulligan bottom)
- [ ] Write tests for each choice type

---

## Phase 12: Card Script System

### 12.1 Effect DSL
- [ ] `Effect` sealed class hierarchy
- [ ] `DealDamage(target, amount)`
- [ ] `DrawCards(player, count)`
- [ ] `GainLife(player, amount)`
- [ ] `DestroyTarget(target)`
- [ ] `CreateToken(definition, controller)`
- [ ] Write tests for each effect

### 12.2 Card Scripting
- [ ] `CardScript` - defines a card's behavior
- [ ] Link `CardDefinition` to its script
- [ ] Script for static abilities (keywords)
- [ ] Script for triggered abilities
- [ ] Script for spell effects
- [ ] Write tests for scripted cards

### 12.3 Condition System
- [ ] `Condition` sealed class
- [ ] "If you control...", "If your life total is..."
- [ ] Conditional effects
- [ ] Write tests for conditions

---

## Phase 13: Portal Set Implementation

The complete Portal set is available in `scryfall-portal.json` in the project root.

**Approach**: Card scripts will be generated using an LLM (Claude) that produces Kotlin code or DSL scripts for each card based on the Scryfall data. This avoids building a complex automated parser for oracle text, while still achieving complete set coverage.

### 13.1 Set Infrastructure
- [ ] `Set` - metadata (name, code, release date)
- [ ] `CardCatalog` - registry of all card definitions
- [ ] Set-based card loading
- [ ] Write tests for set loading

### 13.2 Scryfall Data Import
- [ ] JSON parser for Scryfall card data
- [ ] Map Scryfall fields to `CardDefinition`
- [ ] Parse mana costs from string format
- [ ] Parse type lines
- [ ] Extract keywords from oracle text
- [ ] Write tests for JSON parsing

### 13.3 Complete Portal Set Scripts
Generate scripts for all cards in the Portal set using LLM assistance:
- [ ] Basic lands (Plains, Island, Swamp, Mountain, Forest)
- [ ] Vanilla creatures (no abilities)
- [ ] French vanilla creatures (keywords only)
- [ ] Sorceries (damage, life gain, card draw, destruction, etc.)
- [ ] Creatures with ETB triggers
- [ ] Creatures with death triggers
- [ ] Creatures with activated abilities
- [ ] Enchantments (if any in Portal)
- [ ] Write integration tests for each card

### 13.4 Card Script Generation Workflow
- [ ] Export card list from scryfall-portal.json
- [ ] Group cards by complexity (vanilla, keywords-only, abilities)
- [ ] Generate scripts per card using LLM
- [ ] Review and test each generated script
- [ ] Full set validation (all cards load and have valid scripts)

---

## Phase 14: Integration Testing

### 14.1 Full Game Simulations
- [ ] Test complete game from start to finish
- [ ] Test various board states
- [ ] Test combat scenarios
- [ ] Test spell interactions

### 14.2 Scenario Tests
- [ ] "Player A attacks with flier, Player B cannot block"
- [ ] "Player casts Lava Axe, opponent goes to 15 life"
- [ ] "Creature dies, death trigger fires"
- [ ] "Player draws from empty library, loses game"

### 14.3 Regression Test Suite
- [ ] Catalog of known edge cases
- [ ] Automated game replay tests
- [ ] Golden file testing for deterministic scenarios

---

## Phase 15: Engine API

### 15.1 Public API Design
- [ ] `GameEngine` - main entry point
- [ ] `createGame(players, decks)` → `GameState`
- [ ] `executeAction(state, action)` → `GameState`
- [ ] `getAvailableActions(state)` → `List<Action>`
- [ ] `isGameOver(state)` → `Boolean`
- [ ] Write API documentation

### 15.2 Event System
- [ ] `GameEvent` sealed class (CardDrawn, DamageDelt, CreatureDied, etc.)
- [ ] Event emission during state transitions
- [ ] Observer pattern for UI integration
- [ ] Write tests for event emission

---

## Notes

### Design Principles
- **Immutable state**: All state transitions create new state objects
- **Pure functions**: Actions are deterministic given same state and inputs
- **Separation of concerns**: Card definitions vs. game rules vs. player interface
- **Testability**: Every component can be tested in isolation
- **Extensibility**: New sets add new cards, not new core rules (mostly)

### Out of Scope (for now)
- Planeswalkers (not in Portal)
- Multiplayer (more than 2 players)
- Sideboard
- Best-of-3 match structure
- Deck construction validation (minimum 60 cards, 4-of limit)