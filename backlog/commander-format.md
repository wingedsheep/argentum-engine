# Commander Format

Add Commander format support to the Argentum Engine. Commander is a 100-card singleton format with a designated
legendary creature commander, 40 starting life, and a 21-commander-damage loss condition. Phase 1 targeted **1v1
Commander**; multiplayer pods landed later on top of the N-player work — see § Phase 3.

## Status (2026-05-08)

Deck-construction layer + deckbuilder UI shipped ahead of the engine work. The runtime engine (command zone setup,
casting from command, commander tax, zone-change redirect, commander damage SBA) is still untouched.

**Done**

- ✅ **1.7 — Deck validator commander rules.** `DeckValidator.validate(Deck, format)` overload enforces commander
  eligibility (legendary creatures + planeswalkers + "can be your commander" override clause), color-identity
  subset, and `MISSING_COMMANDER` for commander-shape formats. Legacy `Map<String, Int>` overload preserved for
  back-compat.
- ✅ **`Deck.commander: String?` field** in `mtg-sdk` (default null; backward-compatible across all existing tests
  and call sites).
- ✅ **`MtgaDeckFormat` parser/serializer** with explicit `Commander` section + format-gated first-card fallback
  for paste-imports from Moxfield / Archidekt / Arena.
- ✅ **`DecksController.validate`** accepts an optional `commander` and routes to the `Deck` overload when present.
- ✅ **`DeckFormat.isCommanderShape`** helper (Commander, Brawl, Standard Brawl).
- ✅ **`CommanderEligibility`** in game-server (legendary creature, planeswalker with override clause, or any card
  with explicit "can be your commander" oracle text).
- ✅ **Phase 2 — color identity (most of it).** `CardDefinition.colorIdentity` now reads CR 903.4 properly:
  mana cost colors **+** oracle-text mana symbols (incl. hybrid `{W/U}`, `{2/G}`, Phyrexian `{W/P}`) **+** basic
  land subtypes (Plains→W, Island→U, Swamp→B, Mountain→R, Forest→G — applies to dual lands like Tundra too).
- ✅ **1.8 partial — deckbuilder UI.**
  - Crown toggle button on each row in commander-shape formats; gated to legendary creatures + planeswalkers,
    server-side `CommanderEligibility` is the authoritative gate.
  - Active commander row gets a gold border + filled crown badge.
  - Commander hoists to a dedicated **Commander** group at the top of the deck list, regardless of card type.
  - `SavedDeck.commander?` persisted in localStorage; restored on load, cleared on new / import / non-commander
    format / card removal.
  - Validation request threads `commander` to the server so identity violations show live as the user designates.
  - Color filter chips on the left rail render real mana SVG icons (W/U/B/R/G/C) instead of colored dots.

**Pending follow-ups (this section)**

These surfaced during the deck-construction work and are scoped tightly enough to land independently of the
larger Phase 1 engine plan.

- [ ] **Color indicators (rule 204).** `CardDefinition` has no field for them; `colorIdentity` therefore can't
  fold them in. Affects MDFC backs and the small set of cards that have an indicator without a colored mana
  symbol. There's a TODO in the `colorIdentity` getter — when an `indicator: Set<Color>?` (or similar) field is
  added to `CardDefinition`, union it into the identity computation. Tests live in
  `mtg-sdk/.../model/ColorIdentityTest.kt`; add an "indicator without mana cost contributes to identity" case
  alongside the existing ones.
- [ ] **Wire `commander` through deck-submission paths.** `Deck.commander` is plumbed end-to-end for
  `/api/decks/validate`, but the lobby / quick-game / tournament submission DTOs still send flat
  `Record<string, number>` payloads. Until they're updated, those flows can't enforce commander color identity at
  submit time. Specifically:
  - `web-client/src/types/messages.ts` — `SubmitQuickGameLobbyDeckMessage`, `SubmitSealedDeckMessage`, etc.: add
    optional `commander?: string`.
  - `game-server/.../handler/QuickGameLobbyHandler.kt:101, 270` and `LobbyHandler.kt:1829` — switch to the
    `Deck` overload when the client supplies a commander.
  - Quick Game lobby UI / Custom-Decks-Tournament UI — pass through the saved deck's `commander` field on
    submission. Today they default to null and validation runs in legacy mode.
