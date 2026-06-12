# True Multiplayer (3–4 Player Free-for-All)

Add real multiplayer to the Argentum Engine: a single game with 3–4 players competing as individuals
(CR 806, Free-for-All), playable online through a lobby with premade decks or draft/sealed pools. The headline
use case is 4-player Commander pods, but this plan is **format-agnostic and Commander-aware**: it builds
multiplayer as its own capability, checked at every decision against what Commander will need, so the two
projects compose without rework (Commander engine work lives in [`commander-format.md`](commander-format.md)).

## Locked scope decisions (2026-06-12)

- **Pod size: 2–4 players.** The UI is designed around the worst case of 3 opponent boards. Larger pods are
  out of scope (no "scales to N" hedging in the layout math).
- **UI approach: one viewed opponent + opponent rail.** (Revised 2026-06-12; the earlier equal-split idea
  died on the screen budget — the opponent half is sized for exactly one board.) One opponent's board is
  shown full-size at a time; the others live off-screen and **slide into view** when selected. An
  always-visible **opponent rail** (life, hand count, warnings, attention cues per opponent) is the
  overview guarantee.
- **First playable milestone: one online multi-player game via the lobby flow** — premade/custom decks or a
  draft/sealed pool, then a single Free-for-All game. Explicitly *not* a tournament: no rounds, no bracket,
  no best-of-N. Dev-loop testing rides on the scenario builder + hotseat seam, which is already N-player
  capable.
- **AI seats: deferred.** The built-in AI is deeply 1v1 (see survey); multiplayer pods start humans-only.
  Making the AI pod-competent is its own project (last section).

## Rules baseline (verified against the CR)

The multiplayer variant we implement is **Free-for-All (CR 806) with the attack-multiple-players option
(CR 802)** — the same baseline Commander uses:

- **CR 806.2b** — exactly one of attack-left / attack-right / attack-multiple-players must be used. We use
  attack multiple players; attack-left/right and limited range of influence (806.2a says it's usually unused
  in FFA) are permanently out of scope.
- **CR 802.2** — the active player doesn't choose *a* defending player; **all opponents are defending players**
  for the combat. Each attacking creature is declared attacking one specific player or planeswalker.
- **CR 802.2a / 508.5a** — any rule or effect that refers to "the defending player" means **one specific
  defending player, determined per attacking creature** (the player that creature attacks). If a spell or
  ability could refer to multiple defending players, its controller chooses one.
- **CR 101.4 (APNAP)** — simultaneous choices resolve in turn order starting from the active player. Already
  the engine's model; with N players the iteration just gets longer.
- **CR 800.4a–c** — multiplayer games **continue after a player leaves**. When a player leaves: all objects
  they own leave the game; control effects they granted/received end; their stack objects not represented by
  cards cease to exist; anything they still control is exiled. This happens immediately (not an SBA).
- **CR 104.2a** — a player wins when all their opponents have left the game.
- **CR 800.7** — in multiplayer, the starting player does **not** skip their first draw step (unlike
  two-player games).

Deliberately out of scope: limited range of influence (801), Grand Melee (807), team variants (Two-Headed
Giant, Alliance), planechase/archenemy. Monarch and initiative are follow-up mechanics, not prerequisites.

---

## Survey — what the codebase already supports (2026-06-12)

The architecture is in better shape than expected: **GameState, turn order, priority, attack declaration,
and game-end are already N-player correct.** The 2-player assumptions are concentrated in one engine seam
(the single-`opponentId` effect context), one combat step (blocker declaration), the server session/DTO
layer, the client's "the opponent" rendering, and the AI.

### Engine: already N-player ✓

- `GameState.turnOrder: List<EntityId>` — list-based, no pairs, no `[0]`/`[1]` indexing
  (`rules-engine/.../state/GameState.kt:65`). `getNextPlayer()` is cyclic modulo (`GameState.kt:636`).
- Priority: `priorityPassedBy: Set<EntityId>` + `allPlayersPassed()` checks `containsAll(turnOrder)` —
  correct for any N (`GameState.kt:547`, `PassPriorityHandler.kt:82`).
- Attack declaration: `declareAttackers(attackers: Map<EntityId, EntityId>)` already maps each creature to
  its own defender (player or planeswalker), and `CombatEnumerator` enumerates **all** opponents +
  their planeswalkers as attack targets (`AttackPhaseManager.kt:64`, `CombatEnumerator.kt:56-60`).
  `AttackingComponent.defenderId` is per-creature (`AttackingComponent.kt:15`).
- Game end: `GameEndCheck` filters out `PlayerLostComponent` holders and ends the game when ≤1 remains —
  N-player correct (`GameEndCheck.kt:22-49`).
- `GameInitializer` accepts `players: List<PlayerConfig>` with `size >= 2` — no cap.
- Hotseat / input authority: `state.actorFor(playerId)` + `HotseatControlComponent` is seat-count agnostic;
  the scenario builder's single-client multi-seat control works for N players today.
- Multiplayer-relevant mechanics already present: goad (per-goader tracking), `MustAttackPlayerComponent`,
  and — ahead of the commander-format doc's plan — **commander damage is already in `GameState`** as a
  per-`(commanderId, defendingPlayerId)` list with `CommanderDamageLossCheck` registered
  (`GameState.kt:162`, `mechanics/sba/player/CommanderDamageLossCheck.kt`).

### Engine: needs work ✗

- **The single-opponent context.** `GameState.getOpponent(playerId) = turnOrder.find { it != playerId }`
  (`GameState.kt:323`, comment even says "for 2-player games") and `EffectContext.opponentId: EntityId?`
  (`core/EffectContext.kt:27`). ~75 call sites across engine/server/AI read one of these; ~213 mentions of
  `opponentId` in rules-engine alone. Sites include `TargetResolutionUtils` (resolves `Player.Opponent` /
  `TargetOpponent` to `context.opponentId`), `DynamicAmountEvaluator`, `StackResolver` (3×),
  `StateProjector`, `ForEachExecutor.kt:98` (picks `firstOrNull { it != playerId }`), `ManaSolver` (3×),
  `PredicateEvaluator`, `EffectApplicator`, `AttackPhaseManager`/`BlockPhaseManager`.
- **Blocker declaration assumes one defender.** `TurnManager.kt:321`:
  `val defendingPlayer = newState.turnOrder.firstOrNull { it != activePlayer }` — one player gets the
  declare-blockers priority/decision for the whole combat. Needs per-defending-player block declaration in
  APNAP order. (`CombatDamageManager`'s per-target damage assignment is already flexible.)
- **No leave-the-game processing (CR 800.4a).** Today a player losing just sets `PlayerLostComponent` and the
  game ends at ≤1 active player — fine for 2 players, wrong for 4: the game must continue with the loser's
  cards removed, their control effects ended, their stack objects gone, their turn skipped.
- **`ConcedeHandler.kt:27`** resolves the winner via `getOpponent` — meaningless with 3 players.
- **First-turn draw**: verify the "starting player skips first draw" logic is gated on player count
  (CR 800.7 — no skip in multiplayer).
- SDK `Player.Opponent` (singular, non-target) is semantically ambiguous in multiplayer — needs an audit
  (most uses really mean `EachOpponent` or `TargetOpponent`; see Phase 0).

### Server: needs work ✗ (but the storage is right)

- `GameSession.players` is **already a map**; `player1`/`player2` are derived getters
  (`GameSession.kt:165-166`) — but ~dozens of call sites use them: WebSocket routing sends to
  `player1`/`player2` explicitly (`GamePlayHandler.kt:551-552, 787-796`), `getOpponentId()` returns one
  opponent (`GameSession.kt:237-239`), auto-concede in `ConnectionHandler` (~10 sites).
- DTOs are singular-opponent: `GameStarted(opponentName)` (`ServerMessage.kt:62`),
  `opponentDecisionStatus` (`ServerMessage.kt:96`), `SpectatorStateBuilder.buildState(state, p1, p2, …)`
  with `player1Id`/`player2Id` fields.
- `QuickGameLobby.MAX_PLAYERS = 2` hard cap (`QuickGameLobby.kt:52`).
- **Tournament lobby infrastructure is N-player already**: lobbies support 2–8 players
  (`LobbyHandler.kt:551`), grid draft scales boosters for 2–4 (`LobbyHandler.kt:550`), deck submission and
  AI identity creation are per-seat. Only the *match* layer pairs players into 2-player games
  (`TournamentMatch.player1Id/player2Id`, `TournamentMatchHandler.startSingleMatch`).
- Decision routing is fine: the engine's `state.actorFor()` is consulted per decision; only the broadcast
  loops and the DTO shape need generalizing.

### Web client: needs work ✗ (but the state shape is right)

- `ClientGameState.players` is **already a list** (`types/gameState.ts`), and the store has no singular
  player/opponent fields. The 2-player assumption lives in `useOpponent()`
  (`store/selectors.ts:175-194`, `find(p => p.playerId !== viewingPlayerId)`) and in every component that
  calls it.
- Layout is a 5-row grid: opponent hand / opponent battlefield / center HUD / your battlefield / your hand
  (`GameBoard.tsx:379-389`, `board/styles.ts:4-46`) — exactly one opponent slot.
- `ClientCombatState` has singular `attackingPlayerId` / `defendingPlayerId`
  (`rules-engine/.../view/ClientDTO.kt:591-594`, mirrored in `types/gameState.ts:527-539`) — though the
  client-side declaration state already maps attacker → target (`attackerTargets: Record<EntityId, EntityId>`).
- Spectator mode hardcodes `player1Id`/`player2Id` (`SpectatorGameBoard.tsx:74-75`).
- Targeting, zone piles, life display, hand fans are all per-player components already — they just need to be
  instantiated N−1 times and fed a player ID instead of "the opponent".

### AI: deeply 1v1 ✗ (deferred)

- 22 `getOpponent()` calls across `DecisionResponder`, `CombatAdvisor`, `Searcher`, `BoardFeatures`,
  advisor modules. Board evaluation is a single me-minus-opponent differential; minimax models exactly one
  opponent response; the LLM formatter prompts "YOU vs OPPONENT". Gym observation/config layers are already
  N-player (`EnvConfig.kt:48` allows ≥2), but reward evaluation is 2-player shaped.
- Consequence: **multiplayer pods launch humans-only**; AI work is the final section, not a blocker.

---

## Design principles

1. **One game, N seats — not a special mode.** A 2-player game is the degenerate case of the N-player code
   path (one opponent column, one defending player). No `if (multiplayer)` branches; everything iterates
   seats. The existing 2-player experience must come out pixel-identical (or consciously improved).
2. **Kill "the opponent" at the seam, not at call sites.** `EffectContext.opponentId` is the root; every
   downstream fix falls out of replacing it with explicit semantics (a chosen target, an iteration, or the
   per-creature defending player). Migrating call sites one by one against the old field would leave the trap
   armed for the next card.
3. **CR 802.2a is the model for "defending player".** Per-attacking-creature, stored where the attack is
   stored (`AttackingComponent.defenderId` — already right). Combat code derives "the defending player" from
   the creature in question, never from `turnOrder`.
4. **Overview always, one board at a time (UI).** Every opponent's *vital state* (life, hand count,
   warnings, what-just-happened cues) is visible at all times via the opponent rail; full boards are
   viewed one at a time at full fidelity and switch instantly. Cards are never shrunk below today's
   2-player scale — the screen budget compresses *how many boards* you see, never *how well* you see one.