- [ ] **Update the now-stale "structural rules" comment** in `DeckValidator.profileFor` (line ~140). Color
  identity and commander eligibility are now enforced when a `Deck` is supplied. Only **partner / Background /
  Friends Forever pairs** remain as a TODO at the deck-construction layer (Phase 4 territory, but the comment
  should reflect current scope).
- [ ] **Engine command-zone instantiation (Phase 1.2 entry point).** `GameInitializer.kt:139` currently iterates
  `Deck.cards` into the library. The data model now carries a `commander` field, but the engine still ignores
  it. Wiring this is the natural next step before any Phase 1.3+ work (cast from command, tax, damage, redirect)
  starts paying off.

Everything below this section is the original Phase 1 → Phase 4 plan, unchanged.

---

## Engine survey (what already exists)

- **Multiplayer:** `GameState.initial()` (`rules-engine/.../state/GameState.kt:458`) and `GameInitializer`
  (`:96`) already require `playerIds.size >= 2` — no hard 2-player cap.
- **Command zone:** `Zone.COMMAND` already exists in `mtg-sdk/.../core/Zone.kt:7-24` alongside the other zones.
- **Configurable starting life:** `PlayerConfig(startingLife: Int = 20)` in `GameInitializer.kt:27`.
- **Cost modification:** `CostCalculator.calculateEffectiveCost()` (`rules-engine/.../mechanics/mana/CostCalculator.kt:58-80`)
  evaluates `SpellCostReduction` static abilities — symmetric cost-*increase* path is needed for tax.
- **Zone-change replacement precedent:** `ExileOnLeaveBattlefieldComponent`
  (`rules-engine/.../components/battlefield/BattlefieldComponents.kt:90-94`) shows the unconditional-redirect pattern.
- **Combat damage tracking:** `HasDealtCombatDamageToPlayerComponent` (`:397-403`) is a lifetime flag,
  not (source, target) pairs — needs a new tracker.
- **Color identity:** `CardDefinition.colorIdentity` exists at `mtg-sdk/.../model/CardDefinition.kt:94-100` but is
  computed from mana cost only (does not yet scan rules text for hybrid/phyrexian/color-word symbols).
- **Format concept:** does not exist yet. No `Format` enum, no game-mode config.

## Design principle

**Commander as data, not branches.** Introduce a `Format` config object that the engine reads, rather than scattering
`if (isCommander)` across handlers. The cost is upfront discipline (one config object grows tentacles for life total,
hand size, mulligan rules, win conditions). The payoff is that Brawl, Oathbreaker, Pauper Commander, and 1v1 Commander
become trivial config variants, not new code paths.

---

## Phase 1 — 1v1 Commander

### Suggested implementation order

1. **1.1** — `Format` config object (foundation; no behavior change)
2. **1.2** — `CommanderComponent` + setup (commanders exist in the right zone)
3. **1.7** — Deck validator (can be done in parallel by another contributor; gates UI work)
4. **1.5** — Zone-change replacement, flag-gated always-divert (do this *before* damage tests so commanders don't vanish to graveyard during combat scenarios)
5. **1.3** — Cast from command zone
6. **1.4** — Commander tax (depends on 1.3)
7. **1.6** — Commander damage tracking + lethal SBA

### Definition of done (Phase 1 ship gate)

- [ ] Two players can start a Commander game (40 life, commander in `Zone.COMMAND`)
- [ ] Commander can be cast from command zone; tax escalates correctly across recasts
- [ ] Commander destroyed/exiled/bounced/milled returns to command zone (always-divert flag on)
- [ ] 21 cumulative combat damage from a single commander wins the game (`LossReason.COMMANDER_DAMAGE`)
- [ ] Deck validator rejects off-color, non-singleton, wrong-size, or non-legendary-creature-commander decks
- [ ] Web client renders the command zone, shows per-commander damage tallies, and allows casting from command
- [ ] One full e2e Playwright scenario: deck-build → game start → cast commander → combat damage → opponent loses by commander damage

### 1.1 `Format` config object

- New `mtg-sdk/.../model/Format.kt`:
  ```kotlin
  sealed interface Format {
      object Standard : Format
      data class Commander(
          val commanderDamageThreshold: Int = 21,
          val deckSize: Int = 100,
          val startingLife: Int = 40,
          val startingHandSize: Int = 7,
          val alwaysDivertToCommand: Boolean = true, // see 1.5
      ) : Format
  }
  ```
- `GameInitializer.GameConfig` (`:40`) — add `format: Format = Format.Standard`.
- `GameInitializer.PlayerConfig` (`:24`) — add `commanderCardName: String? = null`.
- `GameState` (`:105`) — add `val format: Format = Format.Standard`. Default keeps existing tests untouched.

**Decision A (locked): `CommanderRegistryComponent` on the player entity** holding `commanderIds: List<EntityId>`.
Rejected alternative: a `Map<EntityId, List<EntityId>>` field on `GameState`. The component approach stays
ECS-shaped, falls out of player-entity queries, serializes naturally, and Partner / Background later just append to
the list without a schema change.

**Tests:** `FormatSerializationTest`, `GameInitializerCommanderTest` (asserts life=40, format set, commanders in
`Zone.COMMAND`).

### 1.2 `CommanderComponent` + initial command-zone setup

- New `rules-engine/.../components/identity/CommanderComponent.kt`:
  ```kotlin
  data class CommanderComponent(val ownerId: EntityId, val castsFromCommandZone: Int = 0) : Component
  ```
- Sibling `CommanderRegistryComponent(val commanderIds: List<EntityId>)` (player-attached).
- Register both in `rules-engine/.../core/Serialization.kt:304` near `ExileOnLeaveBattlefieldComponent`.
- `GameInitializer.initializeGame()` step 3 (line 136) — when `config.format is Format.Commander`, find the deck card
  by `commanderCardName`, attach `CommanderComponent`, route to `ZoneKey(playerId, Zone.COMMAND)` instead of library,
  attach `CommanderRegistryComponent` to the player.

Phase 1 does **not** modify the legend rule. Commanders are *additionally* legendary; the legend rule remains
battlefield-only.

**Tests:** `CommanderSetupTest` — both commanders in `Zone.COMMAND` at game start, life totals = 40.

### 1.3 Casting from the command zone

- `rules-engine/.../legalactions/enumerators/CastSpellEnumerator.kt` — mirror the HAND (line 128) and GRAVEYARD
  (line 1428) enumeration paths for `COMMAND`; gate on `CommanderComponent` ownership.
- `CastSpellHandler.execute()` and `validate()` — accept `castFromZone == Zone.COMMAND` for cards with
  `CommanderComponent`. Verify `CastZoneResolver` (private to the handler package) also permits it — likely a hidden
  checkpoint.

**Tests:** game-server scenario test — command-zone commander appears in legal actions and successfully resolves to
the battlefield.

### 1.4 Commander tax in `CostCalculator`

- `CostCalculator.kt:58` — extend `calculateEffectiveCost()` with `fromZone: Zone? = null` (default preserves all
  existing call sites). After `calculateFilterCostIncrease` (line 90), add a commander-tax helper that reads the
  card's `CommanderComponent` and applies `+2 * castsFromCommandZone` generic mana when `fromZone == COMMAND`.
- `CastSpellHandler.kt:310, :1125` — pass `fromZone` through (already obtained via `zoneResolver`).
- `mechanics/stack/StackResolver.kt` — increment `castsFromCommandZone` **on cast commit** (after payment, before
  push to stack), not on resolution.

**Decision B (locked): increment on cast-commit** (after payment, before push to stack), not on resolution.
Per CR 903.8, the additional cost is paid *to cast*; countered commanders still owe the higher tax next time.
Surfaced here only because the engine has no precedent for "cost counter that increments on cast" — implement as
its own helper next to `castsFromCommandZone`.

**Tests:** unit test on `CostCalculator` with `CommanderComponent(castsFromCommandZone = 2)` produces effective
generic cost +4 only when `fromZone == COMMAND`. Scenario: cast commander, kill it, recast — second cast costs more.

### 1.5 Command-zone replacement on zone change ⚠ biggest decision

When a card with `CommanderComponent` would move to graveyard, exile, hand, or library (from any zone), the owner
*may* divert it to the command zone instead.

- `rules-engine/.../handlers/effects/ZoneMovementUtils.kt:363` — `checkZoneChangeRedirect()`. After the
  `ExileOnLeaveBattlefieldComponent` self-check at line 371, add a parallel branch for `CommanderComponent`.

**Decision C (locked): ship Phase 1 with `alwaysDivertToCommand = true`; defer player-choice to Phase 1.5.**
Rationale: the choice between "divert to command zone" and "stay in graveyard" only matters for graveyard-recursion
archetypes (Muldrotha, Meren, Karador). For everything else, divert is correct 100% of the time. The plumbing cost
of paused `ZoneTransitionResult` is large — `moveToZone` is called from combat dies, bounce, exile, scry-back,
mill, tucker, and stack-resolution paths, each of which would need to handle a paused outcome. Ship a playable 1v1
game first, layer the choice on once the surface is stable. The flag becomes a behavior toggle, not technical debt.

| | Always-divert (Phase 1) | Paused decision (Phase 1.5) |
|---|---|---|
| Fidelity | Right ~95% of the time; wrong for graveyard-recursion archetypes | Matches MTG rules |
| Plumbing cost | One-line `if` in `checkZoneChangeRedirect` | `CommanderZoneChoiceContinuation` + paused `ZoneTransitionResult` + audit every caller |
| New types | None | Continuation + result extension |

**Tests:** scenarios for destroy / bounce / exile / mill / tucker — each routes the commander to `Zone.COMMAND` (or
to the chosen destination, in Phase 1.5).

### 1.6 Commander damage tracking + lethal SBA

- `GameState.kt:105` — add `val commanderDamage: Map<Pair<EntityId, EntityId>, Int> = emptyMap()` keyed by
  `(commanderEntityId, defendingPlayerId)`. Helper `recordCommanderDamage(...)` near `addDelayedTrigger` (line 437).
- `rules-engine/.../mechanics/combat/CombatDamageManager.kt` — at the two `DamageDealtEvent(..., true,
  targetIsPlayer = true)` emission sites (~lines 561, 698), accumulate when source has `CommanderComponent`. Use
  `effectiveAmount` (post-prevention), not `originalAmount`. Token-copy commanders must NOT contribute (CR 903.10a
  — token copies aren't the commander) — gate on `!container.has<TokenComponent>()`.
- New SBA `rules-engine/.../mechanics/sba/player/CommanderDamageLossCheck.kt` modeled on `PlayerLifeLossCheck.kt`. Add
  `LossReason.COMMANDER_DAMAGE` at `PlayerComponents.kt:237`. Register in `PlayerSbaModule`. Add `SbaOrder.COMMANDER_DAMAGE_LOSS`
  ordinal next to `PLAYER_LIFE_LOSS`. SBA reads threshold from `state.format`; no-ops if not Commander format.

**Tests:** unit — state with `commanderDamage = (cmdr, victim) -> 21` produces `PlayerLostEvent(victim,
COMMANDER_DAMAGE)`. Scenario — 11+10 unblocked commander damage at 39 life makes the opponent lose by commander
damage, not life loss.

### 1.7 Commander deck-construction validation

Lives at the deck-construction layer, not the runtime engine. The engine stays format-agnostic about deck legality.

- `game-server/.../deck/DeckValidator.kt` — add `validateCommander(deckList, commanderName)` reusing existing
  `countsByBaseName` / `errors` infrastructure with `MIN_DECK_SIZE = 100`, `MAX_COPIES_NON_BASIC = 1`, basic-land
  exemption, and a color-identity subset check (`card.colorIdentity ⊆ commander.colorIdentity`).
- New error codes: `WRONG_COMMANDER_DECK_SIZE`, `NOT_SINGLETON`, `COLOR_IDENTITY_VIOLATION`, `INVALID_COMMANDER`
  (commander must be a legendary creature, with a future hook for "can be your commander" oracle text).

**Tests:** `DeckValidatorCommanderTest` — 99 cards fail, dup non-basic fails, dup basics pass, off-color non-basic
fails, on-color all-singleton 100 passes.

### 1.8 Web client (minimum viable)

The engine work is moot without a UI. Phase 1 frontend scope:

- **Command zone widget** — separate area near the player's hand, shows commander card face-up. Click to cast (same
  flow as casting from hand).
- **Commander damage tally** — on the opponent life-total widget, per-commander mini-counter (`5/21` style). Reads
  from `commanderDamage` map in masked client state. Threshold flashes when ≥ 18 (warning) and turns red at 21.
- **Tax indicator** — small `+4` badge on the command-zone card showing the current tax cost.
- **Deck builder** — Commander format toggle, commander picker (legendary creatures only for Phase 1), color-identity
  filtered card pool.

Phase 1.5 adds the divert-yes-no decision modal (reuses existing decision modal infrastructure).

---

## Phase 1.5 — Proper zone-change replacement (player choice)

Replace `alwaysDivertToCommand` heuristic from 1.5 with a real `YesNoDecision` flow. Add
`CommanderZoneChoiceContinuation`, extend `ZoneTransitionResult` with `pendingDecision`, audit the callers of
`ZoneTransitionService.moveToZone`. Test fixtures for graveyard-recursion strategies (commander stays in graveyard
for Muldrotha-shaped plays).

## Phase 2 — Color identity polish

- Deepen `CardDefinition.colorIdentity` to scan oracle text for mana symbols (hybrid, phyrexian, mono symbols in
  activated/triggered abilities) and color indicators / color words. Crackling Doom currently passes Phase-1
  validation when it shouldn't.
- Hook for "can be your commander" / "partner" / "friends forever" oracle-text detection so non-legendary commanders
  (planeswalkers in commander variants) can be supported by data, not by special-casing.

## Phase 3 — Multiplayer pods — **done** (issue #1456)

Most of this fell out of `backlog/multiplayer.md` rather than needing commander-specific work: the
"choose an opponent" audit, the attacker-declares-defender step and the >2-player lobby paths all landed
there. What was left was small, and it shipped:

- ✅ Commander runs at any table size. `Format.Commander` has no player-count field, commander damage is
  tallied per *(commander, defending player)* pair (`GameState.commanderDamage`), the command zone is
  per player, and the CR 903.9a zone choice loops `turnOrder`. `CommanderPodTest` pins all of it at
  four seats; `FreeForAllLobbyTest` plays a four-player premade Commander pod end to end.
- ✅ Drafted / sealed Commander at a pod table. The client's 1v1-only gate is gone; the one remaining
  block is Two-Headed Giant, whose shared team life total (CR 810.4) contradicts Commander's per-player
  40 — `LobbyHandler.handleStartTournamentLobby` refuses it server-side and the lobby says why.
- ✅ Pod-tuned config. `CommanderPreset.POD` (60 cards / 40 life / 21 damage) is what every multiplayer
  table plays at, resolved from the table by `TournamentLobby.effectiveCommanderPreset`. The host's
  Brawl-25 / Commander-30 choice stays a 1v1 pacing knob.
- ✅ **Commander is a lobby axis, not three fields.** Opening pods up exposed the taxonomy problem
  underneath: "this game runs Commander" was reachable through the pool-building `TournamentFormat`, the
  deck-legality `DeckFormat`, and the quick lobby's own format, so every consumer re-derived it — and the
  gate that blocked premade Commander pods was a copy that structurally could not see the premade path.
  `GameRules` (`mtg-sdk/.../core/Format.kt`) is now the lobby's counterpart to `Format.usesCommanders`:
  `TournamentLobby.rules` / `usesCommanderRules` is the single authority, `commanderRulesTableConflict`
  is the single statement of the 2HG conflict, and the client renders a **Rules** row between Cards and
  Table. The Commander pack formats and commander deck legality now only *default* the axis.

- ✅ **Commander with AI seats** (issue #1453). `CommanderDeckGenerator` (`:ai`) picks a legal commander
  (CR 903.3, via the SDK's shared `CommanderEligibility`) and builds a singleton deck inside its colour
  identity to the format's exact size — CR 903.5a/b/c, with a basics-only manabase so CR 903.5d holds by
  construction. `RandomDeckResolver` returns a `GeneratedDeck` (list *plus* commander) so the two halves
  are decided together, and it asks for commander-ness on the **Rules** axis rather than reading it off
  `DeckFormat`, which is what a premade Commander pod with no legality restriction needs. Every entry
  point is un-gated: quick-lobby Auto / From sets, a human's Random pool, premade tournament seats, and
  limited pools (`generateFromPool`, for a drafted or sealed Commander seat).

Deliberately still open:

- Commander-aware *play* — commander tax when evaluating casts, commander damage as a win/loss axis, and
  an intentional CR 903.9a command-zone choice. The AI plays legal Commander today (it answers the zone
  choice by simulating both branches, and `CostCalculator` already charges it the tax), just not well.
  See "AI advisor coverage" under Risks below.
- Commander deck *synergy* — the generator picks on rating and curve, not on what the commander wants.
  Partners / Backgrounds and colourless-identity commanders are out of its scope too (Phase 4).
- AI politics (group dynamics, kingmaker avoidance) — separate research project.
- **Out of scope permanently:** range of influence (CR 801) — `backlog/multiplayer.md` says so too — and
  the politics mechanics (monarch / initiative / voting).

## Phase 4 — Partner / Background / Companion / commander variants

- Plural commanders per player (already supported by `CommanderRegistryComponent.commanderIds: List<EntityId>` from
  Phase 1).
- Partner / Friends Forever / Partner With keyword detection at deck-construction time.
- Background pairing rules (`commanderName` becomes `commanderNames: List<String>` in `PlayerConfig`).
- Companion sideboard slot (Companion is technically a separate mechanic, but lives naturally near commander zone
  setup).
- Commander variants: Brawl (60 cards, standard-legal), Oathbreaker (planeswalker + signature spell, 60 cards,
  30 starting life), Pauper Commander (uncommon commander, common deck). These are **values of `GameRules`**,
  not new draft shapes or deck formats — that is the slot the Rules axis was built to have (see Phase 3).

---

## Risks and unknowns

- **Phase 1.5 plumbing is the largest unknown.** Existing replacement effects (`ExileOnLeaveBattlefieldComponent`,
  `RedirectZoneChange`) are unconditional; `ZoneChangeRedirectResult` has no "paused" notion. The flag-gated
  always-divert in Phase 1 sidesteps this — but Phase 1.5 will need to thread a paused result through every caller of
  `ZoneTransitionService.moveToZone`.
- **`CastZoneResolver`** (private to `CastSpellHandler` package) likely gates which zones a card can be cast from;
  verify before promising 1.3 is a small change.
- **Token copies of commanders** must not contribute to commander damage and aren't themselves the commander
  (CR 903.10a). Verify `CombatDamageManager` source IDs reference the original commander permanent on the
  battlefield, not a token clone.
- **Mulligan rules** differ in Commander (free first mulligan, partial mulligans). Phase 1 leaves vanilla mulligans
  in place; flag as known gap.
- **State projector** — confirm battlefield-only continuous effects (e.g., a Goblin Lord) don't accidentally apply to
  a Goblin commander sitting in `Zone.COMMAND`. Quick smoke test before merge.
- **Serialization compatibility** — adding `format` and `commanderDamage` fields to `GameState` may break persisted
  states. Check `game-server/persistence/` (if it exists) for migration needs.
- **AI advisor coverage** — commander tax, commander damage, and the command-zone choice all need advisor logic for
  the AI to play Commander competently. Out of scope for Phase 1 (engine correctness first), but a real follow-up.
- **E2E tests** — the web client needs UI for the command zone (visible, drag-to-cast), commander-damage tracker
  (per-commander tally on the opponent's life-total widget), and the divert-yes-no decision modal (Phase 1.5).
  Significant frontend work that's not in this engine plan.