5. **Seat identity is a first-class visual.** With 4 players, "who did that?" becomes the dominant UX
   question. Every player gets a stable seat color used consistently: board frame, life orb, log entries,
   combat arrows, stack item borders, targeting glows.
6. **Commander-aware at every decision.** Each phase below ends with a "Commander check" — the things the
   commander-format plan will need from that layer, verified to fit without rework.

---

## Phase 0 — Kill the single-opponent context (engine groundwork, no behavior change) ✅

**Status: landed.** `Player.Opponent` and `EffectContext.opponentId` are gone; `GameState.getOpponent`
is replaced by `getOpponents(playerId)` (turn-order, excluding lost players) + `activePlayers`. New
vocabulary: `Player.DefendingPlayer` (CR 802.2a, resolved from the source's attack assignment) and
`Player.AnOpponent` (genuinely non-targeted "an opponent" chooser shapes only — Callous Oppressor,
Risky Move; resolves to first opponent in turn order until the choose-an-opponent flow exists).
Central single-player resolution lives in `TargetResolutionUtils.resolvePlayerRef`. The built-in AI
keeps an explicitly 1v1 `soleOpponent` helper in the `ai` module. Verified by
`MultiplayerSmokeTest` (4-player init, priority round-trip, turn cycle, EachOpponent fan-out,
DefendingPlayer resolution).

The riskiest work, done first, while everything is still verifiable against the existing 2-player test corpus.

### 0.1 SDK semantic audit of `Player.Opponent`

`Player` (mtg-sdk) currently has `Opponent` (singular, resolves to `context.opponentId`), `EachOpponent`,
`Each`, `TargetOpponent`, `TargetPlayer`. In multiplayer, bare `Opponent` has no meaning — real cards say
"target opponent", "each opponent", "an opponent chosen at random", or "the defending player".

- Audit every card using `Player.Opponent` (grep the card corpus + `CardDefinitionSnapshotTest` goldens).
  Expected outcome per card: reclassify as `TargetOpponent` (needs a real target requirement),
  `EachOpponent`, or `DefendingPlayer` (new, combat-scoped, per CR 802.2a).
- Then **delete `Player.Opponent`** from the SDK, or keep it as a deprecated alias that fails card lint
  (`CardLinter`) so no new card uses it. Snapshot diffs make the reclassification reviewable per card.
- Add `Player.DefendingPlayer` resolving via the attacking creature in context (the `AttackingComponent`
  of the ability's source), not via turn order.

### 0.2 Replace `EffectContext.opponentId`

- Remove the field. Each of the ~75 read sites resolves through one of:
  - **Chosen target** — `TargetOpponent`/`TargetPlayer` reads the bound target from the resolution context
    (already how proper targets work; `TargetEnumerationUtils` already enumerates
    `turnOrder.filter { it != playerId }`, which is N-correct).
  - **Iteration** — `EachOpponent`/`Each` (already N-correct).
  - **Combat derivation** — defending player per CR 802.2a from the relevant attacking creature.
  - **Genuine 2-player shortcut** (e.g. `ForEachExecutor.kt:98`, `ManaSolver`, `StateProjector`,
    `StackResolver` sites) — rewrite to whichever of the above the call actually means.
- Replace `GameState.getOpponent(playerId)` with `getOpponents(playerId): List<EntityId>` (turn-order,
  excluding lost players). Keep no single-opponent helper; a deprecated shim that throws on >2 players is
  acceptable during migration only.
- `ConcedeHandler` — concede marks the conceding player lost; the *winner* is whoever `GameEndCheck`
  determines (only meaningful when one player remains). No `getOpponent` call.

### 0.3 Verification gate

- Full `just test` green with **zero** card snapshot diffs except the audited `Player.Opponent`
  reclassifications.
- `CardLinter` rule: `Player.Opponent` (if kept as alias) is an error in new cards.
- A new `MultiplayerSmokeTest` in rules-engine: a 4-player `GameState.initial()`, a full turn cycle,
  priority round-trips, `getOpponents` returns 3, an `EachOpponent` burn spell hits all three.

**Commander check:** `TargetOpponent` enumeration across 3 opponents is exactly what commander-damage-aware
cards and "each opponent" staples (Syphon Mind effects) need. Nothing commander-specific in this phase.

---

## Phase 1 — N-player engine correctness

### 1.1 Multi-defender combat

Attack declaration already supports per-creature defenders. The work is the defending side:

- **Declare blockers per defending player, APNAP order** (CR 101.4). Replace the single
  `defendingPlayer` priority assignment (`TurnManager.kt:321`) with an iteration: for each player in APNAP
  order who has at least one creature attacking them (or their planeswalkers), run a declare-blockers
  decision scoped to *the attackers attacking them*. Model as a combat continuation
  (`CombatContinuations.kt` already hosts the pause/resume scaffolding) so each defender's declaration can
  pause for the client independently.
- **Damage assignment**: `CombatDamageManager` already assigns per-target; verify ordering (active player
  assigns all attackers' damage, then each defending player's blockers in APNAP order, CR 510.1).
- **"Defending player" reads** in triggers/effects (`AttackRestrictionRules`, menace-style block
  requirements, `BlockPhaseManager`) must resolve per-creature via `AttackingComponent.defenderId`
  (CR 802.2a), not via the priority holder.
- **Client DTO**: `ClientCombatState.defendingPlayerId` (singular) → per-attacker defender map (the data
  already exists in `AttackingComponent`); keep a derived singular field during client migration if needed.

**Tests:** 3-player scenario — A attacks B with one creature and C with another; B and C each get their own
block decision; a "whenever a creature attacks you" trigger fires only for the right defender; damage lands
on the right players. Menace blocked by two creatures from the correct defender only.

### 1.2 Leaving the game (CR 800.4a–c)

The biggest new engine feature. When `PlayerLostComponent` is attached (loss, concede, disconnect-forfeit):

- New `PlayerLeavesGameProcessor` invoked immediately (CR 800.4a says not an SBA — run it at the point the
  loss is recorded, before normal processing resumes):
  1. All objects **owned** by the leaver leave the game (every zone, including stack cards, exile,
     command zone). Emit `GameEvent`s. Trigger semantics: remaining players' triggers fire normally off
     these zone changes; the **leaver's** triggered abilities are never put on the stack (CR 800.4d).
  2. Control-granting effects from/to the leaver end (floating effects scan).
  3. Their stack objects not represented by cards cease to exist.
  4. Objects they still control (but don't own) are exiled (CR 800.4c interactions).
  5. If the leaver had priority, priority passes to the next player in turn order (CR 800.4a).
  6. If it's the leaver's turn, the turn **continues to completion without an active player** — priority
     the active player would receive goes to the next player in turn order (CR 800.4j); a turn that the
     leaver *would begin* doesn't begin (CR 800.4k).
  7. Combat damage that would be assigned to the leaver isn't assigned (CR 800.4e); choices the leaver
     would make are delegated (controller picks another player / next in turn order, CR 800.4g–h);
     "until that player's next turn" durations last until that turn would have begun (CR 800.4m).
- `turnOrder` keeps the leaver (entity history stays intact) but every iteration helper
  (`getNextPlayer`, `getOpponents`, APNAP loops, `allPlayersPassed`) filters `PlayerLostComponent`.
  Audit all `turnOrder` consumers for this filter — central helpers, not call-site filters.
- `GameEndCheck` already ends the game at ≤1 active player (CR 104.2a) — unchanged.

**Tests:** 4-player scenarios — player B loses mid-combat (their attackers/blockers removed, combat
continues); B owned an aura on A's creature (leaves); B controlled A's creature via theft (reverts);
A controlled B's creature (exiled); B had a spell on the stack (gone); turn passes correctly when the
active player concedes.

### 1.3 Turn-structure details

- CR 800.7: starting player draws on turn one in multiplayer — gate the skip on `turnOrder.size == 2`.
- Mulligans: each player mulligans independently; verify the existing flow iterates all players (likely
  fine; the decision protocol is per-player).
- Verify extra-turn and skip-turn components interact correctly with a lost player (extra turn owned by a
  leaver is skipped).

### 1.4 Dev-loop playability gate (milestone: hotseat pod)

Before any server/client work: a full 4-player game must be playable through the **scenario builder +
hotseat** seam and the **gym self-play harness** (drive 4 seats over HTTP per
`docs/gym-self-play-testing.md`). Widen the scenario builder config to N seats if it's currently 2-seat
(`POST /api/scenarios` — verify). This is the cheap proof that Phase 0+1 actually work.

**Commander check:** commander damage is tracked per `(commander, defendingPlayer)` pair already; the
leave-game processor must also handle a leaver's *commander* (it's owned by them — leaves with them) and
their commander-damage rows (keep for history, irrelevant once they've left). 40-life config is per-player
`PlayerConfig.startingLife` — already fine.

---

## Phase 2 — Server: sessions, DTOs, spectators

### 2.1 GameSession generalization

- Delete the `player1`/`player2` getters (`GameSession.kt:165-166`); migrate every call site to iterate
  `players.values` or look up by ID. Routing patterns become broadcasts:
  `players.values.filter { it.playerId != actor }.forEach { send(...) }`.
- `getOpponentId()` → `getOpponentIds(playerId): List<EntityId>`.
- Auto-concede / reconnect (`ConnectionHandler`): disconnect of one player must not end a 4-player game —
  pause-or-forfeit policy per seat. Reuse the existing reconnect window; on forfeit, the engine's
  leave-game processing (Phase 1.2) keeps the game running for the other three.

### 2.2 DTO reshaping (breaking, do it in one PR)

- `GameStarted(opponentName)` → `GameStarted(players: List<PlayerSeatInfo>)` where `PlayerSeatInfo` =
  `(playerId, name, seatIndex, isYou, isAi)`. Seat index drives stable UI ordering and seat colors.
- `opponentDecisionStatus` (singular) → `decisionStatus: DecisionStatusInfo?` carrying the deciding
  player's ID — there's still only ever *one* pending decision, so the shape stays singular but gains
  `playerId` (today's field already works for any non-decider; only the name implies 2 players).
- `StopOverrideInfo.opponentTurnStops` — stops apply to "turns other than yours"; keep singular semantics
  (a stop set for opponents' turns collectively), defer per-opponent stop config.
- Masked state: `createStateUpdate(playerId)` already builds per-recipient state from the engine's masked
  view; verify hidden-zone masking is per-zone-owner (it is in the engine's view layer) and that
  4 recipients each get a correctly masked state. No "opponent" concept should remain in `StateUpdate`.

### 2.3 Spectators + replays

- `SpectatorStateBuilder.buildState(state, p1, p2, …)` → takes the session's player list; spectator state
  carries `players: List<PlayerSeatInfo>` instead of `player1Id`/`player2Id`.
- Replay format: verify the recorder is player-count agnostic (it stores engine events — likely fine) and
  that the replay browser/viewer tolerates N players. The `tournament-newspaper` admin tooling reads
  replays — flag, don't fix, if it assumes 2 players.

**Commander check:** `PlayerSeatInfo` gets an optional `commanderCardId` later (command-zone widget per
opponent column); the DTO shape leaves room. Commander-damage rows already ride `ClientPlayer.commanderDamage`.

---

## Phase 3 — Client UI/UX (the centerpiece)

### 3.1 Layout: one viewed board + opponent rail

The opponent half of today's screen is sized for **exactly one board** — that stays true. Multiplayer shows
one opponent at full 2-player scale; the other boards live off-screen in a horizontal strip and **slide into
view** when selected. The overview guarantee moves into an always-visible **opponent rail** of summary chips.

```
┌──────────────────────────────────────────────────┐
│ ▸[●A ❤34 🂠5] [●B ❤27 🂠2 ⚠] [●C ❤40 🂠7]        │ ← opponent rail: one chip per
├──────────────────────────────────────────────────┤   opponent, always visible
│   hand(fan) · piles 🂠 ⚰ · command zone           │ ┐
│ ┌──────────────────────────────────────────────┐ │ │ viewed opponent board —
│ │      VIEWED opponent battlefield             │ │ ├ today's opponent half,
│ │      (full size, today's card scale)         │ │ │ unchanged scale; slides
│ └──────────────────────────────────────────────┘ │ ┘ horizontally on switch
├──────────────────────────────────────────────────┤
│ HUD: step strip · priority · stack indicator     │
├──────────────────────────────────────────────────┤
│             YOUR battlefield (full width)        │
├──────────────────────────────────────────────────┤
│        your hand · stack(left) · buttons         │
└──────────────────────────────────────────────────┘
```

- **The viewed board is pixel-identical to today's opponent half** (hand fan, battlefield with its
  intentional row asymmetry, zone piles). No new battlefield scaling work — the slot-sizing machinery is
  untouched. With one opponent, the rail collapses and the layout *is* today's 2-player board.
- **Boards are ordered by turn order** in the strip (rotated to read left→right in play order after you);
  switching slides left/right along that order, so spatial direction matches "who's next".
- **Your half is sacred.** Your battlefield, hand, and action buttons keep today's size and position —
  all multiplayer adaptation happens in the opponent half and the rail.

### 3.2 Switching boards (slide + follow)

- **Select**: click an opponent's rail chip; the strip slides their board into view (~200ms horizontal
  slide, seat-colored edge flash on arrival). Keyboard `1`/`2`/`3` and edge arrows / horizontal swipe
  (touch) do the same.
- **Follow-the-action (default on)**: the view slides automatically on *coarse* boundaries — to the active
  opponent when their turn starts, and to the attacking player's board when you're being attacked. It never
  moves mid-step for resolutions (attention cues on the rail cover those — 3.5), and **never while you have
  a pending input or an in-progress targeting selection** (the stale-UI-suppression principle applies to
  camera movement).
- **Pin**: selecting a board manually pins it (chip shows a pin glyph) and suspends follow until unpinned
  (re-click the chip / Esc). A settings toggle turns follow off entirely for players who want a fully
  manual camera.
- Off-screen boards stay mounted (3 boards of render cost ≈ today's 2-player game ×1.5 — cheap) so slides
  are instant and animations on off-screen boards still run their state forward.

### 3.3 The opponent rail

The rail is the overview: every opponent's vital state, always readable, never occluded.

```
┌──────────────────────────────────────────┐
│ ▸ ● Anna   ❤ 34  🂠 5  ⚔3/21  ⌁ 2  ◔     │   ▸ viewed   ● seat color  ❤ life
│   ● Bram   ❤ 27  🂠 2  ⚠ ⚔18/21          │   🂠 hand  ⌁ poison  ◔ deciding
│   ● Cleo   ❤ 40  🂠 7                    │   ⚔ cmd dmg (worst pair, later)
└──────────────────────────────────────────┘
```

- **Seat color**: 4 fixed, colorblind-safe hues assigned by seat index. Used on: rail chips, viewed-board
  frame, log entry names, combat arrows, stack item borders, targeting glow. One legend, everywhere.
- **Per chip**: name, life (with floating ±deltas), hand count, poison, warnings (commander damage ≥18,
  low life), **active-turn ring**, **priority dot**, **deciding spinner** (the DTO now says whose decision
  is pending), and attention pulses (3.5). Chips are the player-level click target for targeting (3.4).
- **Turn order is the rail order**, with a small arrow marking the current turn and the next seat — "whose
  turn is next" is constant table talk in commander pods; answer it ambiently.
- Your own life/mana stays in today's position by your battlefield; you don't get a rail chip (you're
  always on screen). The current center-HUD opponent life orb is absorbed by the rail.

### 3.4 Targeting across seats

- Targets on the **viewed** board use the existing glow/click affordance unchanged.
- Rail chips of opponents with valid targets on their off-screen board get a **"contains targets" halo**;
  clicking the chip slides their board in *without canceling the in-progress selection* — switching boards
  is a view change, never an input mode change.
- **"Target player"**: when a player is a valid target, their chip grows an explicit crosshair badge —
  clicking the badge targets the player; clicking the rest of the chip still just switches the view.
  (Disambiguating view-switch vs target-player on one chip is the interaction to playtest hardest.)
- The targeting banner (TargetingOverlay) lists picks as seat-colored chips — "2/3 targets: ● Goblin (B),
  ● Anna" — so multi-target spells across players stay legible even when their boards aren't in view.

### 3.5 Attention cues (following what's off-screen)

With only one opponent board visible, the rail must answer "what just happened, and to whom":

- **Chip pulses**: when a spell resolves against an off-screen player or their permanents, their chip
  flashes its seat color; deaths/destruction use a sharper red pulse; life deltas float off the chip.
- **Stack items carry caster seat color** (border) and, when the topmost item targets an off-screen
  player's stuff, a thin arrow from stack card to that player's rail chip — "whose spell, at whom" at a
  glance without sliding.
- **Log**: seat-colored player names; clicking a log entry slides the involved board into view. The log
  keeps its position; on mobile it stays hidden as today.

### 3.6 Combat UX

**Declaring attacks (you):**
- Click creatures to select attackers (unchanged). With >1 possible defender, the first selection pops a
  one-time defender pick; after that, assignment is **sticky** (new attackers default to the last defender
  you assigned). Per-creature override: with an attacker selected, click a defender's rail chip (or their
  planeswalker — see flyout below) to (re)assign. Assignment shows as a seat-colored chevron on the
  attacker card.
- **Planeswalker flyout**: a rail chip expands on hover/long-press to show that player's planeswalkers, so
  you can assign attacks to off-screen planeswalkers without sliding (sliding also works — their board view
  is live for assignment clicks).
- An "all → ●B" quick action in the combat button cluster covers the common alpha-strike-one-player case.
- **Arrows**: attacks against the *viewed* defender render full per-creature arrows in that defender's seat
  color; attacks against off-screen defenders bundle into one arrow from the attacker group to the
  defender's rail chip with a creature-count badge.

**Being attacked (you defend):**
- The attacker is the active player, so their board is already in view (follow-the-action). Creatures
  attacking *you* get the existing attacker highlight; creatures attacking other defenders render dimmed.
  Your declare-blockers UI is unchanged — it just may arrive alongside other defenders' (separate,
  APNAP-ordered) block decisions; the server's decision protocol already serializes this.

**Watching combat (not involved):**
- Arrows + chip pulses only. The step strip + rail spinner show "B is declaring blockers".

### 3.7 Mobile / portrait

The model converges: mobile portrait is the same viewed-board + rail design with a denser rail (the
summary chips compress to `●A ❤34 🂠5` pills) and swipe as the primary switch gesture. No separate mobile
layout to design or maintain — the desktop slide/carousel *is* the mobile pattern, which is exactly why the
single-viewed-board approach beats equal-split on this codebase (one layout, two densities).

### 3.8 Dead seats, spectator, replay

- When a player leaves, their board drops out of the strip and their chip becomes a **tombstone** (grayed,
  final life struck through, skull icon, elimination order number). Engine has already removed their cards
  (Phase 1.2), so there's no board to keep.
- Spectator mode: same layout anchored to a chosen seat's perspective (default: seat 0 at the bottom); a
  seat-switcher cycles which player's view occupies the bottom half. Replays reuse this verbatim
  (replay = spectating the recorded stream, as today).
- Hotseat keeps a fixed bottom seat and routes inputs as today (the actorFor seam already stamps seats);
  rotating the whole board per decision would be disorienting. Revisit only if hotseat pods become a real
  use case beyond dev testing.

### 3.9 Component-level changes (summary)

- `selectors.ts`: `useOpponent()` → `useOpponents(): ClientPlayer[]` (turn-order rotated) +
  `useViewedOpponent()` (the slide state). Migrate ~every consumer. Add `useSeatColor(playerId)`.
- New store slice: `boardView` — viewed opponent ID, pinned flag, follow setting; written by chip clicks,
  swipe, keyboard, and the follow-the-action rules (which check pending-input state before moving).
- `GameBoard.tsx` / `board/styles.ts`: opponent half becomes a horizontally sliding strip of
  `OpponentBoard` subtrees (today's opponent-half markup, parameterized by player ID); `Battlefield.tsx`
  and slot sizing are **unchanged**.
- New: `OpponentRail` (+ chip, crosshair badge, planeswalker flyout, tombstone state). Combat arrow
  bundling + arrow-to-chip rendering in `CombatArrows.tsx`.
- `ClientCombatState` consumers: per-attacker defender map.

**Commander check:** the viewed opponent board keeps today's full-size command zone slot (commander art +
tax badge — no shrinking needed, unlike equal-split); rail chips carry the commander-damage warning
(`⚔ 18/21` from the worst commander pair, expandable to the full per-commander row on hover); 40 starting
life fits the chip (`❤ 40`). The 1v1 commander UI from `commander-format.md` §1.8 lands inside the
`OpponentBoard` subtree unchanged.

---

## Phase 4 — Lobby: the Free-for-All game mode (first online milestone)

Reuse the N-player **tournament lobby** infrastructure (2–8 players, deck submission, draft/sealed pool
generation, AI identities) and replace the bracket with a single game:

- New lobby **game mode: `FREE_FOR_ALL`** alongside the tournament bracket mode — *not* a new
  `TournamentFormat`. The lobby's existing format axis (sealed / draft variants / custom decks per the
  quick-game redesign) stays orthogonal: any pool-building format + FFA mode = "draft, then one 4-player
  game".
  - Sealed: works as-is (per-player pools).
  - Booster draft / grid draft: grid draft already scales 2–4 (`LobbyHandler.kt:550`); booster draft pick
    flow is seat-count aware via pool growth (per booster-era memory) — verify with 3.
  - Custom decks: the deck-picker submission path from the quick-game redesign; pods cap at 4
    (`maxPlayers.coerceIn(2, 4)` for FFA mode).
  - Winston draft stays 2-player-only (already enforced, `LobbyHandler.kt:1107`).
- On start: instead of `TournamentMatchHandler.startSingleMatch` pairing, create **one `GameSession` with
  all lobby players** (the session layer is N-ready after Phase 2). `TournamentMatch.player1Id/player2Id`
  is untouched — FFA mode doesn't create matches.
- Game over: standings = elimination order (last alive first); report back to the lobby for a "play again"
  loop (same pod, new game) — the one tournament-ish nicety worth keeping. Replay saved as usual.
- Quick Game stays 2-player (its whole point is instant 1v1 vs AI); `QuickGameLobby.MAX_PLAYERS` untouched.

**Definition of done (ship gate for the whole project):**
- [ ] 4 humans join a lobby, submit custom decks (or draft/sealed), and play one FFA game to completion
- [ ] A player conceding mid-game removes their cards and the game continues 3-way (CR 800.4a)
- [ ] Combat vs two different players in one turn works end-to-end with both defenders blocking
- [ ] The 2-player experience (quick game, tournaments, e2e suite) is visually and behaviorally unchanged
- [ ] Spectating and replaying a 4-player game works
- [ ] One Playwright e2e: 3-player scenario game with multi-defender combat and a mid-game concede

**Commander check:** when Commander Phase 1 (1v1) lands, FFA mode + Commander format = 4-player Commander
pods with zero additional lobby work (format is a deck-validation + game-config axis, mode is a pairing
axis). This is the payoff of keeping them orthogonal.

---

## Later — explicitly deferred

- **AI pod players** (its own project): multi-opponent board evaluation (per-opponent threat vector instead
  of a single differential), attack-target selection (kill priority, retaliation risk), search beyond
  minimax (max^n or shallow rollouts), LLM formatter multi-opponent prompts, gym terminal rewards for
  placement. Until then: no AI seats in FFA lobbies; hotseat covers solo dev testing.
- **Politics mechanics**: monarch, initiative, voting (council's dilemma), tempting offer. Each is an
  `add-feature` once multiplayer exists; none block the core.
- **Per-opponent stop configuration** (turn stops per seat).
- **Range of influence, attack-left/right, Grand Melee, team variants**: permanently out of scope.
- **Pods > 4**: revisit only with demand; the layout math (3.1) does not stretch past 3 opponent columns.

## Risks & unknowns

- **The `Player.Opponent` card audit (0.1)** touches an unknown number of cards; some may be genuinely
  ambiguous (printed text says "an opponent" in a non-targeting way). Budget for Scryfall/oracle checks per
  card; snapshot diffs keep it reviewable.
- **CR 800.4 corner cases** are deep (control-effect webs, last-known-information reads per CR 800.4i,
  delegated choices mid-resolution). The sub-rules cited in 1.2 are verified, but each maps to real engine
  plumbing (continuations that expect a decider who no longer exists, durations keyed to a turn that never
  comes) — budget 1.2 as the largest single engine work item.
- **Decision protocol under concurrent combat decisions**: APNAP-serialized block declarations mean players
  wait on each other; the UI must make "waiting for B to block" obvious (decision status on nameplate) or
  pods will feel stalled.
- **Performance**: state masking + broadcast ×4 per action server-side (linear, already per-recipient),
  and 3 mounted opponent boards client-side (only one visible; keep off-screen boards mounted for instant
  slides unless profiling says otherwise). Watch the projection cost in `StateProjector` once
  `getOpponents` fans out.
- **Replay/admin tooling** (`tournament-newspaper`) reads replays assuming 1v1 reconstruction — flag for a
  later pass; do not let it block the replay format.
- **Disconnect policy** in pods (2.1): a 30-second reconnect window then forfeit is simple but feels bad in
  a 90-minute commander game; consider a pause-vote later. Ship the simple policy first.
